package dev.planka.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("sys_user")
public class UserEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String email;
    private String passwordHash;
    private String nickname;
    private String avatar;
    private String phone;

    private boolean superAdmin;
    private String status;

    private String activationCode;
    private LocalDateTime activationExpiresAt;

    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private int loginFailCount;

    private boolean usingDefaultPassword;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
