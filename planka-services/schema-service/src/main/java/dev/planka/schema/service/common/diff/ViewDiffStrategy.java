package dev.planka.schema.service.common.diff;

import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.changelog.ChangeDetail;
import dev.planka.domain.schema.changelog.FieldChange;
import dev.planka.domain.schema.changelog.FieldChangeType;
import dev.planka.domain.schema.changelog.SemanticChange;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 视图定义差异比较策略
 * <p>
 * 支持列表视图定义的差异比较，包括深层级字段：
 * <ul>
 *     <li>columnConfigs - 列配置（宽度、可见性、冻结等）</li>
 *     <li>sorts - 排序配置</li>
 *     <li>pageConfig - 分页配置</li>
 *     <li>condition - 过滤条件</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ViewDiffStrategy implements SchemaDiffStrategy {

    private final JsonDiffHelper diffHelper;

    /**
     * 字符串字段映射
     */
    private static final Map<String, String> STRING_FIELDS = new LinkedHashMap<>();

    /**
     * 布尔字段映射
     */
    private static final Map<String, String> BOOLEAN_FIELDS = new LinkedHashMap<>();

    /**
     * 列配置的详细字段
     */
    private static final Map<String, String> COLUMN_CONFIG_FIELDS = new LinkedHashMap<>();

    /**
     * 排序配置的详细字段
     */
    private static final Map<String, String> SORT_FIELD_CONFIG = new LinkedHashMap<>();

    static {
        STRING_FIELDS.put("name", "名称");
        STRING_FIELDS.put("description", "描述");
        STRING_FIELDS.put("groupBy", "分组字段");

        BOOLEAN_FIELDS.put("enabled", "启用状态");
        BOOLEAN_FIELDS.put("defaultView", "默认视图");
        BOOLEAN_FIELDS.put("shared", "公开共享");

        // 列配置详细字段
        COLUMN_CONFIG_FIELDS.put("width", "列宽");
        COLUMN_CONFIG_FIELDS.put("visible", "可见");
        COLUMN_CONFIG_FIELDS.put("frozen", "冻结");
        COLUMN_CONFIG_FIELDS.put("resizable", "可调整大小");

        // 排序配置详细字段
        SORT_FIELD_CONFIG.put("direction", "排序方向");
    }

    @Override
    public boolean supports(String schemaSubType) {
        return SchemaSubType.LIST_VIEW.equals(schemaSubType);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public ChangeDetail diff(JsonNode before, JsonNode after) {
        String schemaType = after.path("schemaType").asText("VIEW");
        String schemaSubType = after.path("schemaSubType").asText(SchemaSubType.LIST_VIEW);

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

        // 比较列配置列表（支持详细字段比较）
        detail.getSemanticChanges().addAll(
                diffHelper.compareListWithDetails(
                        before.path("columnConfigs"),
                        after.path("columnConfigs"),
                        "fieldId",
                        "fieldId",
                        "COLUMN_CONFIG",
                        COLUMN_CONFIG_FIELDS
                )
        );

        // 比较排序配置列表（支持详细字段比较）
        detail.getSemanticChanges().addAll(
                diffHelper.compareListWithDetails(
                        before.path("sorts"),
                        after.path("sorts"),
                        "fieldId",
                        "fieldId",
                        "SORT_FIELD",
                        SORT_FIELD_CONFIG
                )
        );

        // 比较分页配置
        detail.getSemanticChanges().addAll(
                comparePageConfig(
                        before.path("pageConfig"),
                        after.path("pageConfig")
                )
        );

        // 比较过滤条件（简化比较，只检测是否有变化）
        detail.getSemanticChanges().addAll(
                compareCondition(
                        before.path("condition"),
                        after.path("condition")
                )
        );

        return detail;
    }

    /**
     * 比较分页配置
     */
    private List<SemanticChange> comparePageConfig(JsonNode before, JsonNode after) {
        List<SemanticChange> changes = new ArrayList<>();

        boolean beforeEmpty = before == null || before.isNull() || before.isMissingNode();
        boolean afterEmpty = after == null || after.isNull() || after.isMissingNode();

        if (beforeEmpty && afterEmpty) {
            return changes;
        }

        if (beforeEmpty) {
            changes.add(SemanticChange.added("PAGE_CONFIG", "pageConfig", "分页配置"));
            return changes;
        }

        if (afterEmpty) {
            changes.add(SemanticChange.removed("PAGE_CONFIG", "pageConfig", "分页配置"));
            return changes;
        }

        // 比较详细字段
        if (!before.equals(after)) {
            List<FieldChange> details = new ArrayList<>();

            // 默认每页大小
            int beforePageSize = before.path("defaultPageSize").asInt(20);
            int afterPageSize = after.path("defaultPageSize").asInt(20);
            if (beforePageSize != afterPageSize) {
                FieldChange change = new FieldChange();
                change.setFieldPath("defaultPageSize");
                change.setFieldLabel("默认每页大小");
                change.setOldValue(beforePageSize);
                change.setNewValue(afterPageSize);
                change.setChangeType(FieldChangeType.MODIFIED);
                change.setValueType("number");
                details.add(change);
            }

            // 虚拟滚动
            boolean beforeVirtual = before.path("enableVirtualScroll").asBoolean(false);
            boolean afterVirtual = after.path("enableVirtualScroll").asBoolean(false);
            if (beforeVirtual != afterVirtual) {
                FieldChange change = new FieldChange();
                change.setFieldPath("enableVirtualScroll");
                change.setFieldLabel("虚拟滚动");
                change.setOldValue(beforeVirtual ? "是" : "否");
                change.setNewValue(afterVirtual ? "是" : "否");
                change.setChangeType(FieldChangeType.MODIFIED);
                change.setValueType("boolean");
                details.add(change);
            }

            if (!details.isEmpty()) {
                changes.add(SemanticChange.modified("PAGE_CONFIG", "pageConfig", "分页配置", details));
            }
        }

        return changes;
    }

    /**
     * 比较过滤条件
     */
    private List<SemanticChange> compareCondition(JsonNode before, JsonNode after) {
        List<SemanticChange> changes = new ArrayList<>();

        boolean beforeEmpty = before == null || before.isNull() || before.isMissingNode();
        boolean afterEmpty = after == null || after.isNull() || after.isMissingNode();

        if (beforeEmpty && afterEmpty) {
            return changes;
        }

        if (beforeEmpty) {
            changes.add(SemanticChange.added("FILTER_CONDITION", "condition", "过滤条件"));
            return changes;
        }

        if (afterEmpty) {
            changes.add(SemanticChange.removed("FILTER_CONDITION", "condition", "过滤条件"));
            return changes;
        }

        if (!before.equals(after)) {
            changes.add(SemanticChange.modified("FILTER_CONDITION", "condition", "过滤条件", null));
        }

        return changes;
    }
}
