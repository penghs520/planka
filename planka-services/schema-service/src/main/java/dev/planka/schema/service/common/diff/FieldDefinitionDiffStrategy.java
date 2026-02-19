package dev.planka.schema.service.common.diff;

import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.changelog.ChangeDetail;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 字段配置差异比较策略
 * <p>
 * 支持所有字段配置类型的差异比较，包括文本、数值、日期、枚举等。
 * 对于枚举字段，特别处理枚举选项列表的变更。
 */
@Component
@RequiredArgsConstructor
public class FieldDefinitionDiffStrategy implements SchemaDiffStrategy {

    private final JsonDiffHelper diffHelper;

    /**
     * 支持的Schema子类型
     */
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            SchemaSubType.TEXT_FIELD,
            SchemaSubType.MULTI_LINE_TEXT_FIELD,
            SchemaSubType.MARKDOWN_FIELD,
            SchemaSubType.NUMBER_FIELD,
            SchemaSubType.DATE_FIELD,
            SchemaSubType.ENUM_FIELD,
            SchemaSubType.ATTACHMENT_FIELD,
            SchemaSubType.WEB_URL_FIELD,
            SchemaSubType.STRUCTURE_FIELD
    );

    /**
     * 通用字符串字段映射
     */
    private static final Map<String, String> COMMON_STRING_FIELDS = new LinkedHashMap<>();

    /**
     * 通用布尔字段映射
     */
    private static final Map<String, String> COMMON_BOOLEAN_FIELDS = new LinkedHashMap<>();

    /**
     * 文本属性特有字段
     */
    private static final Map<String, String> TEXT_FIELDS = new LinkedHashMap<>();

    /**
     * 数值属性特有字段
     */
    private static final Map<String, String> NUMBER_FIELDS = new LinkedHashMap<>();

    /**
     * 日期属性特有字段
     */
    private static final Map<String, String> DATE_FIELDS = new LinkedHashMap<>();

    /**
     * 枚举属性布尔字段
     */
    private static final Map<String, String> ENUM_BOOLEAN_FIELDS = new LinkedHashMap<>();

    /**
     * 枚举选项的详细字段
     */
    private static final Map<String, String> ENUM_OPTION_FIELDS = new LinkedHashMap<>();

    static {
        // 通用字段
        COMMON_STRING_FIELDS.put("name", "名称");
        COMMON_STRING_FIELDS.put("description", "描述");
        COMMON_STRING_FIELDS.put("code", "编码");

        COMMON_BOOLEAN_FIELDS.put("enabled", "启用状态");
        COMMON_BOOLEAN_FIELDS.put("systemField", "系统字段");

        // 文本属性字段（单行、多行、Markdown共用）
        TEXT_FIELDS.put("maxLength", "最大长度");
        TEXT_FIELDS.put("defaultValue", "默认值");
        TEXT_FIELDS.put("placeholder", "输入提示");

        // 数值属性字段
        NUMBER_FIELDS.put("precision", "小数位数");
        NUMBER_FIELDS.put("minValue", "最小值");
        NUMBER_FIELDS.put("maxValue", "最大值");
        NUMBER_FIELDS.put("unit", "单位");

        // 日期属性字段
        DATE_FIELDS.put("dateFormat", "日期格式");
        DATE_FIELDS.put("defaultValueType", "默认值类型");
        DATE_FIELDS.put("fixedDefaultValue", "固定默认值");

        // 枚举属性布尔字段
        ENUM_BOOLEAN_FIELDS.put("multiSelect", "多选");

        // 枚举选项的详细字段
        ENUM_OPTION_FIELDS.put("label", "显示名称");
        ENUM_OPTION_FIELDS.put("value", "值");
        ENUM_OPTION_FIELDS.put("color", "颜色");
        ENUM_OPTION_FIELDS.put("enabled", "启用状态");
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
        String schemaType = after.path("schemaType").asText("FIELD_DEFINITION");
        String schemaSubType = after.path("schemaSubType").asText("UNKNOWN");

        ChangeDetail detail = ChangeDetail.forUpdate(schemaType, schemaSubType);

        // 比较通用字段
        compareCommonFields(before, after, detail);

        // 根据子类型比较特有字段
        switch (schemaSubType) {
            case SchemaSubType.TEXT_FIELD,
                 SchemaSubType.MULTI_LINE_TEXT_FIELD,
                 SchemaSubType.MARKDOWN_FIELD -> compareTextFields(before, after, detail);
            case SchemaSubType.NUMBER_FIELD -> compareNumberFields(before, after, detail);
            case SchemaSubType.DATE_FIELD -> compareDateFields(before, after, detail);
            case SchemaSubType.ENUM_FIELD -> compareEnumFields(before, after, detail);
        }

        return detail;
    }

    private void compareCommonFields(JsonNode before, JsonNode after, ChangeDetail detail) {
        for (var entry : COMMON_STRING_FIELDS.entrySet()) {
            diffHelper.compareScalar(entry.getKey(), entry.getValue(),
                            before.path(entry.getKey()), after.path(entry.getKey()), "string")
                    .ifPresent(detail::addFieldChange);
        }

        for (var entry : COMMON_BOOLEAN_FIELDS.entrySet()) {
            diffHelper.compareBoolean(entry.getKey(), entry.getValue(),
                            before.path(entry.getKey()), after.path(entry.getKey()))
                    .ifPresent(detail::addFieldChange);
        }
    }

    private void compareTextFields(JsonNode before, JsonNode after, ChangeDetail detail) {
        for (var entry : TEXT_FIELDS.entrySet()) {
            diffHelper.compareScalar(entry.getKey(), entry.getValue(),
                            before.path(entry.getKey()), after.path(entry.getKey()), "string")
                    .ifPresent(detail::addFieldChange);
        }
    }

    private void compareNumberFields(JsonNode before, JsonNode after, ChangeDetail detail) {
        for (var entry : NUMBER_FIELDS.entrySet()) {
            diffHelper.compareScalar(entry.getKey(), entry.getValue(),
                            before.path(entry.getKey()), after.path(entry.getKey()), "number")
                    .ifPresent(detail::addFieldChange);
        }
    }

    private void compareDateFields(JsonNode before, JsonNode after, ChangeDetail detail) {
        for (var entry : DATE_FIELDS.entrySet()) {
            diffHelper.compareScalar(entry.getKey(), entry.getValue(),
                            before.path(entry.getKey()), after.path(entry.getKey()), "string")
                    .ifPresent(detail::addFieldChange);
        }
    }

    private void compareEnumFields(JsonNode before, JsonNode after, ChangeDetail detail) {
        // 多选开关
        for (var entry : ENUM_BOOLEAN_FIELDS.entrySet()) {
            diffHelper.compareBoolean(entry.getKey(), entry.getValue(),
                            before.path(entry.getKey()), after.path(entry.getKey()))
                    .ifPresent(detail::addFieldChange);
        }

        // 枚举选项列表（支持详细字段比较）
        detail.getSemanticChanges().addAll(
                diffHelper.compareListWithDetails(
                        before.path("options"),
                        after.path("options"),
                        "id",
                        "label",
                        "ENUM_OPTION",
                        ENUM_OPTION_FIELDS
                )
        );
    }
}
