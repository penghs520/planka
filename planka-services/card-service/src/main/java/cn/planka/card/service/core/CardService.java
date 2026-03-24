package cn.planka.card.service.core;

import cn.planka.api.card.dto.BatchOperationResult;
import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.schema.service.FieldConfigQueryService;
import cn.planka.card.event.CardEventPublisher;
import cn.planka.card.model.CardEntity;
import cn.planka.card.repository.CardRepository;
import cn.planka.card.service.permission.CardPermissionService;
import cn.planka.card.service.permission.exception.PermissionDeniedException;
import cn.planka.card.service.permission.model.BatchPermissionCheckResult;
import cn.planka.card.service.cascadefield.CascadeFieldLinkSyncService;
import cn.planka.card.service.validation.FieldValueValidator;
import cn.planka.common.exception.CommonErrorCode;
import cn.planka.common.result.Result;
import cn.planka.common.util.SystemSchemaIds;
import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTitle;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.card.CardDescription;
import cn.planka.domain.field.FieldId;
import cn.planka.domain.field.FieldValue;
import cn.planka.domain.field.CascadeFieldValue;
import cn.planka.domain.field.CascadeItem;
import cn.planka.domain.schema.definition.fieldconfig.FieldConfig;
import cn.planka.domain.schema.definition.fieldconfig.CascadeFieldConfig;
import cn.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationLevelBinding;
import cn.planka.infra.cache.schema.SchemaCacheService;
import cn.planka.event.card.CardMovedEvent;
import cn.planka.api.card.request.*;
import cn.planka.api.schema.dto.inheritance.FieldConfigListWithSource;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 卡片服务实现
 * <p>
 * 负责卡片的写操作（创建、更新、删除、移动等）。
 * 查询操作请使用 {@link CardQueryService}。
 */
@Service
public class CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cardRepository;
    private final CascadeFieldLinkSyncService cascadeFieldLinkSyncService;
    private final LinkCardService linkCardService;
    private final CardPermissionService permissionService;
    private final CardEventPublisher eventPublisher;
    private final CardEntityConverter entityConverter;
    private final FieldConfigQueryService fieldConfigQueryService;
    private final FieldValueValidator fieldValueValidator;
    private final SchemaCacheService schemaCacheService;

    public CardService(CardRepository cardRepository,
                       CascadeFieldLinkSyncService cascadeFieldLinkSyncService,
                       LinkCardService linkCardService,
                       CardPermissionService permissionService,
                       CardEventPublisher eventPublisher,
                       CardEntityConverter entityConverter,
                       FieldConfigQueryService fieldConfigQueryService,
                       FieldValueValidator fieldValueValidator,
                       SchemaCacheService schemaCacheService) {
        this.cardRepository = cardRepository;
        this.cascadeFieldLinkSyncService = cascadeFieldLinkSyncService;
        this.linkCardService = linkCardService;
        this.permissionService = permissionService;
        this.eventPublisher = eventPublisher;
        this.entityConverter = entityConverter;
        this.fieldConfigQueryService = fieldConfigQueryService;
        this.fieldValueValidator = fieldValueValidator;
        this.schemaCacheService = schemaCacheService;
    }

    // ==================== 写操作 ====================

    public Result<CardId> create(CreateCardRequest request, CardId operatorId) {
        return create(request, operatorId, null);
    }

    public Result<CardId> create(CreateCardRequest request, CardId operatorId, String sourceIp) {
        try {
            permissionService.checkCardOperationForCreate(CardOperation.CREATE, request.typeId(), operatorId);

            // 属性值校验
            List<FieldConfig> fieldConfigs = getFieldConfigsForCardType(request.typeId());
            CardDTO tempCard = buildTempCardForValidation(request);
            FieldValueValidator.ValidationResult validationResult = fieldValueValidator.validateCard(
                    tempCard, fieldConfigs, String.valueOf(operatorId.value()));
            if (!validationResult.valid()) {
                return Result.failure(CommonErrorCode.VALIDATION_ERROR, validationResult.getFormattedError());
            }

            CardEntityConverter.FilteredCreateRequest filtered = entityConverter.filterCascadeFieldValues(request);
            CardEntity cardEntity = entityConverter.toCardEntityForCreate(filtered.request());
            CardId cardId = cardRepository.create(cardEntity);

            applyCascadeFieldValuesForCreate(cardId, request, filtered.cascadeFieldValues(), operatorId, sourceIp);
            eventPublisher.publishCreated(cardEntity, String.valueOf(operatorId.value()));

            // 处理关联属性（覆盖式）
            if (CollectionUtils.isNotEmpty(request.linkUpdates())) {
                for (LinkFieldUpdate linkUpdate : request.linkUpdates()) {
                    UpdateLinkRequest linkRequest = UpdateLinkRequest.builder()
                            .cardId(cardId.asStr())
                            .linkFieldId(linkUpdate.linkFieldId())
                            .targetCardIds(linkUpdate.targetCardIds() != null
                                    ? linkUpdate.targetCardIds()
                                    : Collections.emptyList())
                            .build();

                    Result<Void> linkResult = linkCardService.updateLink(
                            linkRequest, String.valueOf(request.orgId().value()),
                            String.valueOf(operatorId.value()), sourceIp);

                    if (!linkResult.isSuccess()) {
                        logger.error("创建卡片时更新关联属性失败: linkFieldId={}, error={}",
                                linkUpdate.linkFieldId(), linkResult.getMessage());
                    }
                }
            }

            // 自动设置创建人关联
            String creatorLinkTypeId = SystemSchemaIds.creatorLinkTypeId(request.orgId().value());
            String creatorLinkFieldId = creatorLinkTypeId + ":SOURCE";
            UpdateLinkRequest creatorLinkRequest = UpdateLinkRequest.builder()
                    .cardId(cardId.asStr())
                    .linkFieldId(creatorLinkFieldId)
                    .targetCardIds(List.of(String.valueOf(operatorId.value())))
                    .build();

            Result<Void> creatorLinkResult = linkCardService.updateLink(
                    creatorLinkRequest,
                    request.orgId().value(),
                    String.valueOf(operatorId.value()),
                    sourceIp,
                    true); // 跳过架构联动同步，避免循环

            if (!creatorLinkResult.isSuccess()) {
                logger.error("设置创建人关联失败: cardId={}, error={}",
                        cardId, creatorLinkResult.getMessage());
            }

            return Result.success(cardId);
        } catch (Exception e) {
            logger.error("创建卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "创建卡片失败: " + e.getMessage());
        }
    }

    public Result<Void> update(UpdateCardRequest request, CardId operatorId) {
        return update(request, null, operatorId, null);
    }

    public Result<Void> update(UpdateCardRequest request, String orgId, CardId operatorId, String sourceIp) {
        try {
            permissionService.checkCardOperation(CardOperation.EDIT, request.cardId(), operatorId);

            Optional<CardDTO> existingCardOpt = cardRepository.findById(
                    request.cardId(), Yield.all(), String.valueOf(operatorId.value()));
            if (existingCardOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.NOT_FOUND, "卡片不存在: " + request.cardId().value());
            }
            CardDTO existingCard = existingCardOpt.get();
            String effectiveOrgId = orgId != null ? orgId : existingCard.getOrgId().value();

            // 属性编辑权限检查（普通属性 + 关联属性 + 架构属性，含对侧检查）
            checkFieldEditPermission(request, operatorId);

            // 属性值校验（只校验本次修改的属性）
            List<FieldConfig> allFieldConfigs = getFieldConfigsForCardType(existingCard.getTypeId());
            List<FieldConfig> changedFieldConfigs = filterChangedFieldConfigs(allFieldConfigs, request);
            if (!changedFieldConfigs.isEmpty()) {
                CardDTO mergedCard = mergeCardForValidation(existingCard, request);
                FieldValueValidator.ValidationResult validationResult = fieldValueValidator.validateCard(
                        mergedCard, changedFieldConfigs, String.valueOf(operatorId.value()));
                if (!validationResult.valid()) {
                    return Result.failure(CommonErrorCode.VALIDATION_ERROR, validationResult.getFormattedError());
                }
            }

            UpdateCardRequest filteredRequest = filterCascadeFieldValues(
                    request, existingCard, effectiveOrgId, operatorId, sourceIp);

            CardEntity cardEntity = entityConverter.toCardEntityForUpdate(filteredRequest, existingCard);
            cardRepository.update(cardEntity);
            eventPublisher.publishUpdated(existingCard, filteredRequest, String.valueOf(operatorId.value()));

            // 处理关联属性更新（覆盖式）
            if (CollectionUtils.isNotEmpty(request.linkUpdates())) {
                for (LinkFieldUpdate linkUpdate : request.linkUpdates()) {
                    UpdateLinkRequest linkRequest = UpdateLinkRequest.builder()
                            .cardId(request.cardId().asStr())
                            .linkFieldId(linkUpdate.linkFieldId())
                            .targetCardIds(linkUpdate.targetCardIds() != null
                                    ? linkUpdate.targetCardIds()
                                    : Collections.emptyList())
                            .skipPermissionCheck(true)  // 跳过权限检查，因为已经在上层做过检查
                            .build();

                    Result<Void> linkResult = linkCardService.updateLink(
                            linkRequest, effectiveOrgId, String.valueOf(operatorId.value()), sourceIp);

                    if (!linkResult.isSuccess()) {
                        logger.error("更新关联属性失败: linkFieldId={}, error={}",
                                linkUpdate.linkFieldId(), linkResult.getMessage());
                        return linkResult;
                    }
                }
            }

            return Result.success();
        } catch (Exception e) {
            logger.error("更新卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "更新卡片失败: " + e.getMessage());
        }
    }

    public Result<Void> discard(CardId cardId, String discardReason, CardId operatorId) {
        return discard(cardId, discardReason, operatorId, null);
    }

    public Result<Void> discard(CardId cardId, String discardReason, CardId operatorId, String sourceIp) {
        try {
            permissionService.checkCardOperation(CardOperation.DISCARD, cardId, operatorId);

            Optional<CardDTO> cardOpt = cardRepository.findById(cardId, Yield.basic(), String.valueOf(operatorId.value()));
            if (cardOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.NOT_FOUND, "卡片不存在: " + cardId.value());
            }

            cardRepository.discard(cardId, discardReason, String.valueOf(operatorId.value()));
            eventPublisher.publishAbandoned(cardOpt.get(), String.valueOf(operatorId.value()), sourceIp, discardReason);
            return Result.success();
        } catch (Exception e) {
            logger.error("回收卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "回收卡片失败: " + e.getMessage());
        }
    }

    public Result<Void> archive(CardId cardId, CardId operatorId) {
        return archive(cardId, operatorId, null);
    }

    public Result<Void> archive(CardId cardId, CardId operatorId, String sourceIp) {
        try {
            permissionService.checkCardOperation(CardOperation.ARCHIVE, cardId, operatorId);

            Optional<CardDTO> cardOpt = cardRepository.findById(cardId, Yield.basic(), String.valueOf(operatorId.value()));
            if (cardOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.NOT_FOUND, "卡片不存在: " + cardId.value());
            }

            cardRepository.archive(cardId, String.valueOf(operatorId.value()));
            eventPublisher.publishArchived(cardOpt.get(), String.valueOf(operatorId.value()), sourceIp);
            return Result.success();
        } catch (Exception e) {
            logger.error("存档卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "存档卡片失败: " + e.getMessage());
        }
    }

    public Result<Void> restore(CardId cardId, CardId operatorId) {
        return restore(cardId, operatorId, null);
    }

    public Result<Void> restore(CardId cardId, CardId operatorId, String sourceIp) {
        try {
            Optional<CardDTO> cardOpt = cardRepository.findById(cardId, Yield.basic(), String.valueOf(operatorId.value()));
            if (cardOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.NOT_FOUND, "卡片不存在: " + cardId.value());
            }

            cardRepository.restore(cardId, String.valueOf(operatorId.value()));
            eventPublisher.publishRestored(cardOpt.get(), String.valueOf(operatorId.value()), sourceIp);
            return Result.success();
        } catch (Exception e) {
            logger.error("还原卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "还原卡片失败: " + e.getMessage());
        }
    }

    // ==================== 批量操作 ====================

    public Result<BatchOperationResult> batchCreate(List<CreateCardRequest> requests, CardId operatorId) {
        try {
            List<CardEntity> cardEntities = requests.stream()
                    .map(entityConverter::filterCascadeFieldValues)
                    .map(filtered -> entityConverter.toCardEntityForCreate(filtered.request()))
                    .toList();
            List<CardId> successIds = cardRepository.batchCreate(cardEntities);
            return Result.success(BatchOperationResult.success(successIds));
        } catch (Exception e) {
            logger.error("批量创建卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量创建卡片失败: " + e.getMessage());
        }
    }

    public Result<Void> batchDiscard(List<CardId> cardIds, String discardReason, CardId operatorId) {
        return batchDiscard(cardIds, discardReason, operatorId, null);
    }

    public Result<Void> batchDiscard(List<CardId> cardIds, String discardReason, CardId operatorId, String sourceIp) {
        try {
            BatchPermissionCheckResult checkResult = permissionService.batchCheckCardOperation(
                    CardOperation.DISCARD, cardIds, operatorId);
            if (checkResult.isAllDenied()) {
                return Result.failure(CommonErrorCode.PERMISSION_DENIED, "无权限回收任何卡片");
            }

            List<CardId> allowedCardIds = checkResult.getAllowed();
            List<CardDTO> cards = cardRepository.findByIds(allowedCardIds, Yield.basic(), String.valueOf(operatorId.value()));

            cardRepository.batchDiscard(allowedCardIds, discardReason, String.valueOf(operatorId.value()));
            eventPublisher.publishAllAbandoned(cards, String.valueOf(operatorId.value()), sourceIp, discardReason);
            return Result.success();
        } catch (Exception e) {
            logger.error("批量回收卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量回收卡片失败: " + e.getMessage());
        }
    }

    public Result<Void> batchArchive(List<CardId> cardIds, CardId operatorId) {
        return batchArchive(cardIds, operatorId, null);
    }

    public Result<Void> batchArchive(List<CardId> cardIds, CardId operatorId, String sourceIp) {
        try {
            BatchPermissionCheckResult checkResult = permissionService.batchCheckCardOperation(
                    CardOperation.ARCHIVE, cardIds, operatorId);
            if (checkResult.isAllDenied()) {
                return Result.failure(CommonErrorCode.PERMISSION_DENIED, "无权限存档任何卡片");
            }

            List<CardId> allowedCardIds = checkResult.getAllowed();
            List<CardDTO> cards = cardRepository.findByIds(allowedCardIds, Yield.basic(), String.valueOf(operatorId.value()));

            cardRepository.batchArchive(allowedCardIds, String.valueOf(operatorId.value()));
            eventPublisher.publishAllArchived(cards, String.valueOf(operatorId.value()), sourceIp);
            return Result.success();
        } catch (Exception e) {
            logger.error("批量存档卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量存档卡片失败: " + e.getMessage());
        }
    }

    public Result<Void> batchRestore(List<CardId> cardIds, CardId operatorId) {
        return batchRestore(cardIds, operatorId, null);
    }

    public Result<Void> batchRestore(List<CardId> cardIds, CardId operatorId, String sourceIp) {
        try {
            List<CardDTO> cards = cardRepository.findByIds(cardIds, Yield.basic(), String.valueOf(operatorId.value()));

            cardRepository.batchRestore(cardIds, String.valueOf(operatorId.value()));
            eventPublisher.publishAllRestored(cards, String.valueOf(operatorId.value()), sourceIp);
            return Result.success();
        } catch (Exception e) {
            logger.error("批量还原卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量还原卡片失败: " + e.getMessage());
        }
    }

    // ==================== 价值流移动操作 ====================

    @Transactional
    public Result<Void> move(MoveCardRequest request, CardId operatorId) {
        try {
            permissionService.checkCardOperation(CardOperation.MOVE, request.cardId(), operatorId);

            Optional<CardDTO> existingCardOpt = cardRepository.findById(
                    request.cardId(), Yield.all(), String.valueOf(operatorId.value()));
            if (existingCardOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.NOT_FOUND, "卡片不存在: " + request.cardId().value());
            }

            CardDTO existingCard = existingCardOpt.get();
            if (existingCard.getStatusId().equals(request.toStatusId())) {
                logger.debug("卡片状态未变化，跳过移动: cardId={}, statusId={}", request.cardId(), request.toStatusId());
                return Result.success();
            }

            cardRepository.updateStatus(request.cardId(), request.streamId(), request.toStatusId(),
                    String.valueOf(operatorId.value()));
            eventPublisher.publishMoved(existingCard, request, String.valueOf(operatorId.value()));

            logger.info("卡片移动成功: cardId={}, from={}, to={}",
                    request.cardId(), existingCard.getStatusId(), request.toStatusId());
            return Result.success();
        } catch (Exception e) {
            logger.error("移动卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "移动卡片失败: " + e.getMessage());
        }
    }

    @Transactional
    public Result<BatchOperationResult> batchMove(BatchMoveCardRequest request, CardId operatorId) {
        try {
            BatchPermissionCheckResult checkResult = permissionService.batchCheckCardOperation(
                    CardOperation.MOVE, request.cardIds(), operatorId);
            if (checkResult.isAllDenied()) {
                return Result.failure(CommonErrorCode.PERMISSION_DENIED, "无权限移动任何卡片");
            }

            List<CardId> allowedCardIds = checkResult.getAllowed();
            List<CardDTO> existingCards = cardRepository.findByIds(
                    allowedCardIds, Yield.all(), String.valueOf(operatorId.value()));

            Map<String, CardDTO> existingCardMap = existingCards.stream()
                    .collect(Collectors.toMap(card -> card.getId().value(), card -> card));

            List<CardMovedEvent> events = new ArrayList<>();
            List<CardId> movedCardIds = new ArrayList<>();

            for (CardId cardId : allowedCardIds) {
                CardDTO existingCard = existingCardMap.get(cardId.value());
                if (existingCard == null || existingCard.getStatusId().equals(request.toStatusId())) {
                    if (existingCard == null) {
                        logger.warn("批量移动时卡片不存在: {}", cardId);
                    }
                    continue;
                }

                CardMovedEvent event = eventPublisher.buildMoveEvent(
                        existingCard, request.streamId().value(), String.valueOf(cardId.value()),
                        existingCard.getStatusId(), request.toStatusId(), String.valueOf(operatorId.value()));
                events.add(event);
                movedCardIds.add(cardId);
            }

            if (movedCardIds.isEmpty()) {
                return Result.success(BatchOperationResult.success(List.of()));
            }

            cardRepository.batchUpdateStatus(movedCardIds, request.streamId(), request.toStatusId(),
                    String.valueOf(operatorId.value()));
            eventPublisher.publishAllMoved(events);

            logger.info("批量移动卡片成功: count={}, toStatus={}", movedCardIds.size(), request.toStatusId());
            return Result.success(BatchOperationResult.success(movedCardIds));
        } catch (Exception e) {
            logger.error("批量移动卡片失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量移动卡片失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新卡片状态（用于状态迁移）
     * <p>
     * 将指定源状态下的所有卡片迁移到目标状态
     *
     * @param orgId 组织ID
     * @param sourceStatusId 源状态ID
     * @param targetStatusId 目标状态ID
     * @param streamId 价值流ID
     * @param cardTypeId __PLANKA_EINST__ID
     * @param operatorId 操作人ID
     * @return 更新的卡片数量
     */
    @Transactional
    public Result<Integer> batchUpdateCardStatus(
            String orgId,
            String sourceStatusId,
            String targetStatusId,
            String streamId,
            String cardTypeId,
            CardId operatorId) {
        try {
            // 1. 查询源状态下的所有卡片ID
            CardQueryRequest queryRequest = new CardQueryRequest();

            QueryContext queryContext = new QueryContext();
            queryContext.setOrgId(orgId);
            queryContext.setOperatorId(String.valueOf(operatorId.value()));
            queryRequest.setQueryContext(queryContext);

            QueryScope queryScope = new QueryScope();
            queryScope.setCardTypeIds(List.of(cardTypeId));
            queryRequest.setQueryScope(queryScope);

            // 构建状态条件
            cn.planka.domain.schema.definition.condition.StatusConditionItem.StatusSubject subject =
                    new cn.planka.domain.schema.definition.condition.StatusConditionItem.StatusSubject(null, streamId);
            cn.planka.domain.schema.definition.condition.StatusConditionItem.StatusOperator operator =
                    new cn.planka.domain.schema.definition.condition.StatusConditionItem.StatusOperator.Equal(sourceStatusId);
            cn.planka.domain.schema.definition.condition.StatusConditionItem statusConditionItem =
                    new cn.planka.domain.schema.definition.condition.StatusConditionItem(subject, operator);
            cn.planka.domain.schema.definition.condition.Condition condition =
                    cn.planka.domain.schema.definition.condition.Condition.of(statusConditionItem);

            queryRequest.setCondition(condition);
            queryRequest.setYield(Yield.all());

            List<CardDTO> cards = cardRepository.query(queryRequest);

            if (cards.isEmpty()) {
                logger.info("源状态下没有卡片需要迁移: sourceStatusId={}", sourceStatusId);
                return Result.success(0);
            }

            // 2. 提取卡片ID列表
            List<CardId> cardIds = cards.stream()
                    .map(card -> CardId.of(card.getId().value()))
                    .toList();

            // 3. 权限检查
            BatchPermissionCheckResult checkResult = permissionService.batchCheckCardOperation(
                    CardOperation.MOVE, cardIds, operatorId);
            if (checkResult.isAllDenied()) {
                return Result.failure(CommonErrorCode.PERMISSION_DENIED, "无权限迁移任何卡片");
            }

            List<CardId> allowedCardIds = checkResult.getAllowed();

            // 4. 构建移动事件
            List<CardMovedEvent> events = new ArrayList<>();
            for (CardDTO card : cards) {
                if (allowedCardIds.contains(CardId.of(card.getId().value()))) {
                    CardMovedEvent event = eventPublisher.buildMoveEvent(
                            card, streamId, card.getId().value(),
                            card.getStatusId(),
                            cn.planka.domain.stream.StatusId.of(targetStatusId),
                            String.valueOf(operatorId.value()));
                    events.add(event);
                }
            }

            // 5. 批量更新状态
            cardRepository.batchUpdateStatus(allowedCardIds, cn.planka.domain.stream.StreamId.of(streamId),
                    cn.planka.domain.stream.StatusId.of(targetStatusId), String.valueOf(operatorId.value()));

            // 6. 发布事件
            eventPublisher.publishAllMoved(events);

            logger.info("批量迁移卡片状态成功: count={}, from={}, to={}",
                    allowedCardIds.size(), sourceStatusId, targetStatusId);
            return Result.success(allowedCardIds.size());
        } catch (Exception e) {
            logger.error("批量更新卡片状态失败", e);
            if (e instanceof PermissionDeniedException permissionDeniedException) {
                return Result.failure(permissionDeniedException.getErrorCode(), e.getMessage());
            }
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量更新卡片状态失败: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    private void applyCascadeFieldValuesForCreate(CardId cardId, CreateCardRequest request,
                                                    Map<String, CascadeFieldValue> cascadeFieldValues,
                                                    CardId operatorId, String sourceIp) {
        if (cascadeFieldValues.isEmpty()) {
            return;
        }
        for (CascadeFieldValue cascadeFieldValue : cascadeFieldValues.values()) {
            cascadeFieldLinkSyncService.applyCascadeFieldValue(
                    String.valueOf(cardId.value()),
                    request.typeId().value(),
                    cascadeFieldValue,
                    request.orgId().value(),
                    String.valueOf(operatorId.value()),
                    sourceIp,
                    linkCardService::updateLink);
            logger.debug("创建卡片时应用架构属性: cardId={}, fieldId={}", cardId, cascadeFieldValue.getFieldId());
        }
    }

    private void checkFieldEditPermission(UpdateCardRequest request, CardId operatorId) {
        // 1. 收集所有属性 fieldIds（包括架构属性本身）
        Set<FieldId> changedFieldIds = new HashSet<>();
        if (request.fieldValues() != null) {
            for (String key : request.fieldValues().keySet()) {
                changedFieldIds.add(FieldId.of(key));
            }
        }

        // 2. 收集关联属性 linkFieldIds 及对端卡片ID
        Set<String> linkFieldIds = new HashSet<>();
        Map<String, List<String>> targetCardIdsByLinkField = new HashMap<>();

        // 2a. 从 linkUpdates 收集
        if (CollectionUtils.isNotEmpty(request.linkUpdates())) {
            for (LinkFieldUpdate linkUpdate : request.linkUpdates()) {
                linkFieldIds.add(linkUpdate.linkFieldId());
                targetCardIdsByLinkField.put(linkUpdate.linkFieldId(),
                        linkUpdate.targetCardIds() != null ? linkUpdate.targetCardIds() : List.of());
            }
        }

        // 2b. 从架构属性额外收集对应的关联属性（架构属性本身已在 changedFieldIds 中）
        if (request.fieldValues() != null) {
            for (Map.Entry<String, FieldValue<?>> entry : request.fieldValues().entrySet()) {
                if (entry.getValue() instanceof CascadeFieldValue cascadeFieldValue) {
                    collectLinkFieldIdsFromStructure(cascadeFieldValue,
                            linkFieldIds, targetCardIdsByLinkField);
                }
            }
        }

        // 3. 统一调用权限检查（含对侧检查）
        if (!changedFieldIds.isEmpty() || !linkFieldIds.isEmpty()) {
            permissionService.checkFieldEditPermission(
                    request.cardId(), operatorId,
                    changedFieldIds, linkFieldIds, targetCardIdsByLinkField);
        }
    }

    /**
     * 从架构属性中收集对应的关联属性ID和对端卡片ID
     */
    private void collectLinkFieldIdsFromStructure(
            CascadeFieldValue cascadeFieldValue,
            Set<String> linkFieldIds,
            Map<String, List<String>> targetCardIdsByLinkField) {

        String fieldId = cascadeFieldValue.getFieldId();
        var defOpt = schemaCacheService.getById(fieldId);
        if (defOpt.isEmpty() || !(defOpt.get() instanceof CascadeFieldConfig cascadeFieldConfig)) {
            return;
        }

        List<CascadeRelationLevelBinding> bindings = cascadeFieldConfig.getLevelBindings();
        if (bindings == null || bindings.isEmpty()) {
            return;
        }

        // 解析 CascadeItem 链表为 Map<levelIndex, cardId>
        Map<Integer, String> levelCardIds = new HashMap<>();
        CascadeItem item = cascadeFieldValue.getValue();
        int level = 0;
        while (item != null) {
            levelCardIds.put(level, item.getId());
            item = item.getNext();
            level++;
        }

        // 将每个层级绑定的 linkFieldId 加入集合
        for (CascadeRelationLevelBinding binding : bindings) {
            String linkFieldId = binding.linkFieldId().value();
            linkFieldIds.add(linkFieldId);

            String targetCardId = levelCardIds.get(binding.levelIndex());
            targetCardIdsByLinkField.put(linkFieldId,
                    targetCardId != null ? List.of(targetCardId) : List.of());
        }
    }

    private UpdateCardRequest filterCascadeFieldValues(UpdateCardRequest request, CardDTO existingCard,
                                                         String orgId, CardId operatorId, String sourceIp) {
        if (request.fieldValues() == null || request.fieldValues().isEmpty()) {
            return request;
        }

        Map<String, FieldValue<?>> filteredFieldValues = new HashMap<>();
        boolean hasCascadeRelationField = false;

        for (Map.Entry<String, FieldValue<?>> entry : request.fieldValues().entrySet()) {
            if (entry.getValue() instanceof CascadeFieldValue cascadeFieldValue) {
                hasCascadeRelationField = true;
                cascadeFieldLinkSyncService.applyCascadeFieldValue(
                        String.valueOf(request.cardId().value()),
                        existingCard.getTypeId().value(),
                        cascadeFieldValue,
                        orgId, String.valueOf(operatorId.value()), sourceIp,
                        linkCardService::updateLink);
                logger.debug("架构属性已转换为关联更新: cardId={}, fieldId={}", request.cardId(), entry.getKey());
            } else {
                filteredFieldValues.put(entry.getKey(), entry.getValue());
            }
        }

        if (hasCascadeRelationField) {
            return new UpdateCardRequest(
                    request.cardId(),
                    request.title(),
                    request.description(),
                    filteredFieldValues.isEmpty() ? null : filteredFieldValues);
        }
        return request;
    }

    // ==================== 属性值校验辅助方法 ====================

    /**
     * 获取__PLANKA_EINST__的所有属性配置
     */
    private List<FieldConfig> getFieldConfigsForCardType(CardTypeId typeId) {
        Result<FieldConfigListWithSource> result =
                fieldConfigQueryService.getFieldConfigListWithSource(typeId.value());
        if (!result.isSuccess() || result.getData() == null) {
            logger.warn("获取__PLANKA_EINST__属性配置失败: typeId={}, error={}", typeId, result.getMessage());
            return Collections.emptyList();
        }
        return result.getData().getFields();
    }

    /**
     * 将创建请求转换为临时 CardDTO 用于校验
     */
    private CardDTO buildTempCardForValidation(CreateCardRequest request) {
        CardDTO tempCard = new CardDTO();
        tempCard.setTypeId(request.typeId());
        tempCard.setTitle(request.title());
        tempCard.setDescription(request.description() != null ? CardDescription.of(request.description()) : null);
        tempCard.setFieldValues(request.fieldValues() != null ? new HashMap<>(request.fieldValues()) : new HashMap<>());
        return tempCard;
    }

    /**
     * 合并现有卡片和更新请求，用于校验
     */
    private CardDTO mergeCardForValidation(CardDTO existingCard, UpdateCardRequest request) {
        CardDTO mergedCard = new CardDTO();
        mergedCard.setId(existingCard.getId());
        mergedCard.setTypeId(existingCard.getTypeId());
        mergedCard.setTitle(request.title() != null ? CardTitle.pure(request.title()) : existingCard.getTitle());
        mergedCard.setDescription(request.description() != null ? CardDescription.of(request.description()) : existingCard.getDescription());

        // 合并属性值：先复制现有的，再用更新请求覆盖
        Map<String, FieldValue<?>> mergedFieldValues = new HashMap<>();
        if (existingCard.getFieldValues() != null) {
            mergedFieldValues.putAll(existingCard.getFieldValues());
        }
        if (request.fieldValues() != null) {
            mergedFieldValues.putAll(request.fieldValues());
        }
        mergedCard.setFieldValues(mergedFieldValues);

        return mergedCard;
    }

    /**
     * 过滤出本次更新涉及的属性配置
     */
    private List<FieldConfig> filterChangedFieldConfigs(List<FieldConfig> allConfigs, UpdateCardRequest request) {
        if (request.fieldValues() == null || request.fieldValues().isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> changedFieldIds = request.fieldValues().keySet();
        return allConfigs.stream()
                .filter(config -> changedFieldIds.contains(config.getFieldId().value()))
                .collect(Collectors.toList());
    }
}
