package cn.planka.api.schema.dto.inheritance;

import cn.planka.api.schema.dto.FieldOption;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 多个实体类型的共同属性选项响应
 */
@Data
@Builder
public class CommonFieldOptionResponse {

    /**
     * 共同属性列表
     */
    private List<FieldOption> fields;
}
