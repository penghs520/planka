package cn.planka.card.workflow.repository;

import cn.planka.card.workflow.entity.WorkflowApprovalTaskEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkflowApprovalTaskMapper {

    @Insert("INSERT INTO workflow_approval_task (id, org_id, instance_id, node_id, card_id, " +
            "approver_id, status, approval_mode, due_at) " +
            "VALUES (#{id}, #{orgId}, #{instanceId}, #{nodeId}, #{cardId}, " +
            "#{approverId}, #{status}, #{approvalMode}, #{dueAt})")
    int insert(WorkflowApprovalTaskEntity entity);

    @Select("SELECT * FROM workflow_approval_task WHERE id = #{id}")
    WorkflowApprovalTaskEntity selectById(@Param("id") Long id);

    @Select("SELECT * FROM workflow_approval_task WHERE instance_id = #{instanceId} AND node_id = #{nodeId}")
    List<WorkflowApprovalTaskEntity> selectByInstanceIdAndNodeId(@Param("instanceId") Long instanceId,
                                                                  @Param("nodeId") String nodeId);

    @Select("SELECT * FROM workflow_approval_task WHERE approver_id = #{approverId} AND status = 'PENDING' " +
            "AND org_id = #{orgId} ORDER BY created_at DESC")
    List<WorkflowApprovalTaskEntity> selectPendingByApproverId(@Param("approverId") Long approverId,
                                                                @Param("orgId") Long orgId);

    @Update("UPDATE workflow_approval_task SET status = #{status}, comment = #{comment}, " +
            "approved_at = #{approvedAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(WorkflowApprovalTaskEntity entity);

    @Update("UPDATE workflow_approval_task SET status = 'TRANSFERRED', transferred_to = #{transferredTo}, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int transferTask(@Param("id") Long id, @Param("transferredTo") Long transferredTo);
}
