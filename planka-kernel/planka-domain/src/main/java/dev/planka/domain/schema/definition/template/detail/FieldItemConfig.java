package dev.planka.domain.schema.definition.template.detail;

import dev.planka.domain.schema.definition.condition.Condition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 字段项配置
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldItemConfig {

    /** 字段配置ID */
    @JsonProperty("fieldConfigId")
    private String fieldConfigId;

    /** 排序顺序 */
    @JsonProperty("sortOrder")
    private Integer sortOrder = 0;

    /** 宽度百分比（25-100，默认50表示一半宽度） */
    @JsonProperty("widthPercent")
    private Integer widthPercent = 50;

    /** 是否显示标签 */
    @JsonProperty("showLabel")
    private boolean showLabel = true;

    /** 自定义标签（为空则使用字段名） */
    @JsonProperty("customLabel")
    private String customLabel;

    /** 占位提示文本 */
    @JsonProperty("placeholder")
    private String placeholder;

    /** 新建页是否可见 */
    @JsonProperty("visibleOnCreate")
    private Boolean visibleOnCreate = true;

    /** 新建页是否必填 */
    @JsonProperty("requiredOnCreate")
    private Boolean requiredOnCreate = false;

    /** 可见条件 */
    @JsonProperty("visibleCondition")
    private Condition visibleCondition;

    /** 是否从新行开始（强制换行） */
    @JsonProperty("startNewRow")
    private Boolean startNewRow = false;
}
