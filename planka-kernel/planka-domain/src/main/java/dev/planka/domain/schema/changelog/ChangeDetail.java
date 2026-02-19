package dev.planka.domain.schema.changelog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 变更详情 - 顶层容器
 * <p>
 * 存储Schema变更的结构化信息，包含字段级别变更和语义级别变更。
 * 用于替代简单的文本摘要，提供更丰富的变更信息展示。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeDetail {

    /**
     * Schema类型（如 "CARD_TYPE", "FIELD_DEFINITION"）
     */
    private String schemaType;

    /**
     * Schema子类型（如 "ENTITY_CARD_TYPE", "ENUM_FIELD_DEFINITION"）
     */
    private String schemaSubType;

    /**
     * 变更操作类型
     */
    private ChangeAction action;

    /**
     * 字段级别变更列表
     * <p>
     * 用于记录简单字段的变更，如名称、描述、启用状态等。
     */
    @Builder.Default
    private List<FieldChange> changes = new ArrayList<>();

    /**
     * 语义级别变更列表
     * <p>
     * 用于记录复杂嵌套结构的变更，如枚举选项、列配置等。
     */
    @Builder.Default
    private List<SemanticChange> semanticChanges = new ArrayList<>();

    /**
     * 创建CREATE操作的变更详情
     */
    public static ChangeDetail forCreate(String schemaType, String schemaSubType) {
        return ChangeDetail.builder()
                .schemaType(schemaType)
                .schemaSubType(schemaSubType)
                .action(ChangeAction.CREATE)
                .build();
    }

    /**
     * 创建UPDATE操作的变更详情
     */
    public static ChangeDetail forUpdate(String schemaType, String schemaSubType) {
        return ChangeDetail.builder()
                .schemaType(schemaType)
                .schemaSubType(schemaSubType)
                .action(ChangeAction.UPDATE)
                .build();
    }

    /**
     * 创建DELETE操作的变更详情
     */
    public static ChangeDetail forDelete(String schemaType, String schemaSubType) {
        return ChangeDetail.builder()
                .schemaType(schemaType)
                .schemaSubType(schemaSubType)
                .action(ChangeAction.DELETE)
                .build();
    }

    /**
     * 添加字段变更
     */
    public ChangeDetail addFieldChange(FieldChange change) {
        if (this.changes == null) {
            this.changes = new ArrayList<>();
        }
        this.changes.add(change);
        return this;
    }

    /**
     * 添加语义变更
     */
    public ChangeDetail addSemanticChange(SemanticChange change) {
        if (this.semanticChanges == null) {
            this.semanticChanges = new ArrayList<>();
        }
        this.semanticChanges.add(change);
        return this;
    }

    /**
     * 是否有变更
     */
    public boolean hasChanges() {
        return (changes != null && !changes.isEmpty())
                || (semanticChanges != null && !semanticChanges.isEmpty());
    }
}
