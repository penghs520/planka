package dev.planka.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-组织关系实体
 */
@Data
@TableName("sys_user_organization")
public class UserOrganizationEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String userId;
    private String orgId;
    private String memberCardId;

    private String role;
    private String status;

    private String invitedBy;
    private LocalDateTime joinedAt;
    private LocalDateTime updatedAt;
}
