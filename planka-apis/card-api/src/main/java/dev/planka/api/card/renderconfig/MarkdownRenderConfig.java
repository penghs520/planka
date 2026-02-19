package dev.planka.api.card.renderconfig;

import lombok.*;

/**
 * Markdown 类型渲染配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MarkdownRenderConfig extends FieldRenderConfig {

    /** 最大长度 */
    private Integer maxLength;

    /** 占位符 */
    private String placeholder;
}
