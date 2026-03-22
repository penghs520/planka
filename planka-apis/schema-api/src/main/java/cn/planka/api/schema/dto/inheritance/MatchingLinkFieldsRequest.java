package cn.planka.api.schema.dto.inheritance;

import lombok.Data;

import java.util.List;

/**
 * 匹配关联属性请求
 * 用于查找源实体类型和目标实体类型之间可以建立关联的属性
 */
@Data
public class MatchingLinkFieldsRequest {
    /**
     * 源实体类型ID列表（例如：父层级的实体类型）
     */
    private List<String> sourceCardTypeIds;

    /**
     * 目标实体类型ID列表（例如：当前层级的实体类型）
     */
    private List<String> targetCardTypeIds;
}
