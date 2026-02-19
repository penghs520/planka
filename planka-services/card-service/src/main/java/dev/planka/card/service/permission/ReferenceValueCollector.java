package dev.planka.card.service.permission;

import dev.planka.domain.link.Path;
import dev.planka.domain.schema.definition.condition.DateConditionItem;
import dev.planka.domain.schema.definition.condition.LinkConditionItem;
import dev.planka.domain.schema.definition.condition.NumberConditionItem;
import dev.planka.domain.schema.definition.condition.ReferenceSource;

import java.util.Map;
import java.util.Set;

/**
 * ReferenceValue 收集器
 * <p>
 * 负责从条件中收集 ReferenceValue 引用的字段和路径
 */
class ReferenceValueCollector {

    /**
     * 从数字操作符中收集 ReferenceValue
     */
    static void collectFromNumberOperator(
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
     * 从数字值中收集 ReferenceValue
     */
    private static void collectFromNumberValue(
            NumberConditionItem.NumberValue value,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (value instanceof NumberConditionItem.NumberValue.ReferenceValue refValue) {
            ReferenceSource source = refValue.getSource();
            String fieldId = refValue.getFieldId();
            collectFromReferenceSource(source, fieldId, fieldIds, pathTree);
        }
    }

    /**
     * 从日期操作符中收集 ReferenceValue
     */
    static void collectFromDateOperator(
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
     * 从日期值中收集 ReferenceValue
     */
    private static void collectFromDateValue(
            DateConditionItem.DateValue value,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (value instanceof DateConditionItem.DateValue.ReferenceValue refValue) {
            ReferenceSource source = refValue.getSource();
            String fieldId = refValue.getFieldId();
            collectFromReferenceSource(source, fieldId, fieldIds, pathTree);
        }
    }

    /**
     * 从关联操作符中收集 ReferenceValue
     */
    static void collectFromLinkOperator(
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
     * 从关联值中收集 ReferenceValue
     */
    private static void collectFromLinkValue(
            LinkConditionItem.LinkValue value,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (value instanceof LinkConditionItem.LinkValue.ReferenceValue refValue) {
            ReferenceSource source = refValue.getSource();
            // 关联值引用的是关联字段本身，不需要额外的 fieldId
            collectFromReferenceSource(source, null, fieldIds, pathTree);
        }
    }

    /**
     * 从 ReferenceSource 中收集字段
     * <p>
     * 注意：这个方法用于收集卡片条件中的引用，不是成员引用
     */
    private static void collectFromReferenceSource(
            ReferenceSource source,
            String fieldId,
            Set<String> fieldIds,
            Map<String, YieldBuilder.PathNode> pathTree) {

        if (source instanceof ReferenceSource.CurrentCard currentCard) {
            Path path = currentCard.getPath();
            if (fieldId != null) {
                YieldBuilder.addFieldToTree(path, fieldId, fieldIds, pathTree);
            }
        } else if (source instanceof ReferenceSource.ParameterCard paramCard) {
            Path path = paramCard.getPath();
            if (fieldId != null) {
                YieldBuilder.addFieldToTree(path, fieldId, fieldIds, pathTree);
            }
        } else if (source instanceof ReferenceSource.ContextualCard contextCard) {
            Path path = contextCard.getPath();
            if (fieldId != null) {
                YieldBuilder.addFieldToTree(path, fieldId, fieldIds, pathTree);
            }
        }
        // Member 类型的引用在这里不处理，由 MemberReferenceCollector 处理
    }
}
