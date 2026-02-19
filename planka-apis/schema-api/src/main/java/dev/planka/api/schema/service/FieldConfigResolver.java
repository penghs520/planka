package dev.planka.api.schema.service;

import dev.planka.api.schema.spi.SchemaDataProvider;
import dev.planka.common.util.SystemSchemaIds;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;

import java.util.*;

/**
 * 属性配置解析器
 * <p>
 * 负责解析卡片类型的完整属性配置（包括普通属性和关联属性），处理继承链遍历和配置合并。
 * <p>
 * 继承优先级（从高到低）：自身 > 显式父类 > 任意卡属性集
 * <p>
 * 使用 LinkedHashMap + put() 实现覆盖语义：
 * 1. 先添加任意卡属性集配置（最低优先级）
 * 2. 按声明顺序添加显式父类配置（后声明覆盖先声明）
 * 3. 最后添加自身配置（最高优先级）
 */
public class FieldConfigResolver {

    private final SchemaDataProvider dataProvider;

    public FieldConfigResolver(SchemaDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    /**
     * 解析卡片类型的完整属性配置（包括普通属性和关联属性）
     *
     * @param cardType 卡片类型
     * @return 解析结果，包含所有配置
     */
    public ResolvedFieldConfigs resolve(CardTypeDefinition cardType) {
        Objects.requireNonNull(cardType, "cardType 不能为空");

        ResolveContext ctx = new ResolveContext();
        List<String> inheritanceChain = buildInheritanceChain(cardType);

        for (String typeId : inheritanceChain) {
            addConfigsFromCardType(typeId, ctx);
        }

        return new ResolvedFieldConfigs(
                new ArrayList<>(ctx.configResult.values()),
                ctx.configResult,
                ctx.configSources,
                ctx.fromLinkTypeDef
        );
    }

    /**
     * 构建继承链（按优先级从低到高排列）
     */
    private List<String> buildInheritanceChain(CardTypeDefinition cardType) {
        String cardTypeId = cardType.getId().value();
        String rootCardTypeId = SystemSchemaIds.anyTraitTypeId(cardType.getOrgId());
        boolean isRootCardType = cardTypeId.equals(rootCardTypeId);

        List<String> chain = new ArrayList<>();

        // 1. 任意卡属性集（最低优先级，排除自身）
        if (!isRootCardType) {
            chain.add(rootCardTypeId);
        }

        // 2. 显式父类（按声明顺序）
        if (cardType instanceof EntityCardType entityCardType && entityCardType.getParentTypeIds() != null) {
            entityCardType.getParentTypeIds().stream()
                    .map(CardTypeId::value)
                    .forEach(chain::add);
        }

        // 3. 自身（最高优先级）
        chain.add(cardTypeId);

        return chain;
    }

    /** 解析过程中的上下文 */
    private static class ResolveContext {
        final Map<String, FieldConfig> configResult = new LinkedHashMap<>();
        final Map<String, String> configSources = new HashMap<>();
        final Set<String> fromLinkTypeDef = new HashSet<>();
        final Set<String> savedLinkConfigKeys = new HashSet<>();
    }


    // ==================== 私有方法 ====================

    /**
     * 从指定卡片类型添加所有配置（包括已保存配置、关联默认配置）
     */
    private void addConfigsFromCardType(String cardTypeId, ResolveContext ctx) {
        // 1. 添加已保存的属性配置
        for (FieldConfig config : dataProvider.getAllFieldConfigsByCardTypeId(cardTypeId)) {
            String key = config.getFieldId().value();
            ctx.configResult.put(key, config);
            ctx.configSources.put(key, cardTypeId);

            if (config instanceof LinkFieldConfig) {
                ctx.savedLinkConfigKeys.add(key);
                ctx.fromLinkTypeDef.remove(key);
            }
        }

        // 2. 从关联类型定义生成默认配置
        addDefaultLinkConfigs(cardTypeId, ctx);
    }

    private void addDefaultLinkConfigs(String cardTypeId, ResolveContext ctx) {
        for (LinkTypeDefinition linkType : dataProvider.getLinkTypesByCardTypeId(cardTypeId)) {
            if (containsCardTypeId(linkType.getSourceCardTypeIds(), cardTypeId)) {
                addDefaultLinkConfigIfAbsent(cardTypeId, linkType, LinkPosition.SOURCE, ctx);
            }
            if (containsCardTypeId(linkType.getTargetCardTypeIds(), cardTypeId)) {
                addDefaultLinkConfigIfAbsent(cardTypeId, linkType, LinkPosition.TARGET, ctx);
            }
        }
    }

    private void addDefaultLinkConfigIfAbsent(String cardTypeId, LinkTypeDefinition linkType, LinkPosition position, ResolveContext ctx) {
        String key = linkType.getId().value() + ":" + position.name();
        if (ctx.savedLinkConfigKeys.contains(key) || ctx.configResult.containsKey(key)) {
            return;
        }
        LinkFieldConfig config = FieldConfigFactory.createFromLinkType(linkType, CardTypeId.of(cardTypeId), position);
        ctx.configResult.put(key, config);
        ctx.configSources.put(key, cardTypeId);
        ctx.fromLinkTypeDef.add(key);
    }

    private boolean containsCardTypeId(List<CardTypeId> cardTypeIds, String cardTypeId) {
        if (cardTypeIds == null || cardTypeIds.isEmpty()) {
            return false;
        }
        return cardTypeIds.stream().anyMatch(id -> id.value().equals(cardTypeId));
    }

    // ==================== 内部结果类 ====================

    /**
     * 属性配置解析结果（包括普通属性和关联属性）
     *
     * @param fieldConfigs 所有属性配置（包括普通属性和关联属性）
     *                     - 普通属性: key = fieldId
     *                     - 关联属性: key = linkTypeId:position
     * @param configSources 配置来源（cardTypeId）
     * @param fromLinkTypeDefinition 标记哪些关联属性是来自关联类型定义（未持久化的默认配置）
     */
    public record ResolvedFieldConfigs(
            List<FieldConfig> allFieldConfigs,
            Map<String, FieldConfig> fieldConfigs,
            Map<String, String> configSources,
            Set<String> fromLinkTypeDefinition
    ) {}
}
