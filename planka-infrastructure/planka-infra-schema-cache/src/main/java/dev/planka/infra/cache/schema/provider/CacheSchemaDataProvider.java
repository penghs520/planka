package dev.planka.infra.cache.schema.provider;

import dev.planka.api.schema.spi.SchemaDataProvider;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 基于缓存的 Schema 数据查询提供者
 * <p>
 * 通过 SchemaCacheService 查询缓存。
 */
@Component
@RequiredArgsConstructor
public class CacheSchemaDataProvider implements SchemaDataProvider {

    private final SchemaCacheService schemaCacheService;

    @Override
    public Optional<CardTypeDefinition> getCardTypeById(String cardTypeId) {
        return schemaCacheService.getById(cardTypeId)
                .filter(CardTypeDefinition.class::isInstance)
                .map(CardTypeDefinition.class::cast);
    }

    @Override
    public List<SchemaDefinition<?>> getSchemasByIds(Set<String> ids) {
        return new ArrayList<>(schemaCacheService.getByIds(ids).values());
    }

    @Override
    public List<FieldConfig> getAllFieldConfigsByCardTypeId(String cardTypeId) {
        return schemaCacheService.getBySecondaryIndex(
                        CardTypeId.of(cardTypeId), SchemaType.FIELD_CONFIG)
                .stream()
                .filter(FieldConfig.class::isInstance)
                .map(FieldConfig.class::cast)
                .toList();
    }

    @Override
    public List<LinkTypeDefinition> getLinkTypesByCardTypeId(String cardTypeId) {
        return schemaCacheService.getBySecondaryIndex(
                        CardTypeId.of(cardTypeId), SchemaType.LINK_TYPE)
                .stream()
                .filter(LinkTypeDefinition.class::isInstance)
                .map(LinkTypeDefinition.class::cast)
                .toList();
    }
}
