package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.card.CardStyle;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 卡片周期状态条件项
 * <p>
 * 用于查询卡片的生命周期状态（活跃、归档、丢弃）。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CardStyleConditionItem extends AbstractConditionItem {

    private final CardStyleSubject subject;

    private final StyleOperator operator;

    @JsonCreator
    public CardStyleConditionItem(@JsonProperty("subject") CardStyleSubject subject,
                                  @JsonProperty("operator") StyleOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.CARD_CYCLE;
    }

    /**
     * 状态主体
     * <p>
     * 指定要查询的卡片状态，可以通过路径引用关联卡片的状态。
     */
    public record CardStyleSubject(Path path) {

        @JsonCreator
        public CardStyleSubject(@JsonProperty("path") Path path) {
            this.path = path;
        }
    }

    /**
     * 状态操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = StyleOperator.Equal.class, name = "EQ"),
            @JsonSubTypes.Type(value = StyleOperator.NotEqual.class, name = "NE"),
            @JsonSubTypes.Type(value = StyleOperator.In.class, name = "IN"),
            @JsonSubTypes.Type(value = StyleOperator.NotIn.class, name = "NOT_IN"),
    })
    public interface StyleOperator {

        /**
         * 等于
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Equal implements StyleOperator {
            private final CardStyle value;

            @JsonCreator
            public Equal(@JsonProperty("value") CardStyle value) {
                AssertUtils.notNull(value, "value can't be null");
                this.value = value;
            }
        }

        /**
         * 不等于
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotEqual implements StyleOperator {
            private final CardStyle value;

            @JsonCreator
            public NotEqual(@JsonProperty("value") CardStyle value) {
                AssertUtils.notNull(value, "value can't be null");
                this.value = value;
            }
        }

        /**
         * 在列表中
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class In implements StyleOperator {
            private final List<CardStyle> values;

            @JsonCreator
            public In(@JsonProperty("values") List<CardStyle> values) {
                AssertUtils.notEmpty(values, "values can't be empty");
                this.values = values;
            }
        }

        /**
         * 不在列表中
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotIn implements StyleOperator {
            private final List<CardStyle> values;

            @JsonCreator
            public NotIn(@JsonProperty("values") List<CardStyle> values) {
                AssertUtils.notEmpty(values, "values can't be empty");
                this.values = values;
            }
        }
    }
}
