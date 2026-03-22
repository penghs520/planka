package cn.planka.api.schema.vo.cardtype;

import lombok.Builder;
import lombok.Data;

/**
 * 实体类型选项 VO（用于下拉框）
 * <p>
 * 只包含下拉框所需的必要字段，不包含继承类型等详细信息
 */
@Data
@Builder
public class CardTypeOptionVO {
    /**
     * 实体类型 ID
     */
    private String id;

    /**
     * 实体类型名称
     */
    private String name;

    /**
     * Schema 子类型（TRAIT_CARD_TYPE 或 ENTITY_CARD_TYPE）
     */
    private String schemaSubType;
}
