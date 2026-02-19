package dev.planka.schema.service.common.diff;

import dev.planka.domain.schema.changelog.FieldChange;
import dev.planka.domain.schema.changelog.FieldChangeType;
import dev.planka.domain.schema.changelog.SemanticChange;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * JSON差异比较辅助工具
 * <p>
 * 提供通用的JSON节点比较方法，支持标量值、列表等类型的比较。
 */
@Component
public class JsonDiffHelper {

    /**
     * 比较简单标量值
     *
     * @param fieldPath  字段路径
     * @param fieldLabel 字段显示名
     * @param before     变更前的值节点
     * @param after      变更后的值节点
     * @param valueType  值类型标识
     * @return 字段变更（如果有变化），否则返回空
     */
    public Optional<FieldChange> compareScalar(String fieldPath, String fieldLabel,
                                                JsonNode before, JsonNode after, String valueType) {
        Object oldValue = extractValue(before);
        Object newValue = extractValue(after);

        if (Objects.equals(oldValue, newValue)) {
            return Optional.empty();
        }

        FieldChange change = new FieldChange();
        change.setFieldPath(fieldPath);
        change.setFieldLabel(fieldLabel);
        change.setOldValue(oldValue);
        change.setNewValue(newValue);
        change.setValueType(valueType);

        if (oldValue == null) {
            change.setChangeType(FieldChangeType.ADDED);
        } else if (newValue == null) {
            change.setChangeType(FieldChangeType.REMOVED);
        } else {
            change.setChangeType(FieldChangeType.MODIFIED);
        }

        return Optional.of(change);
    }

    /**
     * 比较布尔值
     */
    public Optional<FieldChange> compareBoolean(String fieldPath, String fieldLabel,
                                                 JsonNode before, JsonNode after) {
        Boolean oldValue = before != null && !before.isNull() ? before.asBoolean() : null;
        Boolean newValue = after != null && !after.isNull() ? after.asBoolean() : null;

        if (Objects.equals(oldValue, newValue)) {
            return Optional.empty();
        }

        FieldChange change = new FieldChange();
        change.setFieldPath(fieldPath);
        change.setFieldLabel(fieldLabel);
        change.setOldValue(formatBoolean(oldValue));
        change.setNewValue(formatBoolean(newValue));
        change.setValueType("boolean");

        if (oldValue == null) {
            change.setChangeType(FieldChangeType.ADDED);
        } else if (newValue == null) {
            change.setChangeType(FieldChangeType.REMOVED);
        } else {
            change.setChangeType(FieldChangeType.MODIFIED);
        }

        return Optional.of(change);
    }

    /**
     * 比较列表（基于ID匹配）
     *
     * @param before    变更前的数组节点
     * @param after     变更后的数组节点
     * @param idField   用于匹配的ID字段名
     * @param nameField 用于显示的名称字段名
     * @param category  语义变更类别
     * @return 语义变更列表
     */
    public List<SemanticChange> compareList(JsonNode before, JsonNode after,
                                            String idField, String nameField, String category) {
        return compareListWithDetails(before, after, idField, nameField, category, null);
    }

    /**
     * 比较列表（基于ID匹配），支持详细字段比较
     *
     * @param before       变更前的数组节点
     * @param after        变更后的数组节点
     * @param idField      用于匹配的ID字段名
     * @param nameField    用于显示的名称字段名
     * @param category     语义变更类别
     * @param fieldConfigs 需要比较的字段配置（字段名 -> 显示标签），为null时不比较详细字段
     * @return 语义变更列表
     */
    public List<SemanticChange> compareListWithDetails(JsonNode before, JsonNode after,
                                                        String idField, String nameField, String category,
                                                        Map<String, String> fieldConfigs) {
        Map<String, JsonNode> beforeMap = toMap(before, idField);
        Map<String, JsonNode> afterMap = toMap(after, idField);
        List<SemanticChange> changes = new ArrayList<>();

        // 新增项
        for (var entry : afterMap.entrySet()) {
            if (!beforeMap.containsKey(entry.getKey())) {
                String name = entry.getValue().path(nameField).asText(entry.getKey());
                changes.add(SemanticChange.added(category, entry.getKey(), name));
            }
        }

        // 删除项
        for (var entry : beforeMap.entrySet()) {
            if (!afterMap.containsKey(entry.getKey())) {
                String name = entry.getValue().path(nameField).asText(entry.getKey());
                changes.add(SemanticChange.removed(category, entry.getKey(), name));
            }
        }

        // 修改项（检测内容变化）
        for (var entry : afterMap.entrySet()) {
            String id = entry.getKey();
            if (beforeMap.containsKey(id)) {
                JsonNode beforeItem = beforeMap.get(id);
                JsonNode afterItem = entry.getValue();
                if (!beforeItem.equals(afterItem)) {
                    String name = afterItem.path(nameField).asText(id);
                    List<FieldChange> details = null;
                    if (fieldConfigs != null && !fieldConfigs.isEmpty()) {
                        details = compareObjectFields(beforeItem, afterItem, fieldConfigs);
                    }
                    changes.add(SemanticChange.modified(category, id, name, details));
                }
            }
        }

        return changes;
    }

    /**
     * 比较两个对象的指定字段
     *
     * @param before       变更前的对象节点
     * @param after        变更后的对象节点
     * @param fieldConfigs 需要比较的字段配置（字段名 -> 显示标签）
     * @return 字段变更列表
     */
    public List<FieldChange> compareObjectFields(JsonNode before, JsonNode after,
                                                  Map<String, String> fieldConfigs) {
        List<FieldChange> changes = new ArrayList<>();
        for (var entry : fieldConfigs.entrySet()) {
            String fieldName = entry.getKey();
            String fieldLabel = entry.getValue();
            compareScalar(fieldName, fieldLabel,
                    before.path(fieldName), after.path(fieldName), "string")
                    .ifPresent(changes::add);
        }
        return changes;
    }

    /**
     * 比较ID对象列表（如CardTypeId列表，包含value字段的对象）
     *
     * @param before   变更前的数组节点
     * @param after    变更后的数组节点
     * @param category 语义变更类别
     * @return 语义变更列表
     */
    public List<SemanticChange> compareIdObjectList(JsonNode before, JsonNode after, String category) {
        Set<String> beforeSet = toIdObjectSet(before);
        Set<String> afterSet = toIdObjectSet(after);
        List<SemanticChange> changes = new ArrayList<>();

        // 新增项
        for (String value : afterSet) {
            if (!beforeSet.contains(value)) {
                changes.add(SemanticChange.added(category, value, value));
            }
        }

        // 删除项
        for (String value : beforeSet) {
            if (!afterSet.contains(value)) {
                changes.add(SemanticChange.removed(category, value, value));
            }
        }

        return changes;
    }

    /**
     * 将包含value字段的ID对象数组转换为字符串Set
     */
    private Set<String> toIdObjectSet(JsonNode arrayNode) {
        Set<String> set = new LinkedHashSet<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                if (item.isObject()) {
                    String value = item.path("value").asText(null);
                    if (value != null) {
                        set.add(value);
                    }
                } else if (item.isTextual()) {
                    set.add(item.asText());
                }
            }
        }
        return set;
    }

    /**
     * 比较简单字符串列表
     *
     * @param before   变更前的数组节点
     * @param after    变更后的数组节点
     * @param category 语义变更类别
     * @return 语义变更列表
     */
    public List<SemanticChange> compareStringList(JsonNode before, JsonNode after, String category) {
        Set<String> beforeSet = toStringSet(before);
        Set<String> afterSet = toStringSet(after);
        List<SemanticChange> changes = new ArrayList<>();

        // 新增项
        for (String value : afterSet) {
            if (!beforeSet.contains(value)) {
                changes.add(SemanticChange.added(category, value, value));
            }
        }

        // 删除项
        for (String value : beforeSet) {
            if (!afterSet.contains(value)) {
                changes.add(SemanticChange.removed(category, value, value));
            }
        }

        return changes;
    }

    /**
     * 从JsonNode提取值
     */
    public Object extractValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(item -> list.add(extractValue(item)));
            return list;
        }
        // 对于复杂对象，返回字符串表示
        return node.toString();
    }

    /**
     * 将数组节点转换为Map（按指定字段作为key）
     */
    private Map<String, JsonNode> toMap(JsonNode arrayNode, String keyField) {
        Map<String, JsonNode> map = new LinkedHashMap<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String key = item.path(keyField).asText(null);
                if (key != null) {
                    map.put(key, item);
                }
            }
        }
        return map;
    }

    /**
     * 将数组节点转换为字符串Set
     */
    private Set<String> toStringSet(JsonNode arrayNode) {
        Set<String> set = new LinkedHashSet<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                if (item.isTextual()) {
                    set.add(item.asText());
                }
            }
        }
        return set;
    }

    /**
     * 格式化布尔值为中文
     */
    private String formatBoolean(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? "是" : "否";
    }
}
