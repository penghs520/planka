package dev.planka.oss.plugin;

import lombok.Builder;
import lombok.Getter;

/**
 * 文件上传结果
 */
@Getter
@Builder
public class UploadResult {

    /**
     * 对象存储 Key
     */
    private final String objectKey;

    /**
     * 文件访问 URL
     */
    private final String url;

    /**
     * 文件大小（字节）
     */
    private final long size;

    /**
     * 文件内容类型
     */
    private final String contentType;

    /**
     * 上传是否成功
     */
    private final boolean success;

    /**
     * 错误信息（上传失败时）
     */
    private final String errorMessage;

    /**
     * 创建成功结果
     */
    public static UploadResult success(String objectKey, String url, long size, String contentType) {
        return UploadResult.builder()
            .objectKey(objectKey)
            .url(url)
            .size(size)
            .contentType(contentType)
            .success(true)
            .build();
    }

    /**
     * 创建失败结果
     */
    public static UploadResult failure(String errorMessage) {
        return UploadResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}
