package dev.planka.domain.schema.definition.action;

import dev.planka.domain.schema.definition.action.assignment.FieldAssignment;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 更新卡片执行类型
 * <p>
 * 执行动作时更新当前卡片的字段或状态。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class UpdateCardExecution implements ActionExecutionType {

    /**
     * 字段赋值列表
     */
    @JsonProperty("fieldAssignments")
    private List<FieldAssignment> fieldAssignments;

    /**
     * 目标状态ID
     * <p>
     * 执行后将卡片状态切换到指定状态
     */
    @JsonProperty("targetStatusId")
    private String targetStatusId;

    @Override
    public String getType() {
        return "UPDATE_CARD";
    }
}
