package dev.planka.api.schema.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 属性选项 DTO
 * <p>
 * 用于视图配置时的属性选择，仅包含必要字段。
 * <p>
 * 对于关联属性，id 字段格式为 "{linkTypeId}:{SOURCE|TARGET}"，已包含方向信息。
 */
@Data
@Builder
public class FieldOption {

    /**
     * 属性 ID
     * <p>
     * 对于关联属性，格式为 "{linkTypeId}:{SOURCE|TARGET}"
     */
    private String id;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 属性类型（TEXT、NUMBER、DATE、ENUM、LINK等）
     */
    private String fieldType;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 属性编码
     */
    private String code;

    /**
     * 是否为系统字段
     */
    private boolean systemField;

    /**
     * 枚举选项列表（仅枚举类型字段有效）
     */
    private List<EnumOptionDTO> enumOptions;

    /**
     * 目标卡片类型ID列表（仅关联类型字段有效）
     * <p>
     * 对于 LINK 类型字段，包含该关联属性可以链接到的目标卡片类型ID。
     * SOURCE 端返回 TARGET 端卡片类型ID，TARGET 端返回 SOURCE 端卡片类型ID。
     */
    private List<String> targetCardTypeIds;

    /**
     * 架构线ID（仅架构类型字段有效）
     */
    private String structureId;

    /**
     * 是否多选（仅关联类型字段有效）
     * <p>
     * true: 多选关联，不能展开下一级
     * false: 单选关联，可以展开下一级
     */
    private Boolean multiple;
}
