package cn.planka.schema.service.common.lifecycle;

import cn.planka.domain.schema.SchemaSubType;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.template.CardDetailTemplateDefinition;
import cn.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 卡片详情页模板：每个实体类型仅允许一条持久化模板。
 */
@Component
@RequiredArgsConstructor
public class CardDetailTemplateLifecycleHandler implements SchemaLifecycleHandler<CardDetailTemplateDefinition> {

    private final SchemaQuery schemaQuery;

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CARD_DETAIL_TEMPLATE;
    }

    @Override
    public void beforeCreate(CardDetailTemplateDefinition definition) {
        if (definition.getCardTypeId() == null) {
            return;
        }
        List<SchemaDefinition<?>> existing = schemaQuery.queryBySecondKey(
                definition.getCardTypeId(), SchemaType.CARD_DETAIL_TEMPLATE);
        long count = existing.stream().filter(d -> d instanceof CardDetailTemplateDefinition).count();
        if (count >= 1) {
            throw new IllegalArgumentException("该实体类型已存在详情页模板，每个实体类型仅允许一个");
        }
    }
}
