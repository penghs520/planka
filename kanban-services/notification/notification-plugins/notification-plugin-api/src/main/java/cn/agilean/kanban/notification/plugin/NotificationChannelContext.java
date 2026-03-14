package cn.agilean.kanban.notification.plugin;

/**
 * 通知渠道上下文
 * <p>
 * 提供渠道初始化时所需的环境信息
 */
public interface NotificationChannelContext {

    /**
     * 获取当前环境
     * <p>
     * 如: dev, test, prod
     */
    String getEnvironment();

    /**
     * 获取配置属性
     *
     * @param key 属性键
     * @return 属性值，不存在返回 null
     */
    String getProperty(String key);

    /**
     * 获取配置属性（带默认值）
     *
     * @param key          属性键
     * @param defaultValue 默认值
     * @return 属性值，不存在返回默认值
     */
    String getProperty(String key, String defaultValue);
}
