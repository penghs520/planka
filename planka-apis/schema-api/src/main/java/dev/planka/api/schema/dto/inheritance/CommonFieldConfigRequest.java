package dev.planka.api.schema.dto.inheritance;

import lombok.Data;

import java.util.List;

/**
 * 获取多个卡片类型共同属性配置的请求
 */
@Data
public class CommonFieldConfigRequest {

    /**
     * 卡片类型ID列表（必填）
     */
    private List<String> cardTypeIds;

    /**
     * 属性类型过滤（可选）
     * <p>
     * 如: TEXT, NUMBER, DATE, ENUM, ATTACHMENT, WEB_URL, STRUCTURE, LINK
     */
    private List<String> fieldTypes;
}
