package dev.planka.oss.config;

import dev.planka.oss.plugin.OssPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSS 插件注册中心
 * 管理所有可用的存储插件
 */
@Slf4j
@Component
public class OssPluginRegistry {

    private final Map<String, OssPlugin> plugins = new ConcurrentHashMap<>();

    public OssPluginRegistry(Collection<OssPlugin> pluginBeans) {
        pluginBeans.forEach(plugin -> {
            String pluginId = plugin.pluginId();
            plugins.put(pluginId, plugin);
            log.info("注册存储插件: id={}, name={}, supportsPresignedUrl={}",
                pluginId,
                plugin.name(),
                plugin.supportsPresignedUrl());
        });
        log.info("存储插件注册完成，共 {} 个插件", plugins.size());
    }

    /**
     * 获取指定插件
     */
    public Optional<OssPlugin> getPlugin(String pluginId) {
        return Optional.ofNullable(plugins.get(pluginId));
    }

    /**
     * 获取所有插件
     */
    public Collection<OssPlugin> getAllPlugins() {
        return plugins.values();
    }

    /**
     * 检查插件是否存在
     */
    public boolean hasPlugin(String pluginId) {
        return plugins.containsKey(pluginId);
    }
}
