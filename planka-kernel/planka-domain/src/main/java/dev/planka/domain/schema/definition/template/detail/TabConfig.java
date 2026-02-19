package dev.planka.domain.schema.definition.template.detail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签页配置
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TabConfig {

    /** Tab唯一标识 */
    @JsonProperty("tabId")
    private String tabId;

    /** Tab类型 */
    @JsonProperty("tabType")
    private TabType tabType;

    /** Tab名称 */
    @JsonProperty("name")
    private String name;

    /** 系统Tab类型（仅SYSTEM类型有效） */
    @JsonProperty("systemTabType")
    private SystemTabType systemTabType;

    /** 字段行间距 */
    @JsonProperty("fieldRowSpacing")
    private FieldRowSpacing fieldRowSpacing = FieldRowSpacing.NORMAL;

    /** 区域配置列表（仅BASIC_INFO和CUSTOM类型有效） */
    @JsonProperty("sections")
    private List<SectionConfig> sections = new ArrayList<>();

    /**
     * Tab类型
     */
    public enum TabType {
        /** 系统预置Tab */
        SYSTEM,
        /** 自定义Tab */
        CUSTOM
    }

    /**
     * 系统Tab类型
     */
    public enum SystemTabType {
        /** 基础信息（属性） */
        BASIC_INFO,
        /** 评论 */
        COMMENT,
        /** 操作记录 */
        ACTIVITY_LOG
    }

    /**
     * 字段行间距
     */
    public enum FieldRowSpacing {
        /** 紧凑 */
        COMPACT,
        /** 正常 */
        NORMAL,
        /** 宽松 */
        LOOSE
    }
}
