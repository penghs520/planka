package dev.planka.oss.plugin;

/**
 * 插件上下文
 * 提供插件运行时所需的环境信息
 */
public interface OssPluginContext {

    /**
     * 获取环境（dev/test/prod）
     */
    String getEnvironment();

    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    String getProperty(String key);

    /**
     * 获取配置值（带默认值）
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getProperty(String key, String defaultValue);
}
