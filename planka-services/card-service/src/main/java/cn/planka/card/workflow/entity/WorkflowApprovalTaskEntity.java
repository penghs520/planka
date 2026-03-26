package cn.planka.card.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkflowApprovalTaskEntity {
    private Long id;
    private Long orgId;
    private Long instanceId;
    private String nodeId;
    private Long cardId;
    private Long approverId;
    private String status;
    private String approvalMode;
    private LocalDateTime dueAt;
    private LocalDateTime approvedAt;
    private String comment;
    private String formData;
    private Long transferredTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
