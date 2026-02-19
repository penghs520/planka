package dev.planka.api.card.dto;

import dev.planka.api.card.renderconfig.FieldRenderConfig;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * 卡片详情响应
 * <p>
 * 包含卡片数据、详情模板配置、字段渲染配置和字段控制配置，供前端一次性获取
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDetailResponse {

    /**
     * 卡片数据
     */
    private CardDTO card;

    /**
     * 详情模板配置
     */
    private DetailTemplateDTO template;

    /**
     * 字段渲染元数据列表（与列表视图统一的渲染配置）
     */
    private List<FieldRenderMetaDTO> fieldRenderMetas;

    /**
     * 字段控制配置（必填/只读控制）
     * key: fieldId, value: 字段控制配置
     */
    private Map<String, FieldControlDTO> fieldControls;

    /**
     * 卡片类型信息（用于头部显示）
     */
    private CardTypeInfoDTO cardTypeInfo;

    /**
     * 价值流状态信息（用于头部显示，可能为 null）
     */
    private ValueStreamStatusInfoDTO valueStreamStatusInfo;

    /**
     * 详情模板配置
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailTemplateDTO {
        /**
         * 模板ID
         */
        private String id;

        /**
         * 模板名称
         */
        private String name;

        /**
         * 头部配置
         */
        private HeaderConfigDTO header;

        /**
         * 标签页列表
         */
        private List<TabConfigDTO> tabs;
    }

    /**
     * 头部配置
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeaderConfigDTO {
        private boolean showTypeIcon;
        private boolean showCardNumber;
        private boolean showStatus;
        private String titleFieldConfigId;
        private List<String> quickActionFieldIds;
    }

    /**
     * 标签页配置
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TabConfigDTO {
        private String tabId;
        private String tabType;  // SYSTEM or CUSTOM
        private String name;
        private String systemTabType;  // BASIC_INFO, COMMENT, ACTIVITY_LOG
        private String fieldRowSpacing;
        private List<SectionConfigDTO> sections;
    }

    /**
     * 区域配置
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionConfigDTO {
        private String sectionId;
        private String name;
        private boolean collapsed;
        private boolean collapsible;
        private List<FieldItemConfigDTO> fieldItems;
    }

    /**
     * 字段项配置
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldItemConfigDTO {
        private String fieldConfigId;
        private int widthPercent;
        private String customLabel;
        private String placeholder;
        private boolean startNewRow;
    }

    /**
     * 字段渲染元数据（与列表视图的 ColumnMeta.renderConfig 统一）
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldRenderMetaDTO {
        /** 属性定义 ID（用于匹配 card.fieldValues 的 key） */
        private String fieldId;
        /** 字段名称 */
        private String name;
        /** 渲染配置（多态，与列表视图统一） */
        private FieldRenderConfig renderConfig;
    }

    /**
     * 字段控制配置
     * <p>
     * 控制字段在详情页的编辑状态（可编辑/只读）和必填级别
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldControlDTO {
        /** 是否可编辑 */
        private boolean editable;
        /** 只读原因类型：BUILTIN_FIELD, FIELD_CONFIG, PERMISSION_DENIED, WORKFLOW_RESTRICTION */
        private String readOnlyReasonType;
        /** 只读原因文本（自定义说明） */
        private String readOnlyReasonText;
        /** 必填级别：HINT（仅提示，黄色星号）/ STRICT（强制，红色星号） */
        private String requiredLevel;
        /** 必填原因说明（为什么需要必填） */
        private String requiredReasonText;
    }

    /**
     * 卡片类型信息（用于头部显示）
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardTypeInfoDTO {
        /** 卡片类型ID */
        private String id;
        /** 卡片类型名称 */
        private String name;
    }

    /**
     * 价值流状态信息（用于头部显示）
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValueStreamStatusInfoDTO {
        /** 状态ID */
        private String statusId;
        /** 状态名称 */
        private String statusName;
        /** 状态所属阶段类别：TODO, IN_PROGRESS, DONE, CANCELLED */
        private String stepKind;
        /** 状态颜色 */
        private String color;
    }

}
