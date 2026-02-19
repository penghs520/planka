package dev.planka.domain.schema.definition.cardtype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 卡片编号生成规则
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeGenerationRule {

    /** 前缀 */
    private String prefix;

    /** 日期格式 (e.g., "yyyyMMdd", "yyyy-MM-dd", etc.) */
    private String dateFormat;

    /** 日期和序号连接符 */
    private String dateSequenceConnector;

    /** 序列号长度 (默认 6) */
    private int sequenceLength = 6;
}
