package dev.planka.schema.service.common.reference;

import dev.planka.domain.schema.ReferenceType;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.schema.repository.SchemaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Schema 引用分析器
 * <p>
 * 通过反射分析 SchemaDefinition 中的引用关系。
 * 采用两级策略：
 * 1. SchemaId 接口识别（优先）：所有实现 SchemaId 接口的类型（CardTypeId、StatusId 等）
 * 直接调用 schemaId.value() 和 schemaId.schemaType() 获取信息，无需映射表
 * 2. String 类型兜底：筛选雪花 ID 格式，查询数据库确认
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaReferenceAnalyzer {

    private final SchemaRepository schemaRepository;

    /**
     * 雪花 ID 格式：19位数字
     */
    private static final Pattern SNOWFLAKE_ID_PATTERN = Pattern.compile("^\\d{18,19}$");

    /**
     * 需要排除的元数据字段
     */
    private static final Set<String> EXCLUDED_FIELDS = Set.of(
            "id", "orgId", "state", "contentVersion", "structureVersion",
            "createdAt", "createdBy", "updatedAt", "updatedBy", "deletedAt"
    );

    /**
     * 引用信息
     */
    public record ReferenceInfo(
            String targetId,
            SchemaType targetType,
            ReferenceType referenceType  // COMPOSITION 或 AGGREGATION
    ) {
    }

    /**
     * 待查询的 String ID 信息
     */
    private record PendingStringRef(String id, String fieldName) {
    }

    /**
     * 分析 SchemaDefinition 中的所有引用
     *
     * @param definition 要分析的 Schema 定义
     * @return 引用列表（已去重）
     */
    public List<ReferenceInfo> analyze(SchemaDefinition<?> definition) {
        if (definition == null) {
            return Collections.emptyList();
        }

        // 获取 belongTo 的值，用于判断组合引用
        String belongToId = definition.belongTo() != null ? definition.belongTo().value() : null;

        // 第一阶段：收集 SchemaId 引用和待查询的 String ID
        Map<String, ReferenceInfo> refMap = new HashMap<>();
        List<PendingStringRef> pendingStringRefs = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        analyzeObject(definition, "", refMap, pendingStringRefs, visited, belongToId);

        // 第二阶段：批量查询 String ID 对应的 SchemaType
        if (!pendingStringRefs.isEmpty()) {
            resolvePendingStringRefs(pendingStringRefs, refMap, belongToId);
        }

        // 移除掉自身，例如FieldConfig中id和fieldId可能相等的情况
        refMap.remove(definition.getId().value());
        return new ArrayList<>(refMap.values());
    }

    /**
     * 批量解析待查询的 String ID
     */
    private void resolvePendingStringRefs(List<PendingStringRef> pendingRefs, Map<String, ReferenceInfo> refMap,
                                          String belongToId) {
        // 收集需要查询的 ID（排除已在 refMap 中的）
        Set<String> idsToQuery = pendingRefs.stream()
                .map(PendingStringRef::id)
                .filter(id -> !refMap.containsKey(id))
                .collect(java.util.stream.Collectors.toSet());

        if (idsToQuery.isEmpty()) {
            return;
        }

        // 批量查询
        Map<String, SchemaType> typeMap = batchLookupSchemaTypes(idsToQuery);

        // 填充引用信息
        for (PendingStringRef pending : pendingRefs) {
            if (refMap.containsKey(pending.id)) {
                continue;
            }
            SchemaType targetType = typeMap.get(pending.id);
            if (targetType != null) {
                // 如果该 ID 与 belongTo 相同，则为组合引用
                ReferenceType refType = pending.id.equals(belongToId)
                        ? ReferenceType.COMPOSITION : ReferenceType.AGGREGATION;
                refMap.put(pending.id, new ReferenceInfo(pending.id, targetType, refType));
                log.debug("发现 String 引用: {} -> {} ({})", pending.fieldName, pending.id, targetType);
            }
        }
    }

    /**
     * 递归分析对象中的引用
     */
    private void analyzeObject(Object obj, String path, Map<String, ReferenceInfo> refMap,
                               List<PendingStringRef> pendingStringRefs, Set<Object> visited,
                               String belongToId) {
        if (obj == null) {
            return;
        }

        // 防止循环引用导致的无限递归
        if (!visited.add(obj)) {
            return;
        }

        Class<?> clazz = obj.getClass();

        // 遍历所有字段（包括父类）
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (EXCLUDED_FIELDS.contains(field.getName())) {
                    continue;
                }

                field.setAccessible(true);
                try {
                    Object value = field.get(obj);
                    if (value == null) {
                        continue;
                    }

                    String fieldPath = path.isEmpty() ? field.getName() : path + "." + field.getName();
                    processFieldValue(field, value, fieldPath, refMap, pendingStringRefs, visited, belongToId);

                } catch (IllegalAccessException e) {
                    log.warn("无法访问字段: {}", field.getName(), e);
                }
            }
        }
    }

    /**
     * 处理字段值
     */
    private void processFieldValue(Field field, Object value, String fieldPath,
                                   Map<String, ReferenceInfo> refMap,
                                   List<PendingStringRef> pendingStringRefs,
                                   Set<Object> visited, String belongToId) {
        Class<?> fieldType = field.getType();

        // 第一级：强类型 ID（实现了 SchemaId 接口的类型）
        if (SchemaId.class.isAssignableFrom(fieldType)) {
            handleSchemaId((SchemaId) value, refMap, belongToId);
            return;
        }

        // 第二级：String 类型兜底（收集待查询的 ID）
        if (fieldType == String.class) {
            collectStringRef((String) value, field.getName(), refMap, pendingStringRefs);
            return;
        }

        // Collection 处理
        if (Collection.class.isAssignableFrom(fieldType)) {
            handleCollection((Collection<?>) value, field.getName(), fieldPath, refMap, pendingStringRefs, visited, belongToId);
            return;
        }

        // Map 处理
        if (Map.class.isAssignableFrom(fieldType)) {
            handleMap((Map<?, ?>) value, fieldPath, refMap, pendingStringRefs, visited, belongToId);
            return;
        }

        // 递归处理复杂对象（非基本类型、非 JDK 类型）
        if (shouldRecurse(fieldType)) {
            analyzeObject(value, fieldPath, refMap, pendingStringRefs, visited, belongToId);
        }
    }

    /**
     * 处理 SchemaId 类型字段
     * <p>
     * 利用 SchemaId 接口直接获取 value() 和 schemaType()，无需映射表
     */
    private void handleSchemaId(SchemaId schemaId, Map<String, ReferenceInfo> refMap, String belongToId) {
        if (schemaId == null) {
            return;
        }

        String idValue = schemaId.value();
        SchemaType targetType = schemaId.schemaType();

        if (idValue != null && !refMap.containsKey(idValue)) {
            // 如果该 ID 与 belongTo 相同，则为组合引用
            ReferenceType refType = idValue.equals(belongToId) ? ReferenceType.COMPOSITION : ReferenceType.AGGREGATION;
            refMap.put(idValue, new ReferenceInfo(idValue, targetType, refType));
            log.debug("发现 SchemaId 引用: {} -> {} ({}, {})", idValue, schemaId.value(), targetType, refType);
        }
    }

    /**
     * 收集 String 类型的 ID（延迟批量查询）
     */
    private void collectStringRef(String value, String fieldName, Map<String, ReferenceInfo> refMap,
                                  List<PendingStringRef> pendingStringRefs) {
        if (!isSnowflakeId(value) || refMap.containsKey(value)) {
            return;
        }
        // 收集待查询的 ID，稍后批量查询
        pendingStringRefs.add(new PendingStringRef(value, fieldName));
    }

    /**
     * 处理 Collection 类型
     */
    private void handleCollection(Collection<?> collection, String fieldName, String fieldPath,
                                  Map<String, ReferenceInfo> refMap,
                                  List<PendingStringRef> pendingStringRefs,
                                  Set<Object> visited, String belongToId) {
        int index = 0;
        for (Object item : collection) {
            if (item == null) {
                continue;
            }

            String itemPath = fieldPath + "[" + index + "]";

            // SchemaId 集合
            if (item instanceof SchemaId schemaId) {
                handleSchemaId(schemaId, refMap, belongToId);
            }
            // String 集合
            else if (item instanceof String strValue) {
                collectStringRef(strValue, fieldName, refMap, pendingStringRefs);
            }
            // 复杂对象集合
            else if (shouldRecurse(item.getClass())) {
                analyzeObject(item, itemPath, refMap, pendingStringRefs, visited, belongToId);
            }

            index++;
        }
    }

    /**
     * 处理 Map 类型
     */
    private void handleMap(Map<?, ?> map, String fieldPath, Map<String, ReferenceInfo> refMap,
                           List<PendingStringRef> pendingStringRefs, Set<Object> visited,
                           String belongToId) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            String key = String.valueOf(entry.getKey());
            String itemPath = fieldPath + "[" + key + "]";

            if (value instanceof String strValue) {
                collectStringRef(strValue, key, refMap, pendingStringRefs);
            } else if (shouldRecurse(value.getClass())) {
                analyzeObject(value, itemPath, refMap, pendingStringRefs, visited, belongToId);
            }
        }
    }

    /**
     * 判断是否为雪花 ID 格式
     */
    private boolean isSnowflakeId(String value) {
        return value != null && SNOWFLAKE_ID_PATTERN.matcher(value).matches();
    }

    /**
     * 批量查询数据库确定 Schema 类型
     *
     * @param schemaIds 要查询的 Schema ID 集合
     * @return ID 到 SchemaType 的映射
     */
    private Map<String, SchemaType> batchLookupSchemaTypes(Set<String> schemaIds) {
        if (schemaIds == null || schemaIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SchemaDefinition<?>> schemas = schemaRepository.findByIds(schemaIds);
        Map<String, SchemaType> result = new HashMap<>();
        for (SchemaDefinition<?> schema : schemas) {
            result.put(schema.getId().value(), schema.getSchemaType());
        }
        return result;
    }

    /**
     * 判断是否应该递归分析
     */
    private boolean shouldRecurse(Class<?> clazz) {
        // 排除基本类型
        if (clazz.isPrimitive()) {
            return false;
        }
        // 排除枚举类型（访问 java.lang.Enum 字段会触发模块访问限制）
        if (clazz.isEnum()) {
            return false;
        }
        // 排除数组类型
        if (clazz.isArray()) {
            return false;
        }
        // 只递归分析项目内的类型
        String packageName = clazz.getPackageName();
        return packageName.startsWith("cn.agilean");
    }
}
