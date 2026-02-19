package dev.planka.schema.dto.condition;

import dev.planka.api.schema.dto.EnumOptionDTO;

import java.util.List;
import java.util.Map;

/**
 * 条件显示信息
 * <p>
 * 包含条件中所有需要显示名称的 ID -> Name 映射
 */
public record ConditionDisplayInfo(
        /**
         * 字段名称映射：fieldId -> name
         */
        Map<String, String> fieldNames,

        /**
         * 关联属性名称映射：linkFieldId -> name
         */
        Map<String, String> linkFieldNames,

        /**
         * 枚举选项映射：fieldId -> options
         */
        Map<String, List<EnumOptionDTO>> enumOptions,

        /**
         * 卡片信息映射：cardId -> CardDisplayInfo
         */
        Map<String, CardDisplayInfo> cards,

        /**
         * 状态名称映射：statusId -> name
         */
        Map<String, String> statusNames
) {

    /**
     * 创建空的显示信息
     */
    public static ConditionDisplayInfo empty() {
        return new ConditionDisplayInfo(
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }
}
