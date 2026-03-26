package cn.planka.card.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkflowInstanceEntity {
    private Long id;
    private Long orgId;
    private String workflowId;
    private Long cardId;
    private String cardTypeId;
    private String status;
    private String triggerType;
    private String currentNodeIds;
    private String contextData;
    private String definitionSnapshot;
    private Integer definitionVersion;
    private Long startedBy;
    private Long completedBy;
    private String cancelReason;
    private LocalDateTime timeoutAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
