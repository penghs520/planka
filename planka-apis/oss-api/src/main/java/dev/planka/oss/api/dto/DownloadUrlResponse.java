package dev.planka.oss.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下载 URL 响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadUrlResponse {

    private String url;

    private int expiresIn;
}
