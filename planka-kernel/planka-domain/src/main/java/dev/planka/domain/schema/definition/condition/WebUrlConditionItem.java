package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 链接属性条件项
 * <p>
 * 用于链接属性字段的过滤条件。链接属性存储 Web URL。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class WebUrlConditionItem extends AbstractConditionItem {

    private final WebUrlSubject subject;

    private final WebUrlOperator operator;

    @JsonCreator
    public WebUrlConditionItem(@JsonProperty("subject") WebUrlSubject subject, @JsonProperty("operator") WebUrlOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.WEB_URL;
    }

    public record WebUrlSubject(Path path, String fieldId) {

        @JsonCreator
        public WebUrlSubject(@JsonProperty("path") Path path, @JsonProperty("fieldId") String fieldId) {
            AssertUtils.notBlank(fieldId, "fieldId can't be blank");
            this.path = path;
            this.fieldId = fieldId;
        }
    }

    /**
     * 链接操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = WebUrlOperator.Equal.class, name = "EQ"),
            @JsonSubTypes.Type(value = WebUrlOperator.NotEqual.class, name = "NE"),
            @JsonSubTypes.Type(value = WebUrlOperator.Contains.class, name = "CONTAINS"),
            @JsonSubTypes.Type(value = WebUrlOperator.NotContains.class, name = "NOT_CONTAINS"),
            @JsonSubTypes.Type(value = WebUrlOperator.IsEmpty.class, name = "IS_EMPTY"),
            @JsonSubTypes.Type(value = WebUrlOperator.IsNotEmpty.class, name = "IS_NOT_EMPTY"),
    })
    public interface WebUrlOperator {

        /** 等于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Equal implements WebUrlOperator {
            private final String url;

            @JsonCreator
            public Equal(@JsonProperty("url") String url) {
                this.url = url;
            }
        }

        /** 不等于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotEqual implements WebUrlOperator {
            private final String url;

            @JsonCreator
            public NotEqual(@JsonProperty("url") String url) {
                this.url = url;
            }
        }

        /** 包含 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Contains implements WebUrlOperator {
            private final String value;

            @JsonCreator
            public Contains(@JsonProperty("value") String value) {
                this.value = value;
            }
        }

        /** 不包含 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotContains implements WebUrlOperator {
            private final String value;

            @JsonCreator
            public NotContains(@JsonProperty("value") String value) {
                this.value = value;
            }
        }

        /** 为空 */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsEmpty implements WebUrlOperator {
        }

        /** 不为空 */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsNotEmpty implements WebUrlOperator {
        }
    }
}
