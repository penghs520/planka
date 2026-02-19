package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 关键字条件项
 * <p>
 * 用于全文搜索关键字的过滤条件。在所有可搜索字段中查找关键字。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class KeywordConditionItem extends AbstractConditionItem {

    private final KeywordSubject subject;

    private final KeywordOperator operator;

    @JsonCreator
    public KeywordConditionItem(@JsonProperty("subject") KeywordSubject subject, @JsonProperty("operator") KeywordOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.KEYWORD;
    }

    /**
     * 关键字主体（全局搜索，无需指定字段）
     */
    public record KeywordSubject() {

        @JsonCreator
        public KeywordSubject() {
        }
    }

    /**
     * 关键字操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = KeywordOperator.Contains.class, name = "CONTAINS"),
            @JsonSubTypes.Type(value = KeywordOperator.NotContains.class, name = "NOT_CONTAINS"),
    })
    public interface KeywordOperator {

        /** 包含关键字 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Contains implements KeywordOperator {
            private final String keyword;

            @JsonCreator
            public Contains(@JsonProperty("keyword") String keyword) {
                AssertUtils.notBlank(keyword, "keyword can't be blank");
                this.keyword = keyword;
            }
        }

        /** 不包含关键字 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotContains implements KeywordOperator {
            private final String keyword;

            @JsonCreator
            public NotContains(@JsonProperty("keyword") String keyword) {
                AssertUtils.notBlank(keyword, "keyword can't be blank");
                this.keyword = keyword;
            }
        }
    }
}
