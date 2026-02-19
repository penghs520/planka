package dev.planka.api.schema.spi;

import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Schema 数据查询提供者
 * <p>
 * 定义 FieldConfigQueryService 所需的数据查询能力。
 * schema-service 和 schema-cache 分别实现此接口。
 */
public interface SchemaDataProvider {

    /**
     * 根据ID获取卡片类型定义
     *
     * @param cardTypeId 卡片类型ID
     * @return 卡片类型定义，不存在时返回 Optional.empty()
     */
    Optional<CardTypeDefinition> getCardTypeById(String cardTypeId);

    /**
     * 批量获取 Schema 定义（用于构建名称缓存）
     *
     * @param ids Schema ID 集合
     * @return Schema 定义列表
     */
    List<SchemaDefinition<?>> getSchemasByIds(Set<String> ids);

    /**
     * 按卡片类型ID查询所有属性配置（包括关联配置）
     *
     * @param cardTypeId 卡片类型ID
     * @return 所有属性配置列表（包含 FieldConfig 及其子类 LinkFieldConfig）
     */
    List<FieldConfig> getAllFieldConfigsByCardTypeId(String cardTypeId);

    /**
     * 按卡片类型ID查询关联类型定义
     *
     * @param cardTypeId 卡片类型ID
     * @return 关联类型定义列表
     */
    List<LinkTypeDefinition> getLinkTypesByCardTypeId(String cardTypeId);
}
