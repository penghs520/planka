package dev.planka.card.service.core;

import dev.planka.api.card.dto.BatchOperationResult;
import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.schema.service.FieldConfigQueryService;
import dev.planka.card.converter.StructureFieldValueBuilder;
import dev.planka.card.event.CardEventPublisher;
import dev.planka.card.model.CardEntity;
import dev.planka.card.repository.CardRepository;
import dev.planka.card.service.YieldEnhancer;
import dev.planka.card.service.permission.CardPermissionService;
import dev.planka.card.service.permission.model.BatchPermissionCheckResult;
import dev.planka.card.service.structure.StructureLinkSyncService;
import dev.planka.card.service.validation.FieldValueValidator;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.card.CardDescription;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.field.StructureFieldValue;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.fieldconfig.StructureFieldConfig;
import dev.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import dev.planka.event.card.CardMovedEvent;
import dev.planka.api.card.request.*;
import dev.planka.api.schema.dto.inheritance.FieldConfigListWithSource;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import planka.graph.driver.proto.write.BatchCardCommonResponse;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 卡片服务实现
 */
@Service
public class CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cardRepository;
    private final YieldEnhancer yieldEnhancer;
    private final StructureLinkSyncService structureLinkSyncService;
    private final LinkCardService linkCardService;
    private final CardPermissionService permissionService;
    private final CardEventPublisher eventPublisher;
    private final CardEntityConverter entityConverter;
    private final FieldConfigQueryService fieldConfigQueryService;
    private final FieldValueValidator fieldValueValidator;

    public CardService(CardRepository cardRepository,
                       YieldEnhancer yieldEnhancer,
                       StructureLinkSyncService structureLinkSyncService,
                       LinkCardService linkCardService,
                       CardPermissionService permissionService,
                       CardEventPublisher eventPublisher,
                       CardEntityConverter entityConverter,
                       FieldConfigQueryService fieldConfigQueryService,
                       FieldValueValidator fieldValueValidator) {
        this.cardRepository = cardRepository;
        this.yieldEnhancer = yieldEnhancer;
        this.structureLinkSyncService = structureLinkSyncService;
        this.linkCardService = linkCardService;
        this.permissionService = permissionService;
        this.eventPublisher = eventPublisher;
        this.entityConverter = entityConverter;
        this.fieldConfigQueryService = fieldConfigQueryService;
        this.fieldValueValidator = fieldValueValidator;
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

            CardEntityConverter.FilteredCreateRequest filtered = entityConverter.filterStructureFieldValues(request);
            CardEntity cardEntity = entityConverter.toCardEntityForCreate(filtered.request());
            CardId cardId = cardRepository.create(cardEntity);

            applyStructureFieldValuesForCreate(cardId, request, filtered.structureFieldValues(), operatorId, sourceIp);
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

            return Result.success(cardId);
        } catch (Exception e) {
            logger.error("创建卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "创建卡片失败: " + e.getMessage());
        }
    }

    public Result<Void> update(UpdateCardRequest request, CardId operatorId) {
        return update(request, null, operatorId, null);
    }

    public Result<Void> update(UpdateCardRequest request, String orgId, CardId operatorId, String sourceIp) {
        try {
            permissionService.checkCardOperation(CardOperation.EDIT, request.cardId(), operatorId);
            checkFieldEditPermission(request, operatorId);

            Optional<CardDTO> existingCardOpt = cardRepository.findById(
                    request.cardId(), Yield.all(), String.valueOf(operatorId.value()));
            if (existingCardOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.NOT_FOUND, "卡片不存在: " + request.cardId().value());
            }
            CardDTO existingCard = existingCardOpt.get();
            String effectiveOrgId = orgId != null ? orgId : existingCard.getOrgId().value();

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

            UpdateCardRequest filteredRequest = filterStructureFieldValues(
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
            logger.error("丢弃卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "丢弃卡片失败: " + e.getMessage());
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
            logger.error("归档卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "归档卡片失败: " + e.getMessage());
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
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "还原卡片失败: " + e.getMessage());
        }
    }

    // ==================== 批量操作 ====================

    public Result<BatchOperationResult> batchCreate(List<CreateCardRequest> requests, CardId operatorId) {
        try {
            List<CardEntity> cardEntities = requests.stream()
                    .map(entityConverter::filterStructureFieldValues)
                    .map(filtered -> entityConverter.toCardEntityForCreate(filtered.request()))
                    .toList();
            List<CardId> successIds = cardRepository.batchCreate(cardEntities);
            return Result.success(BatchOperationResult.success(successIds));
        } catch (Exception e) {
            logger.error("批量创建卡片失败", e);
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
                return Result.failure(CommonErrorCode.PERMISSION_DENIED, "无权限丢弃任何卡片");
            }

            List<CardId> allowedCardIds = checkResult.getAllowed();
            List<CardDTO> cards = cardRepository.findByIds(allowedCardIds, Yield.basic(), String.valueOf(operatorId.value()));

            cardRepository.batchDiscard(allowedCardIds, discardReason, String.valueOf(operatorId.value()));
            eventPublisher.publishAllAbandoned(cards, String.valueOf(operatorId.value()), sourceIp, discardReason);
            return Result.success();
        } catch (Exception e) {
            logger.error("批量丢弃卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量丢弃卡片失败: " + e.getMessage());
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
                return Result.failure(CommonErrorCode.PERMISSION_DENIED, "无权限归档任何卡片");
            }

            List<CardId> allowedCardIds = checkResult.getAllowed();
            List<CardDTO> cards = cardRepository.findByIds(allowedCardIds, Yield.basic(), String.valueOf(operatorId.value()));

            cardRepository.batchArchive(allowedCardIds, String.valueOf(operatorId.value()));
            eventPublisher.publishAllArchived(cards, String.valueOf(operatorId.value()), sourceIp);
            return Result.success();
        } catch (Exception e) {
            logger.error("批量归档卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量归档卡片失败: " + e.getMessage());
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
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量移动卡片失败: " + e.getMessage());
        }
    }

    // ==================== 读操作 ====================

    public Result<CardDTO> findById(CardId cardId, Yield yield, CardId operatorId) {
        try {
            Yield enhancedYield = yieldEnhancer.enhance(yield);
            Optional<CardDTO> cardOpt = cardRepository.findById(cardId, enhancedYield, String.valueOf(operatorId.value()));
            if (cardOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.NOT_FOUND, "卡片不存在");
            }
            CardDTO cardDTO = cardOpt.get();
            fillStructureFieldValues(cardDTO, yield);
            return Result.success(cardDTO);
        } catch (Exception e) {
            logger.error("查询卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "查询卡片失败: " + e.getMessage());
        }
    }

    public Result<List<CardDTO>> findByIds(List<CardId> cardIds, Yield yield, CardId operatorId) {
        try {
            Yield enhancedYield = yieldEnhancer.enhance(yield);
            List<CardDTO> cards = cardRepository.findByIds(cardIds, enhancedYield, String.valueOf(operatorId.value()));
            fillStructureFieldValuesForList(cards, yield);
            return Result.success(cards);
        } catch (Exception e) {
            logger.error("批量查询卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量查询卡片失败: " + e.getMessage());
        }
    }

    public Result<List<CardDTO>> query(CardQueryRequest request) {
        try {
            Yield originalYield = request.getYield();
            request.setYield(yieldEnhancer.enhance(originalYield));
            List<CardDTO> cards = cardRepository.query(request);
            fillStructureFieldValuesForList(cards, originalYield);
            return Result.success(cards);
        } catch (Exception e) {
            logger.error("查询卡片列表失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "查询卡片列表失败: " + e.getMessage());
        }
    }

    public Result<PageResult<CardDTO>> pageQuery(CardPageQueryRequest request) {
        try {
            Yield originalYield = request.getYield();
            request.setYield(yieldEnhancer.enhance(originalYield));
            PageResult<CardDTO> result = cardRepository.pageQuery(request);
            if (result.getContent() != null) {
                fillStructureFieldValuesForList(result.getContent(), originalYield);
            }
            return Result.success(result);
        } catch (Exception e) {
            logger.error("分页查询卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "分页查询卡片失败: " + e.getMessage());
        }
    }

    public Result<List<String>> queryIds(CardIdQueryRequest request) {
        try {
            List<String> ids = cardRepository.queryIds(request);
            return Result.success(ids);
        } catch (Exception e) {
            logger.error("查询卡片ID失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "查询卡片ID失败: " + e.getMessage());
        }
    }

    public Result<Integer> count(CardCountRequest request) {
        try {
            Integer count = cardRepository.count(request);
            return Result.success(count);
        } catch (Exception e) {
            logger.error("统计卡片数量失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "统计卡片数量失败: " + e.getMessage());
        }
    }

    public Result<Map<String, CardTitle>> queryCardNames(List<String> cardIds, CardId operatorId) {
        try {
            if (cardIds == null || cardIds.isEmpty()) {
                return Result.success(Map.of());
            }
            Map<String, CardTitle> nameMap = cardRepository.queryCardNames(cardIds, String.valueOf(operatorId.value()));
            return Result.success(nameMap);
        } catch (Exception e) {
            logger.error("批量查询卡片名称失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量查询卡片名称失败: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    private void applyStructureFieldValuesForCreate(CardId cardId, CreateCardRequest request,
                                                    Map<String, StructureFieldValue> structureFieldValues,
                                                    CardId operatorId, String sourceIp) {
        if (structureFieldValues.isEmpty()) {
            return;
        }
        for (StructureFieldValue structureValue : structureFieldValues.values()) {
            structureLinkSyncService.applyStructureFieldValue(
                    String.valueOf(cardId.value()),
                    request.typeId().value(),
                    structureValue,
                    request.orgId().value(),
                    String.valueOf(operatorId.value()),
                    sourceIp,
                    linkCardService::updateLink);
            logger.debug("创建卡片时应用架构属性: cardId={}, fieldId={}", cardId, structureValue.getFieldId());
        }
    }

    private void checkFieldEditPermission(UpdateCardRequest request, CardId operatorId) {
        if (request.fieldValues() == null || request.fieldValues().isEmpty()) {
            return;
        }
        Set<FieldId> changedFieldIds = request.fieldValues().keySet().stream()
                .map(FieldId::of)
                .collect(Collectors.toSet());
        permissionService.checkFieldEditPermission(request.cardId(), operatorId, changedFieldIds);
    }

    private UpdateCardRequest filterStructureFieldValues(UpdateCardRequest request, CardDTO existingCard,
                                                         String orgId, CardId operatorId, String sourceIp) {
        if (request.fieldValues() == null || request.fieldValues().isEmpty()) {
            return request;
        }

        Map<String, FieldValue<?>> filteredFieldValues = new HashMap<>();
        boolean hasStructureField = false;

        for (Map.Entry<String, FieldValue<?>> entry : request.fieldValues().entrySet()) {
            if (entry.getValue() instanceof StructureFieldValue structureValue) {
                hasStructureField = true;
                structureLinkSyncService.applyStructureFieldValue(
                        String.valueOf(request.cardId().value()),
                        existingCard.getTypeId().value(),
                        structureValue,
                        orgId, String.valueOf(operatorId.value()), sourceIp,
                        linkCardService::updateLink);
                logger.debug("架构属性已转换为关联更新: cardId={}, fieldId={}", request.cardId(), entry.getKey());
            } else {
                filteredFieldValues.put(entry.getKey(), entry.getValue());
            }
        }

        if (hasStructureField) {
            return new UpdateCardRequest(
                    request.cardId(),
                    request.title(),
                    request.description(),
                    filteredFieldValues.isEmpty() ? null : filteredFieldValues);
        }
        return request;
    }

    private Result<BatchOperationResult> buildBatchUpdateResult(List<UpdateCardRequest> requests,
                                                                BatchCardCommonResponse response) {
        if (CollectionUtils.isEmpty(response.getFailedIdsList())) {
            return Result.success(BatchOperationResult.success(
                    response.getFailedIdsList().stream()
                            .map(id -> CardId.of(String.valueOf(id)))
                            .toList()));
        }

        List<CardId> successIds = requests.stream()
                .map(UpdateCardRequest::cardId)
                .filter(cardId -> !response.getFailedIdsList().contains(cardId.value()))
                .toList();

        if (CollectionUtils.isEmpty(successIds)) {
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量更新卡片全部失败");
        }

        BatchOperationResult result = BatchOperationResult.success(successIds);
        result.setFailedIds(response.getFailedIdsList().stream()
                .map(id -> CardId.of(String.valueOf(id)))
                .toList());
        return Result.success(result);
    }

    private void fillStructureFieldValues(CardDTO cardDTO, Yield yield) {
        if (yield == null || cardDTO == null) {
            return;
        }
        List<StructureFieldConfig> structureDefs = yieldEnhancer.extractStructureFieldDefs(yield);
        fillStructureFieldValuesRecursive(cardDTO, yield, structureDefs);
    }

    private void fillStructureFieldValuesForList(List<CardDTO> cards, Yield yield) {
        if (yield == null || cards == null || cards.isEmpty()) {
            return;
        }
        List<StructureFieldConfig> structureDefs = yieldEnhancer.extractStructureFieldDefs(yield);
        for (CardDTO cardDTO : cards) {
            fillStructureFieldValuesRecursive(cardDTO, yield, structureDefs);
        }
    }

    private void fillStructureFieldValuesRecursive(CardDTO cardDTO, Yield yield,
                                                   List<StructureFieldConfig> structureDefs) {
        if (yield == null || cardDTO == null) {
            return;
        }

        if (!structureDefs.isEmpty()) {
            Map<String, FieldValue<?>> structureValues = StructureFieldValueBuilder.buildAll(cardDTO, structureDefs);
            if (!structureValues.isEmpty()) {
                if (cardDTO.getFieldValues() == null) {
                    cardDTO.setFieldValues(new HashMap<>());
                }
                cardDTO.getFieldValues().putAll(structureValues);
            }
        }

        if (yield.getLinks() != null && cardDTO.getLinkedCards() != null) {
            for (YieldLink link : yield.getLinks()) {
                if (link.getTargetYield() == null) {
                    continue;
                }
                Set<CardDTO> linkedCards = cardDTO.getLinkedCards().get(link.getLinkFieldId());
                if (linkedCards != null) {
                    for (CardDTO linkedCard : linkedCards) {
                        fillStructureFieldValuesRecursive(linkedCard, link.getTargetYield(), structureDefs);
                    }
                }
            }
        }
    }

    // ==================== 属性值校验辅助方法 ====================

    /**
     * 获取卡片类型的所有属性配置
     */
    private List<FieldConfig> getFieldConfigsForCardType(CardTypeId typeId) {
        Result<FieldConfigListWithSource> result =
                fieldConfigQueryService.getFieldConfigListWithSource(typeId.value());
        if (!result.isSuccess() || result.getData() == null) {
            logger.warn("获取卡片类型属性配置失败: typeId={}, error={}", typeId, result.getMessage());
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
