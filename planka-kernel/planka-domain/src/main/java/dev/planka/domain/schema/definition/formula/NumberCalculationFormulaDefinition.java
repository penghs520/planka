package dev.planka.domain.schema.definition.formula;

import dev.planka.domain.formula.FormulaId;
import dev.planka.domain.schema.SchemaSubType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 数值运算公式定义
 * <p>
 * 使用SPEL表达式对卡片的数值属性进行动态计算。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NumberCalculationFormulaDefinition extends AbstractFormulaDefinition {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION;
    }

    /**
     * SPEL表达式（Spring Expression Language）
     * <p>
     * 支持SPEL的所有语法特性，字段引用使用 #{fieldId} 格式
     */
    @JsonProperty("expression")
    private String expression;

    /**
     * 结构化表达式定义（JSON格式，可选）
     * <p>
     * 用于定义更复杂的表达式结构，可以包含表达式元数据、变量定义、函数调用等
     */
    @JsonProperty("expressionStructure")
    private String expressionStructure;

    /**
     * 结果精度（小数位数，可选，默认使用结果字段的精度配置）
     */
    @JsonProperty("precision")
    private Integer precision;

    @JsonCreator
    public NumberCalculationFormulaDefinition(
            @JsonProperty("id") FormulaId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        // id 可以为 null，AbstractSchemaDefinition 构造函数会自动生成新 ID
        super(id, orgId, name);
    }

    @Override
    public void validate() {
        super.validate();
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("expression 不能为空");
        }

        // expressionStructure 如果指定，必须是有效的 JSON 格式（这里只做基本检查，详细校验在 Service 层）
        if (expressionStructure != null && !expressionStructure.trim().isEmpty()) {
            // 基本格式检查：必须以 { 或 [ 开头
            String trimmed = expressionStructure.trim();
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                throw new IllegalArgumentException("expressionStructure 必须是有效的 JSON 格式");
            }
        }

        // precision 如果指定，必须为非负整数
        if (precision != null && precision < 0) {
            throw new IllegalArgumentException("precision 必须为非负整数");
        }
    }
}
