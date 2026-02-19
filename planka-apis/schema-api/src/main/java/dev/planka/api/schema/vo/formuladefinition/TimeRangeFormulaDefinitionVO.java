package dev.planka.api.schema.vo.formuladefinition;

import dev.planka.domain.schema.SchemaSubType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 时间段公式定义 VO
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TimeRangeFormulaDefinitionVO extends FormulaDefinitionVO {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.TIME_RANGE_FORMULA_DEFINITION;
    }

    /**
     * 开始时间数据源类型
     */
    private String startSourceType;

    /**
     * 开始时间字段ID
     */
    private String startFieldId;

    /**
     * 开始时间价值流ID
     */
    private String startStreamId;

    /**
     * 开始时间价值流状态ID
     */
    private String startStatusId;

    /**
     * 结束时间数据源类型
     */
    private String endSourceType;

    /**
     * 结束时间字段ID
     */
    private String endFieldId;

    /**
     * 结束时间价值流ID
     */
    private String endStreamId;

    /**
     * 结束时间价值流状态ID
     */
    private String endStatusId;

    /**
     * 统计精度
     */
    private String precision;

}
