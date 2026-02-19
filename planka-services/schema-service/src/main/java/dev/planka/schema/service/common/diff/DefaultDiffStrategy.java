package dev.planka.schema.service.common.diff;

import dev.planka.domain.schema.changelog.ChangeDetail;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认差异比较策略
 * <p>
 * 作为兜底策略，仅比较通用字段（name, description, enabled）。
 * 优先级最低，当没有其他策略匹配时使用。
 */
@Component
@RequiredArgsConstructor
public class DefaultDiffStrategy implements SchemaDiffStrategy {

    private final JsonDiffHelper diffHelper;

    /**
     * 通用字段映射：字段路径 -> 显示名
     */
    private static final Map<String, String> COMMON_FIELDS = new LinkedHashMap<>();

    static {
        COMMON_FIELDS.put("name", "名称");
        COMMON_FIELDS.put("description", "描述");
        COMMON_FIELDS.put("enabled", "启用状态");
    }

    @Override
    public boolean supports(String schemaSubType) {
        // 作为兜底策略，支持所有类型
        return true;
    }

    @Override
    public int getPriority() {
        // 最低优先级
        return 1000;
    }

    @Override
    public ChangeDetail diff(JsonNode before, JsonNode after) {
        String schemaType = after.path("schemaType").asText("UNKNOWN");
        String schemaSubType = after.path("schemaSubType").asText("UNKNOWN");

        ChangeDetail detail = ChangeDetail.forUpdate(schemaType, schemaSubType);

        // 比较通用字段
        for (var entry : COMMON_FIELDS.entrySet()) {
            String fieldPath = entry.getKey();
            String fieldLabel = entry.getValue();

            if ("enabled".equals(fieldPath)) {
                diffHelper.compareBoolean(fieldPath, fieldLabel,
                                before.path(fieldPath), after.path(fieldPath))
                        .ifPresent(detail::addFieldChange);
            } else {
                diffHelper.compareScalar(fieldPath, fieldLabel,
                                before.path(fieldPath), after.path(fieldPath), "string")
                        .ifPresent(detail::addFieldChange);
            }
        }

        return detail;
    }
}
