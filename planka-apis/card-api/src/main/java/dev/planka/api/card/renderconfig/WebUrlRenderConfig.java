package dev.planka.api.card.renderconfig;

import lombok.*;

/**
 * 网页链接类型渲染配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WebUrlRenderConfig extends FieldRenderConfig {

    /** 是否验证 URL 格式 */
    private boolean validateUrl;

    /** 是否显示链接预览 */
    private boolean showPreview;
}
