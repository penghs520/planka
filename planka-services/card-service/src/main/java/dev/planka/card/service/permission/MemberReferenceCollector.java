package dev.planka.card.service.permission;

import dev.planka.domain.link.Path;
import dev.planka.domain.schema.definition.condition.*;

import java.util.Map;
import java.util.Set;

/**
 * 成员引用收集器
 * <p>
 * 负责从卡片条件中收集对成员卡片的引用字段
 */
class MemberReferenceCollector {

    /**
     * 从条件节点中收集成员引用的字段
     */
    static void collectMemberReferenceFields(
            ConditionNode node,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (node == null) {
            return;
        }

        if (node instanceof ConditionGroup group) {
            if (group.getChildren() != null) {
                for (ConditionNode child : group.getChildren()) {
                    collectMemberReferenceFields(child, fieldIds, pathTree);
                }
            }
        } else if (node instanceof AbstractConditionItem item) {
            collectFromConditionItem(item, fieldIds, pathTree);
        }
    }

    /**
     * 从条件项中收集成员引用
     */
    private static void collectFromConditionItem(
            AbstractConditionItem item,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (item instanceof NumberConditionItem numberItem) {
            collectFromNumberOperator(numberItem.getOperator(), fieldIds, pathTree);
        } else if (item instanceof DateConditionItem dateItem) {
            collectFromDateOperator(dateItem.getOperator(), fieldIds, pathTree);
        } else if (item instanceof LinkConditionItem linkItem) {
            collectFromLinkOperator(linkItem.getOperator(), fieldIds, pathTree);
        }
    }

    /**
     * 从数字操作符中收集成员引用
     */
    private static void collectFromNumberOperator(
            NumberConditionItem.NumberOperator operator,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (operator instanceof NumberConditionItem.NumberOperator.Equal eq) {
            collectFromNumberValue(eq.getValue(), fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.NotEqual ne) {
            collectFromNumberValue(ne.getValue(), fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.GreaterThan gt) {
            collectFromNumberValue(gt.getValue(), fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.GreaterThanOrEqual ge) {
            collectFromNumberValue(ge.getValue(), fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.LessThan lt) {
            collectFromNumberValue(lt.getValue(), fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.LessThanOrEqual le) {
            collectFromNumberValue(le.getValue(), fieldIds, pathTree);
        } else if (operator instanceof NumberConditionItem.NumberOperator.Between between) {
            collectFromNumberValue(between.getStart(), fieldIds, pathTree);
            collectFromNumberValue(between.getEnd(), fieldIds, pathTree);
        }
    }

    /**
     * 从数字值中收集成员引用
     */
    private static void collectFromNumberValue(
            NumberConditionItem.NumberValue value,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (value instanceof NumberConditionItem.NumberValue.ReferenceValue refValue) {
            ReferenceSource source = refValue.getSource();
            if (source instanceof ReferenceSource.Member member) {
                Path path = member.getPath();
                String fieldId = refValue.getFieldId();
                YieldBuilder.addFieldToTree(path, fieldId, fieldIds, pathTree);
            }
        }
    }

    /**
     * 从日期操作符中收集成员引用
     */
    private static void collectFromDateOperator(
            DateConditionItem.DateOperator operator,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (operator instanceof DateConditionItem.DateOperator.Equal eq) {
            collectFromDateValue(eq.getValue(), fieldIds, pathTree);
        } else if (operator instanceof DateConditionItem.DateOperator.NotEqual ne) {
            collectFromDateValue(ne.getValue(), fieldIds, pathTree);
        } else if (operator instanceof DateConditionItem.DateOperator.Before before) {
            collectFromDateValue(before.getValue(), fieldIds, pathTree);
        } else if (operator instanceof DateConditionItem.DateOperator.After after) {
            collectFromDateValue(after.getValue(), fieldIds, pathTree);
        } else if (operator instanceof DateConditionItem.DateOperator.Between between) {
            collectFromDateValue(between.getStart(), fieldIds, pathTree);
            collectFromDateValue(between.getEnd(), fieldIds, pathTree);
        }
    }

    /**
     * 从日期值中收集成员引用
     */
    private static void collectFromDateValue(
            DateConditionItem.DateValue value,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (value instanceof DateConditionItem.DateValue.ReferenceValue refValue) {
            ReferenceSource source = refValue.getSource();
            if (source instanceof ReferenceSource.Member member) {
                Path path = member.getPath();
                String fieldId = refValue.getFieldId();
                YieldBuilder.addFieldToTree(path, fieldId, fieldIds, pathTree);
            }
        }
    }

    /**
     * 从关联操作符中收集成员引用
     */
    private static void collectFromLinkOperator(
            LinkConditionItem.LinkOperator operator,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (operator instanceof LinkConditionItem.LinkOperator.In in) {
            collectFromLinkValue(in.getValue(), fieldIds, pathTree);
        } else if (operator instanceof LinkConditionItem.LinkOperator.NotIn notIn) {
            collectFromLinkValue(notIn.getValue(), fieldIds, pathTree);
        }
    }

    /**
     * 从关联值中收集成员引用
     */
    private static void collectFromLinkValue(
            LinkConditionItem.LinkValue value,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (value instanceof LinkConditionItem.LinkValue.ReferenceValue refValue) {
            ReferenceSource source = refValue.getSource();
            if (source instanceof ReferenceSource.Member member) {
                Path path = member.getPath();
                // 关联值引用的是关联字段本身
                if (path != null && path.linkNodes() != null && !path.linkNodes().isEmpty()) {
                    YieldBuilder.addFieldToPathTree(path.linkNodes(), null, pathTree);
                }
            }
        }
    }
}
