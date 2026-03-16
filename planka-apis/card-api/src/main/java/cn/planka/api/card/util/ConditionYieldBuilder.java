package cn.planka.api.card.util;

import cn.planka.api.card.request.Yield;
import cn.planka.api.card.request.YieldField;
import cn.planka.api.card.request.YieldLink;
import cn.planka.domain.link.Path;
import cn.planka.domain.schema.definition.condition.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Condition Yield 构建器
 * <p>
 * 从 Condition 中提取Subject中提取字段，以及按 ReferenceSource 类型从ReferenceSource提取字段，构建对应的 Yield。
 * 支持四种 ReferenceSource 类型：CurrentCard、ParameterCard、Member、ContextualCard。
 */
public class ConditionYieldBuilder {

    /**
     * 从 Condition 中收集 CurrentCard 相关的字段构建 Yield
     * <p>
     * 包括：
     * 1. Subject 中 path 为空的字段（当前卡片字段）
     * 2. ReferenceValue 中 source 为 CurrentCard 的字段
     *
     * @param condition 条件定义
     * @return CurrentCard 的 Yield
     */
    public static Yield buildYieldForCurrentCard(Condition condition) {
        return buildYieldForReferenceSource(condition, ReferenceSource.CurrentCard.class);
    }

    /**
     * 从多个 Condition 中收集 CurrentCard 相关的字段构建 Yield
     * <p>
     * 包括：
     * 1. Subject 中 path 为空的字段（当前卡片字段）
     * 2. ReferenceValue 中 source 为 CurrentCard 的字段
     *
     * @param conditions 条件定义数组
     * @return CurrentCard 的 Yield
     */
    public static Yield buildYieldForCurrentCard(Condition... conditions) {
        return buildYieldForReferenceSource(ReferenceSource.CurrentCard.class, conditions);
    }

    /**
     * 从 Condition 列表中收集 CurrentCard 相关的字段构建 Yield
     * <p>
     * 包括：
     * 1. Subject 中 path 为空的字段（当前卡片字段）
     * 2. ReferenceValue 中 source 为 CurrentCard 的字段
     *
     * @param conditions 条件定义列表
     * @return CurrentCard 的 Yield
     */
    public static Yield buildYieldForCurrentCard(List<Condition> conditions) {
        return buildYieldForReferenceSource(ReferenceSource.CurrentCard.class, conditions);
    }

    /**
     * 从 Condition 中收集 ParameterCard 相关的字段构建 Yield
     * <p>
     * 包括 ReferenceValue 中 source 为 ParameterCard 的字段
     *
     * @param condition 条件定义
     * @return ParameterCard 的 Yield
     */
    public static Yield buildYieldForParameterCard(Condition condition) {
        return buildYieldForReferenceSource(condition, ReferenceSource.ParameterCard.class);
    }

    /**
     * 从多个 Condition 中收集 ParameterCard 相关的字段构建 Yield
     * <p>
     * 包括 ReferenceValue 中 source 为 ParameterCard 的字段
     *
     * @param conditions 条件定义数组
     * @return ParameterCard 的 Yield
     */
    public static Yield buildYieldForParameterCard(Condition... conditions) {
        return buildYieldForReferenceSource(ReferenceSource.ParameterCard.class, conditions);
    }

    /**
     * 从 Condition 列表中收集 ParameterCard 相关的字段构建 Yield
     * <p>
     * 包括 ReferenceValue 中 source 为 ParameterCard 的字段
     *
     * @param conditions 条件定义列表
     * @return ParameterCard 的 Yield
     */
    public static Yield buildYieldForParameterCard(List<Condition> conditions) {
        return buildYieldForReferenceSource(ReferenceSource.ParameterCard.class, conditions);
    }

    /**
     * 从 Condition 中收集 Member 相关的字段构建 Yield
     * <p>
     * 包括 ReferenceValue 中 source 为 Member 的字段
     *
     * @param condition 条件定义
     * @return Member 的 Yield
     */
    public static Yield buildYieldForMemberCard(Condition condition) {
        return buildYieldForReferenceSource(condition, ReferenceSource.Member.class);
    }

    /**
     * 从多个 Condition 中收集 Member 相关的字段构建 Yield
     * <p>
     * 包括 ReferenceValue 中 source 为 Member 的字段
     *
     * @param conditions 条件定义数组
     * @return Member 的 Yield
     */
    public static Yield buildYieldForMemberCard(Condition... conditions) {
        return buildYieldForReferenceSource(ReferenceSource.Member.class, conditions);
    }

    /**
     * 从 Condition 列表中收集 Member 相关的字段构建 Yield
     * <p>
     * 包括 ReferenceValue 中 source 为 Member 的字段
     *
     * @param conditions 条件定义列表
     * @return Member 的 Yield
     */
    public static Yield buildYieldForMemberCard(List<Condition> conditions) {
        return buildYieldForReferenceSource(ReferenceSource.Member.class, conditions);
    }

    /**
     * 从 Condition 中收集 ContextualCard 相关的字段构建 Yield
     * <p>
     * 包括 ReferenceValue 中 source 为 ContextualCard 的字段
     *
     * @param condition 条件定义
     * @return ContextualCard 的 Yield
     */
    public static Yield buildYieldForContextualCard(Condition condition) {
        return buildYieldForReferenceSource(condition, ReferenceSource.ContextualCard.class);
    }

    /**
     * 从多个 Condition 中收集 ContextualCard 相关的字段构建 Yield
     * <p>
     * 包括 ReferenceValue 中 source 为 ContextualCard 的字段
     *
     * @param conditions 条件定义数组
     * @return ContextualCard 的 Yield
     */
    public static Yield buildYieldForContextualCard(Condition... conditions) {
        return buildYieldForReferenceSource(ReferenceSource.ContextualCard.class, conditions);
    }

    /**
     * 从 Condition 列表中收集 ContextualCard 相关的字段构建 Yield
     * <p>
     * 包括 ReferenceValue 中 source 为 ContextualCard 的字段
     *
     * @param conditions 条件定义列表
     * @return ContextualCard 的 Yield
     */
    public static Yield buildYieldForContextualCard(List<Condition> conditions) {
        return buildYieldForReferenceSource(ReferenceSource.ContextualCard.class, conditions);
    }

    /**
     * 根据 ReferenceSource 类型构建 Yield
     */
    private static Yield buildYieldForReferenceSource(Condition condition, Class<? extends ReferenceSource> sourceType) {
        if (condition == null || condition.isEmpty()) {
            return Yield.basic();
        }

        Set<String> fieldIds = new HashSet<>();
        Map<String, PathNode> pathTree = new HashMap<>();

        collectFieldsByReferenceSource(condition.getRoot(), sourceType, fieldIds, pathTree);

        return buildYieldFromTree(fieldIds, pathTree);
    }

    /**
     * 根据 ReferenceSource 类型从多个 Condition 构建 Yield
     */
    private static Yield buildYieldForReferenceSource(Class<? extends ReferenceSource> sourceType, Condition... conditions) {
        if (conditions == null || conditions.length == 0) {
            return Yield.basic();
        }

        Set<String> fieldIds = new HashSet<>();
        Map<String, PathNode> pathTree = new HashMap<>();

        for (Condition condition : conditions) {
            if (condition != null && !condition.isEmpty()) {
                collectFieldsByReferenceSource(condition.getRoot(), sourceType, fieldIds, pathTree);
            }
        }

        return buildYieldFromTree(fieldIds, pathTree);
    }

    /**
     * 根据 ReferenceSource 类型从 Condition 列表构建 Yield
     */
    private static Yield buildYieldForReferenceSource(Class<? extends ReferenceSource> sourceType, List<Condition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return Yield.basic();
        }

        Set<String> fieldIds = new HashSet<>();
        Map<String, PathNode> pathTree = new HashMap<>();

        for (Condition condition : conditions) {
            if (condition != null && !condition.isEmpty()) {
                collectFieldsByReferenceSource(condition.getRoot(), sourceType, fieldIds, pathTree);
            }
        }

        return buildYieldFromTree(fieldIds, pathTree);
    }

    /**
     * 递归收集指定 ReferenceSource 类型的字段
     */
    private static void collectFieldsByReferenceSource(
            ConditionNode node,
            Class<? extends ReferenceSource> sourceType,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (node == null) {
            return;
        }

        if (node instanceof ConditionGroup group) {
            if (group.getChildren() != null) {
                for (ConditionNode child : group.getChildren()) {
                    collectFieldsByReferenceSource(child, sourceType, fieldIds, pathTree);
                }
            }
        } else if (node instanceof AbstractConditionItem item) {
            collectFromConditionItem(item, sourceType, fieldIds, pathTree);
        }
    }

    /**
     * 从条件项中收集字段
     */
    private static void collectFromConditionItem(
            AbstractConditionItem item,
            Class<? extends ReferenceSource> sourceType,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        // CurrentCard 特殊处理：还包括 Subject 中 path 为空的字段
        if (sourceType == ReferenceSource.CurrentCard.class) {
            collectSubjectFields(item, fieldIds, pathTree);
        }

        // 收集 ReferenceValue 中的字段
        collectReferenceValues(item, sourceType, fieldIds, pathTree);
    }

    /**
     * 收集 Subject 中的字段（path 为空的字段属于 CurrentCard）
     */
    private static void collectSubjectFields(
            AbstractConditionItem item,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        Path path = null;
        String fieldId = null;

        if (item instanceof TextConditionItem textItem) {
            path = textItem.getSubject().path();
            fieldId = textItem.getSubject().fieldId();
        } else if (item instanceof NumberConditionItem numberItem) {
            path = numberItem.getSubject().path();
            fieldId = numberItem.getSubject().fieldId();
        } else if (item instanceof EnumConditionItem enumItem) {
            path = enumItem.getSubject().path();
            fieldId = enumItem.getSubject().fieldId();
        } else if (item instanceof DateConditionItem dateItem) {
            DateConditionItem.DateSubject subject = dateItem.getSubject();
            path = subject.getPath();
            if (subject instanceof DateConditionItem.DateSubject.FieldDateSubject fieldSubject) {
                fieldId = fieldSubject.getFieldId();
            }
        } else if (item instanceof LinkConditionItem linkItem) {
            path = linkItem.getSubject().path();
            fieldId = linkItem.getSubject().linkFieldId().value();
        }

        if (fieldId != null) {
            addFieldToTree(path, fieldId, fieldIds, pathTree);
        }
    }

    /**
     * 收集 ReferenceValue 中的字段
     */
    private static void collectReferenceValues(
            AbstractConditionItem item,
            Class<? extends ReferenceSource> sourceType,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (item instanceof NumberConditionItem numberItem) {
            collectFromNumberOperator(numberItem.getOperator(), sourceType, fieldIds, pathTree);
        } else if (item instanceof DateConditionItem dateItem) {
            collectFromDateOperator(dateItem.getOperator(), sourceType, fieldIds, pathTree);
        } else if (item instanceof LinkConditionItem linkItem) {
            collectFromLinkOperator(linkItem.getOperator(), sourceType, fieldIds, pathTree);
        }
    }

    /**
     * 从数字操作符中收集 ReferenceValue
     */
    private static void collectFromNumberOperator(
            NumberConditionItem.NumberOperator operator,
            Class<? extends ReferenceSource> sourceType,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (operator instanceof NumberConditionItem.NumberOperator.Equal eq) {
            collectFromNumberValue(eq.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.NotEqual ne) {
            collectFromNumberValue(ne.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.GreaterThan gt) {
            collectFromNumberValue(gt.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.GreaterThanOrEqual ge) {
            collectFromNumberValue(ge.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.LessThan lt) {
            collectFromNumberValue(lt.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.LessThanOrEqual le) {
            collectFromNumberValue(le.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.Between between) {
            collectFromNumberValue(between.getStart(), sourceType, fieldIds, pathTree);
            collectFromNumberValue(between.getEnd(), sourceType, fieldIds, pathTree);
        }
    }

    /**
     * 从数字值中收集 ReferenceValue
     */
    private static void collectFromNumberValue(
            NumberConditionItem.NumberValue value,
            Class<? extends ReferenceSource> sourceType,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (value instanceof NumberConditionItem.NumberValue.ReferenceValue refValue) {
            ReferenceSource source = refValue.getSource();
            String fieldId = refValue.getFieldId();
            if (sourceType.isInstance(source)) {
                Path path = extractPathFromReferenceSource(source);
                addFieldToTree(path, fieldId, fieldIds, pathTree);
            }
        }
    }

    /**
     * 从日期操作符中收集 ReferenceValue
     */
    private static void collectFromDateOperator(
            DateConditionItem.DateOperator operator,
            Class<? extends ReferenceSource> sourceType,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (operator instanceof DateConditionItem.DateOperator.Equal eq) {
            collectFromDateValue(eq.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof DateConditionItem.DateOperator.NotEqual ne) {
            collectFromDateValue(ne.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof DateConditionItem.DateOperator.Before before) {
            collectFromDateValue(before.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof DateConditionItem.DateOperator.After after) {
            collectFromDateValue(after.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof DateConditionItem.DateOperator.Between between) {
            collectFromDateValue(between.getStart(), sourceType, fieldIds, pathTree);
            collectFromDateValue(between.getEnd(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof DateConditionItem.DateOperator.NotBetween notBetween) {
            collectFromDateValue(notBetween.getStart(), sourceType, fieldIds, pathTree);
            collectFromDateValue(notBetween.getEnd(), sourceType, fieldIds, pathTree);
        }
    }

    /**
     * 从日期值中收集 ReferenceValue
     */
    private static void collectFromDateValue(
            DateConditionItem.DateValue value,
            Class<? extends ReferenceSource> sourceType,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (value instanceof DateConditionItem.DateValue.ReferenceValue refValue) {
            ReferenceSource source = refValue.getSource();
            String fieldId = refValue.getFieldId();
            if (sourceType.isInstance(source)) {
                Path path = extractPathFromReferenceSource(source);
                addFieldToTree(path, fieldId, fieldIds, pathTree);
            }
        }
    }

    /**
     * 从关联操作符中收集 ReferenceValue
     */
    private static void collectFromLinkOperator(
            LinkConditionItem.LinkOperator operator,
            Class<? extends ReferenceSource> sourceType,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (operator instanceof LinkConditionItem.LinkOperator.In in) {
            collectFromLinkValue(in.getValue(), sourceType, fieldIds, pathTree);
        } else if (operator instanceof LinkConditionItem.LinkOperator.NotIn notIn) {
            collectFromLinkValue(notIn.getValue(), sourceType, fieldIds, pathTree);
        }
    }

    /**
     * 从关联值中收集 ReferenceValue
     */
    private static void collectFromLinkValue(
            LinkConditionItem.LinkValue value,
            Class<? extends ReferenceSource> sourceType,
            Set<String> fieldIds,
            Map<String, PathNode> pathTree) {

        if (value instanceof LinkConditionItem.LinkValue.ReferenceValue refValue) {
            ReferenceSource source = refValue.getSource();
            if (sourceType.isInstance(source)) {
                Path path = extractPathFromReferenceSource(source);
                // 关联值的 ReferenceValue 引用的是关联字段本身，不需要额外的 fieldId
                addFieldToTree(path, null, fieldIds, pathTree);
            }
        }
    }

    /**
     * 从 ReferenceSource 中提取 Path
     */
    private static Path extractPathFromReferenceSource(ReferenceSource source) {
        if (source instanceof ReferenceSource.CurrentCard currentCard) {
            return currentCard.getPath();
        } else if (source instanceof ReferenceSource.ParameterCard paramCard) {
            return paramCard.getPath();
        } else if (source instanceof ReferenceSource.Member member) {
            return member.getPath();
        } else if (source instanceof ReferenceSource.ContextualCard contextualCard) {
            return contextualCard.getPath();
        }
        return null;
    }

    /**
     * 将字段添加到路径树中
     */
    private static void addFieldToTree(
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
            addFieldToPathTree(path.linkNodes(), fieldId, pathTree);
        }
    }

    /**
     * 将字段添加到路径树中（递归构建）
     */
    private static void addFieldToPathTree(
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
    private static Yield buildYieldFromTree(
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
    private static YieldLink buildYieldLink(String linkFieldId, PathNode node) {
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
    private static class PathNode {
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
