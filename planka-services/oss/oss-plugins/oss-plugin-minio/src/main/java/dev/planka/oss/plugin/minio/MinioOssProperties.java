package dev.planka.oss.plugin.minio;

import lombok.Getter;
import lombok.Setter;

/**
 * MinIO 存储配置
 */
@Getter
@Setter
public class MinioOssProperties {

    /**
     * 是否启用
     */
    private boolean enabled = false;

    /**
     * MinIO 服务端点
     */
    private String endpoint = "http://localhost:9000";

    /**
     * Access Key
     */
    private String accessKey = "minioadmin";

    /**
     * Secret Key
     */
    private String secretKey = "minioadmin";

    /**
     * 存储桶名称
     */
    private String bucket = "planka";

    /**
     * 预签名 URL 过期时间（秒）
     */
    private int presignedUrlExpiration = 3600;
}
