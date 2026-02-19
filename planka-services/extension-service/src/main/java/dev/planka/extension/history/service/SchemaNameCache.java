package dev.planka.extension.history.service;

import dev.planka.api.schema.SchemaServiceClient;
import dev.planka.common.result.Result;
import dev.planka.domain.link.LinkFieldIdUtils;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.fieldconfig.EnumFieldConfig;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.infra.cache.schema.SchemaCacheService;
import dev.planka.infra.cache.schema.query.ValueStreamCacheQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Schema 名称缓存服务
 * <p>
 * 提供属性名称、状态名称、枚举选项名称的查询功能。
 * 通过 SchemaCacheService 获取数据（二级缓存）。
 * 对于缓存中查不到的 Schema，调用 SchemaServiceClient.getByIdsWithDeleted 查询已删除的。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaNameCache {

    private final SchemaCacheService schemaCacheService;
    private final ValueStreamCacheQuery valueStreamCacheQuery;
    private final SchemaServiceClient schemaServiceClient;

    /**
     * Schema 名称信息（包含删除状态）
     */
    public record SchemaNameInfo(String name, boolean deleted) {}

    /**
     * 获取单个字段名称
     *
     * @param fieldId 字段ID
     * @return 字段名称，如果获取失败返回 null
     */
    public String getFieldName(String fieldId) {
        if (fieldId == null || fieldId.isEmpty()) {
            return null;
        }

        return schemaCacheService.getById(fieldId)
                .filter(def -> def instanceof FieldConfig)
                .map(def -> ((FieldConfig) def).getName())
                .orElse(null);
    }

    /**
     * 批量获取字段名称（包含已删除的字段）
     * <p>
     * 先查缓存，缓存中查不到的再调用 getByIdsWithDeleted 查询已删除的
     *
     * @param fieldIds 字段ID集合
     * @return fieldId -> SchemaNameInfo 映射（包含名称和删除状态）
     */
    public Map<String, SchemaNameInfo> getFieldNames(Set<String> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, SchemaNameInfo> result = new HashMap<>();
        Set<String> missIds = new HashSet<>();

        // 1. 先查缓存
        Map<String, SchemaDefinition<?>> cachedSchemas = schemaCacheService.getByIds(fieldIds);
        for (String fieldId : fieldIds) {
            SchemaDefinition<?> def = cachedSchemas.get(fieldId);
            if (def instanceof FieldConfig fieldConfig) {
                result.put(fieldId, new SchemaNameInfo(fieldConfig.getName(), false));
            } else {
                missIds.add(fieldId);
            }
        }

        // 2. 缓存中没有的，调用 getByIdsWithDeleted 查询（可能是已删除的）
        if (!missIds.isEmpty()) {
            try {
                Result<List<SchemaDefinition<?>>> feignResult =
                        schemaServiceClient.getByIdsWithDeleted(new ArrayList<>(missIds));
                if (feignResult.isSuccess() && feignResult.getData() != null) {
                    for (SchemaDefinition<?> def : feignResult.getData()) {
                        if (def instanceof FieldConfig fieldConfig) {
                            // 从 getByIdsWithDeleted 查到的都标记为已删除
                            result.put(def.getId().value(), new SchemaNameInfo(fieldConfig.getName(), true));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to query deleted schemas: ids={}", missIds, e);
            }
        }

        return result;
    }

    /**
     * 获取枚举选项名称（包含已删除的枚举属性）
     *
     * @param fieldId   属性ID
     * @param optionIds 枚举选项ID集合
     * @param deleted   属性是否已删除
     * @return optionId -> optionName 映射
     */
    public Map<String, String> getEnumOptionNames(String fieldId, Set<String> optionIds, boolean deleted) {
        if (fieldId == null || optionIds == null || optionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        SchemaDefinition<?> def;
        if (deleted) {
            // 已删除的属性，调用 getByIdsWithDeleted
            try {
                Result<List<SchemaDefinition<?>>> result =
                        schemaServiceClient.getByIdsWithDeleted(List.of(fieldId));
                if (result.isSuccess() && result.getData() != null && !result.getData().isEmpty()) {
                    def = result.getData().get(0);
                } else {
                    return Collections.emptyMap();
                }
            } catch (Exception e) {
                log.error("Failed to query deleted schema: id={}", fieldId, e);
                return Collections.emptyMap();
            }
        } else {
            // 未删除的属性，查缓存
            def = schemaCacheService.getById(fieldId).orElse(null);
        }

        if (!(def instanceof EnumFieldConfig enumConfig)) {
            return Collections.emptyMap();
        }

        Map<String, String> nameMap = new HashMap<>();
        if (enumConfig.getOptions() != null) {
            for (var option : enumConfig.getOptions()) {
                if (optionIds.contains(option.id())) {
                    nameMap.put(option.id(), option.label());
                }
            }
        }
        return nameMap;
    }

    /**
     * 批量获取状态名称
     *
     * @param cardTypeId 卡片类型ID
     * @param statusIds  状态ID集合
     * @return statusId -> statusName 映射
     */
    public Map<String, String> getStatusNames(String cardTypeId, Set<String> statusIds) {
        if (cardTypeId == null || statusIds == null || statusIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // 1. 通过卡片类型的价值流定义
            Optional<ValueStreamDefinition> valueStreamOpt = valueStreamCacheQuery.getValueStreamByCardTypeId(cardTypeId);

            if (valueStreamOpt.isEmpty()) {
                return Collections.emptyMap();
            }

            // 2. 从价值流定义中提取状态名称
            Map<String, String> nameMap = new HashMap<>();
            ValueStreamDefinition valueStream = valueStreamOpt.get();
            if (valueStream.getStepList() != null) {
                for (var step : valueStream.getStepList()) {
                    if (step.getStatusList() != null) {
                        for (var status : step.getStatusList()) {
                            String sid = status.getId().toString();
                            if (statusIds.contains(sid)) {
                                nameMap.put(sid, status.getName());
                            }
                        }
                    }
                }
            }
            return nameMap;
        } catch (Exception e) {
            log.error("Failed to get status names: cardTypeId={}, statusIds={}", cardTypeId, statusIds, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 获取单个关联属性名称
     * <p>
     * linkFieldId 格式为 "{linkTypeId}:{SOURCE|TARGET}"
     * 根据位置返回 sourceName 或 targetName
     *
     * @param linkFieldId 关联属性ID
     * @return 关联属性名称，如果获取失败返回 null
     */
    public String getLinkFieldName(String linkFieldId) {
        if (!LinkFieldIdUtils.isValidFormat(linkFieldId)) {
            return null;
        }

        String linkTypeId = LinkFieldIdUtils.getLinkTypeId(linkFieldId);
        LinkPosition position = LinkFieldIdUtils.getPosition(linkFieldId);

        return schemaCacheService.getById(linkTypeId)
                .filter(def -> def instanceof LinkTypeDefinition)
                .map(def -> {
                    LinkTypeDefinition linkType = (LinkTypeDefinition) def;
                    return position == LinkPosition.SOURCE
                            ? linkType.getSourceName()
                            : linkType.getTargetName();
                })
                .orElse(null);
    }

    /**
     * 批量获取关联属性名称（包含已删除的关联类型）
     * <p>
     * 先查缓存，缓存中查不到的再调用 getByIdsWithDeleted 查询已删除的
     *
     * @param linkFieldIds 关联属性ID集合
     * @return linkFieldId -> SchemaNameInfo 映射（包含名称和删除状态）
     */
    public Map<String, SchemaNameInfo> getLinkFieldNames(Set<String> linkFieldIds) {
        if (linkFieldIds == null || linkFieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 收集需要查询的 linkTypeId
        Map<String, Set<String>> linkTypeIdToFieldIds = new HashMap<>();
        for (String linkFieldId : linkFieldIds) {
            if (LinkFieldIdUtils.isValidFormat(linkFieldId)) {
                String linkTypeId = LinkFieldIdUtils.getLinkTypeId(linkFieldId);
                linkTypeIdToFieldIds.computeIfAbsent(linkTypeId, k -> new HashSet<>())
                        .add(linkFieldId);
            }
        }

        if (linkTypeIdToFieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, SchemaNameInfo> result = new HashMap<>();
        Set<String> missLinkTypeIds = new HashSet<>();

        // 1. 先查缓存
        Map<String, SchemaDefinition<?>> cachedSchemas = schemaCacheService.getByIds(linkTypeIdToFieldIds.keySet());
        for (Map.Entry<String, Set<String>> entry : linkTypeIdToFieldIds.entrySet()) {
            String linkTypeId = entry.getKey();
            SchemaDefinition<?> def = cachedSchemas.get(linkTypeId);
            if (def instanceof LinkTypeDefinition linkType) {
                for (String linkFieldId : entry.getValue()) {
                    LinkPosition position = LinkFieldIdUtils.getPosition(linkFieldId);
                    String name = position == LinkPosition.SOURCE
                            ? linkType.getSourceName()
                            : linkType.getTargetName();
                    result.put(linkFieldId, new SchemaNameInfo(name, false));
                }
            } else {
                missLinkTypeIds.add(linkTypeId);
            }
        }

        // 2. 缓存中没有的，调用 getByIdsWithDeleted 查询（可能是已删除的）
        if (!missLinkTypeIds.isEmpty()) {
            try {
                Result<List<SchemaDefinition<?>>> feignResult =
                        schemaServiceClient.getByIdsWithDeleted(new ArrayList<>(missLinkTypeIds));
                if (feignResult.isSuccess() && feignResult.getData() != null) {
                    for (SchemaDefinition<?> def : feignResult.getData()) {
                        if (def instanceof LinkTypeDefinition linkType) {
                            String linkTypeId = def.getId().value();
                            Set<String> fieldIds = linkTypeIdToFieldIds.get(linkTypeId);
                            if (fieldIds != null) {
                                for (String linkFieldId : fieldIds) {
                                    LinkPosition position = LinkFieldIdUtils.getPosition(linkFieldId);
                                    String name = position == LinkPosition.SOURCE
                                            ? linkType.getSourceName()
                                            : linkType.getTargetName();
                                    result.put(linkFieldId, new SchemaNameInfo(name, true));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to query deleted link types: ids={}", missLinkTypeIds, e);
            }
        }

        return result;
    }
}
