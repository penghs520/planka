package cn.planka.card.workflow.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WorkflowEventLogEntity {
    private Long id;
    private Long instanceId;
    private String eventType;
    private String nodeId;
    private Long taskId;
    private Long operatorId;
    private String eventData;
    private LocalDateTime occurredAt;
}
