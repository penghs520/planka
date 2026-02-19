package dev.planka.oss.plugin;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

/**
 * 对象存储客户端接口
 */
public interface OssClient {

    /**
     * 上传文件
     *
     * @param request 上传请求
     * @return 上传结果
     */
    UploadResult upload(UploadRequest request);

    /**
     * 下载文件
     *
     * @param objectKey 对象 Key
     * @return 文件输入流
     */
    Optional<InputStream> download(String objectKey);

    /**
     * 删除文件
     *
     * @param objectKey 对象 Key
     * @return 是否删除成功
     */
    boolean delete(String objectKey);

    /**
     * 判断文件是否存在
     *
     * @param objectKey 对象 Key
     * @return 是否存在
     */
    boolean exists(String objectKey);

    /**
     * 生成预签名上传 URL
     *
     * @param objectKey   对象 Key
     * @param contentType 内容类型
     * @param expiration  过期时间
     * @return 预签名 URL
     */
    Optional<String> generatePresignedUploadUrl(String objectKey, String contentType, Duration expiration);

    /**
     * 生成预签名下载 URL
     *
     * @param objectKey  对象 Key
     * @param expiration 过期时间
     * @return 预签名 URL
     */
    Optional<String> generatePresignedDownloadUrl(String objectKey, Duration expiration);
}
