package dev.planka.card.controller.request;

import dev.planka.domain.schema.definition.action.assignment.FixedValue;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 执行卡片动作请求
 */
@Getter
@Setter
public class ExecuteActionRequest {

    /**
     * 用户输入的字段值
     * key: fieldId, value: 用户输入的固定值
     */
    private Map<String, FixedValue> userInputs;
}
