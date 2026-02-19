package dev.planka.schema.dto;

import dev.planka.domain.schema.definition.fieldconfig.EnumFieldConfig;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 新建页表单 VO
 * <p>
 * 用于前端渲染新建卡片表单，包含模板布局和字段配置信息
 */
@Data
public class CreatePageFormVO {
    
    /** 模板 ID（如果使用的是默认生成的模板，则为 null） */
    private String templateId;
    
    /** 模板名称 */
    private String templateName;
    
    /** 卡片类型 ID */
    private String cardTypeId;
    
    /** 卡片类型名称 */
    private String cardTypeName;
    
    /** 字段配置列表（包含布局信息和字段配置） */
    private List<CreatePageFieldVO> fields;
    
    /** 空间绑定架构属性ID（当卡片类型绑定空间时，该字段必填） */
    private String spaceBindingFieldId;

    // ==================== 字段 VO 基类和子类 ====================

    /**
     * 新建页字段 VO 基类
     */
    @Data
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.PROPERTY,
            property = "fieldType"
    )
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TextFieldVO.class, name = "TEXT"),
            @JsonSubTypes.Type(value = NumberFieldVO.class, name = "NUMBER"),
            @JsonSubTypes.Type(value = DateFieldVO.class, name = "DATE"),
            @JsonSubTypes.Type(value = EnumFieldVO.class, name = "ENUM"),
            @JsonSubTypes.Type(value = AttachmentFieldVO.class, name = "ATTACHMENT"),
            @JsonSubTypes.Type(value = WebUrlFieldVO.class, name = "WEB_URL"),
            @JsonSubTypes.Type(value = StructureFieldVO.class, name = "STRUCTURE"),
            @JsonSubTypes.Type(value = LinkFieldVO.class, name = "LINK"),
    })
    public static abstract class CreatePageFieldVO {
        /** 字段 ID */
        private String fieldId;
        
        /** 字段名称 */
        private String name;
        
        /** 宽度百分比 (50 or 100) */
        private int widthPercent;
        
        /** 是否必填 */
        private boolean required;
        
        /** 是否只读 */
        private boolean readOnly;
        
        /** 占位符文本 */
        private String placeholder;

        /** 获取字段类型 */
        public abstract String getFieldType();
    }

    /**
     * 文本字段 VO (单行/多行/Markdown)
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TextFieldVO extends CreatePageFieldVO {
        /** 文本类型: SINGLE_LINE, MULTI_LINE, MARKDOWN */
        private String textType;
        
        /** 最大长度 */
        private Integer maxLength;
        
        /** 默认值 */
        private String defaultValue;
        
        @Override
        public String getFieldType() {
            // 根据 textType 返回对应的字段类型
            if ("MULTI_LINE".equals(textType)) {
                return "TEXTAREA";
            }
            if ("MARKDOWN".equals(textType)) {
                return "MARKDOWN";
            }
            return "TEXT";
        }
    }

    /**
     * 数字字段 VO
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class NumberFieldVO extends CreatePageFieldVO {
        /** 最小值 */
        private Double minValue;
        
        /** 最大值 */
        private Double maxValue;
        
        /** 小数位数 */
        private Integer precision;
        
        /** 单位 */
        private String unit;
        
        /** 是否显示千分位 */
        private boolean showThousandSeparator;
        
        /** 默认值 */
        private Double defaultValue;
        
        @Override
        public String getFieldType() {
            return "NUMBER";
        }
    }

    /**
     * 日期字段 VO
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DateFieldVO extends CreatePageFieldVO {
        /** 日期格式: DATE, DATETIME, DATETIME_SECOND, YEAR_MONTH */
        private String dateFormat;

        /** 是否使用当前时间作为默认值 */
        private boolean useNowAsDefault;

        @Override
        public String getFieldType() {
            return "DATE";
        }
    }

    /**
     * 枚举字段 VO
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class EnumFieldVO extends CreatePageFieldVO {
        /** 枚举选项列表 */
        private List<EnumOptionVO> options;

        /** 是否多选 */
        private boolean multiSelect;

        /** 默认选项 ID 列表 */
        private List<String> defaultOptionIds;

        @Override
        public String getFieldType() {
            return "ENUM";
        }
    }

    /**
     * 枚举选项 VO
     */
    @Data
    public static class EnumOptionVO {
        /** 选项 ID */
        private String id;
        /** 显示标签 */
        private String label;
        /** 选项值 */
        private String value;
        /** 是否启用 */
        private boolean enabled;
        /** 显示颜色 */
        private String color;
        /** 排序号 */
        private int order;

        public static EnumOptionVO from(EnumFieldConfig.EnumOptionDefinition definition) {
            if (definition == null) {
                return null;
            }
            EnumOptionVO vo = new EnumOptionVO();
            vo.setId(definition.id());
            vo.setLabel(definition.label());
            vo.setValue(definition.value());
            vo.setEnabled(definition.enabled());
            vo.setColor(definition.color());
            vo.setOrder(definition.order());
            return vo;
        }
    }

    /**
     * 附件字段 VO
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AttachmentFieldVO extends CreatePageFieldVO {
        /** 允许的文件类型列表 */
        private List<String> allowedFileTypes;
        
        /** 单个文件最大大小（字节） */
        private Long maxFileSize;
        
        /** 最大文件数量 */
        private Integer maxFileCount;
        
        @Override
        public String getFieldType() {
            return "ATTACHMENT";
        }
    }

    /**
     * 网页链接字段 VO
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class WebUrlFieldVO extends CreatePageFieldVO {
        /** 是否验证 URL 格式 */
        private boolean validateUrl;
        
        /** 是否显示预览 */
        private boolean showPreview;
        
        /** 默认 URL */
        private String defaultUrl;
        
        /** 默认链接文本 */
        private String defaultLinkText;
        
        @Override
        public String getFieldType() {
            return "WEB_URL";
        }
    }

    /**
     * 架构层级字段 VO
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class StructureFieldVO extends CreatePageFieldVO {
        /** 架构 ID */
        private String structureId;
        
        /** 是否只允许选择叶子节点 */
        private boolean leafOnly;
        
        /** 默认节点 ID */
        private String defaultNodeId;
        
        @Override
        public String getFieldType() {
            return "STRUCTURE";
        }
    }

    /**
     * 关联字段 VO
     * <p>
     * 关联属性的配置信息来自 LinkFieldConfig，无需包含 LinkTypeDefinition 的详细信息
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class LinkFieldVO extends CreatePageFieldVO {
        /** 是否多选 */
        private boolean multiple;
        
        @Override
        public String getFieldType() {
            return "LINK";
        }
    }
}
