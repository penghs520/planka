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

/**
 * 数字属性的卡片类型级别配置
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NumberFieldConfig extends FieldConfig {

    /**
     * 数字显示格式
     */
    public enum DisplayFormat {
        /** 整数/小数（普通格式） */
        NORMAL,
        /** 百分数 */
        PERCENT,
        /** 千位分隔符 */
        THOUSAND_SEPARATOR
    }

    /**
     * 百分数显示效果
     */
    public enum PercentStyle {
        /** 数字效果（如：75%） */
        NUMBER,
        /** 进度条效果 */
        PROGRESS_BAR
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.NUMBER_FIELD;
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.NUMBER;
    }

    /** 最小值 */
    @JsonProperty("minValue")
    private Double minValue;

    /** 最大值 */
    @JsonProperty("maxValue")
    private Double maxValue;

    /** 小数位数 */
    @JsonProperty("precision")
    private Integer precision;

    /** 单位（百分数模式下无效） */
    @JsonProperty("unit")
    private String unit;

    /** 显示格式 */
    @JsonProperty("displayFormat")
    private DisplayFormat displayFormat;

    /** 百分数显示效果（仅当 displayFormat 为 PERCENT 时有效） */
    @JsonProperty("percentStyle")
    private PercentStyle percentStyle;

    /**
     * 是否显示千分位
     * @deprecated 使用 displayFormat == THOUSAND_SEPARATOR 代替
     */
    @Deprecated
    @JsonProperty("showThousandSeparator")
    private boolean showThousandSeparator = false;

    /** 默认值 */
    @JsonProperty("defaultValue")
    private Double defaultValue;

    @JsonCreator
    public NumberFieldConfig(
            @JsonProperty("id") FieldConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("fieldId") FieldId fieldId,
            @JsonProperty("systemField") boolean systemField) {
        super(id, orgId, name, cardTypeId, fieldId, systemField);
    }

    public DisplayFormat getDisplayFormat() {
        if (displayFormat != null) {
            return displayFormat;
        }
        return showThousandSeparator ? DisplayFormat.THOUSAND_SEPARATOR : DisplayFormat.NORMAL;
    }

    public PercentStyle getPercentStyle() {
        return percentStyle != null ? percentStyle : PercentStyle.NUMBER;
    }

    /**
     * @deprecated 使用 displayFormat == THOUSAND_SEPARATOR 代替
     */
    @Deprecated
    public boolean isShowThousandSeparator() {
        if (displayFormat == DisplayFormat.THOUSAND_SEPARATOR) {
            return true;
        }
        return showThousandSeparator;
    }

}
