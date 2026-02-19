package dev.planka.api.card.renderconfig;

import lombok.*;

/**
 * 数字类型渲染配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NumberRenderConfig extends FieldRenderConfig {

    /** 小数位数 */
    private Integer precision;

    /** 单位（百分数模式下为空） */
    private String unit;

    /** 最小值 */
    private Double minValue;

    /** 最大值 */
    private Double maxValue;

    /** 显示格式：NORMAL, PERCENT, THOUSAND_SEPARATOR */
    private String displayFormat;

    /** 百分数显示效果：NUMBER, PROGRESS_BAR（仅当 displayFormat 为 PERCENT 时有效） */
    private String percentStyle;

    /**
     * 是否显示千分位
     * @deprecated 使用 displayFormat == "THOUSAND_SEPARATOR" 代替
     */
    @Deprecated
    private boolean showThousandSeparator;
}
