package dev.planka.oss.plugin;

/**
 * OSS 存储插件接口（SPI 扩展点）
 * 所有存储插件必须实现此接口
 */
public interface OssPlugin {

    /**
     * 插件 ID（唯一标识）
     */
    String pluginId();

    /**
     * 插件名称
     */
    String name();

    /**
     * 插件版本
     */
    default String version() {
        return "1.0.0";
    }

    /**
     * 插件提供者
     */
    default String provider() {
        return "Agilean";
    }

    /**
     * 获取存储客户端
     */
    OssClient getClient();

    /**
     * 获取插件定义
     */
    OssPluginDef def();

    /**
     * 初始化插件（可选）
     *
     * @param context 插件上下文
     */
    default void initialize(OssPluginContext context) {
        // 默认空实现
    }

    /**
     * 销毁插件（可选）
     */
    default void destroy() {
        // 默认空实现
    }

    /**
     * 是否支持预签名 URL
     */
    default boolean supportsPresignedUrl() {
        return false;
    }
}
