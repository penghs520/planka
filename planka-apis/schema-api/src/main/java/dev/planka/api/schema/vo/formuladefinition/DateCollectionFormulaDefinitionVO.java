package dev.planka.api.schema.vo.formuladefinition;

import dev.planka.domain.schema.SchemaSubType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 日期汇集公式定义 VO
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DateCollectionFormulaDefinitionVO extends FormulaDefinitionVO {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION;
    }

    /**
     * 关联属性ID
     */
    private String linkFieldId;

    /**
     * 目标卡片类型ID列表
     */
    private List<String> targetCardTypeIds;

    /**
     * 关联卡片中的源日期字段ID
     */
    private String sourceFieldId;

    /**
     * 汇集方式
     */
    private String aggregationType;

    /**
     * 过滤条件（JSON格式）
     */
    private String filterCondition;

}
