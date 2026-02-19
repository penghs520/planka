package dev.planka.api.schema.vo.formuladefinition;

import dev.planka.domain.schema.SchemaSubType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 时间点公式定义 VO
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TimePointFormulaDefinitionVO extends FormulaDefinitionVO {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.TIME_POINT_FORMULA_DEFINITION;
    }

    /**
     * 数据源类型
     */
    private String sourceType;

    /**
     * 源日期字段ID
     */
    private String sourceFieldId;

    /**
     * 价值流ID
     */
    private String streamId;

    /**
     * 价值流状态ID
     */
    private String statusId;

}
