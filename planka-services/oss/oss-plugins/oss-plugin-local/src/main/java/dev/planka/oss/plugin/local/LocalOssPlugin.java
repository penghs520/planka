package dev.planka.oss.plugin.local;

import dev.planka.oss.plugin.OssClient;
import dev.planka.oss.plugin.OssPlugin;
import dev.planka.oss.plugin.OssPluginContext;
import dev.planka.oss.plugin.OssPluginDef;
import lombok.extern.slf4j.Slf4j;

/**
 * 本地文件系统存储插件
 */
@Slf4j
public class LocalOssPlugin implements OssPlugin {

    public static final String PLUGIN_ID = "local";

    private final LocalOssClient client;
    private final OssPluginDef def;

    public LocalOssPlugin(LocalOssProperties properties) {
        this.client = new LocalOssClient(properties);
        this.def = OssPluginDef.builder()
            .id(PLUGIN_ID)
            .name("本地文件系统")
            .description("使用本地文件系统存储文件")
            .version(version())
            .provider(provider())
            .supportsPresignedUrl(false)
            .build();
        log.info("本地存储插件初始化完成: basePath={}", properties.getBasePath());
    }

    @Override
    public String pluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String name() {
        return "本地文件系统";
    }

    @Override
    public OssClient getClient() {
        return client;
    }

    @Override
    public OssPluginDef def() {
        return def;
    }

    @Override
    public void initialize(OssPluginContext context) {
        log.info("本地存储插件已初始化，环境: {}", context.getEnvironment());
    }

    @Override
    public boolean supportsPresignedUrl() {
        return false;
    }
}
