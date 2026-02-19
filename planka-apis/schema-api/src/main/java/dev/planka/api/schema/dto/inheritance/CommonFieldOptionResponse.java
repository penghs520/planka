package dev.planka.api.schema.dto.inheritance;

import dev.planka.api.schema.dto.FieldOption;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 多个卡片类型的共同属性选项响应
 */
@Data
@Builder
public class CommonFieldOptionResponse {

    /**
     * 共同属性列表
     */
    private List<FieldOption> fields;
}
