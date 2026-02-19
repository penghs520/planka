package dev.planka.api.schema.vo.notification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知渠道配置 VO
 */
@Data
@Builder
public class NotificationChannelConfigVO {

    /** 渠道配置 ID */
    private String id;

    /** 组织 ID */
    private String orgId;

    /** 渠道名称 */
    private String name;

    /** 渠道类型标识：builtin, email, feishu, dingtalk, wecom */
    private String channelId;

    /** 渠道配置参数 */
    private Map<String, Object> config;

    /** 是否为默认渠道 */
    private boolean isDefault;

    /** 优先级 */
    private int priority;

    /** 是否启用 */
    private boolean enabled;

    /** 内容版本号 */
    private int contentVersion;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
