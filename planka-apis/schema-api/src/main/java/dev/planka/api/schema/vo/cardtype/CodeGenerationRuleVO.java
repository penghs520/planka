package dev.planka.api.schema.vo.cardtype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 卡片编号生成规则 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeGenerationRuleVO {

    /** 前缀 */
    private String prefix;

    /** 日期格式 (e.g., "yyyyMMdd", "yyyy-MM-dd", etc.) */
    private String dateFormat;

    /** 日期和序号连接符 */
    private String dateSequenceConnector;

    /** 序列号长度 (默认 6) */
    private int sequenceLength;
}
