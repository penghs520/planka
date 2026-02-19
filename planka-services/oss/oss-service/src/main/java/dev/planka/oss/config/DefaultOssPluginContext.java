package dev.planka.oss.config;

import dev.planka.oss.plugin.OssPluginContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * OSS 插件上下文实现
 */
@Component
public class DefaultOssPluginContext implements OssPluginContext {

    private final Environment environment;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public DefaultOssPluginContext(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String getEnvironment() {
        return activeProfile;
    }

    @Override
    public String getProperty(String key) {
        return environment.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }
}
