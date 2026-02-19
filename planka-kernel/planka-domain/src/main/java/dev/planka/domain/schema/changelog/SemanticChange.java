package dev.planka.domain.schema.changelog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 语义级别变更
 * <p>
 * 用于描述复杂嵌套结构的变更，如枚举选项列表、列配置列表等。
 * 提供更高层次的业务语义描述，如"添加了枚举选项'高优先级'"。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticChange {

    /**
     * 变更类别（如 "ENUM_ITEM", "COLUMN_CONFIG", "PARENT_TYPE"）
     */
    private String category;

    /**
     * 操作类型
     */
    private SemanticChangeType operation;

    /**
     * 目标项标识（如枚举选项ID）
     */
    private String targetId;

    /**
     * 目标项名称（如枚举选项名称）
     */
    private String targetName;

    /**
     * 详细字段变更（当operation为MODIFIED时使用）
     */
    @Builder.Default
    private List<FieldChange> details = new ArrayList<>();

    /**
     * 创建新增语义变更
     */
    public static SemanticChange added(String category, String targetId, String targetName) {
        return SemanticChange.builder()
                .category(category)
                .operation(SemanticChangeType.ADDED)
                .targetId(targetId)
                .targetName(targetName)
                .build();
    }

    /**
     * 创建移除语义变更
     */
    public static SemanticChange removed(String category, String targetId, String targetName) {
        return SemanticChange.builder()
                .category(category)
                .operation(SemanticChangeType.REMOVED)
                .targetId(targetId)
                .targetName(targetName)
                .build();
    }

    /**
     * 创建修改语义变更
     */
    public static SemanticChange modified(String category, String targetId, String targetName, List<FieldChange> details) {
        return SemanticChange.builder()
                .category(category)
                .operation(SemanticChangeType.MODIFIED)
                .targetId(targetId)
                .targetName(targetName)
                .details(details != null ? details : new ArrayList<>())
                .build();
    }

    /**
     * 创建重排序语义变更
     */
    public static SemanticChange reordered(String category) {
        return SemanticChange.builder()
                .category(category)
                .operation(SemanticChangeType.REORDERED)
                .build();
    }
}
