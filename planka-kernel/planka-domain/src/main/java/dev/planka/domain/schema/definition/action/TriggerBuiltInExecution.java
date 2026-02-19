package dev.planka.domain.schema.definition.action;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 触发内置操作执行类型
 * <p>
 * 执行动作时触发系统内置的操作。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TriggerBuiltInExecution implements ActionExecutionType {

    /**
     * 内置动作类型
     */
    @JsonProperty("builtInActionType")
    private BuiltInActionType builtInActionType;

    @Override
    public String getType() {
        return "TRIGGER_BUILT_IN";
    }
}
