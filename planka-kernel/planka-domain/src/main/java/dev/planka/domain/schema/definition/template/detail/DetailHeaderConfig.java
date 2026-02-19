package dev.planka.domain.schema.definition.template.detail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 详情页头部配置
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DetailHeaderConfig {

    /** 是否显示卡片类型图标 */
    @JsonProperty("showTypeIcon")
    private boolean showTypeIcon = true;

    /** 是否显示卡片编号 */
    @JsonProperty("showCardNumber")
    private boolean showCardNumber = true;

    /** 是否显示状态标签 */
    @JsonProperty("showStatus")
    private boolean showStatus = true;

    /** 标题字段配置ID */
    @JsonProperty("titleFieldConfigId")
    private String titleFieldConfigId;

    /** 头部快捷操作字段ID列表 */
    @JsonProperty("quickActionFieldIds")
    private List<String> quickActionFieldIds = new ArrayList<>();
}
