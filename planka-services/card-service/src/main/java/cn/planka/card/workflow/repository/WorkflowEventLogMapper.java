package cn.planka.card.workflow.repository;

import cn.planka.card.workflow.entity.WorkflowEventLogEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkflowEventLogMapper {

    @Insert("INSERT INTO workflow_event_log (instance_id, event_type, node_id, task_id, " +
            "operator_id, event_data, occurred_at) " +
            "VALUES (#{instanceId}, #{eventType}, #{nodeId}, #{taskId}, " +
            "#{operatorId}, #{eventData}, #{occurredAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WorkflowEventLogEntity entity);

    @Select("SELECT * FROM workflow_event_log WHERE instance_id = #{instanceId} ORDER BY occurred_at ASC")
    List<WorkflowEventLogEntity> selectByInstanceId(@Param("instanceId") Long instanceId);
}
