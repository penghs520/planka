package dev.planka.schema.service.condition;

import dev.planka.domain.link.Path;
import dev.planka.domain.schema.definition.condition.*;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 条件 ID 收集器
 * <p>
 * 遍历 Condition 树，收集所有需要解析显示名称的 ID
 */
@Getter
public class ConditionIdCollector {

    private final Set<String> fieldIds = new HashSet<>();
    private final Set<String> linkFieldIds = new HashSet<>();
    private final Set<String> enumFieldIds = new HashSet<>();
    private final Set<String> cardIds = new HashSet<>();
    private final Set<String> statusIds = new HashSet<>();

    /**
     * streamId -> Set<statusId> 的映射
     * <p>
     * 用于批量解析状态名称，需要知道每个 statusId 属于哪个 stream
     */
    private final Map<String, Set<String>> streamStatusMap = new HashMap<>();

    /**
     * 收集条件中的所有 ID
     */
    public void collect(Condition condition) {
        if (condition == null || condition.getRoot() == null) {
            return;
        }
        collectFromNode(condition.getRoot());
    }

    private void collectFromNode(ConditionNode node) {
        if (node instanceof ConditionGroup group) {
            if (group.getChildren() != null) {
                group.getChildren().forEach(this::collectFromNode);
            }
        } else if (node instanceof AbstractConditionItem) {
            collectFromItem(node);
        }
    }

    private void collectFromItem(ConditionNode node) {
        if (node instanceof TextConditionItem item) {
            collectFromTextItem(item);
        } else if (node instanceof NumberConditionItem item) {
            collectFromNumberItem(item);
        } else if (node instanceof DateConditionItem item) {
            collectFromDateItem(item);
        } else if (node instanceof EnumConditionItem item) {
            collectFromEnumItem(item);
        } else if (node instanceof StatusConditionItem item) {
            collectFromStatusItem(item);
        } else if (node instanceof LinkConditionItem item) {
            collectFromLinkItem(item);
        } else if (node instanceof SystemUserConditionItem item) {
            collectFromSystemUserItem(item);
        }
        // 其他类型暂不处理（如 TitleConditionItem, CodeConditionItem 等系统字段）
    }

    private void collectFromTextItem(TextConditionItem item) {
        if (item.getSubject() != null) {
            collectPath(item.getSubject().path());
            if (item.getSubject().fieldId() != null) {
                fieldIds.add(item.getSubject().fieldId());
            }
        }
    }

    private void collectFromNumberItem(NumberConditionItem item) {
        if (item.getSubject() != null) {
            collectPath(item.getSubject().path());
            if (item.getSubject().fieldId() != null) {
                fieldIds.add(item.getSubject().fieldId());
            }
        }
        // 收集引用值中的路径
        collectFromNumberOperator(item.getOperator());
    }

    private void collectFromNumberOperator(NumberConditionItem.NumberOperator operator) {
        if (operator == null) return;

        if (operator instanceof NumberConditionItem.NumberOperator.Equal eq) {
            collectFromNumberValue(eq.getValue());
        } else if (operator instanceof NumberConditionItem.NumberOperator.NotEqual ne) {
            collectFromNumberValue(ne.getValue());
        } else if (operator instanceof NumberConditionItem.NumberOperator.GreaterThan gt) {
            collectFromNumberValue(gt.getValue());
        } else if (operator instanceof NumberConditionItem.NumberOperator.GreaterThanOrEqual ge) {
            collectFromNumberValue(ge.getValue());
        } else if (operator instanceof NumberConditionItem.NumberOperator.LessThan lt) {
            collectFromNumberValue(lt.getValue());
        } else if (operator instanceof NumberConditionItem.NumberOperator.LessThanOrEqual le) {
            collectFromNumberValue(le.getValue());
        } else if (operator instanceof NumberConditionItem.NumberOperator.Between between) {
            collectFromNumberValue(between.getStart());
            collectFromNumberValue(between.getEnd());
        }
    }

    private void collectFromNumberValue(NumberConditionItem.NumberValue value) {
        if (value instanceof NumberConditionItem.NumberValue.ReferenceValue ref) {
            collectFromReferenceSource(ref.getSource());
            if (ref.getFieldId() != null) {
                fieldIds.add(ref.getFieldId());
            }
        }
    }

    private void collectFromDateItem(DateConditionItem item) {
        if (item.getSubject() != null) {
            collectPath(item.getSubject().getPath());
            if (item.getSubject() instanceof DateConditionItem.DateSubject.FieldDateSubject fieldSubject) {
                if (fieldSubject.getFieldId() != null) {
                    fieldIds.add(fieldSubject.getFieldId());
                }
            }
        }
        // 收集引用值中的路径
        collectFromDateOperator(item.getOperator());
    }

    private void collectFromDateOperator(DateConditionItem.DateOperator operator) {
        if (operator == null) return;

        if (operator instanceof DateConditionItem.DateOperator.Equal eq) {
            collectFromDateValue(eq.getValue());
        } else if (operator instanceof DateConditionItem.DateOperator.NotEqual ne) {
            collectFromDateValue(ne.getValue());
        } else if (operator instanceof DateConditionItem.DateOperator.Before before) {
            collectFromDateValue(before.getValue());
        } else if (operator instanceof DateConditionItem.DateOperator.After after) {
            collectFromDateValue(after.getValue());
        } else if (operator instanceof DateConditionItem.DateOperator.Between between) {
            collectFromDateValue(between.getStart());
            collectFromDateValue(between.getEnd());
        }
    }

    private void collectFromDateValue(DateConditionItem.DateValue value) {
        if (value instanceof DateConditionItem.DateValue.ReferenceValue ref) {
            collectFromReferenceSource(ref.getSource());
            if (ref.getFieldId() != null) {
                fieldIds.add(ref.getFieldId());
            }
        }
    }

    private void collectFromEnumItem(EnumConditionItem item) {
        if (item.getSubject() != null) {
            collectPath(item.getSubject().path());
            if (item.getSubject().fieldId() != null) {
                fieldIds.add(item.getSubject().fieldId());
                enumFieldIds.add(item.getSubject().fieldId());
            }
        }
    }

    private void collectFromStatusItem(StatusConditionItem item) {
        if (item.getSubject() != null) {
            collectPath(item.getSubject().path());
            String streamId = item.getSubject().streamId();
            if (streamId != null) {
                statusIds.add(streamId);
                // 收集操作符中的 statusId
                collectStatusIdsFromOperator(streamId, item.getOperator());
            }
        }
    }

    private void collectStatusIdsFromOperator(String streamId, StatusConditionItem.StatusOperator operator) {
        if (operator == null) return;

        Set<String> statusIdSet = streamStatusMap.computeIfAbsent(streamId, k -> new HashSet<>());

        if (operator instanceof StatusConditionItem.StatusOperator.Equal eq && eq.getStatusId() != null) {
            statusIdSet.add(eq.getStatusId());
        } else if (operator instanceof StatusConditionItem.StatusOperator.NotEqual ne && ne.getStatusId() != null) {
            statusIdSet.add(ne.getStatusId());
        } else if (operator instanceof StatusConditionItem.StatusOperator.In in && in.getStatusIds() != null) {
            statusIdSet.addAll(in.getStatusIds());
        } else if (operator instanceof StatusConditionItem.StatusOperator.NotIn notIn && notIn.getStatusIds() != null) {
            statusIdSet.addAll(notIn.getStatusIds());
        } else if (operator instanceof StatusConditionItem.StatusOperator.Reached reached && reached.getStatusId() != null) {
            statusIdSet.add(reached.getStatusId());
        } else if (operator instanceof StatusConditionItem.StatusOperator.NotReached notReached && notReached.getStatusId() != null) {
            statusIdSet.add(notReached.getStatusId());
        } else if (operator instanceof StatusConditionItem.StatusOperator.Passed passed && passed.getStatusId() != null) {
            statusIdSet.add(passed.getStatusId());
        }
    }

    private void collectFromLinkItem(LinkConditionItem item) {
        if (item.getSubject() != null) {
            collectPath(item.getSubject().path());
            if (item.getSubject().linkFieldId() != null) {
                linkFieldIds.add(item.getSubject().linkFieldId().toString());
            }
        }
        // 收集操作符中的值
        collectFromLinkOperator(item.getOperator());
    }

    private void collectFromLinkOperator(LinkConditionItem.LinkOperator operator) {
        if (operator == null) return;

        LinkConditionItem.LinkValue value = null;
        if (operator instanceof LinkConditionItem.LinkOperator.In in) {
            value = in.getValue();
        } else if (operator instanceof LinkConditionItem.LinkOperator.NotIn notIn) {
            value = notIn.getValue();
        }

        if (value != null) {
            collectFromLinkValue(value);
        }
    }

    private void collectFromLinkValue(LinkConditionItem.LinkValue value) {
        if (value instanceof LinkConditionItem.LinkValue.StaticValue staticValue) {
            // 收集静态卡片 ID
            if (staticValue.getCardIds() != null) {
                cardIds.addAll(staticValue.getCardIds());
            }
        } else if (value instanceof LinkConditionItem.LinkValue.ReferenceValue refValue) {
            // 收集引用路径中的 linkFieldIds
            collectFromReferenceSource(refValue.getSource());
        }
    }

    private void collectFromSystemUserItem(SystemUserConditionItem item) {
        // SystemUserConditionItem 使用系统字段，暂不需要收集额外 ID
        // 如果有 path，收集路径中的 linkFieldIds
        if (item.getSubject() != null) {
            collectPath(item.getSubject().path());
        }
    }

    private void collectFromReferenceSource(ReferenceSource source) {
        if (source == null) return;

        Path path = null;
        if (source instanceof ReferenceSource.CurrentCard currentCard) {
            path = currentCard.getPath();
        } else if (source instanceof ReferenceSource.ParameterCard paramCard) {
            path = paramCard.getPath();
        } else if (source instanceof ReferenceSource.Member member) {
            path = member.getPath();
        } else if (source instanceof ReferenceSource.ContextualCard contextual) {
            path = contextual.getPath();
        }

        collectPath(path);
    }

    private void collectPath(Path path) {
        if (path != null && path.linkNodes() != null) {
            linkFieldIds.addAll(path.linkNodes());
        }
    }
}
