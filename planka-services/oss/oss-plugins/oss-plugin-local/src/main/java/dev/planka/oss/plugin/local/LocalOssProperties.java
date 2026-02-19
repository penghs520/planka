package dev.planka.oss.plugin.local;

import lombok.Getter;
import lombok.Setter;

/**
 * 本地存储配置
 */
@Getter
@Setter
public class LocalOssProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 基础存储路径
     */
    private String basePath = "/data/oss";

    /**
     * 文件访问基础 URL
     */
    private String baseUrl = "/files";
}
