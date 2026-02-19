package dev.planka.domain.schema.definition.action;

import dev.planka.domain.schema.definition.action.assignment.FieldAssignment;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 新建关联卡片执行类型
 * <p>
 * 执行动作时自动创建一张新卡片并建立关联关系。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CreateLinkedCardExecution implements ActionExecutionType {

    /**
     * 关联类型ID
     */
    @JsonProperty("linkTypeId")
    private String linkTypeId;

    /**
     * 目标卡片类型ID
     */
    @JsonProperty("targetCardTypeId")
    private String targetCardTypeId;

    /**
     * 标题模板
     * <p>
     * 支持 ${fieldId} 和 ${$title} 等表达式引用当前卡片的字段值。
     * 例如："子任务 - ${$title}"
     */
    @JsonProperty("titleTemplate")
    private String titleTemplate;

    /**
     * 新卡片的字段赋值配置
     */
    @JsonProperty("fieldAssignments")
    private List<FieldAssignment> fieldAssignments;

    /**
     * 是否显示创建弹窗
     * <p>
     * true: 弹出创建对话框让用户编辑后确认
     * false: 静默创建
     */
    @JsonProperty("showCreateDialog")
    private boolean showCreateDialog = true;

    @Override
    public String getType() {
        return "CREATE_LINKED_CARD";
    }
}
