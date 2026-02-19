package dev.planka.api.schema.request.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 更新通知渠道配置请求
 */
@Getter
@Setter
public class UpdateNotificationChannelRequest {

    /** 渠道名称 */
    @NotBlank(message = "渠道名称不能为空")
    private String name;

    /** 渠道配置参数 */
    private Map<String, Object> config;

    /** 是否为默认渠道 */
    private boolean isDefault;

    /** 优先级 */
    private int priority = 100;

    /** 期望版本号（乐观锁） */
    private Integer expectedVersion;
}
