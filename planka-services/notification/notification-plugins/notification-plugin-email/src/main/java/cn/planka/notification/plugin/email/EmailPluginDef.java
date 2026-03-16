package cn.planka.notification.plugin.email;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * 邮件通知插件主类
 * <p>
 * PF4J 插件入口，管理插件生命周期
 * </p>
 *
 * @author Planka
 * @since 2.0.0
 */
@Slf4j
public class EmailPluginDef extends Plugin {

    public EmailPluginDef(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        log.info("邮件通知插件启动: version={}", wrapper.getDescriptor().getVersion());
    }

    @Override
    public void stop() {
        log.info("邮件通知插件停止: version={}", wrapper.getDescriptor().getVersion());
    }
}
