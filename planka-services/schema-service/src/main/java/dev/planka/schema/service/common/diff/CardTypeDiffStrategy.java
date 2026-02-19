package dev.planka.schema.service.common.diff;

import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.changelog.ChangeDetail;
import dev.planka.domain.schema.changelog.FieldChange;
import dev.planka.domain.schema.changelog.FieldChangeType;
import dev.planka.domain.schema.changelog.SemanticChange;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 卡片类型差异比较策略
 * <p>
 * 支持属性集和实体类型的差异比较，包括深层级字段：
 * <ul>
 *     <li>parentTypeIds - 继承的属性集</li>
 *     <li>quickCreateLinkConfigs - 快速创建配置</li>
 *     <li>permissionConfig - 权限配置</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CardTypeDiffStrategy implements SchemaDiffStrategy {

    private final JsonDiffHelper diffHelper;

    /**
     * 支持的Schema子类型
     */
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            SchemaSubType.TRAIT_CARD_TYPE,
            SchemaSubType.ENTITY_CARD_TYPE
    );

    /**
     * 字符串字段映射
     */
    private static final Map<String, String> STRING_FIELDS = new LinkedHashMap<>();

    /**
     * 布尔字段映射
     */
    private static final Map<String, String> BOOLEAN_FIELDS = new LinkedHashMap<>();

    /**
     * 快速创建配置的详细字段
     */
    private static final Map<String, String> QUICK_CREATE_FIELDS = new LinkedHashMap<>();

    static {
        STRING_FIELDS.put("name", "名称");
        STRING_FIELDS.put("description", "描述");
        STRING_FIELDS.put("code", "编码");
        STRING_FIELDS.put("icon", "图标");
        STRING_FIELDS.put("color", "颜色");
        STRING_FIELDS.put("valueStreamId", "价值流");
        STRING_FIELDS.put("defaultDetailTemplateId", "默认详情模板");
        STRING_FIELDS.put("defaultCardFaceId", "默认卡面");

        BOOLEAN_FIELDS.put("enabled", "启用状态");
        BOOLEAN_FIELDS.put("systemType", "系统内置");

        QUICK_CREATE_FIELDS.put("name", "配置名称");
        QUICK_CREATE_FIELDS.put("description", "描述");
    }

    @Override
    public boolean supports(String schemaSubType) {
        return SUPPORTED_TYPES.contains(schemaSubType);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public ChangeDetail diff(JsonNode before, JsonNode after) {
        String schemaType = after.path("schemaType").asText("CARD_TYPE");
        String schemaSubType = after.path("schemaSubType").asText("UNKNOWN");

        ChangeDetail detail = ChangeDetail.forUpdate(schemaType, schemaSubType);

        // 比较字符串字段
        for (var entry : STRING_FIELDS.entrySet()) {
            diffHelper.compareScalar(entry.getKey(), entry.getValue(),
                            before.path(entry.getKey()), after.path(entry.getKey()), "string")
                    .ifPresent(detail::addFieldChange);
        }

        // 比较布尔字段
        for (var entry : BOOLEAN_FIELDS.entrySet()) {
            diffHelper.compareBoolean(entry.getKey(), entry.getValue(),
                            before.path(entry.getKey()), after.path(entry.getKey()))
                    .ifPresent(detail::addFieldChange);
        }

        // 比较继承的父类型列表（仅实体类型）
        if (SchemaSubType.ENTITY_CARD_TYPE.equals(schemaSubType)) {
            detail.getSemanticChanges().addAll(
                    diffHelper.compareStringList(
                            before.path("parentTypeIds"),
                            after.path("parentTypeIds"),
                            "PARENT_TYPE"
                    )
            );

            // 比较快速创建配置列表
            detail.getSemanticChanges().addAll(
                    compareQuickCreateConfigs(
                            before.path("quickCreateLinkConfigs"),
                            after.path("quickCreateLinkConfigs")
                    )
            );

            // 比较权限配置
            detail.getSemanticChanges().addAll(
                    comparePermissionConfig(
                            before.path("permissionConfig"),
                            after.path("permissionConfig")
                    )
            );
        }

        return detail;
    }

    /**
     * 比较快速创建配置列表
     */
    private List<SemanticChange> compareQuickCreateConfigs(JsonNode before, JsonNode after) {
        // 使用 linkFieldId.value 作为唯一标识
        return diffHelper.compareListWithDetails(
                before, after,
                "linkFieldId", "name",
                "QUICK_CREATE_CONFIG",
                QUICK_CREATE_FIELDS
        );
    }

    /**
     * 比较权限配置
     * <p>
     * 权限配置包含三个子列表：卡片操作权限、属性权限、附件权限
     */
    private List<SemanticChange> comparePermissionConfig(JsonNode before, JsonNode after) {
        List<SemanticChange> changes = new ArrayList<>();

        boolean beforeEmpty = before == null || before.isNull() || before.isMissingNode();
        boolean afterEmpty = after == null || after.isNull() || after.isMissingNode();

        // 整体权限配置的新增/删除
        if (beforeEmpty && !afterEmpty) {
            changes.add(SemanticChange.added("PERMISSION_CONFIG", "permissionConfig", "权限配置"));
            return changes;
        }
        if (!beforeEmpty && afterEmpty) {
            changes.add(SemanticChange.removed("PERMISSION_CONFIG", "permissionConfig", "权限配置"));
            return changes;
        }
        if (beforeEmpty) {
            return changes;
        }

        // 比较卡片操作权限
        changes.addAll(compareCardOperationPermissions(
                before.path("cardOperations"),
                after.path("cardOperations")
        ));

        // 比较属性权限
        changes.addAll(compareFieldPermissions(
                before.path("fieldPermissions"),
                after.path("fieldPermissions")
        ));

        // 比较附件权限
        changes.addAll(compareAttachmentPermissions(
                before.path("attachmentPermissions"),
                after.path("attachmentPermissions")
        ));

        return changes;
    }

    /**
     * 比较卡片操作权限列表
     */
    private List<SemanticChange> compareCardOperationPermissions(JsonNode before, JsonNode after) {
        List<SemanticChange> changes = new ArrayList<>();
        Map<String, JsonNode> beforeMap = toOperationMap(before, "operation");
        Map<String, JsonNode> afterMap = toOperationMap(after, "operation");

        // 新增的操作权限
        for (var entry : afterMap.entrySet()) {
            if (!beforeMap.containsKey(entry.getKey())) {
                String label = getOperationLabel(entry.getKey());
                changes.add(SemanticChange.added("CARD_OPERATION_PERMISSION", entry.getKey(), label + "权限"));
            }
        }

        // 删除的操作权限
        for (var entry : beforeMap.entrySet()) {
            if (!afterMap.containsKey(entry.getKey())) {
                String label = getOperationLabel(entry.getKey());
                changes.add(SemanticChange.removed("CARD_OPERATION_PERMISSION", entry.getKey(), label + "权限"));
            }
        }

        // 修改的操作权限（检测条件变化）
        for (var entry : afterMap.entrySet()) {
            String op = entry.getKey();
            if (beforeMap.containsKey(op)) {
                JsonNode beforeItem = beforeMap.get(op);
                JsonNode afterItem = entry.getValue();
                if (!beforeItem.equals(afterItem)) {
                    String label = getOperationLabel(op);
                    List<FieldChange> details = comparePermissionDetails(beforeItem, afterItem);
                    changes.add(SemanticChange.modified("CARD_OPERATION_PERMISSION", op, label + "权限", details));
                }
            }
        }

        return changes;
    }

    /**
     * 比较属性权限列表
     */
    private List<SemanticChange> compareFieldPermissions(JsonNode before, JsonNode after) {
        List<SemanticChange> changes = new ArrayList<>();
        Map<String, JsonNode> beforeMap = toOperationMap(before, "operation");
        Map<String, JsonNode> afterMap = toOperationMap(after, "operation");

        for (var entry : afterMap.entrySet()) {
            if (!beforeMap.containsKey(entry.getKey())) {
                String label = getFieldOperationLabel(entry.getKey());
                changes.add(SemanticChange.added("FIELD_PERMISSION", entry.getKey(), label + "权限"));
            }
        }

        for (var entry : beforeMap.entrySet()) {
            if (!afterMap.containsKey(entry.getKey())) {
                String label = getFieldOperationLabel(entry.getKey());
                changes.add(SemanticChange.removed("FIELD_PERMISSION", entry.getKey(), label + "权限"));
            }
        }

        for (var entry : afterMap.entrySet()) {
            String op = entry.getKey();
            if (beforeMap.containsKey(op)) {
                JsonNode beforeItem = beforeMap.get(op);
                JsonNode afterItem = entry.getValue();
                if (!beforeItem.equals(afterItem)) {
                    String label = getFieldOperationLabel(op);
                    List<FieldChange> details = comparePermissionDetails(beforeItem, afterItem);
                    changes.add(SemanticChange.modified("FIELD_PERMISSION", op, label + "权限", details));
                }
            }
        }

        return changes;
    }

    /**
     * 比较附件权限列表
     */
    private List<SemanticChange> compareAttachmentPermissions(JsonNode before, JsonNode after) {
        List<SemanticChange> changes = new ArrayList<>();
        Map<String, JsonNode> beforeMap = toOperationMap(before, "attachmentOperation");
        Map<String, JsonNode> afterMap = toOperationMap(after, "attachmentOperation");

        for (var entry : afterMap.entrySet()) {
            if (!beforeMap.containsKey(entry.getKey())) {
                String label = getAttachmentOperationLabel(entry.getKey());
                changes.add(SemanticChange.added("ATTACHMENT_PERMISSION", entry.getKey(), label + "权限"));
            }
        }

        for (var entry : beforeMap.entrySet()) {
            if (!afterMap.containsKey(entry.getKey())) {
                String label = getAttachmentOperationLabel(entry.getKey());
                changes.add(SemanticChange.removed("ATTACHMENT_PERMISSION", entry.getKey(), label + "权限"));
            }
        }

        for (var entry : afterMap.entrySet()) {
            String op = entry.getKey();
            if (beforeMap.containsKey(op)) {
                JsonNode beforeItem = beforeMap.get(op);
                JsonNode afterItem = entry.getValue();
                if (!beforeItem.equals(afterItem)) {
                    String label = getAttachmentOperationLabel(op);
                    List<FieldChange> details = comparePermissionDetails(beforeItem, afterItem);
                    changes.add(SemanticChange.modified("ATTACHMENT_PERMISSION", op, label + "权限", details));
                }
            }
        }

        return changes;
    }

    /**
     * 比较权限配置的详细变更
     */
    private List<FieldChange> comparePermissionDetails(JsonNode before, JsonNode after) {
        List<FieldChange> details = new ArrayList<>();

        // 比较提示信息
        diffHelper.compareScalar("alertMessage", "提示信息",
                        before.path("alertMessage"), after.path("alertMessage"), "string")
                .ifPresent(details::add);

        // 比较条件数量变化
        int beforeCardConditions = countArraySize(before.path("cardConditions"));
        int afterCardConditions = countArraySize(after.path("cardConditions"));
        if (beforeCardConditions != afterCardConditions) {
            FieldChange change = new FieldChange();
            change.setFieldPath("cardConditions");
            change.setFieldLabel("卡片条件");
            change.setOldValue(beforeCardConditions + "个条件");
            change.setNewValue(afterCardConditions + "个条件");
            change.setChangeType(FieldChangeType.MODIFIED);
            change.setValueType("string");
            details.add(change);
        }

        int beforeOperatorConditions = countArraySize(before.path("operatorConditions"));
        int afterOperatorConditions = countArraySize(after.path("operatorConditions"));
        if (beforeOperatorConditions != afterOperatorConditions) {
            FieldChange change = new FieldChange();
            change.setFieldPath("operatorConditions");
            change.setFieldLabel("操作人条件");
            change.setOldValue(beforeOperatorConditions + "个条件");
            change.setNewValue(afterOperatorConditions + "个条件");
            change.setChangeType(FieldChangeType.MODIFIED);
            change.setValueType("string");
            details.add(change);
        }

        return details;
    }

    /**
     * 将数组转换为以指定字段为key的Map
     */
    private Map<String, JsonNode> toOperationMap(JsonNode arrayNode, String keyField) {
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

    private int countArraySize(JsonNode node) {
        if (node == null || !node.isArray()) {
            return 0;
        }
        return node.size();
    }

    private String getOperationLabel(String operation) {
        return switch (operation) {
            case "CREATE" -> "创建卡片";
            case "READ" -> "查看卡片";
            case "EDIT" -> "编辑卡片";
            case "MOVE" -> "移动卡片";
            case "ROLLBACK" -> "回退卡片";
            case "ARCHIVE" -> "归档卡片";
            case "DISCARD" -> "丢弃卡片";
            default -> operation;
        };
    }

    private String getFieldOperationLabel(String operation) {
        return switch (operation) {
            case "READ" -> "属性查看";
            case "DESENSITIZED_READ" -> "属性脱敏查看";
            case "EDIT" -> "属性编辑";
            default -> operation;
        };
    }

    private String getAttachmentOperationLabel(String operation) {
        return switch (operation) {
            case "UPLOAD" -> "附件上传";
            case "DOWNLOAD" -> "附件下载";
            case "EDIT" -> "附件编辑";
            case "PREVIEW" -> "附件预览";
            case "DELETE" -> "附件删除";
            default -> operation;
        };
    }
}
