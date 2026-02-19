package dev.planka.oss.plugin;

import lombok.Builder;
import lombok.Getter;

/**
 * OSS 插件定义
 */
@Getter
@Builder
public class OssPluginDef {

    /**
     * 插件 ID
     */
    private final String id;

    /**
     * 插件名称
     */
    private final String name;

    /**
     * 插件描述
     */
    private final String description;

    /**
     * 插件版本
     */
    private final String version;

    /**
     * 插件提供者
     */
    private final String provider;

    /**
     * 是否支持预签名 URL
     */
    private final boolean supportsPresignedUrl;
}
