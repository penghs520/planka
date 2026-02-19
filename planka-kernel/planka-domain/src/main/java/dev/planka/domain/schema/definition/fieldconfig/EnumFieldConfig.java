package dev.planka.domain.schema.definition.fieldconfig;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.SchemaSubType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 枚举属性的卡片类型级别配置
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EnumFieldConfig extends FieldConfig {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.ENUM_FIELD;
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.ENUM;
    }

    /** 枚举选项列表 */
    @JsonProperty("options")
    private List<EnumOptionDefinition> options;

    /** 是否多选 */
    @JsonProperty("multiSelect")
    private boolean multiSelect = false;

    /** 默认选项ID列表 */
    @JsonProperty("defaultOptionIds")
    private List<String> defaultOptionIds;

    @JsonCreator
    public EnumFieldConfig(
            @JsonProperty("id") FieldConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("fieldId") FieldId fieldId,
            @JsonProperty("systemField") boolean systemField) {
        super(id, orgId, name, cardTypeId, fieldId, systemField);
    }

    /**
     * 枚举选项定义
     * <p>
     * 用于定义枚举属性的可选值列表。
     *
     * @param id      选项唯一标识
     * @param label   显示标签
     * @param value   选项值（用于存储和传输）
     * @param enabled 是否启用
     * @param color   显示颜色（可选）
     * @param order   排序号
     */
    public static record EnumOptionDefinition(
            String id,
            String label,
            String value,
            boolean enabled,
            String color,
            int order
    ) {
        @JsonCreator
        public EnumOptionDefinition(
                @JsonProperty("id") String id,
                @JsonProperty("label") String label,
                @JsonProperty("value") String value,
                @JsonProperty("enabled") boolean enabled,
                @JsonProperty("color") String color,
                @JsonProperty("order") int order
        ) {
            this.id = id;
            this.label = label;
            this.value = value;
            this.enabled = enabled;
            this.color = color;
            this.order = order;
        }
    }
}
