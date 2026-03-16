package cn.planka.card.service.permission;

import cn.planka.api.card.request.Yield;
import cn.planka.api.card.util.ConditionYieldBuilder;
import cn.planka.domain.schema.definition.condition.*;
import cn.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import cn.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Yield 构建器
 * <p>
 * 根据权限配置中的条件，构建查询所需的 Yield（支持多级关联）
 * <p>
 * 核心功能：
 * 1. 从权限条件中提取字段ID和关联路径
 * 2. 构建树形的 Yield 结构（支持多级关联）
 * 3. 处理 ReferenceValue（从卡片条件中提取成员卡片需要的字段）
 */
@Slf4j
@Component
public class PermissionConfigYieldBuilder {

    /**
     * 构建目标卡片的 Yield
     * <p>
     * 分析「卡片条件」涉及的字段和关联路径
     *
     * @param configs   权限配置列表
     * @param operation 操作类型
     * @return 目标卡片的 Yield
     */
    public Yield buildCardYield(
            List<PermissionConfigDefinition> configs,
            CardOperation operation) {

        if (configs == null || configs.isEmpty()) {
            return Yield.basic();
        }

        // 收集所有卡片条件
        List<Condition> cardConditions = collectCardConditions(configs, operation);

        return ConditionYieldBuilder.buildYieldForCurrentCard(cardConditions);
    }

    /**
     * 构建成员卡片的 Yield
     * <p>
     * 1. 操作人条件涉及的成员属性
     * 2. 卡片条件中通过 ReferenceValue 引用的成员属性
     *
     * @param configs   权限配置列表
     * @param operation 操作类型
     * @return 成员卡片的 Yield
     */
    public Yield buildMemberYield(
            List<PermissionConfigDefinition> configs,
            CardOperation operation) {

        if (configs == null || configs.isEmpty()) {
            return Yield.basic();
        }

        // 收集所有操作人条件
        List<Condition> operatorConditions = collectOperatorConditions(configs, operation);

        // 在这里 操作人就是就是当前卡
        Yield yield1 = ConditionYieldBuilder.buildYieldForCurrentCard(operatorConditions);

        // 操作人条件也可能使用了 成员卡的条件，而操作人卡也是成员卡，所以也需要包含进来
        Yield yield2 = ConditionYieldBuilder.buildYieldForMemberCard(operatorConditions);

        // 操作卡条件中也可能使用了 成员卡的条件
        // 收集所有卡片条件（用于提取 ReferenceValue）
        List<Condition> cardConditions = collectCardConditions(configs, operation);
        Yield yield3 = ConditionYieldBuilder.buildYieldForMemberCard(cardConditions);

        return Yield.merge(yield1, yield2, yield3);
    }

    /**
     * 收集卡片条件
     */
    private List<Condition> collectCardConditions(
            List<PermissionConfigDefinition> configs,
            CardOperation operation) {

        List<Condition> conditions = new ArrayList<>();

        for (PermissionConfigDefinition config : configs) {
            if (config.getCardOperations() == null) {
                continue;
            }

            for (var cardOp : config.getCardOperations()) {
                if (cardOp.getOperations() != null && cardOp.getOperations().contains(operation) && cardOp.getCardConditions() != null) {
                    conditions.addAll(cardOp.getCardConditions());
                }
            }
        }

        return conditions;
    }

    /**
     * 收集操作人条件
     */
    private List<Condition> collectOperatorConditions(
            List<PermissionConfigDefinition> configs,
            CardOperation operation) {

        List<Condition> conditions = new ArrayList<>();

        for (PermissionConfigDefinition config : configs) {
            if (config.getCardOperations() == null) {
                continue;
            }

            for (var cardOp : config.getCardOperations()) {
                if (cardOp.getOperations() != null && cardOp.getOperations().contains(operation) && cardOp.getOperatorConditions() != null) {
                    conditions.addAll(cardOp.getOperatorConditions());
                }
            }
        }
        return conditions;
    }

    /**
     * 构建字段权限评估所需的目标卡片 Yield
     * <p>
     * 从字段权限的卡片条件中提取所需字段
     */
    public Yield buildCardYieldForFieldPermission(List<PermissionConfigDefinition> configs) {
        List<Condition> cardConditions = collectFieldPermissionCardConditions(configs);
        if (cardConditions.isEmpty()) {
            return Yield.basic();
        }
        return ConditionYieldBuilder.buildYieldForCurrentCard(cardConditions);
    }

    /**
     * 构建字段权限评估所需的成员卡片 Yield
     * <p>
     * 从字段权限的操作人条件和卡片条件中提取所需字段
     */
    public Yield buildMemberYieldForFieldPermission(List<PermissionConfigDefinition> configs) {
        List<Condition> operatorConditions = collectFieldPermissionOperatorConditions(configs);
        List<Condition> cardConditions = collectFieldPermissionCardConditions(configs);

        Yield yield1 = ConditionYieldBuilder.buildYieldForCurrentCard(operatorConditions);
        Yield yield2 = ConditionYieldBuilder.buildYieldForMemberCard(operatorConditions);
        Yield yield3 = ConditionYieldBuilder.buildYieldForMemberCard(cardConditions);

        return Yield.merge(yield1, yield2, yield3);
    }

    /**
     * 收集字段权限的卡片条件
     */
    private List<Condition> collectFieldPermissionCardConditions(List<PermissionConfigDefinition> configs) {
        List<Condition> conditions = new ArrayList<>();
        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }
            for (var fieldPerm : config.getFieldPermissions()) {
                if (fieldPerm.getCardConditions() != null) {
                    conditions.addAll(fieldPerm.getCardConditions());
                }
            }
        }
        return conditions;
    }

    /**
     * 收集字段权限的操作人条件
     */
    private List<Condition> collectFieldPermissionOperatorConditions(List<PermissionConfigDefinition> configs) {
        List<Condition> conditions = new ArrayList<>();
        for (PermissionConfigDefinition config : configs) {
            if (config.getFieldPermissions() == null) {
                continue;
            }
            for (var fieldPerm : config.getFieldPermissions()) {
                if (fieldPerm.getOperatorConditions() != null) {
                    conditions.addAll(fieldPerm.getOperatorConditions());
                }
            }
        }
        return conditions;
    }
}
