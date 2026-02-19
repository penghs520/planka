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
 * 日期属性的卡片类型级别配置
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DateFieldConfig extends FieldConfig {

    /**
     * 日期格式枚举
     */
    public enum DateFormat {
        /** 年-月-日 (2024-01-15) */
        DATE,
        /** 年-月-日 时:分 (2024-01-15 14:30) */
        DATETIME,
        /** 年-月-日 时:分:秒 (2024-01-15 14:30:00) */
        DATETIME_SECOND,
        /** 年-月 (2024-01) */
        YEAR_MONTH,
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.DATE_FIELD;
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.DATE;
    }

    /** 日期格式 */
    @JsonProperty("dateFormat")
    private DateFormat dateFormat;

    /**
     * 是否使用当前时间作为默认值
     */
    @JsonProperty("useNowAsDefault")
    private Boolean useNowAsDefault;

    public boolean isUseNowAsDefault() {
        return useNowAsDefault != null && useNowAsDefault;
    }

    @JsonCreator
    public DateFieldConfig(
            @JsonProperty("id") FieldConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("fieldId") FieldId fieldId,
            @JsonProperty("systemField") boolean systemField) {
        super(id, orgId, name, cardTypeId, fieldId, systemField);
    }

}
