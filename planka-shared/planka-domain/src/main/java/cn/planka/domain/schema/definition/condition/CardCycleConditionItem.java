package cn.planka.domain.schema.definition.condition;

import cn.planka.common.util.AssertUtils;
import cn.planka.domain.card.CardCycle;
import cn.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 卡片生命周期条件项
 * <p>
 * 用于查询卡片的生命周期状态（活跃、存档、回收）。
 * nodeType 为 CARD_CYCLE。
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
         * 包含任一
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class In implements LifecycleOperator {
            private final List<CardCycle> values;

            @JsonCreator
            public In(@JsonProperty("values") List<CardCycle> values) {
                AssertUtils.notEmpty(values, "values can't be empty");
                this.values = values;
            }
        }

        /**
         * 不包含任一
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotIn implements LifecycleOperator, NegativeOperator {
            private final List<CardCycle> values;

            @JsonCreator
            public NotIn(@JsonProperty("values") List<CardCycle> values) {
                AssertUtils.notEmpty(values, "values can't be empty");
                this.values = values;
            }
        }
    }
}
