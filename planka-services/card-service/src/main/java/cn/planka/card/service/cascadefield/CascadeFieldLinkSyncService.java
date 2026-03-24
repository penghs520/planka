package cn.planka.card.service.cascadefield;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.UpdateLinkRequest;
import cn.planka.api.card.request.Yield;
import cn.planka.api.card.request.YieldLink;
import cn.planka.card.converter.CascadeFieldValueBuilder;
import cn.planka.card.repository.CardRepository;
import cn.planka.domain.card.CardId;
import cn.planka.domain.field.CascadeFieldValue;
import cn.planka.domain.field.CascadeItem;
import cn.planka.domain.link.LinkFieldId;
import cn.planka.domain.link.LinkFieldIdUtils;
import cn.planka.domain.link.LinkPosition;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.fieldconfig.CascadeFieldConfig;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationDefinition;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationLevel;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationLevelBinding;
import cn.planka.event.card.CardLinkUpdatedEvent;
import cn.planka.event.card.CardUpdatedEvent;
import cn.planka.event.publisher.EventPublisher;
import cn.planka.infra.cache.schema.SchemaCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import zgraph.driver.ZgraphWriteClient;
import zgraph.driver.proto.model.Link;
import zgraph.driver.proto.write.BatchCreateLinkRequest;
import zgraph.driver.proto.write.BatchDeleteLinkRequest;
import zgraph.driver.proto.write.BatchLinkCommonResponse;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 级联关系同步服务
 * <p>
 * 负责处理级联属性和关联属性之间的联动更新：
 * <ul>
 *   <li>入口1：关联属性更新时，自动同步其他层级的关联</li>
 *   <li>入口2：级联属性更新时，将其转换为多个关联属性的更新</li>
 * </ul>
 */
@Service
public class CascadeFieldLinkSyncService {

    private static final Logger logger = LoggerFactory.getLogger(CascadeFieldLinkSyncService.class);
    private static final String CARD_TYPE_INDEX = "CARD_TYPE";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final SchemaCacheService schemaCacheService;
    private final CardRepository cardRepository;
    private final ZgraphWriteClient writeClient;
    private final EventPublisher eventPublisher;

    public CascadeFieldLinkSyncService(SchemaCacheService schemaCacheService,
                                    CardRepository cardRepository,
                                    ZgraphWriteClient writeClient,
                                    EventPublisher eventPublisher) {
        this.schemaCacheService = schemaCacheService;
        this.cardRepository = cardRepository;
        this.writeClient = writeClient;
        this.eventPublisher = eventPublisher;
    }

    // ==================== 入口1：关联属性更新时的联动同步 ====================

    /**
     * 关联属性更新后执行级联层级联动同步
     * <p>
     * 当用户通过关联属性编辑器修改关联时，自动同步其他层级的关联：
     * <ul>
     *   <li>关联下级时：自动同步上级（向上遍历）</li>
     *   <li>关联上级时：清除下级（向下清空）</li>
     * </ul>
     * <p>
     * 一个关联属性可能被多个级联属性绑定，此方法会处理所有受影响的级联属性。
     *
     * @param cardId              当前卡片ID
     * @param cardTypeId          实体类型ID
     * @param linkFieldId         被更新的关联属性ID
     * @param targetCardIds       新的关联目标卡片ID列表
     * @param oldCascadeFieldValues  关联更新前的级联属性值映射（由调用方预先计算）
     * @param orgId               组织ID
     * @param operatorId          操作人ID
     * @param sourceIp            来源IP
     * @return 同步结果，包含额外的关联变更
     */
    public CascadeFieldSyncResult syncCascadeFieldLinks(String cardId, String cardTypeId,
                                                   String linkFieldId, List<String> targetCardIds,
                                                   Map<String, CascadeFieldValue> oldCascadeFieldValues,
                                                   String orgId, String operatorId, String sourceIp) {
        // 1. 查找所有匹配的级联绑定
        List<CascadeFieldBindingMatch> matches = findAllStructureBindings(cardTypeId, linkFieldId);
        if (matches.isEmpty()) {
            return CascadeFieldSyncResult.noSync();
        }

        Map<String, Set<String>> allAddedLinks = new HashMap<>();
        Map<String, Set<String>> allRemovedLinks = new HashMap<>();

        // 2. 遍历每个级联绑定，执行同步
        for (CascadeFieldBindingMatch match : matches) {
            int currentLevel = match.levelIndex();
            CascadeFieldConfig cascadeFieldDef = match.cascadeFieldDef();
            String cascadeFieldId = cascadeFieldDef.getId().value();

            logger.debug("检测到级联属性联动：cardId={}, linkFieldId={}, cascadeFieldId={}, levelIndex={}",
                    cardId, linkFieldId, cascadeFieldId, currentLevel);

            // 3. 获取级联关系定义
            CascadeRelationDefinition cascadeRelationDef = getCascadeRelationDefinition(cascadeFieldDef.getCascadeRelationId().value());
            if (cascadeRelationDef == null) {
                logger.warn("级联关系定义不存在: {}", cascadeFieldDef.getCascadeRelationId());
                continue;
            }

            // 4. 根据层级执行不同逻辑
            CascadeFieldSyncResult syncResult;
            if (targetCardIds.isEmpty()) {
                // 清空关联时，清除当前层级以下的所有层级
                syncResult = clearLowerLevels(cardId, cascadeFieldDef, currentLevel, orgId, operatorId, sourceIp);
            } else if (currentLevel > 0) {
                // 关联非根层级：同步上级
                String targetCardId = targetCardIds.get(0); // 级联属性应该是单选的
                syncResult = syncUpperLevels(cardId, match, cascadeRelationDef, targetCardId, orgId, operatorId, sourceIp);
            } else {
                // 关联根层级（currentLevel=0）：清除下级
                syncResult = clearLowerLevels(cardId, cascadeFieldDef, currentLevel, orgId, operatorId, sourceIp);
            }

            // 合并同步结果
            mergeLinks(allAddedLinks, syncResult.addedLinks());
            mergeLinks(allRemovedLinks, syncResult.removedLinks());

            // 5. 计算级联属性新值并发布变更事件
            CascadeFieldValue oldValue = oldCascadeFieldValues != null
                    ? oldCascadeFieldValues.get(cascadeFieldId) : null;
            CascadeFieldValue newValue = buildCurrentStructureValue(cardId, cascadeFieldDef, operatorId);
            publishStructureFieldChangeEvent(cardId, cardTypeId, cascadeFieldDef,
                    oldValue, newValue, orgId, operatorId, sourceIp);
        }

        return CascadeFieldSyncResult.of(allAddedLinks, allRemovedLinks);
    }

    /**
     * 合并关联变更
     */
    private void mergeLinks(Map<String, Set<String>> target, Map<String, Set<String>> source) {
        for (Map.Entry<String, Set<String>> entry : source.entrySet()) {
            target.computeIfAbsent(entry.getKey(), k -> new HashSet<>()).addAll(entry.getValue());
        }
    }

    /**
     * 计算指定关联属性对应的所有级联属性当前值
     * <p>
     * 供 LinkCardService 在更新关联前调用，用于获取所有受影响级联属性的旧值。
     * 一个关联属性可能被多个级联属性绑定。
     *
     * @param cardId      卡片ID
     * @param cardTypeId  实体类型ID
     * @param linkFieldId 关联属性ID
     * @param operatorId  操作人ID
     * @return 级联属性ID到当前值的映射，如果关联属性不属于任何级联绑定则返回空Map
     */
    public Map<String, CascadeFieldValue> getCascadeFieldValuesBeforeUpdate(String cardId, String cardTypeId,
                                                                            String linkFieldId, String operatorId) {
        List<CascadeFieldBindingMatch> matches = findAllStructureBindings(cardTypeId, linkFieldId);
        if (matches.isEmpty()) {
            return Map.of();
        }

        Map<String, CascadeFieldValue> result = new HashMap<>();
        for (CascadeFieldBindingMatch match : matches) {
            CascadeFieldConfig cascadeFieldDef = match.cascadeFieldDef();
            String fieldId = cascadeFieldDef.getId().value();
            // 避免重复计算同一个级联属性
            if (!result.containsKey(fieldId)) {
                result.put(fieldId, buildCurrentStructureValue(cardId, cascadeFieldDef, operatorId));
            }
        }
        return result;
    }

    /**
     * 计算指定关联属性对应的级联属性当前值（向后兼容）
     * <p>
     * 供 LinkCardService 在更新关联前调用，用于获取级联属性旧值
     *
     * @param cardId      卡片ID
     * @param cardTypeId  实体类型ID
     * @param linkFieldId 关联属性ID
     * @param operatorId  操作人ID
     * @return 级联属性当前值，如果关联属性不属于级联绑定则返回 null
     * @deprecated 使用 {@link #getCascadeFieldValuesBeforeUpdate} 代替
     */
    @Deprecated
    public CascadeFieldValue getStructureValueBeforeUpdate(String cardId, String cardTypeId,
                                                              String linkFieldId, String operatorId) {
        Optional<CascadeFieldBindingMatch> matchOpt = findStructureBinding(cardTypeId, linkFieldId);
        if (matchOpt.isEmpty()) {
            return null;
        }
        return buildCurrentStructureValue(cardId, matchOpt.get().cascadeFieldDef(), operatorId);
    }

    // ==================== 入口2：级联属性更新转换为关联更新 ====================

    /**
     * 将级联属性值转换为多个关联属性更新
     * <p>
     * 当用户通过级联属性编辑器直接选择完整路径时，将 CascadeFieldValue 解析为
     * 多个关联属性的更新操作。
     *
     * @param cardId         卡片ID
     * @param cardTypeId     实体类型ID
     * @param cascadeFieldValue 级联属性值（链表结构）
     * @param orgId          组织ID
     * @param operatorId     操作人ID
     * @param sourceIp       来源IP
     * @param linkUpdater    关联更新回调（用于调用 LinkCardService，避免循环依赖）
     */
    public void applyCascadeFieldValue(String cardId, String cardTypeId,
                                         CascadeFieldValue cascadeFieldValue,
                                         String orgId, String operatorId, String sourceIp,
                                         LinkUpdater linkUpdater) {
        // 1. 根据 fieldId 获取 CascadeFieldConfig
        String fieldId = cascadeFieldValue.getFieldId();
        Optional<SchemaDefinition<?>> defOpt = schemaCacheService.getById(fieldId);
        if (defOpt.isEmpty() || !(defOpt.get() instanceof CascadeFieldConfig cascadeFieldDef)) {
            logger.warn("级联属性定义不存在: {}", fieldId);
            return;
        }

        List<CascadeRelationLevelBinding> bindings = cascadeFieldDef.getLevelBindings();
        if (bindings == null || bindings.isEmpty()) {
            return;
        }

        // 2. 计算级联属性旧值（用于事件记录）
        CascadeFieldValue oldStructureValue = buildCurrentStructureValue(cardId, cascadeFieldDef, operatorId);

        // 3. 解析 CascadeItem 链表为 Map<levelIndex, cardId>
        Map<Integer, String> levelCardIds = parseCascadeItems(cascadeFieldValue.getValue());

        // 4. 遍历每个 levelBinding，更新对应的关联
        // 注意：按层级从小到大排序，先更新上级再更新下级
        List<CascadeRelationLevelBinding> sortedBindings = bindings.stream()
                .sorted(Comparator.comparingInt(CascadeRelationLevelBinding::levelIndex))
                .toList();

        for (CascadeRelationLevelBinding binding : sortedBindings) {
            int levelIndex = binding.levelIndex();
            String targetLinkFieldId = binding.linkFieldId().value();
            String targetCardId = levelCardIds.get(levelIndex);

            // 构建更新请求
            List<String> targetCardIds = targetCardId != null
                    ? List.of(targetCardId)
                    : List.of();  // 空列表表示清空关联

            // 调用 LinkUpdater 更新关联，skipSync=true 避免循环联动
            UpdateLinkRequest request = UpdateLinkRequest.builder()
                    .cardId(cardId)
                    .linkFieldId(targetLinkFieldId)
                    .targetCardIds(targetCardIds)
                    .skipPermissionCheck(true)  // 跳过权限检查，级联属性联动不需要权限检查
                    .build();
            linkUpdater.updateLink(request, orgId, operatorId, sourceIp, true);

            logger.debug("级联属性更新关联：cardId={}, linkFieldId={}, targetCardId={}",
                    cardId, targetLinkFieldId, targetCardId);
        }

        // 5. 发布级联属性变更事件
        publishStructureFieldChangeEvent(cardId, cardTypeId, cascadeFieldDef,
                oldStructureValue, cascadeFieldValue, orgId, operatorId, sourceIp);
    }

    // ==================== 核心算法：查找级联绑定 ====================

    /**
     * 查找关联属性对应的所有级联绑定
     * <p>
     * 遍历该实体类型的所有级联属性定义，检查当前 linkFieldId 是否属于某个级联属性的层级绑定。
     * 一个关联属性可能被多个级联属性绑定。
     *
     * @param cardTypeId  实体类型ID
     * @param linkFieldId 关联属性ID
     * @return 所有匹配的级联绑定信息列表
     */
    public List<CascadeFieldBindingMatch> findAllStructureBindings(String cardTypeId, String linkFieldId) {
        List<CascadeFieldBindingMatch> matches = new ArrayList<>();

        // 获取该实体类型的所有级联属性定义
        List<SchemaDefinition<?>> schemas = schemaCacheService.getBySecondaryIndex(
                CARD_TYPE_INDEX, cardTypeId, SchemaType.FIELD_CONFIG);

        for (SchemaDefinition<?> schema : schemas) {
            if (!(schema instanceof CascadeFieldConfig cascadeFieldDef)) {
                continue;
            }

            List<CascadeRelationLevelBinding> bindings = cascadeFieldDef.getLevelBindings();
            if (bindings == null) {
                continue;
            }

            // 遍历每个层级绑定，查找匹配的 linkFieldId
            for (CascadeRelationLevelBinding binding : bindings) {
                if (binding.linkFieldId().value().equals(linkFieldId)) {
                    matches.add(new CascadeFieldBindingMatch(
                            cascadeFieldDef, binding, binding.levelIndex()));
                }
            }
        }

        return matches;
    }

    /**
     * 查找关联属性对应的级联绑定（返回第一个匹配）
     * <p>
     * 向后兼容方法，返回第一个匹配的绑定。
     *
     * @param cardTypeId  实体类型ID
     * @param linkFieldId 关联属性ID
     * @return 匹配的级联绑定信息，不存在时返回 Optional.empty()
     */
    public Optional<CascadeFieldBindingMatch> findStructureBinding(String cardTypeId, String linkFieldId) {
        List<CascadeFieldBindingMatch> matches = findAllStructureBindings(cardTypeId, linkFieldId);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    // ==================== 核心算法：向上同步 ====================

    /**
     * 关联下级时，向上同步所有上级层级的关联
     * <p>
     * 例如：用户故事关联小队A时，查询小队A的所属部落，自动更新用户故事的所属部落
     */
    private CascadeFieldSyncResult syncUpperLevels(String cardId, CascadeFieldBindingMatch match,
                                                 CascadeRelationDefinition cascadeRelationDef,
                                                 String targetCardId,
                                                 String orgId, String operatorId, String sourceIp) {
        CascadeFieldConfig cascadeFieldDef = match.cascadeFieldDef();
        int currentLevel = match.levelIndex();

        Map<String, Set<String>> addedLinks = new HashMap<>();
        Map<String, Set<String>> removedLinks = new HashMap<>();

        // 从当前层级向上遍历到根层级
        String currentTargetCardId = targetCardId;
        for (int level = currentLevel; level > 0; level--) {
            int parentLevel = level - 1;

            // 获取当前层级的级联关系层级定义
            CascadeRelationLevel relationLevel = findCascadeRelationLevel(cascadeRelationDef, level);
            if (relationLevel == null || relationLevel.parentLinkFieldId() == null) {
                logger.warn("级联关系层级定义缺失 parentLinkFieldId: cascadeRelationId={}, level={}",
                        cascadeRelationDef.getId(), level);
                break;
            }

            // 获取业务卡片与上级层级的绑定
            Optional<CascadeRelationLevelBinding> parentBindingOpt =
                    findLevelBinding(cascadeFieldDef, parentLevel);
            if (parentBindingOpt.isEmpty()) {
                logger.warn("级联属性未绑定层级: cascadeFieldId={}, level={}",
                        cascadeFieldDef.getId(), parentLevel);
                break;
            }

            // 查询级联关系节点的上级关联（如小队A的所属部落）
            String parentCardId = queryParentCard(currentTargetCardId, relationLevel.parentLinkFieldId(), operatorId);

            // 更新业务卡片与上级层级的关联
            String parentLinkFieldId = parentBindingOpt.get().linkFieldId().value();
            Set<String> currentParentIds = getCurrentLinkedCardIds(cardId, parentLinkFieldId, operatorId);

            // 计算变更
            Set<String> newParentIds = parentCardId != null ? Set.of(parentCardId) : Set.of();
            Set<String> toRemove = new HashSet<>(currentParentIds);
            toRemove.removeAll(newParentIds);
            Set<String> toAdd = new HashSet<>(newParentIds);
            toAdd.removeAll(currentParentIds);

            // 执行关联变更
            if (!toRemove.isEmpty()) {
                deleteLinks(cardId, parentLinkFieldId, toRemove);
                removedLinks.put(parentLinkFieldId, toRemove);
            }
            if (!toAdd.isEmpty()) {
                createLinks(cardId, parentLinkFieldId, toAdd);
                addedLinks.put(parentLinkFieldId, toAdd);

                // 发布关联更新事件
                publishSyncLinkEvent(cardId, parentLinkFieldId, toAdd, toRemove, orgId, operatorId, sourceIp);
            }

            // 继续向上追溯
            currentTargetCardId = parentCardId;
            if (currentTargetCardId == null) {
                break; // 上级为空，停止向上追溯
            }
        }

        return CascadeFieldSyncResult.of(addedLinks, removedLinks);
    }

    // ==================== 核心算法：向下清除 ====================

    /**
     * 关联上级或清空时，清除所有下级层级的关联
     * <p>
     * 例如：用户故事直接关联部落时，清除下级的小队关联
     */
    private CascadeFieldSyncResult clearLowerLevels(String cardId,
                                                  CascadeFieldConfig cascadeFieldDef,
                                                  int currentLevel,
                                                  String orgId, String operatorId, String sourceIp) {
        List<CascadeRelationLevelBinding> bindings = cascadeFieldDef.getLevelBindings();
        if (bindings == null) {
            return CascadeFieldSyncResult.noSync();
        }

        Map<String, Set<String>> removedLinks = new HashMap<>();

        // 遍历所有比当前层级更深的绑定
        for (CascadeRelationLevelBinding binding : bindings) {
            if (binding.levelIndex() <= currentLevel) {
                continue;
            }

            String linkFieldId = binding.linkFieldId().value();
            Set<String> currentLinkedIds = getCurrentLinkedCardIds(cardId, linkFieldId, operatorId);

            if (!currentLinkedIds.isEmpty()) {
                deleteLinks(cardId, linkFieldId, currentLinkedIds);
                removedLinks.put(linkFieldId, currentLinkedIds);

                // 发布关联更新事件
                publishSyncLinkEvent(cardId, linkFieldId, Set.of(), currentLinkedIds, orgId, operatorId, sourceIp);
            }
        }

        return CascadeFieldSyncResult.of(Map.of(), removedLinks);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取级联关系定义
     */
    private CascadeRelationDefinition getCascadeRelationDefinition(String cascadeRelationDefinitionId) {
        return schemaCacheService.getById(cascadeRelationDefinitionId)
                .filter(schema -> schema instanceof CascadeRelationDefinition)
                .map(schema -> (CascadeRelationDefinition) schema)
                .orElse(null);
    }

    /**
     * 查找级联关系层级定义
     */
    private CascadeRelationLevel findCascadeRelationLevel(CascadeRelationDefinition cascadeRelationDef, int levelIndex) {
        if (cascadeRelationDef.getLevels() == null) {
            return null;
        }
        return cascadeRelationDef.getLevels().stream()
                .filter(level -> level.index() == levelIndex)
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找层级绑定
     */
    private Optional<CascadeRelationLevelBinding> findLevelBinding(CascadeFieldConfig cascadeFieldDef, int levelIndex) {
        if (cascadeFieldDef.getLevelBindings() == null) {
            return Optional.empty();
        }
        return cascadeFieldDef.getLevelBindings().stream()
                .filter(b -> b.levelIndex() == levelIndex)
                .findFirst();
    }

    /**
     * 查询级联节点的上级卡片ID
     */
    private String queryParentCard(String cardId, LinkFieldId parentLinkFieldId, String operatorId) {
        Set<String> parentIds = getCurrentLinkedCardIds(cardId, parentLinkFieldId.value(), operatorId);
        return parentIds.isEmpty() ? null : parentIds.iterator().next();
    }

    /**
     * 获取当前卡片的关联卡片ID集合
     */
    private Set<String> getCurrentLinkedCardIds(String cardId, String linkFieldId, String operatorId) {
        CardId id = CardId.of(cardId);
        Yield yield = new Yield();
        YieldLink yieldLink = new YieldLink();
        yieldLink.setLinkFieldId(linkFieldId);
        yield.setLinks(List.of(yieldLink));

        Optional<CardDTO> cardOpt = cardRepository.findById(id, yield, operatorId);
        if (cardOpt.isEmpty() || cardOpt.get().getLinkedCards() == null) {
            return Collections.emptySet();
        }

        Set<CardDTO> linkedCards = cardOpt.get().getLinkedCards().get(linkFieldId);
        if (linkedCards == null) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>();
        for (CardDTO card : linkedCards) {
            result.add(String.valueOf(card.getId().value()));
        }
        return result;
    }

    /**
     * 解析 CascadeItem 链表为 levelIndex -> cardId 映射
     */
    private Map<Integer, String> parseCascadeItems(CascadeItem item) {
        Map<Integer, String> result = new HashMap<>();
        int level = 0;
        while (item != null) {
            result.put(level, item.getId());
            item = item.getNext();
            level++;
        }
        return result;
    }

    /**
     * 构建当前卡片的级联属性值
     */
    private CascadeFieldValue buildCurrentStructureValue(String cardId,
                                                           CascadeFieldConfig cascadeFieldDef,
                                                           String operatorId) {
        // 构建 Yield 包含所有层级绑定的关联查询
        List<CascadeRelationLevelBinding> bindings = cascadeFieldDef.getLevelBindings();
        if (bindings == null || bindings.isEmpty()) {
            return new CascadeFieldValue(cascadeFieldDef.getId().value(), null);
        }

        Yield yield = new Yield();
        List<YieldLink> yieldLinks = new ArrayList<>();
        for (CascadeRelationLevelBinding binding : bindings) {
            YieldLink yieldLink = new YieldLink();
            yieldLink.setLinkFieldId(binding.linkFieldId().value());
            yieldLink.setTargetYield(Yield.basic());
            yieldLinks.add(yieldLink);
        }
        yield.setLinks(yieldLinks);

        // 查询卡片
        CardId id = CardId.of(cardId);
        Optional<CardDTO> cardOpt = cardRepository.findById(id, yield, operatorId);
        if (cardOpt.isEmpty()) {
            return new CascadeFieldValue(cascadeFieldDef.getId().value(), null);
        }

        return CascadeFieldValueBuilder.build(cardOpt.get(), cascadeFieldDef);
    }

    // ==================== 关联操作 ====================

    private void deleteLinks(String cardId, String linkFieldId, Set<String> targetCardIds) {
        String linkTypeIdValue = LinkFieldIdUtils.getLinkTypeId(linkFieldId);
        LinkPosition linkPosition = LinkFieldIdUtils.getPosition(linkFieldId);
        List<Link> linksToDelete = buildLinks(cardId, linkTypeIdValue, linkPosition, targetCardIds);

        BatchDeleteLinkRequest request = BatchDeleteLinkRequest.newBuilder()
                .addAllLinks(linksToDelete)
                .build();

        try {
            CompletableFuture<BatchLinkCommonResponse> future = writeClient.batchDeleteLink(request);
            BatchLinkCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.debug("联动删除关联完成，成功: {}", response.getSuccess());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("联动删除关联失败: " + e.getMessage(), e);
        }
    }

    private void createLinks(String cardId, String linkFieldId, Set<String> targetCardIds) {
        String linkTypeIdValue = LinkFieldIdUtils.getLinkTypeId(linkFieldId);
        LinkPosition linkPosition = LinkFieldIdUtils.getPosition(linkFieldId);
        List<Link> linksToCreate = buildLinks(cardId, linkTypeIdValue, linkPosition, targetCardIds);

        BatchCreateLinkRequest request = BatchCreateLinkRequest.newBuilder()
                .addAllLinks(linksToCreate)
                .build();

        try {
            CompletableFuture<BatchLinkCommonResponse> future = writeClient.batchCreateLink(request);
            BatchLinkCommonResponse response = future.get(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            logger.debug("联动创建关联完成，成功: {}", response.getSuccess());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("联动创建关联失败: " + e.getMessage(), e);
        }
    }

    private List<Link> buildLinks(String cardId, String linkTypeIdValue,
                                   LinkPosition linkPosition, Set<String> targetCardIds) {
        List<Link> links = new ArrayList<>();
        for (String targetCardId : targetCardIds) {
            long srcId, destId;
            if (linkPosition == LinkPosition.SOURCE) {
                srcId = Long.parseLong(cardId);
                destId = Long.parseLong(targetCardId);
            } else {
                srcId = Long.parseLong(targetCardId);
                destId = Long.parseLong(cardId);
            }
            links.add(Link.newBuilder()
                    .setLtId(linkTypeIdValue)
                    .setSrcId(srcId)
                    .setDestId(destId)
                    .build());
        }
        return links;
    }

    // ==================== 事件发布 ====================

    /**
     * 发布联动产生的关联更新事件
     */
    private void publishSyncLinkEvent(String cardId, String linkFieldId,
                                       Set<String> addedIds, Set<String> removedIds,
                                       String orgId, String operatorId, String sourceIp) {
        // 获取实体类型
        CardId id = CardId.of(cardId);
        Optional<CardDTO> cardOpt = cardRepository.findById(id, Yield.basic(), operatorId);
        if (cardOpt.isEmpty()) {
            return;
        }
        CardDTO card = cardOpt.get();

        // 构建事件
        List<CardLinkUpdatedEvent.LinkedCardRef> addedRefs = new ArrayList<>();
        for (String addedId : addedIds) {
            addedRefs.add(new CardLinkUpdatedEvent.LinkedCardRef(addedId, "", ""));
        }

        List<CardLinkUpdatedEvent.LinkedCardRef> removedRefs = new ArrayList<>();
        for (String removedId : removedIds) {
            removedRefs.add(new CardLinkUpdatedEvent.LinkedCardRef(removedId, "", ""));
        }

        CardLinkUpdatedEvent event = new CardLinkUpdatedEvent(
                orgId, operatorId, sourceIp, null,
                cardId, card.getTypeId().value())
                .withLinkField(linkFieldId, linkFieldId, true)
                .withAddedCards(addedRefs)
                .withRemovedCards(removedRefs);

        if (event.hasChanges()) {
            eventPublisher.publish(event);
            logger.debug("发布联动关联更新事件: cardId={}, linkFieldId={}", cardId, linkFieldId);
        }
    }

    /**
     * 发布级联属性变更事件
     */
    private void publishStructureFieldChangeEvent(String cardId, String cardTypeId,
                                                   CascadeFieldConfig cascadeFieldDef,
                                                   CascadeFieldValue oldValue,
                                                   CascadeFieldValue newValue,
                                                   String orgId, String operatorId, String sourceIp) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        // 获取组织ID（如果为空，从卡片获取）
        String effectiveOrgId = orgId;
        if (effectiveOrgId == null) {
            CardId id = CardId.of(cardId);
            Optional<CardDTO> cardOpt = cardRepository.findById(id, Yield.basic(), operatorId);
            if (cardOpt.isPresent()) {
                effectiveOrgId = cardOpt.get().getOrgId().value();
            }
        }

        CardUpdatedEvent event = new CardUpdatedEvent(
                effectiveOrgId, operatorId, sourceIp, null, cardTypeId, cardId);
        event.addFieldChange(cascadeFieldDef.getId().value(), oldValue, newValue);

        eventPublisher.publish(event);
        logger.debug("发布级联属性变更事件: cardId={}, fieldId={}", cardId, cascadeFieldDef.getId());
    }

    /**
     * 关联更新回调接口
     * <p>
     * 用于避免与 LinkCardService 的循环依赖
     */
    @FunctionalInterface
    public interface LinkUpdater {
        void updateLink(UpdateLinkRequest request, String orgId, String operatorId, String sourceIp, boolean skipSync);
    }
}
