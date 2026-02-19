package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.card.CardStyle;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 卡片生命周期条件项
 * <p>
 * 与 CardStyleConditionItem 功能相同，但 nodeType 为 CARD_CYCLE。
 * 用于前端兼容，前端使用 CARD_CYCLE 作为卡片生命周期状态的节点类型。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CardCycleConditionItem extends AbstractConditionItem {

    private final CardCycleSubject subject;

    private final LifecycleOperator operator;

    @JsonCreator
    public CardCycleConditionItem(@JsonProperty("subject") CardCycleSubject subject,
                                  @JsonProperty("operator") LifecycleOperator operator) {
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
     * 卡片生命周期主体
     */
    public record CardCycleSubject(Path path) {

        @JsonCreator
        public CardCycleSubject(@JsonProperty("path") Path path) {
            this.path = path;
        }
    }

    /**
     * 生命周期操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LifecycleOperator.In.class, name = "IN"),
            @JsonSubTypes.Type(value = LifecycleOperator.NotIn.class, name = "NOT_IN"),
    })
    public interface LifecycleOperator {

        /**
         * 在列表中
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class In implements LifecycleOperator {
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
        class NotIn implements LifecycleOperator {
            private final List<CardStyle> values;

            @JsonCreator
            public NotIn(@JsonProperty("values") List<CardStyle> values) {
                AssertUtils.notEmpty(values, "values can't be empty");
                this.values = values;
            }
        }
    }
}
