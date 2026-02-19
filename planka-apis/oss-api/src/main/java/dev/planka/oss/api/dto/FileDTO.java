package dev.planka.oss.api.dto;

import dev.planka.oss.plugin.FileCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {

    private String id;

    private String orgId;

    /**
     * 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID）
     */
    private String operatorId;

    private FileCategory category;

    private String originalName;

    private String url;

    private Long size;

    private String contentType;

    private LocalDateTime createdAt;
}
