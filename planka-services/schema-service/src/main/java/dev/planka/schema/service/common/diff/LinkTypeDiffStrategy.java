package dev.planka.schema.service.common.diff;

import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.changelog.ChangeDetail;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 关联类型差异比较策略
 * <p>
 * 支持关联类型定义的差异比较。
 */
@Component
@RequiredArgsConstructor
public class LinkTypeDiffStrategy implements SchemaDiffStrategy {

    private final JsonDiffHelper diffHelper;

    /**
     * 字符串字段映射
     */
    private static final Map<String, String> STRING_FIELDS = new LinkedHashMap<>();

    /**
     * 布尔字段映射
     */
    private static final Map<String, String> BOOLEAN_FIELDS = new LinkedHashMap<>();

    static {
        STRING_FIELDS.put("name", "名称");
        STRING_FIELDS.put("description", "描述");
        STRING_FIELDS.put("sourceName", "源端名称");
        STRING_FIELDS.put("targetName", "目标端名称");

        BOOLEAN_FIELDS.put("enabled", "启用状态");
        BOOLEAN_FIELDS.put("sourceVisible", "源端显示");
        BOOLEAN_FIELDS.put("targetVisible", "目标端显示");
        BOOLEAN_FIELDS.put("sourceMultiSelect", "源端多选");
        BOOLEAN_FIELDS.put("targetMultiSelect", "目标端多选");
        BOOLEAN_FIELDS.put("systemLinkType", "系统内置");
    }

    @Override
    public boolean supports(String schemaSubType) {
        return SchemaSubType.LINK_TYPE.equals(schemaSubType);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public ChangeDetail diff(JsonNode before, JsonNode after) {
        String schemaType = after.path("schemaType").asText("LINK_TYPE");
        String schemaSubType = after.path("schemaSubType").asText(SchemaSubType.LINK_TYPE);

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

        // 比较源端卡片类型列表（使用ID对象比较）
        detail.getSemanticChanges().addAll(
                diffHelper.compareIdObjectList(
                        before.path("sourceCardTypeIds"),
                        after.path("sourceCardTypeIds"),
                        "SOURCE_CARD_TYPE"
                )
        );

        // 比较目标端卡片类型列表（使用ID对象比较）
        detail.getSemanticChanges().addAll(
                diffHelper.compareIdObjectList(
                        before.path("targetCardTypeIds"),
                        after.path("targetCardTypeIds"),
                        "TARGET_CARD_TYPE"
                )
        );

        return detail;
    }
}
