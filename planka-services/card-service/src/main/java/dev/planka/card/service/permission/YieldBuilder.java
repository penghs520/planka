package dev.planka.card.service.permission;

import dev.planka.api.card.request.Yield;
import dev.planka.api.card.request.YieldField;
import dev.planka.api.card.request.YieldLink;
import dev.planka.domain.link.Path;
import dev.planka.domain.schema.definition.condition.*;
import dev.planka.domain.schema.definition.permission.PermissionConfig.CardOperation;
import dev.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

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
public class YieldBuilder {

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

        if (cardConditions.isEmpty()) {
            return Yield.basic();
        }

        // 收集字段ID和路径
        Set<String> fieldIds = new HashSet<>();
        Map<String, PathNode> pathTree = new HashMap<>();

        for (Condition condition : cardConditions) {
            if (condition == null || condition.isEmpty()) {
                continue;
            }
            collectFieldsAndPaths(condition.getRoot(), fieldIds, pathTree);
        }

        // 构建 Yield
        return buildYieldFromTree(fieldIds, pathTree);
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

        // 收集所有卡片条件（用于提取 ReferenceValue）
        List<Condition> cardConditions = collectCardConditions(configs, operation);

        Set<String> fieldIds = new HashSet<>();
        Map<String, PathNode> pathTree = new HashMap<>();

        // 1. 从操作人条件中收集字段和路径
        for (Condition condition : operatorConditions) {
            if (condition == null || condition.isEmpty()) {
                continue;
            }
            collectFieldsAndPaths(condition.getRoot(), fieldIds, pathTree);
        }

        // 2. 从卡片条件中收集 ReferenceValue 引用的成员字段
        for (Condition condition : cardConditions) {
            if (condition == null || condition.isEmpty()) {
                continue;
            }
            MemberReferenceCollector.collectMemberReferenceFields(condition.getRoot(), fieldIds, pathTree);
        }

        // 构建 Yield
        return buildYieldFromTree(fieldIds, pathTree);
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
                if (cardOp.getOperation() == operation && cardOp.getCardConditions() != null) {
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
                if (cardOp.getOperation() == operation && cardOp.getOperatorConditions() != null) {
                    conditions.addAll(cardOp.getOperatorConditions());
                }
            }
        }

        return conditions;
    }

    /**
     * 从条件节点中收集字段ID和路径
     */
    private void collectFieldsAndPaths(
            ConditionNode node,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (node == null) {
            return;
        }

        if (node instanceof ConditionGroup group) {
            // 递归处理子节点
            if (group.getChildren() != null) {
                for (ConditionNode child : group.getChildren()) {
                    collectFieldsAndPaths(child, fieldIds, pathTree);
                }
            }
        } else if (node instanceof AbstractConditionItem item) {
            // 处理具体的条件项
            collectFromConditionItem(item, fieldIds, pathTree);
        }
    }

    /**
     * 从条件项中收集字段ID和路径
     */
    private void collectFromConditionItem(
            AbstractConditionItem item,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (item instanceof TextConditionItem textItem) {
            Path path = textItem.getSubject().path();
            String fieldId = textItem.getSubject().fieldId();
            addFieldToTree(path, fieldId, fieldIds, pathTree);

        } else if (item instanceof NumberConditionItem numberItem) {
            Path path = numberItem.getSubject().path();
            String fieldId = numberItem.getSubject().fieldId();
            addFieldToTree(path, fieldId, fieldIds, pathTree);
            // 处理 ReferenceValue
            ReferenceValueCollector.collectFromNumberOperator(numberItem.getOperator(), fieldIds, pathTree);

        } else if (item instanceof DateConditionItem dateItem) {
            DateConditionItem.DateSubject subject = dateItem.getSubject();
            Path path = subject.getPath();

            if (subject instanceof DateConditionItem.DateSubject.FieldDateSubject fieldSubject) {
                String fieldId = fieldSubject.getFieldId();
                addFieldToTree(path, fieldId, fieldIds, pathTree);
            }
            // 处理 ReferenceValue
            ReferenceValueCollector.collectFromDateOperator(dateItem.getOperator(), fieldIds, pathTree);

        } else if (item instanceof EnumConditionItem enumItem) {
            Path path = enumItem.getSubject().path();
            String fieldId = enumItem.getSubject().fieldId();
            addFieldToTree(path, fieldId, fieldIds, pathTree);

        } else if (item instanceof LinkConditionItem linkItem) {
            Path path = linkItem.getSubject().path();
            String linkFieldId = linkItem.getSubject().linkFieldId().value();
            addFieldToTree(path, linkFieldId, fieldIds, pathTree);
            // 处理 ReferenceValue
            ReferenceValueCollector.collectFromLinkOperator(linkItem.getOperator(), fieldIds, pathTree);
        }
        // 其他条件项类型（StatusConditionItem, CardCycleConditionItem 等）不涉及字段
    }

    /**
     * 将字段添加到路径树中
     */
    static void addFieldToTree(
            Path path,
            String fieldId,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (path == null || path.linkNodes() == null || path.linkNodes().isEmpty()) {
            // 当前卡片的字段
            if (fieldId != null) {
                fieldIds.add(fieldId);
            }
        } else {
            // 关联卡片的字段
            List<String> linkNodes = path.linkNodes();
            addFieldToPathTree(linkNodes, fieldId, pathTree);
        }
    }

    /**
     * 将字段添加到路径树中（递归构建）
     */
    static void addFieldToPathTree(
            List<String> linkNodes,
            String fieldId,
            Map<String, PathNode> pathTree) {

        if (linkNodes.isEmpty()) {
            return;
        }

        String firstLink = linkNodes.get(0);
        PathNode node = pathTree.computeIfAbsent(firstLink, k -> new PathNode());

        if (linkNodes.size() == 1) {
            // 叶子节点，添加字段
            if (fieldId != null) {
                node.fieldIds.add(fieldId);
            }
        } else {
            // 继续递归
            List<String> remainingPath = linkNodes.subList(1, linkNodes.size());
            addFieldToPathTree(remainingPath, fieldId, node.children);
        }
    }

    /**
     * 从字段集合和路径树构建 Yield
     */
    private Yield buildYieldFromTree(
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        Yield yield = new Yield();

        // 设置字段
        YieldField yieldField = new YieldField();
        yieldField.setAllFields(false);
        yieldField.setFieldIds(fieldIds);
        yieldField.setIncludeDescription(false);
        yield.setField(yieldField);

        // 设置关联
        if (!pathTree.isEmpty()) {
            List<YieldLink> links = pathTree.entrySet().stream()
                    .map(entry -> buildYieldLink(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
            yield.setLinks(links);
        }

        return yield;
    }

    /**
     * 构建 YieldLink（递归）
     */
    private YieldLink buildYieldLink(String linkFieldId, PathNode node) {
        YieldLink link = new YieldLink();
        link.setLinkFieldId(linkFieldId);

        // 构建目标 Yield
        Yield targetYield = buildYieldFromTree(node.fieldIds, node.children);
        link.setTargetYield(targetYield);

        return link;
    }

    /**
     * 路径树节点
     * <p>
     * 用于构建多级关联的树形结构
     */
    static class PathNode {
        /**
         * 当前节点需要查询的字段ID集合
         */
        Set<String> fieldIds = new HashSet<>();

        /**
         * 子节点（linkFieldId -> PathNode）
         */
        Map<String, PathNode> children = new HashMap<>();
    }
}
