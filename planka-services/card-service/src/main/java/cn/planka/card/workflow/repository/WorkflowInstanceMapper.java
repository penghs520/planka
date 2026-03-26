package cn.planka.card.workflow.repository;

import cn.planka.card.workflow.entity.WorkflowInstanceEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkflowInstanceMapper {

    @Insert("INSERT INTO workflow_instance (id, org_id, workflow_id, card_id, card_type_id, " +
            "status, trigger_type, current_node_ids, context_data, definition_snapshot, " +
            "definition_version, started_by, started_at, version) " +
            "VALUES (#{id}, #{orgId}, #{workflowId}, #{cardId}, #{cardTypeId}, " +
            "#{status}, #{triggerType}, #{currentNodeIds}, #{contextData}, #{definitionSnapshot}, " +
            "#{definitionVersion}, #{startedBy}, #{startedAt}, 0)")
    int insert(WorkflowInstanceEntity entity);

    @Select("SELECT * FROM workflow_instance WHERE id = #{id}")
    WorkflowInstanceEntity selectById(@Param("id") Long id);

    @Select("SELECT * FROM workflow_instance WHERE card_id = #{cardId} AND status = #{status}")
    List<WorkflowInstanceEntity> selectByCardIdAndStatus(@Param("cardId") Long cardId,
                                                          @Param("status") String status);

    @Select("SELECT * FROM workflow_instance WHERE started_by = #{startedBy} AND org_id = #{orgId} " +
            "ORDER BY created_at DESC")
    List<WorkflowInstanceEntity> selectByStartedByAndOrgId(@Param("startedBy") Long startedBy,
                                                            @Param("orgId") Long orgId);

    @Update("UPDATE workflow_instance SET status = #{status}, current_node_ids = #{currentNodeIds}, " +
            "version = version + 1, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} AND version = #{version}")
    int updateStatusWithVersion(WorkflowInstanceEntity entity);

    @Update("UPDATE workflow_instance SET status = #{status}, completed_by = #{completedBy}, " +
            "completed_at = #{completedAt}, version = version + 1, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} AND version = #{version}")
    int completeInstance(WorkflowInstanceEntity entity);

    @Update("UPDATE workflow_instance SET status = 'CANCELLED', cancel_reason = #{cancelReason}, " +
            "version = version + 1, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} AND version = #{version}")
    int cancelInstance(@Param("id") Long id, @Param("cancelReason") String cancelReason,
                       @Param("version") Integer version);
}
