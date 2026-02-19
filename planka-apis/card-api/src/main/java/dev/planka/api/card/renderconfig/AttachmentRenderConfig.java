package dev.planka.api.card.renderconfig;

import lombok.*;

import java.util.List;

/**
 * 附件类型渲染配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AttachmentRenderConfig extends FieldRenderConfig {

    /** 允许的文件类型（扩展名列表） */
    private List<String> allowedFileTypes;

    /** 最大文件大小（字节） */
    private Long maxFileSize;

    /** 最大文件数量 */
    private Integer maxFileCount;
}
