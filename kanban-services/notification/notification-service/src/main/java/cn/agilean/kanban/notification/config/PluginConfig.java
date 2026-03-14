package cn.agilean.kanban.notification.config;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * PF4J 插件管理器配置
 */
@Configuration
public class PluginConfig {

    @Bean
    public PluginManager pluginManager(@Value("${plugin.pluginPath:./plugins}") String pluginPath) {
        return new DefaultPluginManager(Path.of(pluginPath));
    }
}
