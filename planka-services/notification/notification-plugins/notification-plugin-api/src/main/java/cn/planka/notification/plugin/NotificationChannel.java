package cn.planka.notification.plugin;

import java.util.List;

/**
 * 通知渠道插件接口（SPI 扩展点）
 * <p>
 * 所有通知渠道插件必须实现此接口。
 * 插件通过 Spring 自动配置机制注册到系统中。
 */
public interface NotificationChannel {

    /**
     * 渠道 ID（唯一标识）
     * <p>
     * 例如: "builtin", "email", "feishu", "dingtalk", "wecom"
     */
    String channelId();

    /**
     * 渠道名称
     */
    String name();

    /**
     * 渠道版本
     */
    default String version() {
        return "1.0.0";
    }

    /**
     * 渠道提供者
     */
    default String provider() {
        return "Agilean";
    }

    /**
     * 获取渠道定义
     * <p>
     * 包含渠道的配置字段定义，用于前端动态生成配置表单
     */
    NotificationChannelDef def();

    /**
     * 发送通知
     *
     * @param request 通知请求
     * @return 发送结果
     */
    NotificationResult send(NotificationRequest request);

    /**
     * 批量发送通知
     *
     * @param requests 通知请求列表
     * @return 发送结果列表
     */
    default List<NotificationResult> sendBatch(List<NotificationRequest> requests) {
        return requests.stream()
                .map(this::send)
                .toList();
    }

    /**
     * 初始化渠道（可选）
     * <p>
     * 在渠道注册后调用，可用于初始化连接池等资源
     */
    default void initialize(NotificationChannelContext context) {
        // 默认空实现
    }

    /**
     * 销毁渠道（可选）
     * <p>
     * 在应用关闭时调用，可用于释放资源
     */
    default void destroy() {
        // 默认空实现
    }

    /**
     * 是否支持富文本内容
     */
    default boolean supportsRichContent() {
        return false;
    }

    /**
     * 是否支持附件
     */
    default boolean supportsAttachment() {
        return false;
    }

    /**
     * 是否需要外部用户ID映射
     * <p>
     * 如飞书、钉钉等需要将系统用户ID映射为外部平台用户ID
     */
    default boolean requiresExternalUserMapping() {
        return false;
    }
}
