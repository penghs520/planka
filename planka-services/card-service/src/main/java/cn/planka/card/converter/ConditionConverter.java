package cn.planka.card.converter;

import cn.planka.domain.schema.definition.condition.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    public static zgraph.driver.proto.query.Condition toProto(Condition condition) {
        if (condition == null || condition.isEmpty()) {
            return zgraph.driver.proto.query.Condition.getDefaultInstance();
        }

        zgraph.driver.proto.query.Condition.Builder builder = zgraph.driver.proto.query.Condition.newBuilder();
        if (condition.getRoot() != null) {
            builder.setRoot(toProtoConditionNode(condition.getRoot()));
        }
        return builder.build();
    }

    /**
     * 转换条件节点
     * 使用 instanceof 进行类型匹配
     */
    private static zgraph.driver.proto.query.ConditionNode toProtoConditionNode(ConditionNode node) {
        if (node == null) {
            return zgraph.driver.proto.query.ConditionNode.getDefaultInstance();
        }

        zgraph.driver.proto.query.ConditionNode.Builder builder = zgraph.driver.proto.query.ConditionNode.newBuilder();

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
        } else if (node instanceof CardCycleConditionItem item) {
            builder.setState(toProtoCardCycleCondition(item));
        } else if (node instanceof LinkConditionItem item) {
            builder.setLink(toProtoLinkCondition(item));
        } else if (node instanceof KeywordConditionItem item) {
            builder.setKeyword(toProtoKeywordCondition(item));
        } else {
            logger.warn("未知的条件节点类型: {}", node.getClass().getName());
        }

        return builder.build();
    }

    /**
     * 转换条件组
     */
    private static zgraph.driver.proto.query.ConditionGroup toProtoConditionGroup(ConditionGroup group) {
        zgraph.driver.proto.query.ConditionGroup.Builder builder = zgraph.driver.proto.query.ConditionGroup
                .newBuilder();

        // 转换逻辑运算符
        if (group.getOperator() != null) {
            builder.setOperator(group.getOperator() == ConditionGroup.LogicOperator.AND
                    ? zgraph.driver.proto.query.LogicOperator.AND
                    : zgraph.driver.proto.query.LogicOperator.OR);
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

    private static zgraph.driver.proto.query.TitleConditionItem toProtoTitleCondition(TitleConditionItem item) {
        zgraph.driver.proto.query.TitleConditionItem.Builder builder = zgraph.driver.proto.query.TitleConditionItem
                .newBuilder();

        var operator = item.getOperator();
        zgraph.driver.proto.query.TitleOperator.Builder opBuilder = zgraph.driver.proto.query.TitleOperator
                .newBuilder();

        // 使用 instanceof 进行类型匹配
        if (operator instanceof TitleConditionItem.TitleOperator.Contains contains) {
            opBuilder.setContains(zgraph.driver.proto.query.TitleContainsOperator.newBuilder()
                    .setValue(contains.getValue()).build());
        } else {
            // Proto only defines Contains, Equal, In for Title
            // Domain also has NotContains, IsEmpty, IsNotEmpty which don't exist in proto
            logger.warn("标题操作符类型 {} 在 proto 中不支持", operator.getClass().getSimpleName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static zgraph.driver.proto.query.CodeConditionItem toProtoCodeCondition(CodeConditionItem item) {
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

        return zgraph.driver.proto.query.CodeConditionItem.newBuilder()
                .setValue(value != null ? value : "")
                .build();
    }

    private static zgraph.driver.proto.query.TextConditionItem toProtoTextCondition(TextConditionItem item) {
        zgraph.driver.proto.query.TextConditionItem.Builder builder = zgraph.driver.proto.query.TextConditionItem
                .newBuilder();

        // 转换 Subject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            zgraph.driver.proto.query.TextSubject.Builder subjectBuilder = zgraph.driver.proto.query.TextSubject
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
        zgraph.driver.proto.query.TextOperator.Builder opBuilder = zgraph.driver.proto.query.TextOperator.newBuilder();

        if (operator instanceof TextConditionItem.TextOperator.Equal equal) {
            opBuilder.setEqual(zgraph.driver.proto.query.TextEqualOperator.newBuilder()
                    .setValue(equal.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.NotEqual notEqual) {
            opBuilder.setNotEqual(zgraph.driver.proto.query.TextNotEqualOperator.newBuilder()
                    .setValue(notEqual.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.Contains contains) {
            opBuilder.setContains(zgraph.driver.proto.query.TextContainsOperator.newBuilder()
                    .setValue(contains.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.NotContains notContains) {
            opBuilder.setNotContains(zgraph.driver.proto.query.TextNotContainsOperator.newBuilder()
                    .setValue(notContains.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.StartsWith startsWith) {
            opBuilder.setStartsWith(zgraph.driver.proto.query.TextStartsWithOperator.newBuilder()
                    .setValue(startsWith.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.EndsWith endsWith) {
            opBuilder.setEndsWith(zgraph.driver.proto.query.TextEndsWithOperator.newBuilder()
                    .setValue(endsWith.getValue()).build());
        } else if (operator instanceof TextConditionItem.TextOperator.IsEmpty) {
            opBuilder.setIsBlank(zgraph.driver.proto.query.TextIsBlankOperator.getDefaultInstance());
        } else if (operator instanceof TextConditionItem.TextOperator.IsNotEmpty) {
            opBuilder.setIsNotBlank(zgraph.driver.proto.query.TextIsNotBlankOperator.getDefaultInstance());
        } else {
            logger.warn("未知的文本操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static zgraph.driver.proto.query.NumberConditionItem toProtoNumberCondition(NumberConditionItem item) {
        zgraph.driver.proto.query.NumberConditionItem.Builder builder = zgraph.driver.proto.query.NumberConditionItem
                .newBuilder();

        // 转换 Subject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            zgraph.driver.proto.query.NumberSubject.Builder subjectBuilder = zgraph.driver.proto.query.NumberSubject
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
        zgraph.driver.proto.query.NumberOperator.Builder opBuilder = zgraph.driver.proto.query.NumberOperator
                .newBuilder();

        if (operator instanceof NumberConditionItem.NumberOperator.Equal equal) {
            double value = extractNumberValue(equal.getValue());
            opBuilder.setEqual(zgraph.driver.proto.query.NumberEqualOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.NotEqual notEqual) {
            double value = extractNumberValue(notEqual.getValue());
            opBuilder.setNotEqual(zgraph.driver.proto.query.NumberNotEqualOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.GreaterThan gt) {
            double value = extractNumberValue(gt.getValue());
            opBuilder.setGreaterThan(zgraph.driver.proto.query.NumberGreaterThanOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.LessThan lt) {
            double value = extractNumberValue(lt.getValue());
            opBuilder.setLessThan(zgraph.driver.proto.query.NumberLessThanOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.GreaterThanOrEqual gte) {
            double value = extractNumberValue(gte.getValue());
            opBuilder.setGreaterThanOrEqual(zgraph.driver.proto.query.NumberGreaterThanOrEqualOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.LessThanOrEqual lte) {
            double value = extractNumberValue(lte.getValue());
            opBuilder.setLessThanOrEqual(zgraph.driver.proto.query.NumberLessThanOrEqualOperator.newBuilder()
                    .setValue(value).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.Between between) {
            double startValue = extractNumberValue(between.getStart());
            double endValue = extractNumberValue(between.getEnd());
            opBuilder.setBetween(zgraph.driver.proto.query.NumberBetweenOperator.newBuilder()
                    .setMinValue(startValue)
                    .setMaxValue(endValue).build());
        } else if (operator instanceof NumberConditionItem.NumberOperator.IsEmpty) {
            opBuilder.setIsNull(zgraph.driver.proto.query.NumberIsNullOperator.getDefaultInstance());
        } else if (operator instanceof NumberConditionItem.NumberOperator.IsNotEmpty) {
            opBuilder.setIsNotNull(zgraph.driver.proto.query.NumberIsNotNullOperator.getDefaultInstance());
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

    private static zgraph.driver.proto.query.DateConditionItem toProtoDateCondition(DateConditionItem item) {
        zgraph.driver.proto.query.DateConditionItem.Builder builder = zgraph.driver.proto.query.DateConditionItem
                .newBuilder();

        // 转换 Subject - 支持自定义字段和系统字段
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            zgraph.driver.proto.query.DateSubject.Builder subjectBuilder = zgraph.driver.proto.query.DateSubject
                    .newBuilder();
            if (subject.getPath() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.getPath()));
            }
            // 根据 Subject 类型设置 fieldId
            if (subject instanceof DateConditionItem.DateSubject.FieldDateSubject fieldSubject) {
                subjectBuilder.setFieldId(fieldSubject.getFieldId());
            } else if (subject instanceof DateConditionItem.DateSubject.SystemDateSubject systemSubject) {
                // 系统日期字段需要映射为 zgraph 识别的字段名
                subjectBuilder.setFieldId(mapSystemDateField(systemSubject.getSystemField()));
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator - 使用 instanceof 进行类型匹配
        var operator = item.getOperator();
        zgraph.driver.proto.query.DateOperator.Builder opBuilder = zgraph.driver.proto.query.DateOperator.newBuilder();

        if (operator instanceof DateConditionItem.DateOperator.Equal equal) {
            buildDateEqualOperator(opBuilder, equal.getValue());
        } else if (operator instanceof DateConditionItem.DateOperator.NotEqual notEqual) {
            buildDateNotEqualOperator(opBuilder, notEqual.getValue());
        } else if (operator instanceof DateConditionItem.DateOperator.Before before) {
            buildDateBeforeOperator(opBuilder, before.getValue());
        } else if (operator instanceof DateConditionItem.DateOperator.After after) {
            buildDateAfterOperator(opBuilder, after.getValue());
        } else if (operator instanceof DateConditionItem.DateOperator.Between between) {
            buildDateBetweenOperator(opBuilder, between);
        } else if (operator instanceof DateConditionItem.DateOperator.NotBetween notBetween) {
            buildDateNotBetweenOperator(opBuilder, notBetween);
        } else if (operator instanceof DateConditionItem.DateOperator.IsEmpty) {
            opBuilder.setIsNull(zgraph.driver.proto.query.DateIsNullOperator.getDefaultInstance());
        } else if (operator instanceof DateConditionItem.DateOperator.IsNotEmpty) {
            opBuilder.setIsNotNull(zgraph.driver.proto.query.DateIsNotNullOperator.getDefaultInstance());
        } else {
            logger.warn("未知的日期操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    /**
     * 构建 Equal 操作符 - 支持静态值和引用值
     * 对于静态日期值，转换为 Between 操作符以包含完整的一天 [当天开始, 当天结束]
     */
    private static void buildDateEqualOperator(zgraph.driver.proto.query.DateOperator.Builder opBuilder,
                                               DateConditionItem.DateValue dateValue) {
        if (dateValue instanceof DateConditionItem.DateValue.ReferenceValue refValue) {
            // 引用值保持 Equal 操作符
            var equalOpBuilder = zgraph.driver.proto.query.DateEqualOperator.newBuilder();
            equalOpBuilder.setReferDate(buildReferDate(refValue));
            opBuilder.setEqual(equalOpBuilder.build());
        } else if (dateValue instanceof DateConditionItem.DateValue.Specific specific) {
            // 静态日期值转换为 Between [当天开始, 当天最后一毫秒]
            long startMillis = parseSpecificDateAsStartOfDay(specific.getValue());
            long endMillis = parseSpecificDateAsEndOfDay(specific.getValue());
            opBuilder.setBetween(zgraph.driver.proto.query.DateBetweenOperator.newBuilder()
                    .setStaticStartValue(startMillis)
                    .setStaticEndValue(endMillis)
                    .build());
        } else {
            // 其他类型（理论上不应该出现，因为 ConditionResolver 已经解析过了）
            throw new IllegalArgumentException("不支持的日期条件值类型: " +
                    (dateValue != null ? dateValue.getClass().getSimpleName() : "null") +
                    ", 期望 Specific 或 ReferenceValue");
        }
    }

    /**
     * 解析日期字符串为当天开始的时间戳
     */
    private static long parseSpecificDateAsStartOfDay(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return 0L;
        }
        try {
            // 尝试直接解析为毫秒时间戳
            return Long.parseLong(dateStr);
        } catch (NumberFormatException e) {
            // 尝试解析日期格式
            try {
                LocalDate date;
                if (dateStr.contains("T")) {
                    // ISO-8601 格式：yyyy-MM-ddTHH:mm:ss，提取日期部分
                    date = LocalDateTime.parse(dateStr).toLocalDate();
                } else if (dateStr.contains(" ")) {
                    // 带空格的格式：yyyy-MM-dd HH:mm:ss，提取日期部分
                    date = LocalDateTime.parse(dateStr.replace(" ", "T")).toLocalDate();
                } else {
                    // 纯日期格式：yyyy-MM-dd
                    date = LocalDate.parse(dateStr);
                }
                return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception ex) {
                logger.warn("无法解析日期字符串: {}", dateStr);
                return 0L;
            }
        }
    }

    /**
     * 解析日期字符串为次日开始的时间戳
     */
    private static long parseSpecificDateAsNextDayStart(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return 0L;
        }
        try {
            // 尝试直接解析为毫秒时间戳，然后加一天
            long millis = Long.parseLong(dateStr);
            return millis + 24 * 60 * 60 * 1000;
        } catch (NumberFormatException e) {
            // 尝试解析日期格式
            try {
                LocalDate date;
                if (dateStr.contains("T")) {
                    // ISO-8601 格式：yyyy-MM-ddTHH:mm:ss，提取日期部分
                    date = LocalDateTime.parse(dateStr).toLocalDate();
                } else if (dateStr.contains(" ")) {
                    // 带空格的格式：yyyy-MM-dd HH:mm:ss，提取日期部分
                    date = LocalDateTime.parse(dateStr.replace(" ", "T")).toLocalDate();
                } else {
                    // 纯日期格式：yyyy-MM-dd
                    date = LocalDate.parse(dateStr);
                }
                return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception ex) {
                logger.warn("无法解析日期字符串: {}", dateStr);
                return 0L;
            }
        }
    }

    /**
     * 构建 NotEqual 操作符 - 支持静态值和引用值
     * 对于静态日期值，转换为 NotBetween 操作符以排除完整的一天 [当天开始, 次日开始)
     */
    private static void buildDateNotEqualOperator(zgraph.driver.proto.query.DateOperator.Builder opBuilder,
                                                  DateConditionItem.DateValue dateValue) {
        if (dateValue instanceof DateConditionItem.DateValue.ReferenceValue refValue) {
            // 引用值保持 NotEqual 操作符
            var notEqualOpBuilder = zgraph.driver.proto.query.DateNotEqualOperator.newBuilder();
            notEqualOpBuilder.setReferDate(buildReferDate(refValue));
            opBuilder.setNotEqual(notEqualOpBuilder.build());
        } else if (dateValue instanceof DateConditionItem.DateValue.Specific specific) {
            // 静态日期值转换为 NotBetween [当天开始, 次日开始)
            long startMillis = parseSpecificDateAsStartOfDay(specific.getValue());
            long endMillis = parseSpecificDateAsNextDayStart(specific.getValue());
            opBuilder.setNotBetween(zgraph.driver.proto.query.DateNotBetweenOperator.newBuilder()
                    .setStaticStartValue(startMillis)
                    .setStaticEndValue(endMillis)
                    .build());
        } else {
            // 其他类型（理论上不应该出现，因为 ConditionResolver 已经解析过了）
            throw new IllegalArgumentException("不支持的日期条件值类型: " +
                    (dateValue != null ? dateValue.getClass().getSimpleName() : "null") +
                    ", 期望 Specific 或 ReferenceValue");
        }
    }

    /**
     * 构建 Before 操作符 - 支持静态值和引用值
     * 对于静态日期值，取当天的开始时间（00:00:00）
     */
    private static void buildDateBeforeOperator(zgraph.driver.proto.query.DateOperator.Builder opBuilder,
                                                DateConditionItem.DateValue dateValue) {
        var beforeOpBuilder = zgraph.driver.proto.query.DateBeforeOperator.newBuilder();
        if (dateValue instanceof DateConditionItem.DateValue.ReferenceValue refValue) {
            beforeOpBuilder.setReferDate(buildReferDate(refValue));
        } else if (dateValue instanceof DateConditionItem.DateValue.Specific specific) {
            // 静态日期值取当天开始时间
            long startMillis = parseSpecificDateAsStartOfDay(specific.getValue());
            beforeOpBuilder.setStaticValue(startMillis);
        } else {
            // 其他类型（理论上不应该出现，因为 ConditionResolver 已经解析过了）
            throw new IllegalArgumentException("不支持的日期条件值类型: " +
                    (dateValue != null ? dateValue.getClass().getSimpleName() : "null") +
                    ", 期望 Specific 或 ReferenceValue");
        }
        opBuilder.setBefore(beforeOpBuilder.build());
    }

    /**
     * 构建 After 操作符 - 支持静态值和引用值
     * 对于静态日期值，取当天的最后一秒时间（23:59:59）
     */
    private static void buildDateAfterOperator(zgraph.driver.proto.query.DateOperator.Builder opBuilder,
                                               DateConditionItem.DateValue dateValue) {
        var afterOpBuilder = zgraph.driver.proto.query.DateAfterOperator.newBuilder();
        if (dateValue instanceof DateConditionItem.DateValue.ReferenceValue refValue) {
            afterOpBuilder.setReferDate(buildReferDate(refValue));
        } else if (dateValue instanceof DateConditionItem.DateValue.Specific specific) {
            // 静态日期值取当天最后一秒时间（23:59:59）
            long endOfDayMillis = parseSpecificDateAsEndOfDay(specific.getValue());
            afterOpBuilder.setStaticValue(endOfDayMillis);
        } else {
            // 其他类型（理论上不应该出现，因为 ConditionResolver 已经解析过了）
            throw new IllegalArgumentException("不支持的日期条件值类型: " +
                    (dateValue != null ? dateValue.getClass().getSimpleName() : "null") +
                    ", 期望 Specific 或 ReferenceValue");
        }
        opBuilder.setAfter(afterOpBuilder.build());
    }

    /**
     * 解析日期字符串为当天最后一秒的时间戳（23:59:59）
     */
    private static long parseSpecificDateAsEndOfDay(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return 0L;
        }
        try {
            // 尝试直接解析为毫秒时间戳，取当天的最后一秒
            long millis = Long.parseLong(dateStr);
            LocalDate date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
            return date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (NumberFormatException e) {
            // 尝试解析日期格式
            try {
                LocalDate date;
                if (dateStr.contains("T")) {
                    // ISO-8601 格式：yyyy-MM-ddTHH:mm:ss，提取日期部分
                    date = LocalDateTime.parse(dateStr).toLocalDate();
                } else if (dateStr.contains(" ")) {
                    // 带空格的格式：yyyy-MM-dd HH:mm:ss，提取日期部分
                    date = LocalDateTime.parse(dateStr.replace(" ", "T")).toLocalDate();
                } else {
                    // 纯日期格式：yyyy-MM-dd
                    date = LocalDate.parse(dateStr);
                }
                return date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception ex) {
                logger.warn("无法解析日期字符串: {}", dateStr);
                return 0L;
            }
        }
    }

    /**
     * 构建 Between 操作符 - 仅支持静态值
     */
    private static void buildDateBetweenOperator(zgraph.driver.proto.query.DateOperator.Builder opBuilder,
                                                 DateConditionItem.DateOperator.Between between) {
        // Between 操作符的 proto 定义只支持静态值，不支持 ReferDate
        if (hasReferenceValue(between.getStart()) || hasReferenceValue(between.getEnd())) {
            logger.warn("Between 操作符包含 ReferenceValue，当前不支持在 zgraph 查询中使用");
            // 返回一个永远不满足的条件（start > end）
            opBuilder.setBetween(zgraph.driver.proto.query.DateBetweenOperator.newBuilder()
                    .setStaticStartValue(1L)
                    .setStaticEndValue(0L).build());
        } else {
            long startValue = extractStaticDateValueAsStartOfDay(between.getStart());
            long endValue = extractStaticDateValueAsEndOfDay(between.getEnd());
            opBuilder.setBetween(zgraph.driver.proto.query.DateBetweenOperator.newBuilder()
                    .setStaticStartValue(startValue)
                    .setStaticEndValue(endValue).build());
        }
    }

    /**
     * 构建 NotBetween 操作符 - 仅支持静态值
     */
    private static void buildDateNotBetweenOperator(zgraph.driver.proto.query.DateOperator.Builder opBuilder,
                                                    DateConditionItem.DateOperator.NotBetween notBetween) {
        // NotBetween 操作符的 proto 定义只支持静态值，不支持 ReferDate
        if (hasReferenceValue(notBetween.getStart()) || hasReferenceValue(notBetween.getEnd())) {
            logger.warn("NotBetween 操作符包含 ReferenceValue，当前不支持在 zgraph 查询中使用");
            // 返回一个永远满足的条件（start > end，这样没有值会落在范围内，"不在范围内"就总是成立）
            opBuilder.setNotBetween(zgraph.driver.proto.query.DateNotBetweenOperator.newBuilder()
                    .setStaticStartValue(1L)
                    .setStaticEndValue(0L).build());
        } else {
            long startValue = extractStaticDateValueAsStartOfDay(notBetween.getStart());
            long endValue = extractStaticDateValueAsEndOfDay(notBetween.getEnd());
            opBuilder.setNotBetween(zgraph.driver.proto.query.DateNotBetweenOperator.newBuilder()
                    .setStaticStartValue(startValue)
                    .setStaticEndValue(endValue).build());
        }
    }


    /**
     * 从 DateValue 中提取时间戳（作为当天开始时间 00:00:00）
     * 只支持 Specific，其他类型（如 KeyDateValue、RecentValue、FutureValue）应由 ConditionResolver 预先解析
     */
    private static long extractStaticDateValueAsStartOfDay(DateConditionItem.DateValue dateValue) {
        if (dateValue instanceof DateConditionItem.DateValue.Specific specific) {
            return parseSpecificDateAsStartOfDay(specific.getValue());
        }

        // 其他类型应该已经被 ConditionResolver 解析为 Specific
        throw new IllegalArgumentException("不支持的日期条件值类型: " +
                (dateValue != null ? dateValue.getClass().getSimpleName() : "null") +
                ", 期望 Specific（动态日期应由 ConditionResolver 预先解析）");
    }

    /**
     * 从 DateValue 中提取时间戳（作为当天结束时间 23:59:59）
     * 只支持 Specific，其他类型（如 KeyDateValue、RecentValue、FutureValue）应由 ConditionResolver 预先解析
     */
    private static long extractStaticDateValueAsEndOfDay(DateConditionItem.DateValue dateValue) {
        if (dateValue instanceof DateConditionItem.DateValue.Specific specific) {
            return parseSpecificDateAsEndOfDay(specific.getValue());
        }

        // 其他类型应该已经被 ConditionResolver 解析为 Specific
        throw new IllegalArgumentException("不支持的日期条件值类型: " +
                (dateValue != null ? dateValue.getClass().getSimpleName() : "null") +
                ", 期望 Specific（动态日期应由 ConditionResolver 预先解析）");
    }

    /**
     * 解析具体日期字符串为时间戳
     * 支持格式：
     * - 毫秒时间戳（纯数字）
     * - ISO-8601 格式：yyyy-MM-ddTHH:mm:ss
     * - 日期格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss
     */
    private static long parseSpecificDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return 0L;
        }
        try {
            // 尝试直接解析为毫秒时间戳
            return Long.parseLong(dateStr);
        } catch (NumberFormatException e) {
            // 尝试解析日期格式
            try {
                if (dateStr.contains("T")) {
                    // ISO-8601 格式：yyyy-MM-ddTHH:mm:ss
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr);
                    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                } else if (dateStr.contains(" ")) {
                    // 带空格的格式：yyyy-MM-dd HH:mm:ss
                    LocalDateTime dateTime = LocalDateTime.parse(dateStr.replace(" ", "T"));
                    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                } else {
                    // 纯日期格式：yyyy-MM-dd
                    LocalDate date = LocalDate.parse(dateStr);
                    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
            } catch (Exception ex) {
                logger.warn("无法解析日期字符串: {}", dateStr);
                return 0L;
            }
        }
    }

    /**
     * 检查 DateValue 是否包含引用值（ReferenceValue）
     */
    private static boolean hasReferenceValue(DateConditionItem.DateValue dateValue) {
        return dateValue instanceof DateConditionItem.DateValue.ReferenceValue;
    }

    private static zgraph.driver.proto.query.EnumConditionItem toProtoEnumCondition(EnumConditionItem item) {
        zgraph.driver.proto.query.EnumConditionItem.Builder builder = zgraph.driver.proto.query.EnumConditionItem
                .newBuilder();

        // 转换 Subject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            zgraph.driver.proto.query.EnumSubject.Builder subjectBuilder = zgraph.driver.proto.query.EnumSubject
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
        zgraph.driver.proto.query.EnumOperator.Builder opBuilder = zgraph.driver.proto.query.EnumOperator.newBuilder();

        if (operator instanceof EnumConditionItem.EnumOperator.Equal equal) {
            opBuilder.setEqual(zgraph.driver.proto.query.EnumEqualOperator.newBuilder()
                    .setStaticValues(zgraph.driver.proto.query.EnumStaticValues.newBuilder()
                            .addValues(equal.getOptionId()).build())
                    .build());
        } else if (operator instanceof EnumConditionItem.EnumOperator.NotEqual notEqual) {
            opBuilder.setNotEqual(zgraph.driver.proto.query.EnumNotEqualOperator.newBuilder()
                    .setStaticValues(zgraph.driver.proto.query.EnumStaticValues.newBuilder()
                            .addValues(notEqual.getOptionId()).build())
                    .build());
        } else if (operator instanceof EnumConditionItem.EnumOperator.In in) {
            opBuilder.setIn(zgraph.driver.proto.query.EnumInOperator.newBuilder()
                    .setStaticValues(zgraph.driver.proto.query.EnumStaticValues.newBuilder()
                            .addAllValues(in.getOptionIds() != null ? in.getOptionIds() : List.of()).build())
                    .build());
        } else if (operator instanceof EnumConditionItem.EnumOperator.NotIn notIn) {
            opBuilder.setNotIn(zgraph.driver.proto.query.EnumNotInOperator.newBuilder()
                    .setStaticValues(zgraph.driver.proto.query.EnumStaticValues.newBuilder()
                            .addAllValues(notIn.getOptionIds() != null ? notIn.getOptionIds() : List.of()).build())
                    .build());
        } else if (operator instanceof EnumConditionItem.EnumOperator.IsEmpty) {
            opBuilder.setIsNull(zgraph.driver.proto.query.EnumIsNullOperator.getDefaultInstance());
        } else if (operator instanceof EnumConditionItem.EnumOperator.IsNotEmpty) {
            opBuilder.setIsNotNull(zgraph.driver.proto.query.EnumIsNotNullOperator.getDefaultInstance());
        } else {
            logger.warn("未知的枚举操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static zgraph.driver.proto.query.StatusConditionItem toProtoStatusCondition(StatusConditionItem item) {
        zgraph.driver.proto.query.StatusConditionItem.Builder builder = zgraph.driver.proto.query.StatusConditionItem
                .newBuilder();

        // 转换 Subject - StatusSubject 是 record，从 subject 中获取 streamId
        String streamId = "";
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            streamId = subject.streamId() != null ? subject.streamId() : "";
            zgraph.driver.proto.query.StatusSubject.Builder subjectBuilder = zgraph.driver.proto.query.StatusSubject
                    .newBuilder();
            if (subject.path() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.path()));
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator - 使用 instanceof 进行类型匹配
        var operator = item.getOperator();
        zgraph.driver.proto.query.StatusOperator.Builder opBuilder = zgraph.driver.proto.query.StatusOperator
                .newBuilder();

        if (operator instanceof StatusConditionItem.StatusOperator.Equal equal) {
            opBuilder.setEqual(zgraph.driver.proto.query.StatusEqualOperator.newBuilder()
                    .setStreamId(streamId)
                    .setStatusId(equal.getStatusId() != null ? equal.getStatusId() : "")
                    .build());
        } else if (operator instanceof StatusConditionItem.StatusOperator.NotEqual notEqual) {
            opBuilder.setNotEqual(zgraph.driver.proto.query.StatusNotEqualOperator.newBuilder()
                    .setStreamId(streamId)
                    .setStatusId(notEqual.getStatusId() != null ? notEqual.getStatusId() : "")
                    .build());
        } else if (operator instanceof StatusConditionItem.StatusOperator.In in) {
            opBuilder.setIn(zgraph.driver.proto.query.StatusInOperator.newBuilder()
                    .setStreamId(streamId)
                    .addAllValues(in.getStatusIds() != null ? in.getStatusIds() : List.of())
                    .build());
        } else if (operator instanceof StatusConditionItem.StatusOperator.NotIn notIn) {
            opBuilder.setNotIn(zgraph.driver.proto.query.StatusNotInOperator.newBuilder()
                    .setStreamId(streamId)
                    .addAllValues(notIn.getStatusIds() != null ? notIn.getStatusIds() : List.of())
                    .build());
        } else {
            logger.warn("未知的状态操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static zgraph.driver.proto.query.LinkConditionItem toProtoLinkCondition(LinkConditionItem item) {
        zgraph.driver.proto.query.LinkConditionItem.Builder builder = zgraph.driver.proto.query.LinkConditionItem
                .newBuilder();

        // 转换 Subject - RelationSubject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            zgraph.driver.proto.query.LinkSubject.Builder subjectBuilder = zgraph.driver.proto.query.LinkSubject
                    .newBuilder();
            if (subject.path() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.path()));
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator - 使用 instanceof 进行类型匹配
        // 注意：LinkConditionItem 使用的是 RelationOperator，不是 LinkOperator
        var operator = item.getOperator();
        zgraph.driver.proto.query.LinkOperator.Builder opBuilder = zgraph.driver.proto.query.LinkOperator.newBuilder();

        if (operator instanceof LinkConditionItem.LinkOperator.IsEmpty) {
            opBuilder.setIsNull(zgraph.driver.proto.query.LinkIsNullOperator.getDefaultInstance());
        } else if (operator instanceof LinkConditionItem.LinkOperator.HasAny) {
            opBuilder.setIsNotNull(zgraph.driver.proto.query.LinkIsNotNullOperator.getDefaultInstance());
        } else if (operator instanceof LinkConditionItem.LinkOperator.In in) {
            zgraph.driver.proto.query.LinkInOperator.Builder inOpBuilder = zgraph.driver.proto.query.LinkInOperator
                    .newBuilder();
            buildLinkOperatorValue(in.getValue(), inOpBuilder);
            opBuilder.setIn(inOpBuilder.build());
        } else if (operator instanceof LinkConditionItem.LinkOperator.NotIn notIn) {
            zgraph.driver.proto.query.LinkNotInOperator.Builder notInOpBuilder = zgraph.driver.proto.query.LinkNotInOperator
                    .newBuilder();
            buildLinkOperatorValue(notIn.getValue(), notInOpBuilder);
            opBuilder.setNotIn(notInOpBuilder.build());
        } else {
            logger.warn("未知的关联操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    private static zgraph.driver.proto.query.KeywordConditionItem toProtoKeywordCondition(KeywordConditionItem item) {
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

        return zgraph.driver.proto.query.KeywordConditionItem.newBuilder()
                .setValue(keyword != null ? keyword : "")
                .build();
    }

    /**
     * 构建 LinkInOperator 的值（ReferLink 或 SpecialLink）
     */
    private static void buildLinkOperatorValue(LinkConditionItem.LinkValue value,
            zgraph.driver.proto.query.LinkInOperator.Builder builder) {
        if (value == null) {
            return;
        }

        if (value instanceof LinkConditionItem.LinkValue.StaticValue staticValue) {
            // 静态卡片列表 -> SpecialLink
            zgraph.driver.proto.query.SpecialLink.Builder specialLinkBuilder = zgraph.driver.proto.query.SpecialLink
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
            zgraph.driver.proto.query.LinkNotInOperator.Builder builder) {
        if (value == null) {
            return;
        }

        if (value instanceof LinkConditionItem.LinkValue.StaticValue staticValue) {
            // 静态卡片列表 -> SpecialLink
            zgraph.driver.proto.query.SpecialLink.Builder specialLinkBuilder = zgraph.driver.proto.query.SpecialLink
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
    private static zgraph.driver.proto.query.ReferLink buildReferLink(ReferenceSource source) {
        zgraph.driver.proto.query.ReferLink.Builder builder = zgraph.driver.proto.query.ReferLink.newBuilder();
        buildReferSource(builder::setReferOnCurrentCard, builder::setReferOnParameterCard,
                builder::setReferOnMember, builder::setReferOnContextualCard, source);
        return builder.build();
    }

    /**
     * 构建 ReferDate proto 对象
     */
    private static zgraph.driver.proto.query.ReferDate buildReferDate(DateConditionItem.DateValue.ReferenceValue refValue) {
        var referDateBuilder = zgraph.driver.proto.query.ReferDate.newBuilder();
        referDateBuilder.setFieldId(refValue.getFieldId());
        buildReferSource(referDateBuilder::setReferOnCurrentCard, referDateBuilder::setReferOnParameterCard,
                referDateBuilder::setReferOnMember, referDateBuilder::setReferOnContextualCard, refValue.getSource());
        return referDateBuilder.build();
    }

    /**
     * 通用方法：构建引用源对象
     */
    private static void buildReferSource(
            java.util.function.Consumer<zgraph.driver.proto.query.ReferOnCurrentCard> currentCardSetter,
            java.util.function.Consumer<zgraph.driver.proto.query.ReferOnParameterCard> parameterCardSetter,
            java.util.function.Consumer<zgraph.driver.proto.query.ReferOnMember> memberSetter,
            java.util.function.Consumer<zgraph.driver.proto.query.ReferOnContextualCard> contextualCardSetter,
            ReferenceSource source) {

        if (source instanceof ReferenceSource.CurrentCard currentCard) {
            var refBuilder = zgraph.driver.proto.query.ReferOnCurrentCard.newBuilder();
            if (currentCard.getPath() != null) {
                refBuilder.setPath(PathConverter.toProto(currentCard.getPath()));
            }
            currentCardSetter.accept(refBuilder.build());
        } else if (source instanceof ReferenceSource.ParameterCard parameterCard) {
            var refBuilder = zgraph.driver.proto.query.ReferOnParameterCard.newBuilder();
            if (parameterCard.getPath() != null) {
                refBuilder.setPath(PathConverter.toProto(parameterCard.getPath()));
            }
            if (parameterCard.getParameterCardTypeId() != null) {
                refBuilder.setParameterCardTypeId(parameterCard.getParameterCardTypeId());
            }
            parameterCardSetter.accept(refBuilder.build());
        } else if (source instanceof ReferenceSource.Member member) {
            var refBuilder = zgraph.driver.proto.query.ReferOnMember.newBuilder();
            if (member.getPath() != null) {
                refBuilder.setPath(PathConverter.toProto(member.getPath()));
            }
            memberSetter.accept(refBuilder.build());
        } else if (source instanceof ReferenceSource.ContextualCard contextualCard) {
            var refBuilder = zgraph.driver.proto.query.ReferOnContextualCard.newBuilder();
            if (contextualCard.getPath() != null) {
                refBuilder.setPath(PathConverter.toProto(contextualCard.getPath()));
            }
            if (contextualCard.getContextualVertexId() != null) {
                refBuilder.setContextualCardId(contextualCard.getContextualVertexId());
            }
            contextualCardSetter.accept(refBuilder.build());
        } else {
            logger.warn("未知的ReferenceSource类型: {}", source != null ? source.getClass().getName() : "null");
        }
    }

    /**
     * 转换卡片生命周期条件项（CARD_CYCLE）
     * 复用 StateConditionItem 的 proto 结构
     */
    private static zgraph.driver.proto.query.StateConditionItem toProtoCardCycleCondition(CardCycleConditionItem item) {
        zgraph.driver.proto.query.StateConditionItem.Builder builder = zgraph.driver.proto.query.StateConditionItem
                .newBuilder();

        // 转换 Subject
        if (item.getSubject() != null) {
            var subject = item.getSubject();
            zgraph.driver.proto.query.StateSubject.Builder subjectBuilder = zgraph.driver.proto.query.StateSubject
                    .newBuilder();
            if (subject.path() != null) {
                subjectBuilder.setPath(PathConverter.toProto(subject.path()));
            }
            builder.setSubject(subjectBuilder.build());
        }

        // 转换 Operator
        var operator = item.getOperator();
        zgraph.driver.proto.query.StateOperator.Builder opBuilder = zgraph.driver.proto.query.StateOperator
                .newBuilder();

        if (operator instanceof CardCycleConditionItem.LifecycleOperator.In in) {
            List<String> stateNames = in.getValues() != null
                    ? in.getValues().stream().map(Enum::name).toList()
                    : List.of();
            opBuilder.setIn(zgraph.driver.proto.query.StateInOperator.newBuilder()
                    .addAllValues(stateNames)
                    .build());
        } else if (operator instanceof CardCycleConditionItem.LifecycleOperator.NotIn notIn) {
            List<String> stateNames = notIn.getValues() != null
                    ? notIn.getValues().stream().map(Enum::name).toList()
                    : List.of();
            opBuilder.setNotIn(zgraph.driver.proto.query.StateNotInOperator.newBuilder()
                    .addAllValues(stateNames)
                    .build());
        } else {
            logger.warn("未知的卡片生命周期操作符类型: {}", operator.getClass().getName());
        }

        builder.setOperator(opBuilder.build());
        return builder.build();
    }

    /**
     * 将系统日期字段枚举映射为 zgraph 识别的字段名
     * <p>
     * zgraph 使用以下字段名表示系统日期字段：
     * - created_at: 创建时间
     * - updated_at: 更新时间
     * - discarded_at: 丢弃时间
     * - archived_at: 归档时间
     */
    private static String mapSystemDateField(DateConditionItem.SystemDateField systemField) {
        return switch (systemField) {
            case CREATED_AT -> "created_at";
            case UPDATED_AT -> "updated_at";
            case DISCARDED_AT -> "discarded_at";
            case ARCHIVED_AT -> "archived_at";
        };
    }

}
