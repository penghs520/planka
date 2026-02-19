package dev.planka.api.schema.vo.cardtype;

import dev.planka.domain.schema.SchemaSubType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 属性集 VO
 * <p>
 * 属性集只能被继承，不能直接创建卡片。
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TraitCardTypeVO extends CardTypeVO {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.TRAIT_CARD_TYPE;
    }
}
