package dev.planka.oss.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS 服务配置
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /**
     * 当前激活的插件 ID
     */
    private String activePlugin = "local";

    /**
     * 最大文件大小 (MB)
     */
    private int maxFileSize = 100;
}
