package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 关联属性条件项
 * <p>
 * 用于关联属性字段的过滤条件。关联属性关联到其他卡片。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LinkConditionItem extends AbstractConditionItem {

    private final LinkSubject subject;

    private final LinkOperator operator;

    @JsonCreator
    public LinkConditionItem(@JsonProperty("subject") LinkSubject subject, @JsonProperty("operator") LinkOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.LINK;
    }

    public record LinkSubject(Path path, LinkFieldId linkFieldId) {

        @JsonCreator
        public LinkSubject(@JsonProperty("path") Path path, @JsonProperty("linkFieldId") LinkFieldId linkFieldId) {
            this.path = path;
            this.linkFieldId = linkFieldId;
        }
    }

    /**
     * 关联值接口 - 用于表示静态卡片列表或引用值
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LinkValue.StaticValue.class, name = "STATIC"),
            @JsonSubTypes.Type(value = LinkValue.ReferenceValue.class, name = "REFERENCE"),
    })
    public interface LinkValue {

        /**
         * 静态卡片列表值
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class StaticValue implements LinkValue {
            private final List<String> cardIds;

            @JsonCreator
            public StaticValue(@JsonProperty("cardIds") List<String> cardIds) {
                this.cardIds = cardIds;
            }
        }

        /**
         * 引用关联值
         * <p>
         * 引用其他卡片的关联属性值，支持多级路径引用。
         * 例如：引用当前卡.父需求的所有子任务
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class ReferenceValue implements LinkValue {
            /**
             * 引用源（当前卡片、参数卡片、成员、上下文卡片等）
             */
            private final ReferenceSource source;

            @JsonCreator
            public ReferenceValue(@JsonProperty("source") ReferenceSource source) {
                AssertUtils.notNull(source, "source can't be null");
                this.source = source;
            }
        }
    }

    /**
     * 关联操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LinkOperator.In.class, name = "IN"),
            @JsonSubTypes.Type(value = LinkOperator.NotIn.class, name = "NOT_IN"),
            @JsonSubTypes.Type(value = LinkOperator.HasAny.class, name = "HAS_ANY"),
            @JsonSubTypes.Type(value = LinkOperator.IsEmpty.class, name = "IS_EMPTY"),
    })
    public interface LinkOperator {

        /**
         * 关联的卡片在列表中
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class In implements LinkOperator {
            private final LinkValue value;

            @JsonCreator
            public In(@JsonProperty("value") LinkValue value) {
                this.value = value;
            }
        }

        /**
         * 关联的卡片不在列表中
         */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotIn implements LinkOperator {
            private final LinkValue value;

            @JsonCreator
            public NotIn(@JsonProperty("value") LinkValue value) {
                this.value = value;
            }
        }

        /**
         * 有任何关联
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class HasAny implements LinkOperator {
        }

        /**
         * 没有关联
         */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsEmpty implements LinkOperator {
        }
    }
}
