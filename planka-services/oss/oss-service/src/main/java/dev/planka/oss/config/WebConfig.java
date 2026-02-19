package dev.planka.oss.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置 - 静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${oss.local.base-path:/data/oss}")
    private String basePath;

    @Value("${oss.local.base-url:/files}")
    private String baseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String expandedBasePath = expandPath(basePath);
        String resourcePattern = baseUrl.endsWith("/") ? baseUrl + "**" : baseUrl + "/**";
        String resourceLocation = "file:" + (expandedBasePath.endsWith("/") ? expandedBasePath : expandedBasePath + "/");

        registry.addResourceHandler(resourcePattern)
            .addResourceLocations(resourceLocation);
    }

    /**
     * 展开路径中的 ~ 为用户主目录
     */
    private String expandPath(String path) {
        if (path != null && path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}
