package dev.planka.api.schema.vo.cardtype;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaSubType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Set;

/**
 * 实体类型 VO
 * <p>
 * 实体类型可以继承多个属性集，可用于创建卡片实例。
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EntityCardTypeVO extends CardTypeVO {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.ENTITY_CARD_TYPE;
    }

    /**
     * 编号生成规则
     */
    private CodeGenerationRuleVO codeGenerationRule;

    /**
     * 继承的属性集 ID 列表
     */
    private Set<CardTypeId> parentTypeIds;

    /**
     * 继承的属性集信息列表
     */
    private List<CardTypeInfo> parentTypes;

    /**
     * 使用的价值流定义 ID
     */
    private String valueStreamId;

    /**
     * 默认卡面定义 ID
     */
    private String defaultCardFaceId;

}
