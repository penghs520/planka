package dev.planka.oss.plugin.minio;

import dev.planka.oss.plugin.OssClient;
import dev.planka.oss.plugin.OssPlugin;
import dev.planka.oss.plugin.OssPluginContext;
import dev.planka.oss.plugin.OssPluginDef;
import lombok.extern.slf4j.Slf4j;

/**
 * MinIO 对象存储插件
 */
@Slf4j
public class MinioOssPlugin implements OssPlugin {

    public static final String PLUGIN_ID = "minio";

    private final MinioOssClient client;
    private final OssPluginDef def;

    public MinioOssPlugin(MinioOssProperties properties) {
        this.client = new MinioOssClient(properties);
        this.def = OssPluginDef.builder()
            .id(PLUGIN_ID)
            .name("MinIO")
            .description("MinIO 对象存储")
            .version(version())
            .provider(provider())
            .supportsPresignedUrl(true)
            .build();
        log.info("MinIO 存储插件初始化完成: endpoint={}, bucket={}",
            properties.getEndpoint(), properties.getBucket());
    }

    @Override
    public String pluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String name() {
        return "MinIO";
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
        log.info("MinIO 存储插件已初始化，环境: {}", context.getEnvironment());
    }

    @Override
    public boolean supportsPresignedUrl() {
        return true;
    }
}
