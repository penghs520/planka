package dev.planka.card.converter;

import dev.planka.domain.card.CardStyle;
import dev.planka.domain.schema.definition.condition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 条件转换器
 * 将 domain Condition 转换为 zgraph protobuf Condition
 * 使用 instanceof 类型匹配进行转换
 */
public class ConditionConverter {

    private static final Logger logger = LoggerFactory.getLogger(ConditionConverter.class);

    private ConditionConverter() {
    }

    /**
     * 转换为 protobuf Condition
     */
    public static planka.graph.driver.proto.query.Condition toProto(Condition condition) {
        if (condition == null || condition.isEmpty()) {
            return planka.graph.driver.proto.query.Condition.getDefaultInstance();
        }

        planka.graph.driver.proto.query.Condition.Builder builder = planka.graph.driver.proto.query.Condition.newBuilder();
        if (condition.getRoot() != null) {
            builder.setRoot(toProtoConditionNode(condition.getRoot()));
        }
        return builder.build();
    }

    /**
     * 转换条件节点
     * 使用 instanceof 进行类型匹配
     */
    private static planka.graph.driver.proto.query.ConditionNode toProtoConditionNode(ConditionNode node) {
        if (node == null) {
            return planka.graph.driver.proto.query.ConditionNode.getDefaultInstance();
        }

        planka.graph.driver.proto.query.ConditionNode.Builder builder = planka.graph.driver.proto.query.ConditionNode.newBuilder();

        // 使用 instanceof 进行类型匹配
        if (node instanceof ConditionGroup group) {
            builder.setGroup(toProtoConditionGroup(group));
        } else if (node instanceof TitleConditionItem item) {
            builder.setTitle(toProtoTitleCondition(item));
        } else if (node instanceof CodeConditionItem item) {
            builder.setCode(toProtoCodeCondition(item));
        } else if (node instanceof TextConditionItem item) {
            builder.setText(toProtoTextCondition(item));
        } else if (node instanceof NumberConditionItem item) {
            builder.setNumber(toProtoNumberCondition(item));
        } else if (node instanceof DateConditionItem item) {
            builder.setDate(toProtoDateCondition(item));
        } else if (node instanceof EnumConditionItem item) {
            builder.setEnumItem(toProtoEnumCondition(item));
        } else if (node instanceof StatusConditionItem item) {
            builder.setStatus(toProtoStatusCondition(item));
        } else if (node instanceof CardStyleConditionItem item) {
            builder.setState(toProtoStateCondition(item));
        } else if (node instanceof CardCycleConditionItem item) {
            builder.setState(toProtoCardCycleCondition(item));
        } else if (node instanceof LinkConditionItem item) {
            builder.setLink(toProtoLinkCondition(item));
        } else if (node instanceof KeywordConditionItem item) {
            builder.setKeyword(toProtoKeywordCondition(item));
        } else if (node instanceof SystemUserConditionItem item) {
            // SystemUserConditionItem 需要特殊处理，暂时记录日志
            logger.warn("SystemUserConditionItem 的 proto 转换待实现，nodeType: {}", item.getNodeType());
        } else {
            logger.warn("未知的条件节点类型: {}", node.getClass().getName());
        }

        return builder.build();
    }

    /**
     * 转换条件组
     */
    private static planka.graph.driver.proto.query.ConditionGroup toProtoConditionGroup(ConditionGroup group) {
        planka.graph.driver.proto.query.ConditionGroup.Builder builder = planka.graph.driver.proto.query.ConditionGroup
                .newBuilder();

        // 转换逻辑运算符
        if (group.getOperator() != null) {
            builder.setOperator(group.getOperator() == ConditionGroup.LogicOperator.AND
                    ? planka.graph.driver.proto.query.LogicOperator.AND
                    : planka.graph.driver.proto.query.LogicOperator.OR);
        }

        // 递归转换子节点
        if (group.getChildren() != null) {
            for (ConditionNode child : group.getChildren()) {
                builder.addChildren(toProtoConditionNode(child));
            }
        }

        return builder.build();
    }

    // ==================== 条件项转换 ====================

    private static planka.graph.driver.proto.query.TitleConditionItem toProtoTitleCondition(TitleConditionItem item) {
        planka.graph.driver.proto.query.TitleConditionItem.Builder builder = planka.graph.driver.proto.query.TitleConditionItem
                .newBuilder();

        var operator = item.getOperator();
        planka.graph.driver.proto.query.TitleOperator.Builder opBuilder = planka.graph.driver.proto.query.TitleOperator
                .newBuilder();

        // 使用 instanceof 进行类型匹配
        if (operator instanceof TitleConditionItem.TitleOperator.Contains contains) {
            opBuilder.setContains(planka.graph.driver.proto.query.TitleContainsOperator.newBuilder()
                    .setValue(contains.getValue()).build());
        } else {
            // Proto only defines Contains, Equal, In for Title
            // Domain also has NotContains, IsEmpty, IsNotEmpty which don't exist in proto
            logger.warn("标题操作符类型 {} 在 proto 中不支持", operator.getClass().getSimpleName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static planka.graph.driver.proto.query.CodeConditionItem toProtoCodeCondition(CodeConditionItem item) {
        String value = "";
        var operator = item.getOperator();

        // 使用 instanceof 进行类型匹配
        if (operator instanceof CodeConditionItem.CodeOperator.Equal equal) {
            value = equal.getValue();
        } else if (operator instanceof CodeConditionItem.CodeOperator.Contains contains) {
            value = contains.getValue();
        } else {
            logger.warn("未知的编号操作符类型: {}", operator.getClass().getName());
        }

        return planka.graph.driver.proto.query.CodeConditionItem.newBuilder()
                .setValue(value != null ? value : "")
                .build();
    }

    private static planka.graph.driver.proto.query.TextConditionItem toProtoTextCondition(TextConditionItem item) {
        planka.graph.driver.proto.query.TextConditionItem.Builder builder = planka.graph.driver.proto.query.TextConditionItem
                .newBuilder();

        // 转换 Subject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            planka.graph.driver.proto.query.TextSubject.Builder subjectBuilder = planka.graph.driver.proto.query.TextSubject
                    .newBuilder();
            if (subject.path() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.path()));
            }
            if (subject.fieldId() != null) {
                subjectBuilder.setFieldId(subject.fieldId());
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator - 使用 instanceof 进行类型匹配
        var operator = item.getOperator();
        planka.graph.driver.proto.query.TextOperator.Builder opBuilder = planka.graph.driver.proto.query.TextOperator.newBuilder();

        if (operator instanceof TextConditionItem.TextOperator.Equal equal) {
            opBuilder.setEqual(planka.graph.driver.proto.query.TextEqualOperator.newBuilder()
                    .setValue(equal.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.NotEqual notEqual) {
            opBuilder.setNotEqual(planka.graph.driver.proto.query.TextNotEqualOperator.newBuilder()
                    .setValue(notEqual.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.Contains contains) {
            opBuilder.setContains(planka.graph.driver.proto.query.TextContainsOperator.newBuilder()
                    .setValue(contains.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.NotContains notContains) {
            opBuilder.setNotContains(planka.graph.driver.proto.query.TextNotContainsOperator.newBuilder()
                    .setValue(notContains.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.StartsWith startsWith) {
            opBuilder.setStartsWith(planka.graph.driver.proto.query.TextStartsWithOperator.newBuilder()
                    .setValue(startsWith.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.EndsWith endsWith) {
            opBuilder.setEndsWith(planka.graph.driver.proto.query.TextEndsWithOperator.newBuilder()
                    .setValue(endsWith.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.IsEmpty) {
            opBuilder.setIsBlank(planka.graph.driver.proto.query.TextIsBlankOperator.getDefaultInstance());
        } else if (operator instanceof TextConditionItem.TextOperator.IsNotEmpty) {
            opBuilder.setIsNotBlank(planka.graph.driver.proto.query.TextIsNotBlankOperator.getDefaultInstance());
        } else {
            logger.warn("未知的文本操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static planka.graph.driver.proto.query.NumberConditionItem toProtoNumberCondition(NumberConditionItem item) {
        planka.graph.driver.proto.query.NumberConditionItem.Builder builder = planka.graph.driver.proto.query.NumberConditionItem
                .newBuilder();

        // 转换 Subject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            planka.graph.driver.proto.query.NumberSubject.Builder subjectBuilder = planka.graph.driver.proto.query.NumberSubject
                    .newBuilder();
            if (subject.path() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.path()));
            }
            if (subject.fieldId() != null) {
                subjectBuilder.setFieldId(subject.fieldId());
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator - 使用 instanceof 进行类型匹配
        var operator = item.getOperator();
        planka.graph.driver.proto.query.NumberOperator.Builder opBuilder = planka.graph.driver.proto.query.NumberOperator
                .newBuilder();

        if (operator instanceof NumberConditionItem.NumberOperator.Equal equal) {
            double value = extractNumberValue(equal.getValue());
            opBuilder.setEqual(planka.graph.driver.proto.query.NumberEqualOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.NotEqual notEqual) {
            double value = extractNumberValue(notEqual.getValue());
            opBuilder.setNotEqual(planka.graph.driver.proto.query.NumberNotEqualOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.GreaterThan gt) {
            double value = extractNumberValue(gt.getValue());
            opBuilder.setGreaterThan(planka.graph.driver.proto.query.NumberGreaterThanOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.LessThan lt) {
            double value = extractNumberValue(lt.getValue());
            opBuilder.setLessThan(planka.graph.driver.proto.query.NumberLessThanOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.GreaterThanOrEqual gte) {
            double value = extractNumberValue(gte.getValue());
            opBuilder.setGreaterThanOrEqual(planka.graph.driver.proto.query.NumberGreaterThanOrEqualOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.LessThanOrEqual lte) {
            double value = extractNumberValue(lte.getValue());
            opBuilder.setLessThanOrEqual(planka.graph.driver.proto.query.NumberLessThanOrEqualOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.Between between) {
            double startValue = extractNumberValue(between.getStart());
            double endValue = extractNumberValue(between.getEnd());
            opBuilder.setBetween(planka.graph.driver.proto.query.NumberBetweenOperator.newBuilder()
                    .setMinValue(startValue)
                    .setMaxValue(endValue).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.IsEmpty) {
            opBuilder.setIsNull(planka.graph.driver.proto.query.NumberIsNullOperator.getDefaultInstance());
        } else if (operator instanceof NumberConditionItem.NumberOperator.IsNotEmpty) {
            opBuilder.setIsNotNull(planka.graph.driver.proto.query.NumberIsNotNullOperator.getDefaultInstance());
        } else {
            logger.warn("未知的数字操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    /**
     * 从 NumberValue 中提取数字值
     * 仅支持 StaticValue，ReferenceValue 需要运行时解析
     */
    private static double extractNumberValue(NumberConditionItem.NumberValue numberValue) {
        if (numberValue instanceof NumberConditionItem.NumberValue.StaticValue staticValue) {
            return staticValue.getValue() != null ? staticValue.getValue() : 0.0;
        }
        // ReferenceValue 需要运行时解析，这里返回0
        logger.debug("NumberValue.ReferenceValue 需要运行时解析，当前返回默认值0");
        return 0.0;
    }

    private static planka.graph.driver.proto.query.DateConditionItem toProtoDateCondition(DateConditionItem item) {
        planka.graph.driver.proto.query.DateConditionItem.Builder builder = planka.graph.driver.proto.query.DateConditionItem
                .newBuilder();

        // 转换 Subject - 支持自定义字段和系统字段
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            planka.graph.driver.proto.query.DateSubject.Builder subjectBuilder = planka.graph.driver.proto.query.DateSubject
                    .newBuilder();
            if (subject.getPath() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.getPath()));
            }
            // 根据 Subject 类型设置 fieldId
            if (subject instanceof DateConditionItem.DateSubject.FieldDateSubject fieldSubject) {
                subjectBuilder.setFieldId(fieldSubject.getFieldId());
            } else if (subject instanceof DateConditionItem.DateSubject.SystemDateSubject systemSubject) {
                // 系统日期字段使用枚举名作为标识
                subjectBuilder.setFieldId(systemSubject.getSystemField().name());
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator - 使用 instanceof 进行类型匹配
        var operator = item.getOperator();
        planka.graph.driver.proto.query.DateOperator.Builder opBuilder = planka.graph.driver.proto.query.DateOperator.newBuilder();

        if (operator instanceof DateConditionItem.DateOperator.Equal equal) {
            long value = extractDateValue(equal.getValue());
            opBuilder.setEqual(planka.graph.driver.proto.query.DateEqualOperator.newBuilder()
                    .setStaticValue(value).build());
        } else if (operator instanceof DateConditionItem.DateOperator.Before before) {
            long value = extractDateValue(before.getValue());
            opBuilder.setBefore(planka.graph.driver.proto.query.DateBeforeOperator.newBuilder()
                    .setStaticValue(value).build());
        } else if (operator instanceof DateConditionItem.DateOperator.After after) {
            long value = extractDateValue(after.getValue());
            opBuilder.setAfter(planka.graph.driver.proto.query.DateAfterOperator.newBuilder()
                    .setStaticValue(value).build());
        } else if (operator instanceof DateConditionItem.DateOperator.Between between) {
            long startValue = extractDateValue(between.getStart());
            long endValue = extractDateValue(between.getEnd());
            opBuilder.setBetween(planka.graph.driver.proto.query.DateBetweenOperator.newBuilder()
                    .setStaticStartValue(startValue)
                    .setStaticEndValue(endValue).build());
        } else if (operator instanceof DateConditionItem.DateOperator.IsEmpty) {
            opBuilder.setIsNull(planka.graph.driver.proto.query.DateIsNullOperator.getDefaultInstance());
        } else if (operator instanceof DateConditionItem.DateOperator.IsNotEmpty) {
            opBuilder.setIsNotNull(planka.graph.driver.proto.query.DateIsNotNullOperator.getDefaultInstance());
        } else {
            logger.warn("未知的日期操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    /**
     * 从 DateValue 中提取时间戳
     * 仅支持 Specific，KeyDateValue 和 ReferenceValue 需要运行时解析
     */
    private static long extractDateValue(DateConditionItem.DateValue dateValue) {
        if (dateValue instanceof DateConditionItem.DateValue.Specific specific) {
            // 尝试解析日期字符串为时间戳
            String dateStr = specific.getValue();
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    // 简化处理：假设是毫秒时间戳或解析失败返回0
                    return Long.parseLong(dateStr);
                } catch (NumberFormatException e) {
                    // 如果不是时间戳格式，返回当前时间作为占位符
                    logger.debug("无法解析日期字符串: {}", dateStr);
                    return System.currentTimeMillis();
                }
            }
        }
        // KeyDateValue 和 ReferenceValue 需要运行时解析
        logger.debug("DateValue 类型 {} 需要运行时解析，当前返回当前时间戳", dateValue.getClass().getSimpleName());
        return System.currentTimeMillis();
    }

    private static planka.graph.driver.proto.query.EnumConditionItem toProtoEnumCondition(EnumConditionItem item) {
        planka.graph.driver.proto.query.EnumConditionItem.Builder builder = planka.graph.driver.proto.query.EnumConditionItem
                .newBuilder();

        // 转换 Subject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            planka.graph.driver.proto.query.EnumSubject.Builder subjectBuilder = planka.graph.driver.proto.query.EnumSubject
                    .newBuilder();
            if (subject.path() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.path()));
            }
            if (subject.fieldId() != null) {
                subjectBuilder.setFieldId(subject.fieldId());
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator - 使用 instanceof 进行类型匹配
        var operator = item.getOperator();
        planka.graph.driver.proto.query.EnumOperator.Builder opBuilder = planka.graph.driver.proto.query.EnumOperator.newBuilder();

        if (operator instanceof EnumConditionItem.EnumOperator.Equal equal) {
            opBuilder.setEqual(planka.graph.driver.proto.query.EnumEqualOperator.newBuilder()
                    .setStaticValues(planka.graph.driver.proto.query.EnumStaticValues.newBuilder()
                            .addValues(equal.getOptionId()).build())
                    .build());
        } else if (operator instanceof EnumConditionItem.EnumOperator.NotEqual notEqual) {
            opBuilder.setNotEqual(planka.graph.driver.proto.query.EnumNotEqualOperator.newBuilder()
                    .setStaticValues(planka.graph.driver.proto.query.EnumStaticValues.newBuilder()
                            .addValues(notEqual.getOptionId()).build())
                    .build());
        } else if (operator instanceof EnumConditionItem.EnumOperator.In in) {
            opBuilder.setIn(planka.graph.driver.proto.query.EnumInOperator.newBuilder()
                    .setStaticValues(planka.graph.driver.proto.query.EnumStaticValues.newBuilder()
                            .addAllValues(in.getOptionIds() != null ? in.getOptionIds() : List.of()).build())
                    .build());
        } else if (operator instanceof EnumConditionItem.EnumOperator.NotIn notIn) {
            opBuilder.setNotIn(planka.graph.driver.proto.query.EnumNotInOperator.newBuilder()
                    .setStaticValues(planka.graph.driver.proto.query.EnumStaticValues.newBuilder()
                            .addAllValues(notIn.getOptionIds() != null ? notIn.getOptionIds() : List.of()).build())
                    .build());
        } else if (operator instanceof EnumConditionItem.EnumOperator.IsEmpty) {
            opBuilder.setIsNull(planka.graph.driver.proto.query.EnumIsNullOperator.getDefaultInstance());
        } else if (operator instanceof EnumConditionItem.EnumOperator.IsNotEmpty) {
            opBuilder.setIsNotNull(planka.graph.driver.proto.query.EnumIsNotNullOperator.getDefaultInstance());
        } else {
            logger.warn("未知的枚举操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static planka.graph.driver.proto.query.StatusConditionItem toProtoStatusCondition(StatusConditionItem item) {
        planka.graph.driver.proto.query.StatusConditionItem.Builder builder = planka.graph.driver.proto.query.StatusConditionItem
                .newBuilder();

        // 转换 Subject - StatusSubject 是 record，从 subject 中获取 streamId
        String streamId = "";
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            streamId = subject.streamId() != null ? subject.streamId() : "";
            planka.graph.driver.proto.query.StatusSubject.Builder subjectBuilder = planka.graph.driver.proto.query.StatusSubject
                    .newBuilder();
            if (subject.path() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.path()));
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator - 使用 instanceof 进行类型匹配
        var operator = item.getOperator();
        planka.graph.driver.proto.query.StatusOperator.Builder opBuilder = planka.graph.driver.proto.query.StatusOperator
                .newBuilder();

        if (operator instanceof StatusConditionItem.StatusOperator.Equal equal) {
            opBuilder.setEqual(planka.graph.driver.proto.query.StatusEqualOperator.newBuilder()
                    .setStreamId(streamId)
                    .setStatusId(equal.getStatusId() != null ? equal.getStatusId() : "")
                    .build());
        } else if (operator instanceof StatusConditionItem.StatusOperator.NotEqual notEqual) {
            opBuilder.setNotEqual(planka.graph.driver.proto.query.StatusNotEqualOperator.newBuilder()
                    .setStreamId(streamId)
                    .setStatusId(notEqual.getStatusId() != null ? notEqual.getStatusId() : "")
                    .build());
        } else if (operator instanceof StatusConditionItem.StatusOperator.In in) {
            opBuilder.setIn(planka.graph.driver.proto.query.StatusInOperator.newBuilder()
                    .setStreamId(streamId)
                    .addAllValues(in.getStatusIds() != null ? in.getStatusIds() : List.of())
                    .build());
        } else if (operator instanceof StatusConditionItem.StatusOperator.NotIn notIn) {
            opBuilder.setNotIn(planka.graph.driver.proto.query.StatusNotInOperator.newBuilder()
                    .setStreamId(streamId)
                    .addAllValues(notIn.getStatusIds() != null ? notIn.getStatusIds() : List.of())
                    .build());
        } else {
            logger.warn("未知的状态操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static planka.graph.driver.proto.query.StateConditionItem toProtoStateCondition(CardStyleConditionItem item) {
        planka.graph.driver.proto.query.StateConditionItem.Builder builder = planka.graph.driver.proto.query.StateConditionItem
                .newBuilder();

        // 转换 Subject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            planka.graph.driver.proto.query.StateSubject.Builder subjectBuilder = planka.graph.driver.proto.query.StateSubject
                    .newBuilder();
            if (subject.path() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.path()));
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator - 使用 instanceof 进行类型匹配
        var operator = item.getOperator();
        planka.graph.driver.proto.query.StateOperator.Builder opBuilder = planka.graph.driver.proto.query.StateOperator
                .newBuilder();

        if (operator instanceof CardStyleConditionItem.StyleOperator.Equal equal) {
            CardStyle state = equal.getValue();
            opBuilder.setEqual(planka.graph.driver.proto.query.StateEqualOperator.newBuilder()
                    .setValue(state != null ? state.name() : "")
                    .build());
        } else if (operator instanceof CardStyleConditionItem.StyleOperator.NotEqual notEqual) {
            CardStyle state = notEqual.getValue();
            opBuilder.setNotEqual(planka.graph.driver.proto.query.StateNotEqualOperator.newBuilder()
                    .setValue(state != null ? state.name() : "")
                    .build());
        } else if (operator instanceof CardStyleConditionItem.StyleOperator.In in) {
            List<String> stateNames = in.getValues() != null
                    ? in.getValues().stream().map(Enum::name).toList()
                    : List.of();
            opBuilder.setIn(planka.graph.driver.proto.query.StateInOperator.newBuilder()
                    .addAllValues(stateNames)
                    .build());
        } else if (operator instanceof CardStyleConditionItem.StyleOperator.NotIn notIn) {
            List<String> stateNames = notIn.getValues() != null
                    ? notIn.getValues().stream().map(Enum::name).toList()
                    : List.of();
            opBuilder.setNotIn(planka.graph.driver.proto.query.StateNotInOperator.newBuilder()
                    .addAllValues(stateNames)
                    .build());
        } else {
            logger.warn("未知的生命周期状态操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static planka.graph.driver.proto.query.LinkConditionItem toProtoLinkCondition(LinkConditionItem item) {
        planka.graph.driver.proto.query.LinkConditionItem.Builder builder = planka.graph.driver.proto.query.LinkConditionItem
                .newBuilder();

        // 转换 Subject - RelationSubject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            planka.graph.driver.proto.query.LinkSubject.Builder subjectBuilder = planka.graph.driver.proto.query.LinkSubject
                    .newBuilder();
            if (subject.path() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.path()));
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator - 使用 instanceof 进行类型匹配
        // 注意：LinkConditionItem 使用的是 RelationOperator，不是 LinkOperator
        var operator = item.getOperator();
        planka.graph.driver.proto.query.LinkOperator.Builder opBuilder = planka.graph.driver.proto.query.LinkOperator.newBuilder();

        if (operator instanceof LinkConditionItem.LinkOperator.IsEmpty) {
            opBuilder.setIsNull(planka.graph.driver.proto.query.LinkIsNullOperator.getDefaultInstance());
        } else if (operator instanceof LinkConditionItem.LinkOperator.HasAny) {
            opBuilder.setIsNotNull(planka.graph.driver.proto.query.LinkIsNotNullOperator.getDefaultInstance());
        } else if (operator instanceof LinkConditionItem.LinkOperator.In in) {
            planka.graph.driver.proto.query.LinkInOperator.Builder inOpBuilder = planka.graph.driver.proto.query.LinkInOperator
                    .newBuilder();
            buildLinkOperatorValue(in.getValue(), inOpBuilder);
            opBuilder.setIn(inOpBuilder.build());
        } else if (operator instanceof LinkConditionItem.LinkOperator.NotIn notIn) {
            planka.graph.driver.proto.query.LinkNotInOperator.Builder notInOpBuilder = planka.graph.driver.proto.query.LinkNotInOperator
                    .newBuilder();
            buildLinkOperatorValue(notIn.getValue(), notInOpBuilder);
            opBuilder.setNotIn(notInOpBuilder.build());
        } else {
            logger.warn("未知的关联操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static planka.graph.driver.proto.query.KeywordConditionItem toProtoKeywordCondition(KeywordConditionItem item) {
        String keyword = "";
        var operator = item.getOperator();

        // 使用 instanceof 进行类型匹配
        if (operator instanceof KeywordConditionItem.KeywordOperator.Contains contains) {
            keyword = contains.getKeyword();
        } else if (operator instanceof KeywordConditionItem.KeywordOperator.NotContains notContains) {
            keyword = notContains.getKeyword();
        } else {
            logger.warn("未知的关键词操作符类型: {}", operator.getClass().getName());
        }

        return planka.graph.driver.proto.query.KeywordConditionItem.newBuilder()
                .setValue(keyword != null ? keyword : "")
                .build();
    }

    /**
     * 构建 LinkInOperator 的值（ReferLink 或 SpecialLink）
     */
    private static void buildLinkOperatorValue(LinkConditionItem.LinkValue value,
            planka.graph.driver.proto.query.LinkInOperator.Builder builder) {
        if (value == null) {
            return;
        }

        if (value instanceof LinkConditionItem.LinkValue.StaticValue staticValue) {
            // 静态卡片列表 -> SpecialLink
            planka.graph.driver.proto.query.SpecialLink.Builder specialLinkBuilder = planka.graph.driver.proto.query.SpecialLink
                    .newBuilder();
            if (staticValue.getCardIds() != null) {
                for (String cardId : staticValue.getCardIds()) {
                    try {
                        specialLinkBuilder.addCardIds(Long.parseLong(cardId));
                    } catch (NumberFormatException e) {
                        logger.warn("无法将卡片ID转换为Long: {}", cardId);
                    }
                }
            }
            builder.setSpecialLink(specialLinkBuilder.build());
        } else if (value instanceof LinkConditionItem.LinkValue.ReferenceValue referenceValue) {
            // 引用值 -> ReferLink
            builder.setReferLink(buildReferLink(referenceValue.getSource()));
        } else {
            logger.warn("未知的LinkValue类型: {}", value.getClass().getName());
        }
    }

    /**
     * 构建 LinkNotInOperator 的值（ReferLink 或 SpecialLink）
     */
    private static void buildLinkOperatorValue(LinkConditionItem.LinkValue value,
            planka.graph.driver.proto.query.LinkNotInOperator.Builder builder) {
        if (value == null) {
            return;
        }

        if (value instanceof LinkConditionItem.LinkValue.StaticValue staticValue) {
            // 静态卡片列表 -> SpecialLink
            planka.graph.driver.proto.query.SpecialLink.Builder specialLinkBuilder = planka.graph.driver.proto.query.SpecialLink
                    .newBuilder();
            if (staticValue.getCardIds() != null) {
                for (String cardId : staticValue.getCardIds()) {
                    try {
                        specialLinkBuilder.addCardIds(Long.parseLong(cardId));
                    } catch (NumberFormatException e) {
                        logger.warn("无法将卡片ID转换为Long: {}", cardId);
                    }
                }
            }
            builder.setSpecialLink(specialLinkBuilder.build());
        } else if (value instanceof LinkConditionItem.LinkValue.ReferenceValue referenceValue) {
            // 引用值 -> ReferLink
            builder.setReferLink(buildReferLink(referenceValue.getSource()));
        } else {
            logger.warn("未知的LinkValue类型: {}", value.getClass().getName());
        }
    }

    /**
     * 构建 ReferLink（引用链接）
     */
    private static planka.graph.driver.proto.query.ReferLink buildReferLink(ReferenceSource source) {
        planka.graph.driver.proto.query.ReferLink.Builder builder = planka.graph.driver.proto.query.ReferLink.newBuilder();

        if (source instanceof ReferenceSource.CurrentCard currentCard) {
            planka.graph.driver.proto.query.ReferOnCurrentCard.Builder refBuilder = planka.graph.driver.proto.query.ReferOnCurrentCard
                    .newBuilder();
            if (currentCard.getPath() != null) {
                refBuilder.setPath(PathConverter.toProto(currentCard.getPath()));
            }
            builder.setReferOnCurrentCard(refBuilder.build());
        } else if (source instanceof ReferenceSource.ParameterCard parameterCard) {
            planka.graph.driver.proto.query.ReferOnParameterCard.Builder refBuilder = planka.graph.driver.proto.query.ReferOnParameterCard
                    .newBuilder();
            if (parameterCard.getPath() != null) {
                refBuilder.setPath(PathConverter.toProto(parameterCard.getPath()));
            }
            if (parameterCard.getParameterCardTypeId() != null) {
                refBuilder.setParameterCardTypeId(parameterCard.getParameterCardTypeId());
            }
            builder.setReferOnParameterCard(refBuilder.build());
        } else if (source instanceof ReferenceSource.Member member) {
            planka.graph.driver.proto.query.ReferOnMember.Builder refBuilder = planka.graph.driver.proto.query.ReferOnMember
                    .newBuilder();
            if (member.getPath() != null) {
                refBuilder.setPath(PathConverter.toProto(member.getPath()));
            }
            builder.setReferOnMember(refBuilder.build());
        } else if (source instanceof ReferenceSource.ContextualCard contextualCard) {
            planka.graph.driver.proto.query.ReferOnContextualCard.Builder refBuilder = planka.graph.driver.proto.query.ReferOnContextualCard
                    .newBuilder();
            if (contextualCard.getPath() != null) {
                refBuilder.setPath(PathConverter.toProto(contextualCard.getPath()));
            }
            if (contextualCard.getContextualVertexId() != null) {
                refBuilder.setContextualCardId(contextualCard.getContextualVertexId());
            }
            builder.setReferOnContextualCard(refBuilder.build());
        } else {
            logger.warn("未知的ReferenceSource类型: {}", source != null ? source.getClass().getName() : "null");
        }

        return builder.build();
    }

    /**
     * 转换卡片生命周期条件项（CARD_CYCLE）
     * 复用 StateConditionItem 的 proto 结构
     */
    private static planka.graph.driver.proto.query.StateConditionItem toProtoCardCycleCondition(CardCycleConditionItem item) {
        planka.graph.driver.proto.query.StateConditionItem.Builder builder = planka.graph.driver.proto.query.StateConditionItem
                .newBuilder();

        // 转换 Subject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            planka.graph.driver.proto.query.StateSubject.Builder subjectBuilder = planka.graph.driver.proto.query.StateSubject
                    .newBuilder();
            if (subject.path() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.path()));
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator
        var operator = item.getOperator();
        planka.graph.driver.proto.query.StateOperator.Builder opBuilder = planka.graph.driver.proto.query.StateOperator
                .newBuilder();

        if (operator instanceof CardCycleConditionItem.LifecycleOperator.In in) {
            List<String> stateNames = in.getValues() != null
                    ? in.getValues().stream().map(Enum::name).toList()
                    : List.of();
            opBuilder.setIn(planka.graph.driver.proto.query.StateInOperator.newBuilder()
                    .addAllValues(stateNames)
                    .build());
        } else if (operator instanceof CardCycleConditionItem.LifecycleOperator.NotIn notIn) {
            List<String> stateNames = notIn.getValues() != null
                    ? notIn.getValues().stream().map(Enum::name).toList()
                    : List.of();
            opBuilder.setNotIn(planka.graph.driver.proto.query.StateNotInOperator.newBuilder()
                    .addAllValues(stateNames)
                    .build());
        } else {
            logger.warn("未知的卡片生命周期操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

}
