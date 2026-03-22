package cn.planka.card.service.permission;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.Yield;
import cn.planka.card.repository.CardRepository;
import cn.planka.card.service.evaluator.ConditionEvaluator;
import cn.planka.card.service.permission.exception.PermissionDeniedException;
import cn.planka.card.service.permission.model.BatchPermissionCheckResult;
import cn.planka.card.service.permission.model.PermissionDeniedItem;
import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.field.FieldId;
import cn.planka.domain.field.FieldPermissionStatus;
import cn.planka.domain.field.FieldValue;
import cn.planka.domain.link.LinkFieldIdUtils;
import cn.planka.domain.link.LinkPosition;
import cn.planka.domain.link.LinkTypeId;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.condition.Condition;
import cn.planka.domain.schema.definition.fieldconfig.FieldConfig;
import cn.planka.domain.schema.definition.link.LinkTypeDefinition;
import cn.planka.domain.schema.definition.permission.PermissionConfig;
import cn.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import cn.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import cn.planka.infra.cache.card.CardCacheService;
import cn.planka.infra.cache.card.model.CardBasicInfo;
import cn.planka.infra.cache.schema.SchemaCacheService;
import cn.planka.infra.cache.schema.query.CardTypeCacheQuery;
import cn.planka.infra.expression.TextExpressionTemplateResolver;
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
    private final PermissionConfigYieldBuilder permissionConfigYieldBuilder;
    private final CardRepository cardRepository;
    private final TextExpressionTemplateResolver templateResolver;
    private final CardTypeCacheQuery cardTypeCacheQuery;
    private final SchemaCacheService schemaCacheService;

    public CardPermissionServiceImpl(
            CardCacheService cardCacheService,
            PermissionConfigCacheService permissionConfigCacheService,
            ConditionEvaluator conditionEvaluator,
            PermissionConfigYieldBuilder permissionConfigYieldBuilder,
            CardRepository cardRepository,
            TextExpressionTemplateResolver templateResolver,
            CardTypeCacheQuery cardTypeCacheQuery,
            SchemaCacheService schemaCacheService) {
        this.cardCacheService = cardCacheService;
        this.permissionConfigCacheService = permissionConfigCacheService;
        this.conditionEvaluator = conditionEvaluator;
        this.permissionConfigYieldBuilder = permissionConfigYieldBuilder;
        this.cardRepository = cardRepository;
        this.templateResolver = templateResolver;
        this.cardTypeCacheQuery = cardTypeCacheQuery;
        this.schemaCacheService = schemaCacheService;
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
        if (!permissionConfigCacheService.hasCardOperationPermissionConfig(basicInfo.cardTypeId(), operation)) {
            log.debug("无权限配置，直接允许");
            return;
        }

        // 3. 收集权限配置
        List<PermissionConfigDefinition> configs = permissionConfigCacheService.getPermissionConfigs(
                basicInfo.cardTypeId()
        );

        // 4. 构建 Yield
        Yield cardYield = permissionConfigYieldBuilder.buildCardYield(configs, operation);
        Yield memberYield = permissionConfigYieldBuilder.buildMemberYield(configs, operation);

        // 5. 查询完整数据
        CardDTO targetCard = cardRepository.findById(targetCardId, cardYield, "system")
                .orElseThrow(() -> new IllegalStateException("卡片不存在: " + targetCardId.value()));

        CardDTO memberCard = cardRepository.findById(operatorId, memberYield, "system")
                .orElseThrow(() -> new IllegalStateException("操作人卡片不存在: " + operatorId.value()));

        // 6. 评估权限
        boolean hasPermission = evaluatePermission(configs, operation, targetCard, memberCard);

        if (!hasPermission) {
            String alertMessage = extractAlertMessage(configs, operation, targetCardId, operatorId);
            log.warn("卡片操作权限拒绝: cardId={}, operation={}", targetCardId.value(), operation);
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

        // 1. 卫语句 - 检查是否有权限配置
        if (!permissionConfigCacheService.hasCardOperationPermissionConfig(cardTypeId, operation)) {
            log.debug("无权限配置，直接允许");
            return;
        }

        // 2. 收集权限配置
        List<PermissionConfigDefinition> configs = permissionConfigCacheService.getPermissionConfigs(cardTypeId);

        // 3. 构建成员卡片 Yield（仅操作人条件）
        Yield memberYield = permissionConfigYieldBuilder.buildMemberYield(configs, operation);

        // 4. 查询操作人卡片
        CardDTO memberCard = cardRepository.findById(operatorId, memberYield, "system")
                .orElseThrow(() -> new IllegalStateException("操作人卡片不存在: " + operatorId.value()));

        // 5. 评估操作人条件（创建时仅评估操作人条件）
        boolean hasPermission = evaluateOperatorConditions(configs, operation, memberCard);

        if (!hasPermission) {
            String alertMessage = extractAlertMessage(configs, operation, null, operatorId);
            String cardTypeName = cardTypeCacheQuery.getCardTypeName(cardTypeId);
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

        // 收集涉及的__PLANKA_EINST__
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
        Yield cardYield = permissionConfigYieldBuilder.buildCardYield(allConfigs, operation);
        Yield memberYield = permissionConfigYieldBuilder.buildMemberYield(allConfigs, operation);

        // 查询完整数据
        Map<CardId, CardDTO> targetCards = cardRepository
                .findByIds(targetCardIds, cardYield, "system")
                .stream()
                .collect(Collectors.toMap(CardDTO::getId, dto -> dto));

        CardDTO memberCard = cardRepository
                .findById(operatorId, memberYield, "system")
                .orElseThrow(() -> new IllegalStateException("操作人卡片不存在: " + operatorId.value()));

        // 4. 按__PLANKA_EINST__分组校验
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

        // 3. 检查是否有属性编辑权限配置
        if (!hasFieldPermissionConfigForOperation(configs, PermissionConfig.FieldOperation.EDIT)) {
            log.debug("无属性编辑权限配置，直接允许");
            return;
        }

        // 4. 构建 Yield
        Yield cardYield = permissionConfigYieldBuilder.buildCardYield(configs, CardOperation.EDIT);
        Yield memberYield = permissionConfigYieldBuilder.buildMemberYield(configs, CardOperation.EDIT);

        // 5. 查询完整数据
        CardDTO targetCard = cardRepository.findById(targetCardId, cardYield, "system")
                .orElseThrow(() -> new IllegalStateException("卡片不存在: " + targetCardId.value()));

        CardDTO memberCard = cardRepository.findById(operatorId, memberYield, "system")
                .orElseThrow(() -> new IllegalStateException("操作人卡片不存在: " + operatorId.value()));

        // 6. 评估属性编辑权限
        for (FieldId fieldId : changedFieldIds) {
            // 只要有一个属性不满足即返回无权限
            if (!hasFieldEditPermission(configs, fieldId, targetCard, memberCard)) {
                String alertMessage = extractFieldAlertMessage(configs, fieldId, targetCardId, operatorId);
                log.warn("属性编辑权限拒绝: cardId={}, fieldId={}", targetCardId.value(), fieldId.value());
                throw PermissionDeniedException.fieldEdit(targetCardId, Set.of(fieldId), alertMessage);
            }
        }

        log.debug("属性编辑权限校验通过");
    }

    @Override
    public void checkFieldEditPermission(
            CardId targetCardId,
            CardId operatorId,
            Set<FieldId> changedFieldIds,
            Set<String> changedLinkFieldIds,
            Map<String, List<String>> targetCardIdsByLinkField) {

        // 合并所有需要检查的 fieldId（普通属性 + 关联属性）
        Set<FieldId> allFieldIds = new HashSet<>(changedFieldIds);
        for (String linkFieldId : changedLinkFieldIds) {
            allFieldIds.add(FieldId.of(linkFieldId));
        }

        log.debug("检查属性编辑权限（含关联双向）: targetCardId={}, operatorId={}, fieldCount={}, linkFieldCount={}",
                targetCardId, operatorId, changedFieldIds.size(), changedLinkFieldIds.size());

        if (allFieldIds.isEmpty() && changedLinkFieldIds.isEmpty()) {
            return;
        }

        // === 当前侧权限检查 ===
        if (!allFieldIds.isEmpty()) {
            checkFieldEditPermission(targetCardId, operatorId, allFieldIds);
        }

        // === 对侧权限检查（仅关联属性） ===
        if (changedLinkFieldIds.isEmpty()) {
            return;
        }

        checkOppositeSidePermission(targetCardId, operatorId, changedLinkFieldIds, targetCardIdsByLinkField);
    }

    /**
     * 检查对侧__PLANKA_EINST__的关联属性编辑权限
     */
    private void checkOppositeSidePermission(
            CardId targetCardId,
            CardId operatorId,
            Set<String> changedLinkFieldIds,
            Map<String, List<String>> targetCardIdsByLinkField) {

        for (String linkFieldId : changedLinkFieldIds) {
            // 1. 解析 linkTypeId 和 position
            if (!LinkFieldIdUtils.isValidFormat(linkFieldId)) {
                continue;
            }
            String linkTypeId = LinkFieldIdUtils.getLinkTypeId(linkFieldId);
            LinkPosition position = LinkFieldIdUtils.getPosition(linkFieldId);
            LinkPosition oppositePosition = position == LinkPosition.SOURCE
                    ? LinkPosition.TARGET : LinkPosition.SOURCE;
            String oppositeLinkFieldId = LinkFieldIdUtils.build(linkTypeId, oppositePosition);

            // 2. 获取实际对端卡片ID列表
            List<String> oppositeCardIds = targetCardIdsByLinkField.getOrDefault(linkFieldId, List.of());
            if (oppositeCardIds.isEmpty()) {
                continue;
            }

            // 3. 批量获取对端卡片的基本信息，按 cardTypeId 分组
            Set<CardId> oppositeCardIdSet = oppositeCardIds.stream()
                    .map(idStr -> CardId.of(Long.parseLong(idStr)))
                    .collect(java.util.stream.Collectors.toSet());

            Map<CardId, CardBasicInfo> oppositeBasicInfoMap = cardCacheService.getBasicInfoByIds(oppositeCardIdSet);

            Map<CardTypeId, List<CardId>> oppositeCardsByType = new HashMap<>();
            for (String oppositeCardIdStr : oppositeCardIds) {
                CardId oppositeCardId = CardId.of(Long.parseLong(oppositeCardIdStr));
                CardBasicInfo basicInfo = oppositeBasicInfoMap.get(oppositeCardId);
                if (basicInfo == null) {
                    continue;
                }
                oppositeCardsByType
                        .computeIfAbsent(basicInfo.cardTypeId(), k -> new ArrayList<>())
                        .add(oppositeCardId);
            }

            // 4. 对每个对侧__PLANKA_EINST__检查权限
            for (Map.Entry<CardTypeId, List<CardId>> entry : oppositeCardsByType.entrySet()) {
                CardTypeId oppositeCardTypeId = entry.getKey();
                List<CardId> oppositeCardIdList = entry.getValue();

                List<PermissionConfigDefinition> oppositeConfigs =
                        permissionConfigCacheService.getPermissionConfigs(oppositeCardTypeId);

                if (!hasFieldPermissionConfigForOperation(oppositeConfigs, PermissionConfig.FieldOperation.EDIT)) {
                    continue;
                }

                FieldId oppositeFieldId = FieldId.of(oppositeLinkFieldId);

                // 检查对侧是否配置了该对侧 linkFieldId 的 EDIT 权限
                if (!hasFieldInEditPermissionConfig(oppositeConfigs, oppositeFieldId)) {
                    continue;
                }

                // 构建 Yield 查询对端卡片和操作人
                Yield cardYield = permissionConfigYieldBuilder.buildCardYield(oppositeConfigs, CardOperation.EDIT);
                Yield memberYield = permissionConfigYieldBuilder.buildMemberYield(oppositeConfigs, CardOperation.EDIT);

                // 查询操作人卡片（只查一次）
                CardDTO memberCard = cardRepository.findById(operatorId, memberYield, "system")
                        .orElse(null);
                if (memberCard == null) {
                    continue;
                }

                // 提前评估操作人条件（只评估一次）
                boolean operatorConditionMet = evaluateOperatorConditionsForFieldPermission(
                        oppositeConfigs, oppositeFieldId, memberCard);
                if (!operatorConditionMet) {
                    // 操作人条件不满足，所有对端卡片都会被拒绝
                    String fieldName = getLinkFieldName(linkTypeId, position);
                    String alertMessage = extractFieldAlertMessage(
                            oppositeConfigs, oppositeFieldId, oppositeCardIdList.get(0), operatorId);
                    log.warn("对侧关联属性编辑权限拒绝(操作人条件): cardId={}, linkFieldId={}",
                            targetCardId.value(), linkFieldId);
                    throw PermissionDeniedException.oppositeSideFieldEdit(
                            targetCardId,
                            FieldId.of(linkFieldId),
                            fieldName,
                            alertMessage);
                }

                // 批量查询对端卡片
                List<CardDTO> oppositeCardsList = cardRepository.findByIds(
                        oppositeCardIdList, cardYield, "system");

                // 转换为 Map 方便查找
                Map<CardId, CardDTO> oppositeCardsMap = oppositeCardsList.stream()
                        .collect(java.util.stream.Collectors.toMap(CardDTO::getId, card -> card));

                // 用对端卡片评估卡片条件（操作人条件已经评估过了）
                for (CardId oppositeCardId : oppositeCardIdList) {
                    CardDTO oppositeCard = oppositeCardsMap.get(oppositeCardId);
                    if (oppositeCard == null) {
                        continue;
                    }

                    // 只评估卡片条件（操作人条件已经在上面评估过了）
                    boolean cardConditionMet = evaluateCardConditionsForFieldPermission(
                            oppositeConfigs, oppositeFieldId, oppositeCard, memberCard);

                    if (!cardConditionMet) {
                        String fieldName = getLinkFieldName(linkTypeId, position);
                        String alertMessage = extractFieldAlertMessage(
                                oppositeConfigs, oppositeFieldId, oppositeCardId, operatorId);
                        log.warn("对侧关联属性编辑权限拒绝(卡片条件): cardId={}, oppositeCardId={}, linkFieldId={}",
                                targetCardId.value(), oppositeCardId.value(), linkFieldId);
                        throw PermissionDeniedException.oppositeSideFieldEdit(
                                targetCardId,
                                FieldId.of(linkFieldId),
                                fieldName,
                                alertMessage);
                    }
                }
            }
        }
    }

    /**
     * 检查权限配置中是否包含指定属性的 EDIT 权限配置
     */
    private boolean hasFieldInEditPermissionConfig(
            List<PermissionConfigDefinition> configs,
            FieldId fieldId) {
        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }
            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                if (fieldPerm.getOperations() != null
                        && fieldPerm.getOperations().contains(PermissionConfig.FieldOperation.EDIT)
                        && fieldPerm.getFieldIds() != null
                        && fieldPerm.getFieldIds().contains(fieldId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取__PLANKA_EINST__名称
     */
    private String getCardTypeName(CardTypeId cardTypeId) {
        String name = cardTypeCacheQuery.getCardTypeName(cardTypeId);
        return name != null ? name : cardTypeId.value();
    }

    /**
     * 获取关联字段名称
     *
     * @param linkTypeId 关联类型ID
     * @param position   关联位置（SOURCE/TARGET）
     * @return 字段名称
     */
    private String getLinkFieldName(String linkTypeId, LinkPosition position) {
        try {
            // 1. 先尝试从 LinkFieldConfig 获取名称（如果缓存中有）
            String linkFieldId = LinkFieldIdUtils.build(linkTypeId, position);
            Optional<String> fieldConfigName = schemaCacheService.getById(linkFieldId)
                    .filter(def -> def instanceof FieldConfig)
                    .map(SchemaDefinition::getName)
                    .filter(name -> !name.isBlank());
            if (fieldConfigName.isPresent()) {
                return fieldConfigName.get();
            }

            // 2. 回退到 LinkTypeDefinition 的 sourceName/targetName
            LinkTypeId ltId = LinkTypeId.of(linkTypeId);
            return schemaCacheService.getById(ltId.value())
                    .flatMap(def -> {
                        if (def instanceof LinkTypeDefinition linkDef) {
                            return Optional.ofNullable(
                                    position == LinkPosition.SOURCE
                                            ? linkDef.getSourceName()
                                            : linkDef.getTargetName()
                            );
                        }
                        return Optional.empty();
                    })
                    .orElse(linkFieldId);
        } catch (Exception e) {
            log.warn("获取关联字段名称失败: linkTypeId={}, position={}", linkTypeId, position, e);
            return LinkFieldIdUtils.build(linkTypeId, position);
        }
    }

    @Override
    public void applyFieldReadPermissions(
            List<CardDTO> cards,
            CardId operatorId) {

        if (cards == null || cards.isEmpty()) {
            return;
        }

        log.debug("批量应用字段读权限: cardCount={}, operatorId={}", cards.size(), operatorId);

        // 按 CardTypeId 分组
        Map<CardTypeId, List<CardDTO>> groups = cards.stream()
                .filter(c -> c.getTypeId() != null)
                .collect(Collectors.groupingBy(CardDTO::getTypeId));

        for (Map.Entry<CardTypeId, List<CardDTO>> entry : groups.entrySet()) {
            applyFieldReadPermissionsForGroup(entry.getKey(), entry.getValue(), operatorId);
        }

        // 递归处理嵌套关联卡片
        List<CardDTO> nestedCards = collectNestedLinkedCards(cards);
        if (!nestedCards.isEmpty()) {
            applyFieldReadPermissions(nestedCards, operatorId);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 检查是否有任何权限配置（批量场景，多个__PLANKA_EINST__）
     */
    private boolean hasAnyPermissionConfigForBatch(
            Set<CardTypeId> cardTypeIds,
            CardOperation operation) {

        // 检查组织级配置
        for (CardTypeId cardTypeId : cardTypeIds) {
            if (permissionConfigCacheService.hasCardOperationPermissionConfig(cardTypeId, operation)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 收集所有权限配置（多个__PLANKA_EINST__）
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
                if (cardOp.getOperations() == null || !cardOp.getOperations().contains(operation)) {
                    continue;
                }

                // 评估卡片条件（OR 关系）
                boolean cardConditionMet = evaluateCardConditions(
                        cardOp.getCardConditions(),
                        targetCard,
                        memberCard
                );

                // 卡片条件和操作人条件之间是AND关系
                if (!cardConditionMet) {
                    return false;
                }

                // 评估操作人条件（OR 关系）
                return evaluateOperatorConditions(
                        cardOp.getOperatorConditions(),
                        memberCard
                );
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
            if (conditionEvaluator.evaluate(condition, memberCard, memberCard)) {
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
                if (cardOp.getOperations() == null || !cardOp.getOperations().contains(operation)) {
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
                if (cardOp.getOperations() != null && cardOp.getOperations().contains(operation) && cardOp.getAlertMessage() != null) {
                    return templateResolver.resolve(cardOp.getAlertMessage(), targetCardId, operatorId);
                }
            }
        }

        return null;
    }

    /**
     * 分组评估权限
     * <p>
     * 对同一组（相同__PLANKA_EINST__）的卡片进行批量评估
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
     * 检查是否有指定操作类型的属性权限配置
     */
    private boolean hasFieldPermissionConfigForOperation(
            List<PermissionConfigDefinition> configs,
            PermissionConfig.FieldOperation operation) {
        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }
            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                if (fieldPerm.getOperations() != null && fieldPerm.getOperations().contains(operation)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查指定字段是否存在某种操作类型的权限规则（不评估条件）
     */
    private boolean hasFieldPermissionRuleForOperation(
            List<PermissionConfigDefinition> configs,
            FieldId fieldId,
            PermissionConfig.FieldOperation operation) {
        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }
            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                if (fieldPerm.getOperations() != null && fieldPerm.getOperations().contains(operation)
                        && fieldPerm.getFieldIds() != null
                        && fieldPerm.getFieldIds().contains(fieldId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查属性编辑权限
     * <p>
     * 注意：如果该字段没有配置任何权限规则，返回 true（允许）
     */
    private boolean hasFieldEditPermission(
            List<PermissionConfigDefinition> configs,
            FieldId fieldId,
            CardDTO targetCard,
            CardDTO memberCard) {

        boolean hasFieldConfig = false;

        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }

            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                // 检查是否是编辑操作
                if (fieldPerm.getOperations() == null || !fieldPerm.getOperations().contains(PermissionConfig.FieldOperation.EDIT)) {
                    continue;
                }

                // 检查是否包含该属性
                if (fieldPerm.getFieldIds() == null || !fieldPerm.getFieldIds().contains(fieldId)) {
                    continue;
                }

                hasFieldConfig = true;

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

        // 该字段没有配置权限规则 → 允许
        return !hasFieldConfig;
    }

    /**
     * 仅评估属性编辑权限的卡片条件（用于性能优化，操作人条件已提前评估）
     * <p>
     * 注意：如果该字段没有配置任何权限规则，返回 true（允许）
     */
    private boolean evaluateCardConditionsForFieldPermission(
            List<PermissionConfigDefinition> configs,
            FieldId fieldId,
            CardDTO targetCard,
            CardDTO memberCard) {

        boolean hasFieldConfig = false;

        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }

            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                // 检查是否是编辑操作
                if (fieldPerm.getOperations() == null || !fieldPerm.getOperations().contains(PermissionConfig.FieldOperation.EDIT)) {
                    continue;
                }

                // 检查是否包含该属性
                if (fieldPerm.getFieldIds() == null || !fieldPerm.getFieldIds().contains(fieldId)) {
                    continue;
                }

                hasFieldConfig = true;

                // 只评估卡片条件
                boolean cardConditionMet = evaluateCardConditions(
                        fieldPerm.getCardConditions(),
                        targetCard,
                        memberCard
                );

                if (cardConditionMet) {
                    return true;
                }
            }
        }

        // 该字段没有配置权限规则 → 允许
        if (!hasFieldConfig) {
            return true;
        }

        // 配置了权限但条件都不满足 → 拒绝
        return false;
    }

    /**
     * 仅评估属性编辑权限的操作人条件（用于性能优化，提前评估一次）
     * <p>
     * 注意：如果该字段没有配置任何权限规则，返回 true（允许）
     */
    private boolean evaluateOperatorConditionsForFieldPermission(
            List<PermissionConfigDefinition> configs,
            FieldId fieldId,
            CardDTO memberCard) {

        boolean hasFieldConfig = false;

        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }

            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                // 检查是否是编辑操作
                if (fieldPerm.getOperations() == null || !fieldPerm.getOperations().contains(PermissionConfig.FieldOperation.EDIT)) {
                    continue;
                }

                // 检查是否包含该属性
                if (fieldPerm.getFieldIds() == null || !fieldPerm.getFieldIds().contains(fieldId)) {
                    continue;
                }

                hasFieldConfig = true;

                // 只评估操作人条件
                boolean operatorConditionMet = evaluateOperatorConditions(
                        fieldPerm.getOperatorConditions(),
                        memberCard
                );

                if (operatorConditionMet) {
                    return true;
                }
            }
        }

        // 该字段没有配置权限规则 → 允许
        return !hasFieldConfig;
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

    /**
     * 对同一__PLANKA_EINST__的卡片批量应用字段读权限
     */
    private void applyFieldReadPermissionsForGroup(
            CardTypeId cardTypeId,
            List<CardDTO> cards,
            CardId operatorId) {

        // 1. 获取权限配置
        List<PermissionConfigDefinition> configs = permissionConfigCacheService.getPermissionConfigs(cardTypeId);

        // 2. 卫语句：无字段权限配置则跳过
        if (!hasFieldPermissionConfig(configs)) {
            log.debug("__PLANKA_EINST__ {} 无字段权限配置，跳过", cardTypeId);
            return;
        }

        // 3. 提取受控字段集合（READ 操作涉及的字段）
        Set<FieldId> controlledFieldIds = collectControlledReadFieldIds(configs);
        if (controlledFieldIds.isEmpty()) {
            return;
        }

        // 4. 构建 Yield 并查询成员卡片（每组一次）
        Yield memberYield = permissionConfigYieldBuilder.buildMemberYieldForFieldPermission(configs);
        Optional<CardDTO> memberCardOpt = cardRepository.findById(operatorId, memberYield, "system");
        if (memberCardOpt.isEmpty()) {
            log.warn("操作人卡片不存在: {}，所有受控字段设为 NO_PERMISSION", operatorId);
            for (CardDTO card : cards) {
                setAllControlledFieldsNoPermission(card, controlledFieldIds);
            }
            return;
        }
        CardDTO memberCard = memberCardOpt.get();

        // 5. 预计算每条字段权限规则的操作人条件结果（同组内不变）
        Map<PermissionConfig.FieldPermission, Boolean> operatorConditionCache = preEvaluateOperatorConditions(configs, memberCard);

        // 6. 逐卡片评估字段权限（仅需评估卡片条件）
        for (CardDTO card : cards) {
            applyFieldReadPermissionsForCard(card, configs, controlledFieldIds, memberCard, operatorConditionCache);
        }
    }

    /**
     * 预计算所有字段权限规则的操作人条件结果
     * <p>
     * 操作人条件仅依赖 memberCard，同一组内结果固定，避免逐卡片重复评估
     */
    private Map<PermissionConfig.FieldPermission, Boolean> preEvaluateOperatorConditions(
            List<PermissionConfigDefinition> configs,
            CardDTO memberCard) {

        Map<PermissionConfig.FieldPermission, Boolean> cache = new HashMap<>();
        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }
            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                if (fieldPerm.getOperations() != null && fieldPerm.getOperations().contains(PermissionConfig.FieldOperation.READ)) {
                    boolean result = evaluateOperatorConditions(fieldPerm.getOperatorConditions(), memberCard);
                    cache.put(fieldPerm, result);
                }
            }
        }
        return cache;
    }

    /**
     * 对单张卡片应用字段读权限
     */
    private void applyFieldReadPermissionsForCard(
            CardDTO card,
            List<PermissionConfigDefinition> configs,
            Set<FieldId> controlledFieldIds,
            CardDTO memberCard,
            Map<PermissionConfig.FieldPermission, Boolean> operatorConditionCache) {

        // 处理普通属性
        if (card.getFieldValues() != null && !card.getFieldValues().isEmpty()) {
            for (Map.Entry<String, FieldValue<?>> entry : card.getFieldValues().entrySet()) {
                FieldId fieldId = FieldId.of(entry.getKey());
                FieldValue<?> fieldValue = entry.getValue();

                if (!controlledFieldIds.contains(fieldId)) {
                    continue;
                }

                FieldPermissionStatus fieldPermissionStatus = hasFieldReadPermission(configs, fieldId, card, memberCard, operatorConditionCache);
                fieldValue.setPermissionStatus(fieldPermissionStatus);
            }
        }

        // 处理关联属性
        applyLinkFieldReadPermissions(card, configs, controlledFieldIds, memberCard, operatorConditionCache);
    }

    /**
     * 对单张卡片的关联属性应用读权限
     */
    private void applyLinkFieldReadPermissions(
            CardDTO card,
            List<PermissionConfigDefinition> configs,
            Set<FieldId> controlledFieldIds,
            CardDTO memberCard,
            Map<PermissionConfig.FieldPermission, Boolean> operatorConditionCache) {

        if (card.getLinkedCards() == null || card.getLinkedCards().isEmpty()) {
            return;
        }

        for (String linkFieldId : new ArrayList<>(card.getLinkedCards().keySet())) {
            FieldId fieldId = FieldId.of(linkFieldId);
            if (!controlledFieldIds.contains(fieldId)) {
                continue;
            }

            FieldPermissionStatus status = hasFieldReadPermission(configs, fieldId, card, memberCard, operatorConditionCache);
            if (status == FieldPermissionStatus.NO_PERMISSION) {
                if (card.getLinkedCardPermissions() == null) {
                    card.setLinkedCardPermissions(new HashMap<>());
                }
                card.getLinkedCardPermissions().put(linkFieldId, FieldPermissionStatus.NO_PERMISSION);
                card.getLinkedCards().put(linkFieldId, null);
            }
        }
    }

    /**
     * 收集所有受 READ 控制的字段ID
     */
    private Set<FieldId> collectControlledReadFieldIds(List<PermissionConfigDefinition> configs) {
        Set<FieldId> fieldIds = new HashSet<>();
        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }
            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                if (fieldPerm.getOperations() != null && fieldPerm.getOperations().contains(PermissionConfig.FieldOperation.READ)) {
                    if (fieldPerm.getFieldIds() != null) {
                        fieldIds.addAll(fieldPerm.getFieldIds());
                    }
                }
            }
        }
        return fieldIds;
    }

    /**
     * 评估字段读权限
     * <p>
     * 使用预计算的操作人条件缓存，仅评估卡片条件。
     * 只要有 READ 规则且条件满足则返回 FULL_ACCESS，否则返回 NO_PERMISSION
     */
    private FieldPermissionStatus hasFieldReadPermission(
            List<PermissionConfigDefinition> configs,
            FieldId fieldId,
            CardDTO targetCard,
            CardDTO memberCard,
            Map<PermissionConfig.FieldPermission, Boolean> operatorConditionCache) {

        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }

            for (PermissionConfig.FieldPermission fieldPerm : config.getFieldPermissions()) {
                // 只处理包含 READ 操作的权限规则
                if (fieldPerm.getOperations() == null || !fieldPerm.getOperations().contains(PermissionConfig.FieldOperation.READ)) {
                    continue;
                }

                if (fieldPerm.getFieldIds() == null || !fieldPerm.getFieldIds().contains(fieldId)) {
                    continue;
                }

                // 从缓存获取操作人条件结果
                Boolean operatorConditionMet = operatorConditionCache.get(fieldPerm);

                // 评估卡片条件
                boolean conditionMet = operatorConditionMet && evaluateCardConditions(
                        fieldPerm.getCardConditions(),
                        targetCard,
                        memberCard
                );

                // READ 权限满足，返回 FULL_ACCESS
                if (conditionMet) {
                    return FieldPermissionStatus.FULL_ACCESS;
                }
            }
        }

        // 无满足条件的 READ 规则，返回 NO_PERMISSION
        return FieldPermissionStatus.NO_PERMISSION;
    }

    /**
     * 将所有受控字段设为 NO_PERMISSION
     */
    private void setAllControlledFieldsNoPermission(CardDTO card, Set<FieldId> controlledFieldIds) {
        // 处理普通属性
        if (card.getFieldValues() != null) {
            for (Map.Entry<String, FieldValue<?>> entry : card.getFieldValues().entrySet()) {
                if (controlledFieldIds.contains(FieldId.of(entry.getKey()))) {
                    entry.getValue().setPermissionStatus(FieldPermissionStatus.NO_PERMISSION);
                }
            }
        }

        // 处理关联属性
        if (card.getLinkedCards() != null) {
            for (String linkFieldId : new ArrayList<>(card.getLinkedCards().keySet())) {
                if (controlledFieldIds.contains(FieldId.of(linkFieldId))) {
                    if (card.getLinkedCardPermissions() == null) {
                        card.setLinkedCardPermissions(new HashMap<>());
                    }
                    card.getLinkedCardPermissions().put(linkFieldId, FieldPermissionStatus.NO_PERMISSION);
                    card.getLinkedCards().put(linkFieldId, null);
                }
            }
        }
    }

    /**
     * 从卡片列表中收集所有嵌套的关联卡片（排除已被 NO_PERMISSION 清空的）
     */
    private List<CardDTO> collectNestedLinkedCards(List<CardDTO> cards) {
        List<CardDTO> nested = new ArrayList<>();
        for (CardDTO card : cards) {
            if (card.getLinkedCards() == null) {
                continue;
            }
            for (Set<CardDTO> linkedSet : card.getLinkedCards().values()) {
                if (linkedSet != null) {
                    nested.addAll(linkedSet);
                }
            }
        }
        return nested;
    }
}
