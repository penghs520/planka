package cn.planka.notification.plugin;

import org.pf4j.ExtensionPoint;

/**
 * 通知渠道插件扩展点
 * <p>
 * 继承现有 NotificationChannel 接口，添加 PF4J 扩展点标记
 * 保持向后兼容，现有代码无需修改
 * </p>
 *
 * @author Agilean
 * @since 2.0.0
 */
public interface NotificationChannelPlugin extends NotificationChannel, ExtensionPoint {
    // 继承现有接口，无需新增方法
}
