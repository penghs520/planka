package cn.planka.card.workflow.engine;

import lombok.Builder;
import lombok.Data;

/**
 * 工作流执行上下文
 */
@Data
@Builder
public class WorkflowExecutionContext {
    private String traceId;
    private Long instanceId;
    private Long cardId;
    private String cardTypeId;
    private Long orgId;
    private Long initiatorId;
    private String workflowId;
}
