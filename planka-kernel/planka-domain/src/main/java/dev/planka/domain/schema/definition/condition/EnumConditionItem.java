package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 枚举条件项
 * <p>
 * 用于枚举类型字段的过滤条件。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EnumConditionItem extends AbstractConditionItem {

    private final EnumSubject subject;

    private final EnumOperator operator;

    @JsonCreator
    public EnumConditionItem(@JsonProperty("subject") EnumSubject subject, @JsonProperty("operator") EnumOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.ENUM;
    }

    public record EnumSubject(Path path, String fieldId) {

        @JsonCreator
        public EnumSubject(@JsonProperty("path") Path path, @JsonProperty("fieldId") String fieldId) {
            AssertUtils.notBlank(fieldId, "fieldId can't be blank");
            this.path = path;
            this.fieldId = fieldId;
        }
    }

    /**
     * 枚举操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = EnumOperator.Equal.class, name = "EQ"),
            @JsonSubTypes.Type(value = EnumOperator.NotEqual.class, name = "NE"),
            @JsonSubTypes.Type(value = EnumOperator.In.class, name = "IN"),
            @JsonSubTypes.Type(value = EnumOperator.NotIn.class, name = "NOT_IN"),
            @JsonSubTypes.Type(value = EnumOperator.IsEmpty.class, name = "IS_EMPTY"),
            @JsonSubTypes.Type(value = EnumOperator.IsNotEmpty.class, name = "IS_NOT_EMPTY"),
    })
    public interface EnumOperator {

        /** 等于（单选） */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Equal implements EnumOperator {
            private final String optionId;

            @JsonCreator
            public Equal(@JsonProperty("optionId") String optionId) {
                this.optionId = optionId;
            }
        }

        /** 不等于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotEqual implements EnumOperator {
            private final String optionId;

            @JsonCreator
            public NotEqual(@JsonProperty("optionId") String optionId) {
                this.optionId = optionId;
            }
        }

        /** 在列表中 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class In implements EnumOperator {
            private final List<String> optionIds;

            @JsonCreator
            public In(@JsonProperty("optionIds") List<String> optionIds) {
                this.optionIds = optionIds;
            }
        }

        /** 不在列表中 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class NotIn implements EnumOperator {
            private final List<String> optionIds;

            @JsonCreator
            public NotIn(@JsonProperty("optionIds") List<String> optionIds) {
                this.optionIds = optionIds;
            }
        }

        /** 为空 */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsEmpty implements EnumOperator {
        }

        /** 不为空 */
        @JsonIgnoreProperties(ignoreUnknown = true)
        class IsNotEmpty implements EnumOperator {
        }
    }
}
