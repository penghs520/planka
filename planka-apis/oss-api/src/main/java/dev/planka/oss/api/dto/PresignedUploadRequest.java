package dev.planka.oss.api.dto;

import dev.planka.oss.plugin.FileCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预签名上传请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadRequest {

    @NotBlank
    private String orgId;

    @NotBlank
    private String userId;

    @NotNull
    private FileCategory category;

    @NotBlank
    private String fileName;

    @NotBlank
    private String contentType;

    /**
     * 过期时间（秒），默认 3600
     */
    private int expirationSeconds = 3600;
}
