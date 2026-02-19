package dev.planka.api.schema.dto.inheritance;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 匹配关联属性响应
 * 返回源卡片类型中可以与目标卡片类型建立关联的属性
 */
@Data
@Builder
public class MatchingLinkFieldsResponse {
    /**
     * 匹配的关联属性列表（从源卡片类型的视角）
     * <p>
     * 这些属性可以与目标卡片类型建立关联关系
     * 例如：源=当前层级卡片类型，目标=父层级卡片类型
     * 返回的是当前层级中那些能够连接到父层级的所有关联属性（包含单选和多选）
     * 包含 multiple 字段，由客户端根据场景决定是否使用
     */
    private List<MatchingLinkFieldDTO> fields;
}
