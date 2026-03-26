package cn.planka.card.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkflowNodeInstanceEntity {
    private Long id;
    private Long instanceId;
    private String nodeId;
    private String nodeType;
    private String nodeName;
    private String status;
    private String inputData;
    private String outputData;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
