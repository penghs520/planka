package dev.planka.domain.schema.definition.fieldconfig;

import dev.planka.domain.expression.TextExpressionTemplate;
import dev.planka.domain.schema.definition.condition.Condition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 属性值校验规则
 * <p>
 * 使用 Condition 机制定义校验规则，条件满足表示校验通过，不满足表示校验失败。
 * 支持跨属性引用，例如：计划开始时间 < 计划完成时间
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationRule {

    /**
     * 校验条件（条件满足则通过，不满足则失败）
     * 示例：DateConditionItem(fieldId="startDate", operator=Before(referenceField="endDate"))
     */
    @JsonProperty("condition")
    private Condition condition;

    /**
     * 错误消息模板（支持表达式如 ${当前卡.字段名}）
     * 示例："计划开始时间必须早于计划完成时间 ${当前卡.endDate}"
     */
    @JsonProperty("errorMessage")
    private TextExpressionTemplate errorMessage;

    /**
     * 是否启用此校验规则（默认 true）
     */
    @JsonProperty("enabled")
    private boolean enabled = true;

    /**
     * 规则描述（可选，用于配置界面显示）
     */
    @JsonProperty("description")
    private String description;

    @JsonCreator
    public ValidationRule(
            @JsonProperty("condition") Condition condition,
            @JsonProperty("errorMessage") TextExpressionTemplate errorMessage) {
        this.condition = condition;
        this.errorMessage = errorMessage;
    }

    public boolean isEmpty() {
        return condition == null || condition.isEmpty();
    }

    public void validate() {
        if (enabled && !isEmpty()) {
            if (errorMessage == null || errorMessage.template() == null || errorMessage.template().isBlank()) {
                throw new IllegalArgumentException("启用的校验规则必须配置错误消息");
            }
        }
    }
}
