package dev.planka.api.card.renderconfig;

import lombok.*;

/**
 * 日期类型渲染配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DateRenderConfig extends FieldRenderConfig {

    /** 日期格式：DATE, DATETIME, DATETIME_SECOND, YEAR_MONTH */
    private String dateFormat;
}
