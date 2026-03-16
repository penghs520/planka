package cn.planka.notification.plugin.builtin;

import lombok.extern.slf4j.Slf4j;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

/**
 * 站内信插件主类
 * <p>
 * PF4J 插件入口，管理插件生命周期
 * </p>
 *
 * @author Agilean
 * @since 2.0.0
 */
@Slf4j
public class BuiltinPluginDef extends Plugin {

    public BuiltinPluginDef(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void start() {
        log.info("站内信插件启动: version={}", wrapper.getDescriptor().getVersion());
    }

    @Override
    public void stop() {
        log.info("站内信插件停止: version={}", wrapper.getDescriptor().getVersion());
    }
}
