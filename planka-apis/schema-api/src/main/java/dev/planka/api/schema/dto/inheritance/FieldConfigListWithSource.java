package dev.planka.api.schema.dto.inheritance;

import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 卡片类型属性配置列表响应 DTO
 * <p>
 * 包含卡片类型的完整属性配置列表和可能的冲突信息。
 * 直接使用领域对象 FieldConfig，来源信息通过 Map 单独提供。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldConfigListWithSource {

    /** 卡片类型ID */
    private String cardTypeId;

    /** 卡片类型名称 */
    private String cardTypeName;

    /**
     * 完整属性配置列表
     * <p>
     * 直接使用领域对象 FieldConfig。
     */
    private List<FieldConfig> fields;

    /**
     * 属性来源信息
     * <p>
     * Key: fieldId
     * Value: 来源信息（定义来源、配置来源、是否继承）
     */
    private Map<String, FieldSourceInfo> fieldSources;

}
