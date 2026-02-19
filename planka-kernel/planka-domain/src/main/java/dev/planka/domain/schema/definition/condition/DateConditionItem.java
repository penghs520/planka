package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 日期条件项
 * <p>
 * 用于日期类型字段的过滤条件，支持具体日期和关键日期（如今天、本周等）。
 * 同时支持自定义日期字段和系统日期字段（创建时间、更新时间等）。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DateConditionItem extends AbstractConditionItem {

    private final DateSubject subject;

    private final DateOperator operator;

    @JsonCreator
    public DateConditionItem(@JsonProperty("subject") DateSubject subject, @JsonProperty("operator") DateOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.DATE;
    }

    /**
     * 系统日期字段枚举
     */
    public enum SystemDateField {
        /** 创建时间 */
        CREATED_AT,
        /** 更新时间 */
        UPDATED_AT,
        /** 丢弃时间 */
        DISCARDED_AT,
        /** 归档时间 */
        ARCHIVED_AT
    }

    /**
     * 日期主体接口
     * <p>
     * 支持两种类型：
     * - FieldDateSubject: 自定义日期字段，使用 fieldId 标识
     * - SystemDateSubject: 系统日期字段，使用枚举标识
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = DateSubject.FieldDateSubject.class, name = "FIELD"),
            @JsonSubTypes.Type(value = DateSubject.SystemDateSubject.class, name = "SYSTEM"),
    })
    public interface DateSubject {

        /**
         * 获取路径（可选，用于引用关联卡片的日期字段）
         */
        Path getPath();

        /**
         * 自定义日期字段主体
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class FieldDateSubject implements DateSubject {
            private final Path path;
            private final String fieldId;

            @JsonCreator
            public FieldDateSubject(@JsonProperty("path") Path path,
                                    @JsonProperty("fieldId") String fieldId) {
                AssertUtils.notBlank(fieldId, "fieldId can't be blank");
                this.path = path;
                this.fieldId = fieldId;
            }
        }

        /**
         * 系统日期字段主体
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class SystemDateSubject implements DateSubject {
            private final Path path;
            private final SystemDateField systemField;

            @JsonCreator
            public SystemDateSubject(@JsonProperty("path") Path path,
                                     @JsonProperty("systemField") SystemDateField systemField) {
                AssertUtils.notNull(systemField, "systemField can't be null");
                this.path = path;
                this.systemField = systemField;
            }
        }
    }

    /**
     * 日期值接口 - 用于表示具体日期、关键日期或引用值
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = DateValue.Specific.class, name = "SPECIFIC"),
            @JsonSubTypes.Type(value = DateValue.KeyDateValue.class, name = "KEY_DATE"),
            @JsonSubTypes.Type(value = DateValue.ReferenceValue.class, name = "REFERENCE"),
    })
    public interface DateValue {

        /**
         * 具体日期值
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Specific implements DateValue {
            /**
             * 日期值（格式：yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss）
             */
            private final String value;

            @JsonCreator
            public Specific(@JsonProperty("value") String value) {
                this.value = value;
            }
        }

        /**
         * 关键日期值
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class KeyDateValue implements DateValue {
            private final KeyDate keyDate;

            @JsonCreator
            public KeyDateValue(@JsonProperty("keyDate") KeyDate keyDate) {
                this.keyDate = keyDate;
            }
        }

        /**
         * 引用日期值
         * <p>
         * 引用其他卡片的日期属性值，支持多级路径引用。
         * 例如：引用当前卡.父需求.创建时间
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class ReferenceValue implements DateValue {
            /**
             * 引用源（当前卡片、参数卡片、成员、上下文卡片等）
             */
            private final ReferenceSource source;

            /**
             * 引用的日期字段ID
             */
            private final String fieldId;

            @JsonCreator
            public ReferenceValue(@JsonProperty("source") ReferenceSource source,
                                 @JsonProperty("fieldId") String fieldId) {
                AssertUtils.notNull(source, "source can't be null");
                AssertUtils.notBlank(fieldId, "fieldId can't be blank");
                this.source = source;
                this.fieldId = fieldId;
            }
        }
    }

    /**
     * 日期操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = DateOperator.Equal.class, name = "EQ"),
            @JsonSubTypes.Type(value = DateOperator.NotEqual.class, name = "NE"),
            @JsonSubTypes.Type(value = DateOperator.Before.class, name = "BEFORE"),
            @JsonSubTypes.Type(value = DateOperator.After.class, name = "AFTER"),
            @JsonSubTypes.Type(value = DateOperator.Between.class, name = "BETWEEN"),
            @JsonSubTypes.Type(value = DateOperator.IsEmpty.class, name = "IS_EMPTY"),
            @JsonSubTypes.Type(value = DateOperator.IsNotEmpty.class, name = "IS_NOT_EMPTY"),
    })
    public interface DateOperator {

        /**
         * 等于
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Equal implements DateOperator {
            private final DateValue value;

            @JsonCreator
            public Equal(@JsonProperty("value") DateValue value) {
                this.value = value;
            }
        }

        /**
         * 不等于
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotEqual implements DateOperator {
            private final DateValue value;

            @JsonCreator
            public NotEqual(@JsonProperty("value") DateValue value) {
                this.value = value;
            }
        }

        /**
         * 早于
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Before implements DateOperator {
            private final DateValue value;

            @JsonCreator
            public Before(@JsonProperty("value") DateValue value) {
                this.value = value;
            }
        }

        /**
         * 晚于
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class After implements DateOperator {
            private final DateValue value;

            @JsonCreator
            public After(@JsonProperty("value") DateValue value) {
                this.value = value;
            }
        }

        /**
         * 在范围内
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Between implements DateOperator {
            private final DateValue start;
            private final DateValue end;

            @JsonCreator
            public Between(@JsonProperty("start") DateValue start,
                           @JsonProperty("end") DateValue end) {
                this.start = start;
                this.end = end;
            }
        }

        /**
         * 为空
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsEmpty implements DateOperator {
        }

        /**
         * 不为空
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsNotEmpty implements DateOperator {
        }
    }

    /**
     * 关键日期枚举
     */
    public enum KeyDate {
        /**
         * 今天
         */
        TODAY,
        /**
         * 昨天
         */
        YESTERDAY,
        /**
         * 明天
         */
        TOMORROW,
        /**
         * 本周
         */
        THIS_WEEK,
        /**
         * 上周
         */
        LAST_WEEK,
        /**
         * 下周
         */
        NEXT_WEEK,
        /**
         * 本月
         */
        THIS_MONTH,
        /**
         * 上月
         */
        LAST_MONTH,
        /**
         * 下月
         */
        NEXT_MONTH,
        /**
         * 本季度
         */
        THIS_QUARTER,
        /**
         * 本年
         */
        THIS_YEAR,
        /**
         * 过去7天
         */
        LAST_7_DAYS,
        /**
         * 过去30天
         */
        LAST_30_DAYS,
        /**
         * 未来7天
         */
        NEXT_7_DAYS,
        /**
         * 未来30天
         */
        NEXT_30_DAYS
    }
}
