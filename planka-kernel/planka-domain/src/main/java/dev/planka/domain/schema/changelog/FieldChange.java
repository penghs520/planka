package dev.planka.domain.schema.changelog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段级别变更
 * <p>
 * 记录单个字段的变更信息，包括字段路径、显示名、变更类型和新旧值。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldChange {

    /**
     * 字段路径（如 "name", "description"）
     */
    private String fieldPath;

    /**
     * 字段显示名（如 "名称", "描述"）
     */
    private String fieldLabel;

    /**
     * 变更类型
     */
    private FieldChangeType changeType;

    /**
     * 旧值
     */
    private Object oldValue;

    /**
     * 新值
     */
    private Object newValue;

    /**
     * 值类型标识（"string", "number", "boolean", "list"）
     */
    private String valueType;

    /**
     * 创建字段新增变更
     */
    public static FieldChange added(String fieldPath, String fieldLabel, Object newValue, String valueType) {
        return FieldChange.builder()
                .fieldPath(fieldPath)
                .fieldLabel(fieldLabel)
                .changeType(FieldChangeType.ADDED)
                .oldValue(null)
                .newValue(newValue)
                .valueType(valueType)
                .build();
    }

    /**
     * 创建字段修改变更
     */
    public static FieldChange modified(String fieldPath, String fieldLabel, Object oldValue, Object newValue, String valueType) {
        return FieldChange.builder()
                .fieldPath(fieldPath)
                .fieldLabel(fieldLabel)
                .changeType(FieldChangeType.MODIFIED)
                .oldValue(oldValue)
                .newValue(newValue)
                .valueType(valueType)
                .build();
    }

    /**
     * 创建字段移除变更
     */
    public static FieldChange removed(String fieldPath, String fieldLabel, Object oldValue, String valueType) {
        return FieldChange.builder()
                .fieldPath(fieldPath)
                .fieldLabel(fieldLabel)
                .changeType(FieldChangeType.REMOVED)
                .oldValue(oldValue)
                .newValue(null)
                .valueType(valueType)
                .build();
    }
}
