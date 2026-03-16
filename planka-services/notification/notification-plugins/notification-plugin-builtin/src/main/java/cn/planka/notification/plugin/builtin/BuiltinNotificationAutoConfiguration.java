package cn.planka.notification.plugin.builtin;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 系统内置通知自动配置
 */
@Configuration
@ConditionalOnProperty(prefix = "notification.builtin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BuiltinNotificationAutoConfiguration {

    @Bean
    @ConditionalOnBean(BuiltinNotificationSender.class)
    public BuiltinNotificationChannel builtinNotificationChannel(BuiltinNotificationSender sender) {
        return new BuiltinNotificationChannel(sender);
    }
}
