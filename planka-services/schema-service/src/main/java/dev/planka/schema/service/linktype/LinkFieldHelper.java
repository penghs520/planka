package dev.planka.schema.service.linktype;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.schema.repository.SchemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关联字段辅助类
 * <p>
 * 提供基于 LinkFieldId 的便捷操作方法。
 */
@Component
@RequiredArgsConstructor
public class LinkFieldHelper {

    private final SchemaRepository schemaRepository;

    /**
     * 根据关联字段ID获取目标卡片类型ID列表
     * <p>
     * SOURCE 表示当前卡片是源端，返回目标端卡片类型列表
     * TARGET 表示当前卡片是目标端，返回源端卡片类型列表
     *
     * @param linkFieldId 关联字段ID
     * @return 目标卡片类型ID列表，如果关联类型不存在或无限制则返回空列表
     */
    public List<CardTypeId> getTargetCardTypeIds(LinkFieldId linkFieldId) {
        Optional<LinkTypeDefinition> linkTypeOpt = getLinkType(linkFieldId);
        if (linkTypeOpt.isEmpty()) {
            return Collections.emptyList();
        }

        LinkTypeDefinition linkType = linkTypeOpt.get();
        List<CardTypeId> targetCardTypeIds = linkFieldId.getPosition() == LinkPosition.SOURCE
                ? linkType.getTargetCardTypeIds()
                : linkType.getSourceCardTypeIds();

        return targetCardTypeIds != null ? targetCardTypeIds : Collections.emptyList();
    }

    /**
     * 根据关联字段ID获取关联类型定义
     *
     * @param linkFieldId 关联字段ID
     * @return 关联类型定义，如果不存在则返回 empty
     */
    public Optional<LinkTypeDefinition> getLinkType(LinkFieldId linkFieldId) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(linkFieldId.getLinkTypeId());
        if (schemaOpt.isEmpty()) {
            return Optional.empty();
        }

        SchemaDefinition<?> schema = schemaOpt.get();
        if (schema instanceof LinkTypeDefinition linkType) {
            return Optional.of(linkType);
        }

        return Optional.empty();
    }

    /**
     * 检查关联类型是否存在
     *
     * @param linkFieldId 关联字段ID
     * @return 是否存在
     */
    public boolean exists(LinkFieldId linkFieldId) {
        return getLinkType(linkFieldId).isPresent();
    }

    /**
     * 获取关联字段的显示名称
     *
     * @param linkFieldId 关联字段ID
     * @return 显示名称，如果不存在则返回 linkFieldId 的字符串形式
     */
    public String getDisplayName(LinkFieldId linkFieldId) {
        Optional<LinkTypeDefinition> linkTypeOpt = getLinkType(linkFieldId);
        if (linkTypeOpt.isEmpty()) {
            return linkFieldId.value();
        }

        LinkTypeDefinition linkType = linkTypeOpt.get();
        // SOURCE 表示当前在源端，返回 sourceName（源端看向目标端的名称）
        // TARGET 表示当前在目标端，返回 targetName（目标端看向源端的名称）
        return linkFieldId.getPosition() == LinkPosition.SOURCE
                ? linkType.getSourceName()
                : linkType.getTargetName();
    }

    /**
     * 批量获取关联字段的显示名称
     * <p>
     * 将多个 linkFieldId 按 linkTypeId 分组，批量查询后返回名称映射。
     *
     * @param linkFieldIds 关联字段ID集合
     * @return linkFieldId -> displayName 的映射
     */
    public Map<String, String> getDisplayNames(Set<String> linkFieldIds) {
        if (linkFieldIds == null || linkFieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 解析 linkFieldId，收集 linkTypeId
        Map<String, LinkFieldId> parsedIds = new HashMap<>();
        Set<String> linkTypeIds = linkFieldIds.stream()
                .map(idStr -> {
                    try {
                        LinkFieldId linkFieldId = new LinkFieldId(idStr);
                        parsedIds.put(idStr, linkFieldId);
                        return linkFieldId.getLinkTypeId();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (linkTypeIds.isEmpty()) {
            // 解析失败，返回原始 ID 作为名称
            return linkFieldIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> id));
        }

        // 2. 批量查询 LinkTypeDefinition
        List<SchemaDefinition<?>> schemas = schemaRepository.findByIds(linkTypeIds);
        Map<String, LinkTypeDefinition> linkTypeMap = schemas.stream()
                .filter(s -> s instanceof LinkTypeDefinition)
                .map(s -> (LinkTypeDefinition) s)
                .collect(Collectors.toMap(
                        lt -> lt.getId().value(),
                        lt -> lt
                ));

        // 3. 构建结果
        Map<String, String> result = new HashMap<>();
        for (String linkFieldIdStr : linkFieldIds) {
            LinkFieldId linkFieldId = parsedIds.get(linkFieldIdStr);
            if (linkFieldId == null) {
                result.put(linkFieldIdStr, linkFieldIdStr);
                continue;
            }

            LinkTypeDefinition linkType = linkTypeMap.get(linkFieldId.getLinkTypeId());
            if (linkType == null) {
                result.put(linkFieldIdStr, linkFieldIdStr);
                continue;
            }

            // SOURCE 表示当前在源端，返回 sourceName（源端看向目标端的名称）
            // TARGET 表示当前在目标端，返回 targetName（目标端看向源端的名称）
            String displayName = linkFieldId.getPosition() == LinkPosition.SOURCE
                    ? linkType.getSourceName()
                    : linkType.getTargetName();
            result.put(linkFieldIdStr, displayName);
        }

        return result;
    }
}
