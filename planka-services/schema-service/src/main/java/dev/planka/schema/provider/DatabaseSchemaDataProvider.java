package dev.planka.schema.provider;

import dev.planka.api.schema.spi.SchemaDataProvider;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于数据库的 Schema 数据查询提供者
 * <p>
 * 通过 SchemaRepository 和 SchemaQuery 查询数据库。
 */
@Component
@RequiredArgsConstructor
public class DatabaseSchemaDataProvider implements SchemaDataProvider {

    private final SchemaRepository schemaRepository;
    private final SchemaQuery schemaQuery;

    @Override
    public Optional<CardTypeDefinition> getCardTypeById(String cardTypeId) {
        return schemaRepository.findById(cardTypeId)
                .filter(CardTypeDefinition.class::isInstance)
                .map(CardTypeDefinition.class::cast);
    }

    @Override
    public List<SchemaDefinition<?>> getSchemasByIds(Set<String> ids) {
        return schemaRepository.findByIds(ids);
    }

    @Override
    public List<FieldConfig> getAllFieldConfigsByCardTypeId(String cardTypeId) {
        // 通过二级索引查询普通属性配置
        List<FieldConfig> configs = schemaQuery.queryByCardTypeId(cardTypeId, SchemaType.FIELD_CONFIG)
                .stream()
                .filter(FieldConfig.class::isInstance)
                .map(FieldConfig.class::cast)
                .collect(Collectors.toList());

        // 通过 belongTo 查询关联属性配置
        List<LinkFieldConfig> linkConfigs = schemaQuery.queryByBelongTo(cardTypeId, null)
                .stream()
                .filter(LinkFieldConfig.class::isInstance)
                .map(LinkFieldConfig.class::cast)
                .toList();

        configs.addAll(linkConfigs);
        return configs;
    }

    @Override
    public List<LinkTypeDefinition> getLinkTypesByCardTypeId(String cardTypeId) {
        return schemaQuery.queryBySecondKey(CardTypeId.of(cardTypeId), SchemaType.LINK_TYPE)
                .stream()
                .filter(LinkTypeDefinition.class::isInstance)
                .map(LinkTypeDefinition.class::cast)
                .collect(Collectors.toList());
    }
}
