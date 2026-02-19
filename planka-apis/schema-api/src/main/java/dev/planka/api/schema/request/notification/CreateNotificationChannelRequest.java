package dev.planka.api.schema.request.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 创建通知渠道配置请求
 */
@Getter
@Setter
public class CreateNotificationChannelRequest {

    /** 渠道名称 */
    @NotBlank(message = "渠道名称不能为空")
    private String name;

    /** 渠道类型标识：builtin, email, feishu, dingtalk, wecom */
    @NotBlank(message = "渠道类型不能为空")
    private String channelId;

    /** 渠道配置参数 */
    private Map<String, Object> config;

    /** 是否为默认渠道 */
    private boolean isDefault;

    /** 优先级（数字越小优先级越高） */
    private int priority = 100;
}
