package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 数字条件项
 * <p>
 * 用于数字类型字段的过滤条件。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NumberConditionItem extends AbstractConditionItem {

    private final NumberSubject subject;

    private final NumberOperator operator;

    @JsonCreator
    public NumberConditionItem(@JsonProperty("subject") NumberSubject subject, @JsonProperty("operator") NumberOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.NUMBER;
    }

    public record NumberSubject(Path path, String fieldId) {

        @JsonCreator
        public NumberSubject(@JsonProperty("path") Path path, @JsonProperty("fieldId") String fieldId) {
            AssertUtils.notBlank(fieldId, "fieldId can't be blank");
            this.path = path;
            this.fieldId = fieldId;
        }
    }

    /**
     * 数字值接口 - 用于表示静态数字值或引用值
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = NumberValue.StaticValue.class, name = "STATIC"),
            @JsonSubTypes.Type(value = NumberValue.ReferenceValue.class, name = "REFERENCE"),
    })
    public interface NumberValue {

        /**
         * 静态数字值
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class StaticValue implements NumberValue {
            private final Double value;

            @JsonCreator
            public StaticValue(@JsonProperty("value") Double value) {
                this.value = value;
            }
        }

        /**
         * 引用数字值
         * <p>
         * 引用其他卡片的数字属性值，支持多级路径引用。
         * 例如：引用当前卡.父需求.工作量
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class ReferenceValue implements NumberValue {
            /**
             * 引用源（当前卡片、参数卡片、成员、上下文卡片等）
             */
            private final ReferenceSource source;

            /**
             * 引用的数字字段ID
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
     * 数字操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = NumberOperator.Equal.class, name = "EQ"),
            @JsonSubTypes.Type(value = NumberOperator.NotEqual.class, name = "NE"),
            @JsonSubTypes.Type(value = NumberOperator.GreaterThan.class, name = "GT"),
            @JsonSubTypes.Type(value = NumberOperator.GreaterThanOrEqual.class, name = "GE"),
            @JsonSubTypes.Type(value = NumberOperator.LessThan.class, name = "LT"),
            @JsonSubTypes.Type(value = NumberOperator.LessThanOrEqual.class, name = "LE"),
            @JsonSubTypes.Type(value = NumberOperator.Between.class, name = "BETWEEN"),
            @JsonSubTypes.Type(value = NumberOperator.IsEmpty.class, name = "IS_EMPTY"),
            @JsonSubTypes.Type(value = NumberOperator.IsNotEmpty.class, name = "IS_NOT_EMPTY"),
    })
    public interface NumberOperator {

        /** 等于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Equal implements NumberOperator {
            private final NumberValue value;

            @JsonCreator
            public Equal(@JsonProperty("value") NumberValue value) {
                this.value = value;
            }
        }

        /** 不等于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotEqual implements NumberOperator {
            private final NumberValue value;

            @JsonCreator
            public NotEqual(@JsonProperty("value") NumberValue value) {
                this.value = value;
            }
        }

        /** 大于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class GreaterThan implements NumberOperator {
            private final NumberValue value;

            @JsonCreator
            public GreaterThan(@JsonProperty("value") NumberValue value) {
                this.value = value;
            }
        }

        /** 大于等于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class GreaterThanOrEqual implements NumberOperator {
            private final NumberValue value;

            @JsonCreator
            public GreaterThanOrEqual(@JsonProperty("value") NumberValue value) {
                this.value = value;
            }
        }

        /** 小于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class LessThan implements NumberOperator {
            private final NumberValue value;

            @JsonCreator
            public LessThan(@JsonProperty("value") NumberValue value) {
                this.value = value;
            }
        }

        /** 小于等于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class LessThanOrEqual implements NumberOperator {
            private final NumberValue value;

            @JsonCreator
            public LessThanOrEqual(@JsonProperty("value") NumberValue value) {
                this.value = value;
            }
        }

        /** 在范围内 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Between implements NumberOperator {
            private final NumberValue start;
            private final NumberValue end;

            @JsonCreator
            public Between(@JsonProperty("start") NumberValue start,
                           @JsonProperty("end") NumberValue end) {
                this.start = start;
                this.end = end;
            }
        }

        /** 为空 */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsEmpty implements NumberOperator {
        }

        /** 不为空 */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsNotEmpty implements NumberOperator {
        }
    }
}
