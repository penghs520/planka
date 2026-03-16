package cn.planka.notification.plugin;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 混合插件管理器
 * <p>
 * 支持 PF4J 动态插件和 Spring SPI 静态插件双模式
 * 优先从 PF4J 加载，回退到 Spring Bean
 * </p>
 *
 * @author Agilean
 * @since 2.0.0
 */
@Slf4j
@Component
public class HybridPluginManager {

    private final PluginManager pf4jManager;
    private final Map<String, NotificationChannel> springChannels;
    private final Map<String, NotificationChannel> channelCache;

    public HybridPluginManager(
            PluginManager pf4jManager,
            Collection<NotificationChannel> springChannelBeans) {
        this.pf4jManager = pf4jManager;
        this.springChannels = new ConcurrentHashMap<>();
        this.channelCache = new ConcurrentHashMap<>();

        // 注册 Spring Bean 渠道
        springChannelBeans.forEach(channel -> {
            String channelId = channel.channelId();
            springChannels.put(channelId, channel);
            log.info("注册 Spring Bean 渠道: id={}, name={}", channelId, channel.name());
        });

        // 加载 PF4J 插件
        loadPf4jPlugins();
    }

    /**
     * 获取渠道
     */
    public Optional<NotificationChannel> getChannel(String channelId) {
        NotificationChannel cached = channelCache.get(channelId);
        if (cached != null) {
            return Optional.of(cached);
        }

        List<NotificationChannelPlugin> pf4jChannels = pf4jManager.getExtensions(NotificationChannelPlugin.class);
        Optional<NotificationChannelPlugin> pf4jChannel = pf4jChannels.stream()
                .filter(c -> c.channelId().equals(channelId))
                .findFirst();

        if (pf4jChannel.isPresent()) {
            NotificationChannel channel = pf4jChannel.get();
            channelCache.put(channelId, channel);
            log.debug("从 PF4J 插件获取渠道: {}", channelId);
            return Optional.of(channel);
        }

        NotificationChannel springChannel = springChannels.get(channelId);
        if (springChannel != null) {
            channelCache.put(channelId, springChannel);
            log.debug("从 Spring Bean 获取渠道: {}", channelId);
            return Optional.of(springChannel);
        }

        log.warn("渠道不存在: {}", channelId);
        return Optional.empty();
    }

    /**
     * 获取所有渠道
     */
    public Collection<NotificationChannel> getAllChannels() {
        Map<String, NotificationChannel> allChannels = new HashMap<>(springChannels);

        List<NotificationChannelPlugin> pf4jChannels = pf4jManager.getExtensions(NotificationChannelPlugin.class);
        pf4jChannels.forEach(channel -> allChannels.put(channel.channelId(), channel));

        return allChannels.values();
    }

    /**
     * 加载插件
     */
    public void loadPlugin(Path pluginPath) {
        try {
            String pluginId = pf4jManager.loadPlugin(pluginPath);
            pf4jManager.startPlugin(pluginId);
            clearCache();
            log.info("插件加载成功: {}", pluginId);
        } catch (Exception e) {
            log.error("插件加载失败: {}", pluginPath, e);
            throw new RuntimeException("插件加载失败", e);
        }
    }

    /**
     * 启动插件
     */
    public void startPlugin(String pluginId) {
        try {
            pf4jManager.startPlugin(pluginId);
            clearCache();
            log.info("插件启动成功: {}", pluginId);
        } catch (Exception e) {
            log.error("插件启动失败: {}", pluginId, e);
            throw new RuntimeException("插件启动失败", e);
        }
    }

    /**
     * 停止插件
     */
    public void stopPlugin(String pluginId) {
        try {
            pf4jManager.stopPlugin(pluginId);
            clearCache();
            log.info("插件停止成功: {}", pluginId);
        } catch (Exception e) {
            log.error("插件停止失败: {}", pluginId, e);
            throw new RuntimeException("插件停止失败", e);
        }
    }

    /**
     * 卸载插件
     */
    public void unloadPlugin(String pluginId) {
        try {
            pf4jManager.unloadPlugin(pluginId);
            clearCache();
            log.info("插件卸载成功: {}", pluginId);
        } catch (Exception e) {
            log.error("插件卸载失败: {}", pluginId, e);
            throw new RuntimeException("插件卸载失败", e);
        }
    }

    /**
     * 获取所有插件信息
     */
    public List<PluginInfo> getAllPlugins() {
        return pf4jManager.getPlugins().stream()
                .map(plugin -> PluginInfo.builder()
                        .pluginId(plugin.getPluginId())
                        .pluginState(plugin.getPluginState().toString())
                        .version(plugin.getDescriptor().getVersion())
                        .provider(plugin.getDescriptor().getProvider())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 加载 PF4J 插件
     */
    private void loadPf4jPlugins() {
        try {
            pf4jManager.loadPlugins();
            pf4jManager.startPlugins();
            log.info("PF4J 插件加载完成，共 {} 个插件", pf4jManager.getPlugins().size());
        } catch (Exception e) {
            log.error("PF4J 插件加载失败", e);
        }
    }

    /**
     * 清除缓存
     */
    private void clearCache() {
        channelCache.clear();
    }

    /**
     * 插件信息
     */
    @lombok.Builder
    @lombok.Getter
    public static class PluginInfo {
        private String pluginId;
        private String pluginState;
        private String version;
        private String provider;
    }
}

