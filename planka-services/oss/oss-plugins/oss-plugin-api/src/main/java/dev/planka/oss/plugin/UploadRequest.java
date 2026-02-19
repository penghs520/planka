package dev.planka.oss.plugin;

import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

/**
 * 文件上传请求
 */
@Getter
@Builder
public class UploadRequest {

    /**
     * 组织 ID
     */
    private final String orgId;

    /**
     * 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID）
     */
    private final String operatorId;

    /**
     * 文件类别
     */
    private final FileCategory category;

    /**
     * 原始文件名
     */
    private final String originalName;

    /**
     * 文件内容类型
     */
    private final String contentType;

    /**
     * 文件大小（字节）
     */
    private final long size;

    /**
     * 文件输入流
     */
    private final InputStream inputStream;

    /**
     * 可选的自定义 object key（为空则自动生成）
     */
    private final String customObjectKey;
}
