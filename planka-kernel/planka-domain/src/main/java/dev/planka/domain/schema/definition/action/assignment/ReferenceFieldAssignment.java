package dev.planka.domain.schema.definition.action.assignment;

import dev.planka.domain.link.Path;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 字段引用赋值
 * <p>
 * 引用当前用户或卡片字段的值，支持级联关联。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ReferenceFieldAssignment implements FieldAssignment {

    /**
     * 目标字段ID
     */
    @JsonProperty("fieldId")
    private String fieldId;

    /**
     * 引用来源
     */
    @JsonProperty("source")
    private ReferenceSource source;

    /**
     * 引用的字段ID
     * <p>
     * - source=CURRENT_USER 时：可选，null 表示当前成员卡片的ID，也可以是用户的某个属性字段
     * - source=CURRENT_CARD 时：必填，指定要引用的字段
     */
    @JsonProperty("sourceFieldId")
    private String sourceFieldId;

    /**
     * 关联路径（支持级联）
     * <p>
     * 复用现有 Path 类，每个节点格式为 "linkTypeId:SOURCE" 或 "linkTypeId:TARGET"。
     * 为 null 表示引用当前卡片的字段（无需跳转）。
     */
    @JsonProperty("path")
    private Path path;

    /**
     * 追加模式（仅多选关联字段有效）
     * <p>
     * true: 将引用值追加到现有列表
     * false: 替换为引用值
     */
    @JsonProperty("appendMode")
    private boolean appendMode = false;

    @Override
    public String getAssignmentType() {
        return "REFERENCE_FIELD";
    }

    /**
     * 引用来源枚举
     */
    public enum ReferenceSource {
        /**
         * 当前操作用户
         */
        CURRENT_USER,

        /**
         * 当前卡片
         */
        CURRENT_CARD
    }
}
