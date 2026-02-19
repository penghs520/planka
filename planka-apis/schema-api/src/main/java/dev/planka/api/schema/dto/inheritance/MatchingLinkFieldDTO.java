package dev.planka.api.schema.dto.inheritance;

import dev.planka.domain.link.LinkPosition;
import lombok.Builder;
import lombok.Data;

/**
 * 匹配的关联属性 DTO
 * 用于架构线配置场景，返回能够连接父子层级的关联属性信息
 */
@Data
@Builder
public class MatchingLinkFieldDTO {
    /**
     * 属性配置ID
     */
    private String id;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 关联类型ID
     */
    private String linkTypeId;

    /**
     * 关联位置（SOURCE/TARGET）
     */
    private LinkPosition linkPosition;

    /**
     * 是否多选（false = 单选，true = 多选）
     * 由客户端根据场景决定是否使用
     */
    private Boolean multiple;

    /**
     * 属性编码
     */
    private String code;

    /**
     * 是否为系统内置属性
     */
    private Boolean systemField;

    /**
     * 排序顺序
     */
    private Integer sortOrder;
}
