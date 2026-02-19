package dev.planka.view.config;

import dev.planka.common.feign.InternalRequestInterceptor;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置
 * <p>
 * 配置内部服务调用时自动添加 X-Internal-Request 请求头，
 * 以便 schema-service 识别请求来源并决定查询范围。
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalRequestInterceptor() {
        return new InternalRequestInterceptor();
    }
}
