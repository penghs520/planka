package dev.planka.oss.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 预签名上传响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUploadResponse {

    private String uploadUrl;

    private String objectKey;

    private int expiresIn;
}
