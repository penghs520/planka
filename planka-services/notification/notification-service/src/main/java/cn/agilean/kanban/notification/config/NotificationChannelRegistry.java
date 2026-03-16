package cn.planka.notification.config;

import cn.planka.notification.plugin.HybridPluginManager;
import cn.planka.notification.plugin.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

/**
 * 通知渠道注册中心（兼容层）
 * <p>
 * 委托给 HybridPluginManager 实现
 * 保持向后兼容
 * </p>
 *
 * @author Agilean
 * @since 1.0.0
 * @deprecated 使用 {@link HybridPluginManager} 替代
 */
@Deprecated
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationChannelRegistry {

    private final HybridPluginManager pluginManager;

    /**
     * 获取指定渠道
     */
    public Optional<NotificationChannel> getChannel(String channelId) {
        return pluginManager.getChannel(channelId);
    }

    /**
     * 获取所有渠道
     */
    public Collection<NotificationChannel> getAllChannels() {
        return pluginManager.getAllChannels();
    }

    /**
     * 检查渠道是否存在
     */
    public boolean hasChannel(String channelId) {
        return pluginManager.getChannel(channelId).isPresent();
    }

    /**
     * 获取渠道数量
     */
    public int size() {
        return pluginManager.getAllChannels().size();
    }
}

