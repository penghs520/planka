package dev.planka.api.card.renderconfig;

import lombok.*;

/**
 * 文本类型渲染配置（单行/多行文本共用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TextRenderConfig extends FieldRenderConfig {

    /** 最大长度 */
    private Integer maxLength;

    /** 占位符 */
    private String placeholder;

    /** 是否多行 */
    private boolean multiLine;
}
