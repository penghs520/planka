package dev.planka.api.schema.dto.inheritance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 属性来源信息
 * <p>
 * 记录属性定义和配置的来源卡片类型，以及是否继承。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldSourceInfo {

    /** 属性定义来源的卡片类型ID */
    private String definitionSourceCardTypeId;

    /** 属性定义来源的卡片类型名称 */
    private String definitionSourceCardTypeName;

    /** 属性配置来源的卡片类型ID（null 表示从定义转换，无持久化配置） */
    private String configSourceCardTypeId;

    /** 属性配置来源的卡片类型名称 */
    private String configSourceCardTypeName;

    /** 属性定义是否继承 */
    private boolean definitionInherited;

    /** 属性配置是否继承 */
    private boolean configInherited;

    /** 是否来自关联类型定义（而非已保存的属性配置） */
    private boolean fromLinkTypeDefinition;

    /**
     * 是否有持久化的配置
     */
    public boolean hasPersistedConfig() {
        return configSourceCardTypeId != null;
    }
}
