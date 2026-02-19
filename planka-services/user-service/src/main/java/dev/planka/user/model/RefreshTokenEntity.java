package dev.planka.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 刷新令牌实体
 */
@Data
@TableName("sys_refresh_token")
public class RefreshTokenEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String userId;
    private String tokenHash;
    private String deviceInfo;

    /** 组织ID（可空，仅当用户已切换到某个组织时有值） */
    private String orgId;

    /** 成员卡片ID（可空，仅当用户已切换到某个组织时有值） */
    private String memberCardId;

    /** 用户在组织中的角色（可空） */
    private String role;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private boolean revoked;
}
