package dev.planka.domain.schema.definition.condition;

import dev.planka.common.util.AssertUtils;
import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 卡片编号条件项
 * <p>
 * 用于卡片编号字段的过滤条件。卡片编号是内置文本字段，仅支持包含操作。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CodeConditionItem extends AbstractConditionItem {

    private final CodeSubject subject;

    private final CodeOperator operator;

    @JsonCreator
    public CodeConditionItem(@JsonProperty("subject") CodeSubject subject, @JsonProperty("operator") CodeOperator operator) {
        AssertUtils.notNull(subject, "subject can't be null");
        AssertUtils.notNull(operator, "operator can't be null");
        this.subject = subject;
        this.operator = operator;
    }

    @Override
    public String getNodeType() {
        return NodeType.CODE;
    }

    public record CodeSubject(Path path) {

        @JsonCreator
        public CodeSubject(@JsonProperty("path") Path path) {
            this.path = path;
        }
    }

    /**
     * 卡片编号操作符接口
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CodeOperator.Contains.class, name = "EQ"),
            @JsonSubTypes.Type(value = CodeOperator.Contains.class, name = "CONTAINS"),
    })
    public interface CodeOperator {

        /** 等于 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Equal implements CodeOperator {
            private final String value;

            @JsonCreator
            public Equal(@JsonProperty("value") String value) {
                AssertUtils.notBlank(value, "value can't be blank");
                this.value = value;
            }
        }

        /** 包含 */
        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        class Contains implements CodeOperator {
            private final String value;

            @JsonCreator
            public Contains(@JsonProperty("value") String value) {
                AssertUtils.notBlank(value, "value can't be blank");
                this.value = value;
            }
        }
    }
}
