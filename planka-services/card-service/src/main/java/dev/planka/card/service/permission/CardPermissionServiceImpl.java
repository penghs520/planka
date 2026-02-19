package dev.planka.card.service.permission;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.Yield;
import dev.planka.card.repository.CardRepository;
import dev.planka.card.service.permission.exception.PermissionDeniedException;
import dev.planka.card.service.permission.model.BatchPermissionCheckResult;
import dev.planka.card.service.permission.model.FieldValueWithPermission;
import dev.planka.card.service.permission.model.PermissionDeniedItem;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.permission.PermissionConfig;
import dev.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import dev.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import dev.planka.infra.cache.card.CardCacheService;
import dev.planka.infra.cache.card.model.CardBasicInfo;
import dev.planka.infra.expression.TextExpressionTemplateResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 卡片权限服务实现
 * <p>
 * 核心实现要点：
 * <ul>
 *     <li>使用 CardCacheService 获取卡片基础信息</li>
 *     <li>使用 PermissionConfigCacheService 获取权限配置</li>
 *     <li>使用 ConditionEvaluator 评估条件</li>
 *     <li>使用 YieldBuilder 构建查询 Yield</li>
 *     <li>使用 CardRepository 查询完整数据</li>
 *     <li>实现卫语句快速路径（无权限配置时直接返回）</li>
 *     <li>实现批量分组优化</li>
 * </ul>
 */
@Slf4j
@Service
public class CardPermissionServiceImpl implements CardPermissionService {

    private final CardCacheService cardCacheService;
    private final PermissionConfigCacheService permissionConfigCacheService;
    private final ConditionEvaluator conditionEvaluator;
    private final YieldBuilder yieldBuilder;
    private final CardRepository cardRepository;
    private final TextExpressionTemplateResolver templateResolver;

    public CardPermissionServiceImpl(
            CardCacheService cardCacheService,
            PermissionConfigCacheService permissionConfigCacheService,
            ConditionEvaluator conditionEvaluator,
            YieldBuilder yieldBuilder,
            CardRepository cardRepository,
            TextExpressionTemplateResolver templateResolver) {
        this.cardCacheService = cardCacheService;
        this.permissionConfigCacheService = permissionConfigCacheService;
        this.conditionEvaluator = conditionEvaluator;
        this.yieldBuilder = yieldBuilder;
        this.cardRepository = cardRepository;
        this.templateResolver = templateResolver;
    }

    @Override
    public void checkCardOperation(
            CardOperation operation,
            CardId targetCardId,
            CardId operatorId) {

        log.debug("检查卡片操作权限: operation={}, targetCardId={}, operatorId={}",
                operation, targetCardId, operatorId);

        // 1. 获取卡片基础信息
        Optional<CardBasicInfo> basicInfoOpt = cardCacheService.getBasicInfoById(targetCardId);
        if (basicInfoOpt.isEmpty()) {
            throw new IllegalArgumentException("卡片不存在: " + targetCardId.value());
        }

        CardBasicInfo basicInfo = basicInfoOpt.get();

        // 2. 卫语句 - 检查是否有权限配置
        if (!permissionConfigCacheService.hasPermissionConfig(basicInfo.cardTypeId(), operation)) {
            log.debug("无权限配置，直接允许");
            return;
        }

        // 3. 收集权限配置
        List<PermissionConfigDefinition> configs = permissionConfigCacheService.getPermissionConfigs(
                basicInfo.cardTypeId()
        );

        // 4. 构建 Yield
        Yield cardYield = yieldBuilder.buildCardYield(configs, operation);
        Yield memberYield = yieldBuilder.buildMemberYield(configs, operation);

        // 5. 查询完整数据
        CardDTO targetCard = cardRepository.findById(targetCardId, cardYield, "system")
                .orElseThrow(() -> new IllegalStateException("卡片不存在: " + targetCardId.value()));

        CardDTO memberCard = cardRepository.findById(operatorId, memberYield, "system")
                .orElseThrow(() -> new IllegalStateException("操作人卡片不存在: " + operatorId.value()));

        // 6. 评估权限
        boolean hasPermission = evaluatePermission(configs, operation, targetCard, memberCard);

        if (!hasPermission) {
            String alertMessage = extractAlertMessage(configs, operation, targetCardId, operatorId);
            throw PermissionDeniedException.cardOperation(operation, targetCardId, alertMessage);
        }

        log.debug("权限校验通过");
    }

    @Override
    public void checkCardOperationForCreate(
            CardOperation operation,
            CardTypeId cardTypeId,
            CardId operatorId) {

        log.debug("检查创建卡片权限: operation={}, cardTypeId={}, operatorId={}",
                operation, cardTypeId, operatorId);

        // 1. 卫语句 - 检查是否有组织级权限配置
        if (!permissionConfigCacheService.hasPermissionConfig(cardTypeId, operation)) {
            log.debug("无组织级权限配置，直接允许");
            return;
        }

        // 2. 收集组织级权限配置
        List<PermissionConfigDefinition> configs = permissionConfigCacheService.getPermissionConfigs(cardTypeId);

        // 3. 构建成员卡片 Yield（仅操作人条件）
        Yield memberYield = yieldBuilder.buildMemberYield(configs, operation);

        // 4. 查询操作人卡片
        CardDTO memberCard = cardRepository.findById(operatorId, memberYield, "system")
                .orElseThrow(() -> new IllegalStateException("操作人卡片不存在: " + operatorId.value()));

        // 5. 评估操作人条件（创建时仅评估操作人条件）
        boolean hasPermission = evaluateOperatorConditions(configs, operation, memberCard);

        if (!hasPermission) {
            String alertMessage = extractAlertMessage(configs, operation, null, operatorId);
            String cardTypeName = cardTypeId.value(); // TODO: 从缓存获取卡片类型名称
            throw PermissionDeniedException.cardCreate(cardTypeName, alertMessage);
        }

        log.debug("创建权限校验通过");
    }

    @Override
    public BatchPermissionCheckResult batchCheckCardOperation(
            CardOperation operation,
            List<CardId> targetCardIds,
            CardId operatorId) {

        log.debug("批量检查卡片操作权限: operation={}, cardCount={}, operatorId={}",
                operation, targetCardIds.size(), operatorId);

        if (targetCardIds.isEmpty()) {
            return BatchPermissionCheckResult.empty();
        }

        // 1. 通过卡片缓存快速获取基础信息
        Map<CardId, CardBasicInfo> basicInfos = cardCacheService
                .getBasicInfoByIds(new HashSet<>(targetCardIds));

        // 收集涉及的卡片类型
        Set<CardTypeId> cardTypeIds = basicInfos.values().stream()
                .map(CardBasicInfo::cardTypeId)
                .collect(Collectors.toSet());

        // 2. 卫语句 - 检查是否有任何权限配置
        if (!hasAnyPermissionConfigForBatch(cardTypeIds, operation)) {
            log.debug("无权限配置，全部允许");
            return BatchPermissionCheckResult.allAllowed(targetCardIds);
        }

        // 3. 有权限配置，执行完整校验逻辑
        // 收集所有相关的权限配置
        List<PermissionConfigDefinition> allConfigs = collectAllPermissionConfigs(cardTypeIds);

        // 构建 Yield
        Yield cardYield = yieldBuilder.buildCardYield(allConfigs, operation);
        Yield memberYield = yieldBuilder.buildMemberYield(allConfigs, operation);

        // 查询完整数据
        Map<CardId, CardDTO> targetCards = cardRepository
                .findByIds(targetCardIds, cardYield, "system")
                .stream()
                .collect(Collectors.toMap(CardDTO::getId, dto -> dto));

        CardDTO memberCard = cardRepository
                .findById(operatorId, memberYield, "system")
                .orElseThrow(() -> new IllegalStateException("操作人卡片不存在: " + operatorId.value()));

        // 4. 按卡片类型分组校验
        List<CardId> allowed = new ArrayList<>();
        List<PermissionDeniedItem> denied = new ArrayList<>();

        // 按 CardTypeId 分组
        Map<CardTypeId, List<CardId>> groups = targetCardIds.stream()
                .filter(basicInfos::containsKey)
                .collect(Collectors.groupingBy(cardId -> basicInfos.get(cardId).cardTypeId()));

        // 逐组评估
        for (Map.Entry<CardTypeId, List<CardId>> entry : groups.entrySet()) {
            evaluateGroupPermission(
                    entry.getKey(),
                    entry.getValue(),
                    targetCards,
                    memberCard,
                    operation,
                    allowed,
                    denied
            );
        }

        log.debug("批量权限校验完成: allowed={}, denied={}", allowed.size(), denied.size());
        return new BatchPermissionCheckResult(allowed, denied);
    }

    @Override
    public void checkFieldEditPermission(
            CardId targetCardId,
            CardId operatorId,
            Set<FieldId> changedFieldIds) {

        log.debug("检查属性编辑权限: targetCardId={}, operatorId={}, fieldCount={}",
                targetCardId, operatorId, changedFieldIds.size());

        if (changedFieldIds.isEmpty()) {
            return;
        }

        // 1. 获取卡片基础信息
        Optional<CardBasicInfo> basicInfoOpt = cardCacheService.getBasicInfoById(targetCardId);
        if (basicInfoOpt.isEmpty()) {
            throw new IllegalArgumentException("卡片不存在: " + targetCardId.value());
        }

        CardBasicInfo basicInfo = basicInfoOpt.get();

        // 2. 收集权限配置
        List<PermissionConfigDefinition> configs = permissionConfigCacheService.getPermissionConfigs(
                basicInfo.cardTypeId()
        );

        // 3. 检查是否有属性级权限配置
        if (!hasFieldPermissionConfig(configs)) {
            log.debug("无属性级权限配置，直接允许");
            return;
        }

        // 4. 构建 Yield
        Yield cardYield = yieldBuilder.buildCardYield(configs, CardOperation.EDIT);
        Yield memberYield = yieldBuilder.buildMemberYield(configs, CardOperation.EDIT);

        // 5. 查询完整数据
        CardDTO targetCard = cardRepository.findById(targetCardId, cardYield, "system")
                .orElseThrow(() -> new IllegalStateException("卡片不存在: " + targetCardId.value()));

        CardDTO memberCard = cardRepository.findById(operatorId, memberYield, "system")
                .orElseThrow(() -> new IllegalStateException("操作人卡片不存在: " + operatorId.value()));

        // 6. 评估属性编辑权限
        Set<FieldId> deniedFields = new HashSet<>();
        String alertMessage = null;

        for (FieldId fieldId : changedFieldIds) {
            if (!hasFieldEditPermission(configs, fieldId, targetCard, memberCard)) {
                deniedFields.add(fieldId);
                if (alertMessage == null) {
                    alertMessage = extractFieldAlertMessage(configs, fieldId, targetCardId, operatorId);
                }
            }
        }

        if (!deniedFields.isEmpty()) {
            throw PermissionDeniedException.fieldEdit(targetCardId, deniedFields, alertMessage);
        }

        log.debug("属性编辑权限校验通过");
    }

    @Override
    public List<FieldValueWithPermission> applyFieldPermissions(
            CardDTO card,
            CardId operatorId) {

        log.debug("应用属性权限: cardId={}, operatorId={}", card.getId(), operatorId);

        // TODO: 实现属性权限应用逻辑
        // 1. 收集权限配置
        // 2. 评估每个属性的权限（READ, DESENSITIZED_READ, NO_PERMISSION）
        // 3. 返回带权限状态的属性值列表

        return new ArrayList<>();
    }

    // ========== 辅助方法 ==========

    /**
     * 检查是否有任何权限配置（批量场景，多个卡片类型）
     */
    private boolean hasAnyPermissionConfigForBatch(
            Set<CardTypeId> cardTypeIds,
            CardOperation operation) {

        // 检查组织级配置
        for (CardTypeId cardTypeId : cardTypeIds) {
            if (permissionConfigCacheService.hasPermissionConfig(cardTypeId, operation)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 收集所有权限配置（多个卡片类型）
     */
    private List<PermissionConfigDefinition> collectAllPermissionConfigs(
            Set<CardTypeId> cardTypeIds) {

        List<PermissionConfigDefinition> configs = new ArrayList<>();

        // 组织级配置
        for (CardTypeId cardTypeId : cardTypeIds) {
            configs.addAll(permissionConfigCacheService.getPermissionConfigs(cardTypeId));
        }

        return configs;
    }

    /**
     * 评估权限（卡片条件 AND 操作人条件）
     * <p>
     * 采用白名单模式：满足任一卡片条件 AND 满足任一操作人条件 → 有权限
     */
    private boolean evaluatePermission(
            List<PermissionConfigDefinition> configs,
            CardOperation operation,
            CardDTO targetCard,
            CardDTO memberCard) {

        for (PermissionConfigDefinition config : configs) {
            if (config.getCardOperations() == null) {
                continue;
            }

            for (PermissionConfig.CardOperationPermission cardOp : config.getCardOperations()) {
                if (cardOp.getOperation() != operation) {
                    continue;
                }

                // 评估卡片条件（OR 关系）
                boolean cardConditionMet = evaluateCardConditions(
                        cardOp.getCardConditions(),
                        targetCard,
                        memberCard
                );

                // 评估操作人条件（OR 关系）
                boolean operatorConditionMet = evaluateOperatorConditions(
                        cardOp.getOperatorConditions(),
                        memberCard
                );

                // 卡片条件 AND 操作人条件
                if (cardConditionMet && operatorConditionMet) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 评估卡片条件（OR 关系）
     */
    private boolean evaluateCardConditions(
            List<Condition> conditions,
            CardDTO targetCard,
            CardDTO memberCard) {

        if (conditions == null || conditions.isEmpty()) {
            return true; // 无条件限制，默认满足
        }

        for (Condition condition : conditions) {
            if (conditionEvaluator.evaluate(condition, targetCard, memberCard)) {
                return true; // 满足任一条件即可
            }
        }

        return false;
    }

    /**
     * 评估操作人条件（OR 关系）
     */
    private boolean evaluateOperatorConditions(
            List<Condition> conditions,
            CardDTO memberCard) {

        if (conditions == null || conditions.isEmpty()) {
            return true; // 无条件限制，默认满足
        }

        for (Condition condition : conditions) {
            if (conditionEvaluator.evaluate(condition, memberCard)) {
                return true; // 满足任一条件即可
            }
        }

        return false;
    }

    /**
     * 评估操作人条件（仅操作人条件，用于创建卡片）
     */
    private boolean evaluateOperatorConditions(
            List<PermissionConfigDefinition> configs,
            CardOperation operation,
            CardDTO memberCard) {

        for (PermissionConfigDefinition config : configs) {
            if (config.getCardOperations() == null) {
                continue;
            }

            for (PermissionConfig.CardOperationPermission cardOp : config.getCardOperations()) {
                if (cardOp.getOperation() != operation) {
                    continue;
                }

                // 评估操作人条件（OR 关系）
                if (evaluateOperatorConditions(cardOp.getOperatorConditions(), memberCard)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 提取权限拒绝提示信息
     */
    private String extractAlertMessage(
            List<PermissionConfigDefinition> configs,
            CardOperation operation,
            CardId targetCardId,
            CardId operatorId) {

        for (PermissionConfigDefinition config : configs) {
            if (config.getCardOperations() == null) {
                continue;
            }

            for (PermissionConfig.CardOperationPermission cardOp : config.getCardOperations()) {
                if (cardOp.getOperation() == operation && cardOp.getAlertMessage() != null) {
                    return templateResolver.resolve(cardOp.getAlertMessage(), targetCardId, operatorId);
                }
            }
        }

        return null;
    }

    /**
     * 分组评估权限
     * <p>
     * 对同一组（相同卡片类型）的卡片进行批量评估
     */
    private void evaluateGroupPermission(
            CardTypeId cardTypeId,
            List<CardId> cardIds,
            Map<CardId, CardDTO> targetCards,
            CardDTO memberCard,
            CardOperation operation,
            List<CardId> allowed,
            List<PermissionDeniedItem> denied) {

        // 收集该组的权限配置
        List<PermissionConfigDefinition> configs = permissionConfigCacheService.getPermissionConfigs(
                cardTypeId
        );

        CardId memberCardId = memberCard.getId();

        // 逐个评估
        for (CardId cardId : cardIds) {
            CardDTO targetCard = targetCards.get(cardId);
            if (targetCard == null) {
                denied.add(new PermissionDeniedItem(cardId, "卡片不存在"));
                continue;
            }

            boolean hasPermission = evaluatePermission(configs, operation, targetCard, memberCard);
            if (hasPermission) {
                allowed.add(cardId);
            } else {
                String alertMessage = extractAlertMessage(configs, operation, cardId, memberCardId);
                denied.add(new PermissionDeniedItem(cardId, alertMessage));
            }
        }
    }

    /**
     * 检查是否有属性级权限配置
     */
    private boolean hasFieldPermissionConfig(List<PermissionConfigDefinition> configs) {
        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() != null && !config.getFieldPermissions().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查属性编辑权限
     */
    private boolean hasFieldEditPermission(
            List<PermissionConfigDefinition> configs,
            FieldId fieldId,
            CardDTO targetCard,
            CardDTO memberCard) {

        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }

            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                // 检查是否是编辑操作
                if (fieldPerm.getOperation() != PermissionConfig.FieldOperation.EDIT) {
                    continue;
                }

                // 检查是否包含该属性
                if (fieldPerm.getFieldIds() == null || !fieldPerm.getFieldIds().contains(fieldId)) {
                    continue;
                }

                // 评估卡片条件
                boolean cardConditionMet = evaluateCardConditions(
                        fieldPerm.getCardConditions(),
                        targetCard,
                        memberCard
                );

                // 评估操作人条件
                boolean operatorConditionMet = evaluateOperatorConditions(
                        fieldPerm.getOperatorConditions(),
                        memberCard
                );

                // 卡片条件 AND 操作人条件
                if (cardConditionMet && operatorConditionMet) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 提取属性权限拒绝提示信息
     */
    private String extractFieldAlertMessage(
            List<PermissionConfigDefinition> configs,
            FieldId fieldId,
            CardId targetCardId,
            CardId operatorId) {

        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }

            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                if (fieldPerm.getFieldIds() != null &&
                        fieldPerm.getFieldIds().contains(fieldId) &&
                        fieldPerm.getAlertMessage() != null) {
                    return templateResolver.resolve(fieldPerm.getAlertMessage(), targetCardId, operatorId);
                }
            }
        }

        return null;
    }
}
