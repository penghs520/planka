package dev.planka.domain.field;

import dev.planka.domain.schema.definition.fieldconfig.FieldType;
import lombok.Getter;

/**
 * 内置字段枚举
 * <p>
 * 定义卡片的系统内置属性，用于视图列配置。
 * 内置字段ID使用 $ 前缀与自定义属性区分。
 */
@Getter
public enum BuiltinField {

    /**
     * 创建时间
     */
    CREATED_AT("$createdAt", "创建时间", FieldType.DATE, true, false),

    /**
     * 更新时间
     */
    UPDATED_AT("$updatedAt", "更新时间", FieldType.DATE, true, false),

    /**
     * 价值流状态
     */
    STATUS_ID("$statusId", "价值流状态", FieldType.ENUM, true, true),

    /**
     * 卡片周期（活跃/已归档/已丢弃）
     */
    CARD_STYLE("$cardStyle", "卡片周期", FieldType.ENUM, true, false),

    /**
     * 归档时间
     */
    ARCHIVED_AT("$archivedAt", "归档时间", FieldType.DATE, true, false),

    /**
     * 丢弃时间
     */
    DISCARDED_AT("$discardedAt", "丢弃时间", FieldType.DATE, true, false),

    /**
     * 卡片编号（优先 customCode，其次 codeInOrg）
     */
    CODE("$code", "卡片编号", FieldType.TEXT, true, false);

    /**
     * 字段ID，以 $ 开头
     */
    private final String fieldId;

    /**
     * 显示名称
     */
    private final String displayName;

    /**
     * 字段类型
     */
    private final FieldType fieldType;

    /**
     * 是否可排序
     */
    private final boolean sortable;

    /**
     * 是否可编辑
     */
    private final boolean editable;

    BuiltinField(String fieldId, String displayName, FieldType fieldType, boolean sortable, boolean editable) {
        this.fieldId = fieldId;
        this.displayName = displayName;
        this.fieldType = fieldType;
        this.sortable = sortable;
        this.editable = editable;
    }

    /**
     * 判断是否为内置字段ID
     *
     * @param fieldId 字段ID
     * @return 如果以 $ 开头则为内置字段
     */
    public static boolean isBuiltinField(String fieldId) {
        return fieldId != null && fieldId.startsWith("$");
    }

    /**
     * 根据字段ID查找内置字段
     *
     * @param fieldId 字段ID
     * @return 对应的内置字段枚举值，未找到时返回 null
     */
    public static BuiltinField fromFieldId(String fieldId) {
        if (fieldId == null) {
            return null;
        }
        for (BuiltinField field : values()) {
            if (field.fieldId.equals(fieldId)) {
                return field;
            }
        }
        return null;
    }
}
