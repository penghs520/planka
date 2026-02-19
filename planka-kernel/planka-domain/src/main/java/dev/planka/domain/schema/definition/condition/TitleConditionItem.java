package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 标题条件项
 * <p>
 * 用于卡片标题字段的过滤条件。标题是系统字段，支持文本匹配操作。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TitleConditionItem extends AbstractConditionItem {

    private final TitleSubject subject;

    private final TitleOperator operator;

    @JsonCreator
    public TitleConditionItem(@JsonProperty("subject") TitleSubject subject, @JsonProperty("operator") TitleOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.TITLE;
    }

    public record TitleSubject(Path path) {

        @JsonCreator
        public TitleSubject(@JsonProperty("path") Path path) {
            this.path = path;
        }
    }

    /**
     * 标题操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TitleOperator.Contains.class, name = "CONTAINS"),
            @JsonSubTypes.Type(value = TitleOperator.NotContains.class, name = "NOT_CONTAINS"),
            @JsonSubTypes.Type(value = TitleOperator.IsEmpty.class, name = "IS_EMPTY"),
            @JsonSubTypes.Type(value = TitleOperator.IsNotEmpty.class, name = "IS_NOT_EMPTY"),
    })
    public interface TitleOperator {


        /** 包含 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Contains implements TitleOperator {
            private final String value;

            @JsonCreator
            public Contains(@JsonProperty("value") String value) {
                this.value = value;
            }
        }

        /** 不包含 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotContains implements TitleOperator {
            private final String value;

            @JsonCreator
            public NotContains(@JsonProperty("value") String value) {
                this.value = value;
            }
        }


        /** 为空 */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsEmpty implements TitleOperator {
        }

        /** 不为空 */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsNotEmpty implements TitleOperator {
        }
    }
}
