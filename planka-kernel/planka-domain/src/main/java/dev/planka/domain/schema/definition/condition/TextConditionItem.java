package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 文本条件项
 * <p>
 * 用于文本类型字段的过滤条件。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TextConditionItem extends AbstractConditionItem {

    private final TextSubject subject;

    private final TextOperator operator;

    @JsonCreator
    public TextConditionItem(@JsonProperty("subject") TextSubject subject, @JsonProperty("operator") TextOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.TEXT;
    }

    public record TextSubject(Path path, String fieldId) {

        @JsonCreator
        public TextSubject(@JsonProperty("path") Path path, @JsonProperty("fieldId") String fieldId) {
            AssertUtils.notBlank(fieldId, "fieldId can't be blank");
            this.path = path;
            this.fieldId = fieldId;
        }
    }

    /**
     * 文本操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TextOperator.Equal.class, name = "EQ"),
            @JsonSubTypes.Type(value = TextOperator.NotEqual.class, name = "NE"),
            @JsonSubTypes.Type(value = TextOperator.Contains.class, name = "CONTAINS"),
            @JsonSubTypes.Type(value = TextOperator.NotContains.class, name = "NOT_CONTAINS"),
            @JsonSubTypes.Type(value = TextOperator.StartsWith.class, name = "STARTS_WITH"),
            @JsonSubTypes.Type(value = TextOperator.EndsWith.class, name = "ENDS_WITH"),
            @JsonSubTypes.Type(value = TextOperator.IsEmpty.class, name = "IS_EMPTY"),
            @JsonSubTypes.Type(value = TextOperator.IsNotEmpty.class, name = "IS_NOT_EMPTY"),
    })
    public interface TextOperator {

        /** 等于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Equal implements TextOperator {
            private final String value;

            @JsonCreator
            public Equal(@JsonProperty("value") String value) {
                this.value = value;
            }
        }

        /** 不等于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotEqual implements TextOperator {
            private final String value;

            @JsonCreator
            public NotEqual(@JsonProperty("value") String value) {
                this.value = value;
            }
        }

        /** 包含 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Contains implements TextOperator {
            private final String value;

            @JsonCreator
            public Contains(@JsonProperty("value") String value) {
                this.value = value;
            }
        }

        /** 不包含 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotContains implements TextOperator {
            private final String value;

            @JsonCreator
            public NotContains(@JsonProperty("value") String value) {
                this.value = value;
            }
        }

        /** 以...开始 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class StartsWith implements TextOperator {
            private final String value;

            @JsonCreator
            public StartsWith(@JsonProperty("value") String value) {
                this.value = value;
            }
        }

        /** 以...结束 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class EndsWith implements TextOperator {
            private final String value;

            @JsonCreator
            public EndsWith(@JsonProperty("value") String value) {
                this.value = value;
            }
        }

        /** 为空 */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsEmpty implements TextOperator {
        }

        /** 不为空 */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsNotEmpty implements TextOperator {
        }
    }
}
