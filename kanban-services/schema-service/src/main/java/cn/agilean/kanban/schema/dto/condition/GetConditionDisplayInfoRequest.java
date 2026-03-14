package cn.agilean.kanban.schema.dto.condition;

import cn.agilean.kanban.domain.schema.definition.condition.Condition;
import jakarta.validation.constraints.NotNull;

/**
 * 获取条件显示信息请求
 */
public record GetConditionDisplayInfoRequest(
        @NotNull(message = "condition不能为空")
        Condition condition
) {
}
