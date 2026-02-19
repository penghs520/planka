package dev.planka.api.schema.dto.inheritance;

import lombok.Data;

import java.util.List;

/**
 * 匹配关联属性请求
 * 用于查找源卡片类型和目标卡片类型之间可以建立关联的属性
 */
@Data
public class MatchingLinkFieldsRequest {
    /**
     * 源卡片类型ID列表（例如：父层级的卡片类型）
     */
    private List<String> sourceCardTypeIds;

    /**
     * 目标卡片类型ID列表（例如：当前层级的卡片类型）
     */
    private List<String> targetCardTypeIds;
}
