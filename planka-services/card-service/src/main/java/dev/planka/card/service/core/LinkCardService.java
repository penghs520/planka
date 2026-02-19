package dev.planka.card.service.core;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.repository.CardRepository;
import dev.planka.card.service.structure.StructureLinkSyncService;
import dev.planka.card.service.structure.StructureSyncResult;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardStyle;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.LinkedCard;
import dev.planka.domain.field.StructureFieldValue;
import dev.planka.domain.link.LinkFieldIdUtils;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.condition.TitleConditionItem;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;
import dev.planka.event.card.CardLinkUpdatedEvent;
import dev.planka.event.publisher.EventPublisher;
import dev.planka.infra.cache.schema.SchemaCacheService;
import dev.planka.api.card.request.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import planka.graph.driver.PlankaGraphWriteClient;
import planka.graph.driver.proto.model.Link;
import planka.graph.driver.proto.write.BatchCreateLinkRequest;
import planka.graph.driver.proto.write.BatchDeleteLinkRequest;
import planka.graph.driver.proto.write.BatchLinkCommonResponse;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 关联卡片服务
 * <p>
 * 提供关联卡片的查询和更新功能
 */
@Service
public class LinkCardService {

    private static final Logger logger = LoggerFactory.getLogger(LinkCardService.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final SchemaCacheService schemaCacheService;
    private final CardRepository cardRepository;
    private final PlankaGraphWriteClient writeClient;
    private final EventPublisher eventPublisher;
    private final StructureLinkSyncService structureLinkSyncService;

    public LinkCardService(SchemaCacheService schemaCacheService,
                           CardRepository cardRepository,
                           PlankaGraphWriteClient writeClient,
                           EventPublisher eventPublisher,
                           StructureLinkSyncService structureLinkSyncService) {
        this.schemaCacheService = schemaCacheService;
        this.cardRepository = cardRepository;
        this.writeClient = writeClient;
        this.eventPublisher = eventPublisher;
        this.structureLinkSyncService = structureLinkSyncService;
    }

    /**
     * 查询可关联的卡片列表
     *
     * @param request    查询请求
     * @param orgId      组织ID
     * @param operatorId 操作人ID
     * @return 可关联的卡片分页列表
     */
    public Result<PageResult<LinkedCard>> queryLinkableCards(
            LinkableCardsRequest request, String orgId, String operatorId) {
        try {
            // 1. 从 linkFieldId 解析 linkTypeId 和 position
            String linkFieldId = request.getLinkFieldId();
            String linkTypeIdValue = LinkFieldIdUtils.getLinkTypeId(linkFieldId);
            LinkPosition linkPosition = LinkFieldIdUtils.getPosition(linkFieldId);

            // 2. 获取关联类型定义
            Optional<LinkTypeDefinition> linkTypeOpt = getLinkTypeDefinition(linkTypeIdValue);
            if (linkTypeOpt.isEmpty()) {
                return Result.failure("LINK_TYPE_NOT_FOUND", "关联类型不存在: " + linkTypeIdValue);
            }
            LinkTypeDefinition linkType = linkTypeOpt.get();

            // 3. 根据 LinkPosition 确定对端卡片类型
            List<CardTypeId> targetCardTypeIds = getTargetCardTypeIds(linkType, linkPosition);
            if (targetCardTypeIds.isEmpty()) {
                logger.warn("关联类型 {} 的对端卡片类型为空", linkTypeIdValue);
                return Result.success(PageResult.empty());
            }

            // 3.5. 展开属性集为实体类型
            targetCardTypeIds = expandAbstractCardTypes(targetCardTypeIds, orgId);
            if (targetCardTypeIds.isEmpty()) {
                logger.warn("展开属性集后，对端卡片类型为空");
                return Result.success(PageResult.empty());
            }

            // 4. 构建分页查询请求
            CardPageQueryRequest queryRequest = buildCardPageQueryRequest(
                    targetCardTypeIds, request.getKeyword(), orgId, operatorId, request);

            // 5. 执行查询
            PageResult<CardDTO> cardPage = cardRepository.pageQuery(queryRequest);

            // 6. 转换为 LinkedCard
            List<LinkedCard> linkedCards = cardPage.getContent().stream()
                    .map(this::toLinkedCard)
                    .toList();

            PageResult<LinkedCard> result = PageResult.of(
                    linkedCards,
                    cardPage.getPage(),
                    cardPage.getSize(),
                    cardPage.getTotal());

            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询可关联卡片失败", e);
            return Result.failure("QUERY_ERROR", "查询可关联卡片失败: " + e.getMessage());
        }
    }

    /**
     * 更新关联关系
     * <p>
     * 使用全量替换策略：删除旧的关联，创建新的关联
     *
     * @param request    更新请求
     * @param orgId      组织ID
     * @param operatorId 操作人ID
     * @param sourceIp   来源IP
     * @return 操作结果
     */
    public Result<Void> updateLink(UpdateLinkRequest request, String orgId, String operatorId, String sourceIp) {
        return updateLink(request, orgId, operatorId, sourceIp, false);
    }

    /**
     * 更新关联关系（带 skipSync 参数）
     * <p>
     * 使用全量替换策略：删除旧的关联，创建新的关联
     *
     * @param request    更新请求
     * @param orgId      组织ID
     * @param operatorId 操作人ID
     * @param sourceIp   来源IP
     * @param skipSync   是否跳过架构联动同步（入口2调用时传 true，避免循环）
     * @return 操作结果
     */
    public Result<Void> updateLink(UpdateLinkRequest request, String orgId, String operatorId,
                                   String sourceIp, boolean skipSync) {
        try {
            String cardId = request.getCardId();
            String linkFieldId = request.getLinkFieldId();
            List<String> targetCardIds = request.getTargetCardIds();

            // 1. 从 linkFieldId 解析 linkTypeId 和 position
            String linkTypeIdValue = LinkFieldIdUtils.getLinkTypeId(linkFieldId);
            LinkPosition linkPosition = LinkFieldIdUtils.getPosition(linkFieldId);

            // 2. 获取关联类型定义以验证
            Optional<LinkTypeDefinition> linkTypeOpt = getLinkTypeDefinition(linkTypeIdValue);
            if (linkTypeOpt.isEmpty()) {
                return Result.failure("LINK_TYPE_NOT_FOUND", "关联类型不存在: " + linkTypeIdValue);
            }

            // 3. 获取当前卡片信息（用于事件）
            CardDTO currentCard = getCardBasicInfo(cardId, operatorId);
            if (currentCard == null) {
                return Result.failure("CARD_NOT_FOUND", "卡片不存在: " + cardId);
            }

            // 4. 获取当前卡片的现有关联（包含卡片详情，用于事件）
            Map<String, CardDTO> existingLinkedCards = getExistingLinkedCardsWithDetail(cardId, linkFieldId, operatorId);

            // 4.1 在关联更新之前，预先计算所有受影响架构属性的旧值（用于变更历史记录）
            Map<String, StructureFieldValue> oldStructureValues = Map.of();
            if (!skipSync) {
                oldStructureValues = structureLinkSyncService.getStructureValuesBeforeUpdate(
                        cardId, currentCard.getTypeId().value(), linkFieldId, operatorId);
            }

            // 5. 计算需要删除和新增的关联
            Set<String> targetSet = new HashSet<>(targetCardIds);
            Set<String> toDeleteIds = existingLinkedCards.keySet().stream()
                    .filter(id -> !targetSet.contains(id))
                    .collect(Collectors.toSet());
            Set<String> toCreateIds = targetSet.stream()
                    .filter(id -> !existingLinkedCards.containsKey(id))
                    .collect(Collectors.toSet());

            // 6. 执行删除操作
            if (!toDeleteIds.isEmpty()) {
                deleteLinks(cardId, linkTypeIdValue, linkPosition, toDeleteIds);
            }

            // 7. 执行新增操作
            Map<String, CardDTO> createdCards = new HashMap<>();
            if (!toCreateIds.isEmpty()) {
                createLinks(cardId, linkTypeIdValue, linkPosition, toCreateIds);
                // 获取新增卡片的详情（用于事件）
                createdCards = getCardsBasicInfo(toCreateIds, operatorId);
            }

            // 8. 发布事件（双向发布）
            if (!toDeleteIds.isEmpty() || !toCreateIds.isEmpty()) {
                String linkFieldName = getLinkFieldName(linkFieldId);

                // 8.1 发布主动方事件（当前卡片）
                publishLinkUpdatedEvent(
                        orgId, operatorId, sourceIp,
                        cardId, currentCard.getTypeId().value(),
                        linkFieldId, linkFieldName,
                        existingLinkedCards, toDeleteIds,
                        createdCards, true
                );

                // 8.2 发布被动方事件（对端卡片）
                // 计算对端的 linkFieldId（位置相反）
                String oppositeLinkFieldId = getOppositeLinkFieldId(linkFieldId);
                String oppositeLinkFieldName = getLinkFieldName(oppositeLinkFieldId);

                // 为每个被关联/取消关联的卡片发布事件
                publishOppositeCardEvents(
                        orgId, operatorId, sourceIp,
                        cardId, currentCard,
                        oppositeLinkFieldId, oppositeLinkFieldName,
                        existingLinkedCards, toDeleteIds,
                        createdCards
                );
            }

            // 9. 架构关联联动同步（入口1）
            if (!skipSync) {
                StructureSyncResult syncResult = structureLinkSyncService.syncStructureLinks(
                        cardId, currentCard.getTypeId().value(), linkFieldId, targetCardIds,
                        oldStructureValues, orgId, operatorId, sourceIp);
                if (syncResult.synced()) {
                    logger.debug("架构联动同步完成，addedLinks: {}, removedLinks: {}",
                            syncResult.addedLinks().size(), syncResult.removedLinks().size());
                }
            }

            logger.info("更新关联关系成功，cardId: {}, linkFieldId: {}, 删除: {}, 新增: {}",
                    cardId, linkFieldId, toDeleteIds.size(), toCreateIds.size());

            return Result.success(null);
        } catch (Exception e) {
            logger.error("更新关联关系失败", e);
            return Result.failure("UPDATE_LINK_ERROR", "更新关联关系失败: " + e.getMessage());
        }
    }


    // ==================== 私有方法 ====================

    private Optional<LinkTypeDefinition> getLinkTypeDefinition(String linkTypeId) {
        return schemaCacheService.getById(linkTypeId)
                .filter(schema -> schema instanceof LinkTypeDefinition)
                .map(schema -> (LinkTypeDefinition) schema);
    }

    /**
     * 根据 LinkPosition 确定对端卡片类型
     * <p>
     * 如果当前位置是 SOURCE，说明当前卡片是源端，需要查询目标端卡片类型
     * 如果当前位置是 TARGET，说明当前卡片是目标端，需要查询源端卡片类型
     */
    private List<CardTypeId> getTargetCardTypeIds(LinkTypeDefinition linkType, LinkPosition currentPosition) {
        if (currentPosition == LinkPosition.SOURCE) {
            // 当前卡片是源端，对端是目标端
            return linkType.getTargetCardTypeIds() != null ? linkType.getTargetCardTypeIds() : Collections.emptyList();
        } else {
            // 当前卡片是目标端，对端是源端
            return linkType.getSourceCardTypeIds() != null ? linkType.getSourceCardTypeIds() : Collections.emptyList();
        }
    }

    /**
     * 展开属性集为实体类型
     * <p>
     * 如果 cardTypeIds 中包含属性集（如 member-trait），
     * 则将其替换为对应的实体类型（如 member）
     */
    private List<CardTypeId> expandAbstractCardTypes(List<CardTypeId> cardTypeIds, String orgId) {
        logger.info("开始展开属性集，输入: {}", cardTypeIds.stream().map(CardTypeId::value).toList());

        List<CardTypeId> expandedIds = new ArrayList<>();

        for (CardTypeId cardTypeId : cardTypeIds) {
            String typeIdValue = cardTypeId.value();

            // 检查是否为 member-trait
            if (typeIdValue.endsWith(":member-trait")) {
                // 替换为具体的 member 类型
                String memberTypeId = orgId + ":member";
                expandedIds.add(CardTypeId.of(memberTypeId));
                logger.info("展开属性集: {} -> {}", typeIdValue, memberTypeId);
            } else {
                // 保留原类型
                expandedIds.add(cardTypeId);
            }
        }

        logger.info("展开属性集完成，输出: {}", expandedIds.stream().map(CardTypeId::value).toList());
        return expandedIds;
    }

    private CardPageQueryRequest buildCardPageQueryRequest(
            List<CardTypeId> cardTypeIds,
            String keyword,
            String orgId,
            String operatorId,
            LinkableCardsRequest request) {

        CardPageQueryRequest queryRequest = new CardPageQueryRequest();

        // 查询上下文
        QueryContext queryContext = new QueryContext();
        queryContext.setOrgId(orgId);
        queryContext.setOperatorId(operatorId);
        queryRequest.setQueryContext(queryContext);

        // 查询范围：只查询指定卡片类型且为活跃状态的卡片
        QueryScope queryScope = new QueryScope();
        List<String> cardTypeIdStrings = cardTypeIds.stream().map(CardTypeId::value).toList();
        queryScope.setCardTypeIds(cardTypeIdStrings);
        queryScope.setCardStyles(List.of(CardStyle.ACTIVE));
        queryRequest.setQueryScope(queryScope);

        logger.info("查询可关联卡片 - linkFieldId: {}, cardTypeIds (展开后): {}, keyword: {}",
                request.getLinkFieldId(), cardTypeIdStrings, keyword);

        // 查询条件：关键字搜索（使用 TitleConditionItem）
        if (keyword != null && !keyword.isBlank()) {
            TitleConditionItem titleCondition = new TitleConditionItem(
                    new TitleConditionItem.TitleSubject(null),
                    new TitleConditionItem.TitleOperator.Contains(keyword)
            );
            queryRequest.setCondition(Condition.of(titleCondition));
        }

        // 返回定义：只需要基本字段（不需要自定义字段）
        Yield yield = new Yield();
        YieldField yieldField = new YieldField();
        yieldField.setAllFields(false);
        yield.setField(yieldField);
        queryRequest.setYield(yield);

        // 分页
        SortAndPage sortAndPage = new SortAndPage();
        Page page = new Page();
        page.setPageNum(request.getPage() != null ? request.getPage() : 0);
        page.setPageSize(request.getSize() != null ? request.getSize() : 20);
        sortAndPage.setPage(page);
        queryRequest.setSortAndPage(sortAndPage);

        return queryRequest;
    }

    private LinkedCard toLinkedCard(CardDTO card) {
        return new LinkedCard(card.getId().asStr(), card.getTitle());
    }

    private void deleteLinks(String cardId, String linkTypeIdValue, LinkPosition linkPosition, Set<String> targetCardIds) {
        List<Link> linksToDelete = buildLinks(cardId, linkTypeIdValue, linkPosition, targetCardIds);
        BatchDeleteLinkRequest request = BatchDeleteLinkRequest.newBuilder()
                .addAllLinks(linksToDelete)
                .build();

        try {
            CompletableFuture<BatchLinkCommonResponse> future = writeClient.batchDeleteLink(request);
            BatchLinkCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.debug("删除关联关系完成，成功: {}, 失败: {}", response.getSuccess(), response.getFailedLinksCount());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("删除关联关系失败: " + e.getMessage(), e);
        }
    }

    private void createLinks(String cardId, String linkTypeIdValue, LinkPosition linkPosition, Set<String> targetCardIds) {
        List<Link> linksToCreate = buildLinks(cardId, linkTypeIdValue, linkPosition, targetCardIds);
        BatchCreateLinkRequest request = BatchCreateLinkRequest.newBuilder()
                .addAllLinks(linksToCreate)
                .build();

        try {
            CompletableFuture<BatchLinkCommonResponse> future = writeClient.batchCreateLink(request);
            BatchLinkCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.debug("创建关联关系完成，成功: {}, 失败: {}", response.getSuccess(), response.getFailedLinksCount());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("创建关联关系失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 Link 列表
     * <p>
     * 根据 LinkPosition 确定 srcId 和 destId：
     * - SOURCE：当前卡片是源端，targetCardId 是目标端
     * - TARGET：当前卡片是目标端，targetCardId 是源端
     */
    private List<Link> buildLinks(String cardId, String linkTypeIdValue, LinkPosition linkPosition, Set<String> targetCardIds) {
        return targetCardIds.stream()
                .map(targetCardId -> {
                    long srcId, destId;
                    if (linkPosition == LinkPosition.SOURCE) {
                        srcId = Long.parseLong(cardId);
                        destId = Long.parseLong(targetCardId);
                    } else {
                        srcId = Long.parseLong(targetCardId);
                        destId = Long.parseLong(cardId);
                    }
                    return Link.newBuilder()
                            .setLtId(linkTypeIdValue)
                            .setSrcId(srcId)
                            .setDestId(destId)
                            .build();
                })
                .toList();
    }

    /**
     * 获取卡片基本信息
     */
    private CardDTO getCardBasicInfo(String cardId, String operatorId) {
        CardId id = CardId.of(cardId);
        Yield yield = Yield.basic();
        return cardRepository.findById(id, yield, operatorId).orElse(null);
    }

    /**
     * 获取现有关联卡片（包含详情）
     */
    private Map<String, CardDTO> getExistingLinkedCardsWithDetail(String cardId, String linkFieldId, String operatorId) {
        CardId id = CardId.of(cardId);
        Yield yield = new Yield();
        YieldLink yieldLink = new YieldLink();
        yieldLink.setLinkFieldId(linkFieldId);
        yieldLink.setTargetYield(Yield.basic());
        yield.setLinks(List.of(yieldLink));

        Optional<CardDTO> cardOpt = cardRepository.findById(id, yield, operatorId);
        if (cardOpt.isEmpty() || cardOpt.get().getLinkedCards() == null) {
            return Collections.emptyMap();
        }

        Set<CardDTO> linkedCards = cardOpt.get().getLinkedCards().get(linkFieldId);
        if (linkedCards == null) {
            return Collections.emptyMap();
        }

        return linkedCards.stream()
                .collect(Collectors.toMap(c -> c.getId().asStr(), c -> c));
    }

    /**
     * 批量获取卡片基本信息
     */
    private Map<String, CardDTO> getCardsBasicInfo(Set<String> cardIds, String operatorId) {
        Map<String, CardDTO> result = new HashMap<>();
        for (String cardIdStr : cardIds) {
            CardDTO card = getCardBasicInfo(cardIdStr, operatorId);
            if (card != null) {
                result.put(cardIdStr, card);
            }
        }
        return result;
    }

    /**
     * 获取关联属性名称
     */
    private String getLinkFieldName(String linkFieldId) {
        try {
            Optional<LinkFieldConfig> configOpt = schemaCacheService.getById(linkFieldId)
                    .filter(schema -> schema instanceof LinkFieldConfig)
                    .map(schema -> (LinkFieldConfig) schema);
            return configOpt.map(LinkFieldConfig::getName).orElse(linkFieldId);
        } catch (Exception e) {
            logger.warn("获取关联属性名称失败: {}", linkFieldId, e);
            return linkFieldId;
        }
    }

    /**
     * 获取对端的 linkFieldId（位置相反）
     */
    private String getOppositeLinkFieldId(String linkFieldId) {
        String linkTypeId = LinkFieldIdUtils.getLinkTypeId(linkFieldId);
        LinkPosition position = LinkFieldIdUtils.getPosition(linkFieldId);
        LinkPosition oppositePosition = position == LinkPosition.SOURCE
                ? LinkPosition.TARGET : LinkPosition.SOURCE;
        return LinkFieldIdUtils.build(linkTypeId, oppositePosition);
    }

    /**
     * 发布关联更新事件
     *
     * @param initiator 是否为主动关联方
     */
    private void publishLinkUpdatedEvent(
            String orgId, String operatorId, String sourceIp,
            String cardId, String cardTypeId,
            String linkFieldId, String linkFieldName,
            Map<String, CardDTO> existingCards, Set<String> deletedIds,
            Map<String, CardDTO> createdCards, boolean initiator) {

        // 构建删除的卡片引用列表
        List<CardLinkUpdatedEvent.LinkedCardRef> removedRefs = deletedIds.stream()
                .map(id -> {
                    CardDTO card = existingCards.get(id);
                    String title = card != null && card.getTitle() != null
                            ? card.getTitle().getDisplayValue() : "";
                    String typeId = card != null ? card.getTypeId().value() : "";
                    return new CardLinkUpdatedEvent.LinkedCardRef(id, title, typeId);
                })
                .toList();

        // 构建新增的卡片引用列表
        List<CardLinkUpdatedEvent.LinkedCardRef> addedRefs = createdCards.entrySet().stream()
                .map(entry -> {
                    CardDTO card = entry.getValue();
                    String title = card.getTitle() != null ? card.getTitle().getDisplayValue() : "";
                    return new CardLinkUpdatedEvent.LinkedCardRef(
                            entry.getKey(), title, card.getTypeId().value());
                })
                .toList();

        // 创建并发布事件
        CardLinkUpdatedEvent event = new CardLinkUpdatedEvent(
                orgId, operatorId, sourceIp, null, cardId, cardTypeId)
                .withLinkField(linkFieldId, linkFieldName, initiator)
                .withRemovedCards(removedRefs)
                .withAddedCards(addedRefs);

        if (event.hasChanges()) {
            eventPublisher.publish(event);
            logger.debug("发布关联更新事件: cardId={}, linkFieldId={}, initiator={}, added={}, removed={}",
                    cardId, linkFieldId, initiator, addedRefs.size(), removedRefs.size());
        }
    }

    /**
     * 发布对端卡片的关联更新事件
     * <p>
     * 对于每个被新增或删除关联的对端卡片，都需要发布一个被动关联事件
     */
    private void publishOppositeCardEvents(
            String orgId, String operatorId, String sourceIp,
            String initiatorCardId, CardDTO initiatorCard,
            String oppositeLinkFieldId, String oppositeLinkFieldName,
            Map<String, CardDTO> existingCards, Set<String> deletedIds,
            Map<String, CardDTO> createdCards) {

        // 构建主动方卡片的引用
        String initiatorTitle = initiatorCard.getTitle() != null
                ? initiatorCard.getTitle().getDisplayValue() : "";
        CardLinkUpdatedEvent.LinkedCardRef initiatorRef = new CardLinkUpdatedEvent.LinkedCardRef(
                initiatorCardId, initiatorTitle, initiatorCard.getTypeId().value());

        // 为每个被删除关联的对端卡片发布事件（removed 事件）
        for (String deletedCardId : deletedIds) {
            CardDTO deletedCard = existingCards.get(deletedCardId);
            if (deletedCard == null) continue;

            CardLinkUpdatedEvent event = new CardLinkUpdatedEvent(
                    orgId, operatorId, sourceIp, null,
                    deletedCardId, deletedCard.getTypeId().value())
                    .withLinkField(oppositeLinkFieldId, oppositeLinkFieldName, false)
                    .withRemovedCards(List.of(initiatorRef))
                    .withAddedCards(List.of());

            eventPublisher.publish(event);
            logger.debug("发布对端关联移除事件: cardId={}, linkFieldId={}, removed={}",
                    deletedCardId, oppositeLinkFieldId, initiatorCardId);
        }

        // 为每个被新增关联的对端卡片发布事件（added 事件）
        for (Map.Entry<String, CardDTO> entry : createdCards.entrySet()) {
            String addedCardId = entry.getKey();
            CardDTO addedCard = entry.getValue();

            CardLinkUpdatedEvent event = new CardLinkUpdatedEvent(
                    orgId, operatorId, sourceIp, null,
                    addedCardId, addedCard.getTypeId().value())
                    .withLinkField(oppositeLinkFieldId, oppositeLinkFieldName, false)
                    .withAddedCards(List.of(initiatorRef))
                    .withRemovedCards(List.of());

            eventPublisher.publish(event);
            logger.debug("发布对端关联新增事件: cardId={}, linkFieldId={}, added={}",
                    addedCardId, oppositeLinkFieldId, initiatorCardId);
        }
    }
}
