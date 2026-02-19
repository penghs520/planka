package dev.planka.card.service;

import dev.planka.api.card.request.Yield;
import dev.planka.api.card.request.YieldField;
import dev.planka.api.card.request.YieldLink;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.fieldconfig.StructureFieldConfig;
import dev.planka.domain.schema.definition.structure.StructureLevelBinding;
import dev.planka.infra.cache.schema.SchemaCacheService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Yield 增强器
 * <p>
 * 分析 Yield 中的架构属性需求，自动补充获取架构属性值所需的 YieldLink 链。
 * 递归处理所有层级的 YieldField，确保嵌套查询也能正确获取架构属性。
 */
@Component
public class YieldEnhancer {

    private final SchemaCacheService schemaCacheService;

    public YieldEnhancer(SchemaCacheService schemaCacheService) {
        this.schemaCacheService = schemaCacheService;
    }

    /**
     * 增强 Yield，自动补充架构属性需要的关联查询
     * <p>
     * 递归处理所有层级的 Yield，包括：
     * - 顶层 Yield.field（可能为 null）
     * - 嵌套的 Yield.links[].targetYield.field
     *
     * @param yield 原始 Yield
     * @return 增强后的 Yield（包含架构属性需要的 YieldLink）
     */
    public Yield enhance(Yield yield) {
        if (yield == null) {
            return null;
        }

        // 1. 收集当前层级需要补充的 YieldLink
        List<YieldLink> additionalLinks = collectStructureLinks(yield.getField());

        // 2. 递归处理嵌套的 YieldLink
        List<YieldLink> enhancedLinks = new ArrayList<>();
        if (yield.getLinks() != null) {
            for (YieldLink link : yield.getLinks()) {
                enhancedLinks.add(enhanceYieldLink(link));
            }
        }

        // 3. 合并新增的 YieldLink（去重）
        enhancedLinks = mergeLinks(enhancedLinks, additionalLinks);

        // 4. 构建增强后的 Yield
        Yield enhanced = new Yield();
        enhanced.setField(yield.getField());
        enhanced.setLinks(enhancedLinks.isEmpty() ? null : enhancedLinks);
        return enhanced;
    }

    /**
     * 递归增强 YieldLink
     */
    private YieldLink enhanceYieldLink(YieldLink link) {
        if (link == null) {
            return null;
        }

        YieldLink enhanced = new YieldLink();
        enhanced.setLinkFieldId(link.getLinkFieldId());

        // 递归增强 targetYield
        if (link.getTargetYield() != null) {
            enhanced.setTargetYield(enhance(link.getTargetYield()));
        }

        return enhanced;
    }

    /**
     * 从 YieldField 中收集架构属性需要的 YieldLink
     * <p>
     * 只在 YieldField.fieldIds 显式包含架构属性时才生成 YieldLink，
     * allFields=true 时不自动触发架构属性查询。
     * <p>
     * 注意：架构属性中的所有关联属性都是当前卡片直接关联的，
     * 因此直接平铺返回 YieldLink 列表，不需要构造嵌套的链式结构。
     *
     * @param field YieldField
     * @return 需要补充的 YieldLink 列表
     */
    private List<YieldLink> collectStructureLinks(YieldField field) {
        if (field == null || field.isAllFields() || field.getFieldIds() == null) {
            return List.of();
        }

        // 批量获取 Schema，筛选出 StructureFieldConfig
        Set<String> fieldIds = new HashSet<>(field.getFieldIds());
        Map<String, SchemaDefinition<?>> schemas = schemaCacheService.getByIds(fieldIds);

        List<StructureFieldConfig> structureFields = schemas.values().stream()
                .filter(s -> s instanceof StructureFieldConfig)
                .map(s -> (StructureFieldConfig) s)
                .toList();

        // 为每个架构属性的所有层级绑定生成平铺的 YieldLink 列表
        List<YieldLink> links = new ArrayList<>();
        for (StructureFieldConfig def : structureFields) {
            List<StructureLevelBinding> bindings = def.getLevelBindings();
            if (bindings != null) {
                for (StructureLevelBinding binding : bindings) {
                    YieldLink link = new YieldLink();
                    link.setLinkFieldId(binding.linkFieldId().value());
                    links.add(link);
                }
            }
        }
        return links;
    }

    /**
     * 合并 YieldLink 列表（去重）
     * <p>
     * 根据 linkFieldId 去重，避免重复查询相同的关联属性。
     *
     * @param existing   现有的 YieldLink 列表
     * @param additional 需要添加的 YieldLink 列表
     * @return 合并后的 YieldLink 列表
     */
    private List<YieldLink> mergeLinks(List<YieldLink> existing, List<YieldLink> additional) {
        if (additional.isEmpty()) {
            return existing;
        }

        Set<String> existingIds = new HashSet<>();
        for (YieldLink link : existing) {
            if (link.getLinkFieldId() != null) {
                existingIds.add(link.getLinkFieldId());
            }
        }

        List<YieldLink> result = new ArrayList<>(existing);
        for (YieldLink link : additional) {
            if (link.getLinkFieldId() != null && !existingIds.contains(link.getLinkFieldId())) {
                result.add(link);
                existingIds.add(link.getLinkFieldId());
            }
        }

        return result;
    }

    /**
     * 从 Yield 中递归提取所有架构属性定义
     * <p>
     * 用于 CardService 在查询后构造 StructureFieldValue。
     *
     * @param yield Yield 对象
     * @return 架构属性定义列表
     */
    public List<StructureFieldConfig> extractStructureFieldDefs(Yield yield) {
        Set<String> allFieldIds = new HashSet<>();
        collectFieldIds(yield, allFieldIds);

        if (allFieldIds.isEmpty()) {
            return List.of();
        }

        Map<String, SchemaDefinition<?>> schemas = schemaCacheService.getByIds(allFieldIds);

        return schemas.values().stream()
                .filter(s -> s instanceof StructureFieldConfig)
                .map(s -> (StructureFieldConfig) s)
                .toList();
    }

    /**
     * 递归收集所有层级的 fieldIds
     *
     * @param yield  Yield 对象
     * @param result 收集结果
     */
    private void collectFieldIds(Yield yield, Set<String> result) {
        if (yield == null) {
            return;
        }

        // 收集当前层级的 fieldIds
        if (yield.getField() != null
                && !yield.getField().isAllFields()
                && yield.getField().getFieldIds() != null) {
            result.addAll(yield.getField().getFieldIds());
        }

        // 递归收集嵌套层级的 fieldIds
        if (yield.getLinks() != null) {
            for (YieldLink link : yield.getLinks()) {
                if (link.getTargetYield() != null) {
                    collectFieldIds(link.getTargetYield(), result);
                }
            }
        }
    }
}
