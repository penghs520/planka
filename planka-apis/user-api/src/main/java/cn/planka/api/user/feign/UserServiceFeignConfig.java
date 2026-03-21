package cn.planka.api.user.feign;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * {@link cn.planka.api.user.UserServiceContract} 专用 Feign 配置。
 * 勿加 {@code @Configuration}，避免被组件扫描成全局配置。
 */
public class UserServiceFeignConfig {

    @Bean
    public RequestInterceptor authForwardingFeignInterceptor() {
        return new AuthForwardingFeignInterceptor();
    }
}
