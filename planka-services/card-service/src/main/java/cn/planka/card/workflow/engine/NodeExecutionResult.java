package cn.planka.card.workflow.engine;

import lombok.Getter;

/**
 * 节点执行结果
 */
@Getter
public class NodeExecutionResult {

    private final boolean success;
    private final boolean waitForExternal;
    private final String errorMessage;

    private NodeExecutionResult(boolean success, boolean waitForExternal, String errorMessage) {
        this.success = success;
        this.waitForExternal = waitForExternal;
        this.errorMessage = errorMessage;
    }

    /**
     * 执行成功，继续推进
     */
    public static NodeExecutionResult success() {
        return new NodeExecutionResult(true, false, null);
    }

    /**
     * 需要等待外部操作（如审批）
     */
    public static NodeExecutionResult waitForExternal() {
        return new NodeExecutionResult(true, true, null);
    }

    /**
     * 执行失败
     */
    public static NodeExecutionResult failed(String errorMessage) {
        return new NodeExecutionResult(false, false, errorMessage);
    }
}
