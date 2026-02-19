package dev.planka.api.schema.vo.formuladefinition;

import dev.planka.domain.schema.SchemaSubType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 数值运算公式定义 VO
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class NumberCalculationFormulaDefinitionVO extends FormulaDefinitionVO {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION;
    }

    /**
     * SPEL表达式
     */
    private String expression;

    /**
     * 结构化表达式定义（JSON格式）
     */
    private String expressionStructure;

    /**
     * 结果精度（小数位数）
     */
    private Integer precision;

}
