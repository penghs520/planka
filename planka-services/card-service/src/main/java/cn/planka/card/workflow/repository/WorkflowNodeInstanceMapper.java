package cn.planka.card.workflow.repository;

import cn.planka.card.workflow.entity.WorkflowNodeInstanceEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkflowNodeInstanceMapper {

    @Insert("INSERT INTO workflow_node_instance (id, instance_id, node_id, node_type, node_name, " +
            "status, input_data, started_at) " +
            "VALUES (#{id}, #{instanceId}, #{nodeId}, #{nodeType}, #{nodeName}, " +
            "#{status}, #{inputData}, #{startedAt})")
    int insert(WorkflowNodeInstanceEntity entity);

    @Select("SELECT * FROM workflow_node_instance WHERE id = #{id}")
    WorkflowNodeInstanceEntity selectById(@Param("id") Long id);

    @Select("SELECT * FROM workflow_node_instance WHERE instance_id = #{instanceId}")
    List<WorkflowNodeInstanceEntity> selectByInstanceId(@Param("instanceId") Long instanceId);

    @Select("SELECT * FROM workflow_node_instance WHERE instance_id = #{instanceId} AND node_id = #{nodeId}")
    WorkflowNodeInstanceEntity selectByInstanceIdAndNodeId(@Param("instanceId") Long instanceId,
                                                            @Param("nodeId") String nodeId);

    @Update("UPDATE workflow_node_instance SET status = #{status}, output_data = #{outputData}, " +
            "completed_at = #{completedAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(WorkflowNodeInstanceEntity entity);

    @Update("UPDATE workflow_node_instance SET status = #{status}, error_message = #{errorMessage}, " +
            "completed_at = #{completedAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatusWithError(WorkflowNodeInstanceEntity entity);
}
