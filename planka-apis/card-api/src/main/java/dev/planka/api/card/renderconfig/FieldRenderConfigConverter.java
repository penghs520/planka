package dev.planka.api.card.renderconfig;

import dev.planka.domain.schema.definition.fieldconfig.*;
import dev.planka.domain.schema.definition.fieldconfig.DateFieldConfig.DateFormat;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字段渲染配置转换器
 * <p>
 * 将领域模型的 FieldConfig 转换为 FieldRenderConfig。
 * 提供统一的转换逻辑，供 card-service 和 view-service 共同使用。
 */
public final class FieldRenderConfigConverter {

    private FieldRenderConfigConverter() {
        // 工具类，禁止实例化
    }

    /**
     * 根据字段配置类型转换为对应的渲染配置
     *
     * @param fieldConfig 字段配置
     * @return 渲染配置，如果无法识别类型则返回默认文本配置
     */
    public static FieldRenderConfig convert(FieldConfig fieldConfig) {
        if (fieldConfig == null) {
            return null;
        }
        if (fieldConfig instanceof EnumFieldConfig config) {
            return convertEnum(config);
        }
        if (fieldConfig instanceof NumberFieldConfig config) {
            return convertNumber(config);
        }
        if (fieldConfig instanceof DateFieldConfig config) {
            return convertDate(config);
        }
        if (fieldConfig instanceof SingleLineTextFieldConfig config) {
            return convertText(config, false);
        }
        if (fieldConfig instanceof MultiLineTextFieldConfig config) {
            return convertText(config, true);
        }
        if (fieldConfig instanceof MarkdownFieldConfig config) {
            return convertMarkdown(config);
        }
        if (fieldConfig instanceof AttachmentFieldConfig config) {
            return convertAttachment(config);
        }
        if (fieldConfig instanceof LinkFieldConfig config) {
            return convertLink(config);
        }
        if (fieldConfig instanceof StructureFieldConfig config) {
            return convertStructure(config);
        }
        if (fieldConfig instanceof WebUrlFieldConfig config) {
            return convertWebUrl(config);
        }
        // 未知类型，返回默认文本配置
        return TextRenderConfig.builder()
                .multiLine(false)
                .build();
    }

    /**
     * 转换枚举类型配置
     */
    public static EnumRenderConfig convertEnum(EnumFieldConfig config) {
        List<EnumOptionDTO> options = Collections.emptyList();
        if (config.getOptions() != null) {
            options = config.getOptions().stream()
                    .map(FieldRenderConfigConverter::convertEnumOption)
                    .collect(Collectors.toList());
        }
        return EnumRenderConfig.builder()
                .multiSelect(config.isMultiSelect())
                .options(options)
                .build();
    }

    /**
     * 转换枚举选项
     */
    public static EnumOptionDTO convertEnumOption(EnumFieldConfig.EnumOptionDefinition option) {
        return EnumOptionDTO.builder()
                .id(option.id())
                .label(option.label())
                .color(option.color())
                .enabled(option.enabled())
                .build();
    }

    /**
     * 转换数字类型配置
     */
    public static NumberRenderConfig convertNumber(NumberFieldConfig config) {
        String displayFormat = config.getDisplayFormat() != null
                ? config.getDisplayFormat().name()
                : "NORMAL";
        String percentStyle = config.getPercentStyle() != null
                ? config.getPercentStyle().name()
                : "NUMBER";
        return NumberRenderConfig.builder()
                .precision(config.getPrecision())
                .unit(config.getUnit())
                .showThousandSeparator(config.isShowThousandSeparator())
                .minValue(config.getMinValue())
                .maxValue(config.getMaxValue())
                .displayFormat(displayFormat)
                .percentStyle(percentStyle)
                .build();
    }

    /**
     * 转换日期类型配置
     */
    public static DateRenderConfig convertDate(DateFieldConfig config) {
        String dateFormat = config.getDateFormat() != null
                ? config.getDateFormat().name()
                : DateFormat.DATE.name();
        return DateRenderConfig.builder()
                .dateFormat(dateFormat)
                .build();
    }

    /**
     * 转换单行文本类型配置
     */
    public static TextRenderConfig convertText(SingleLineTextFieldConfig config, boolean multiLine) {
        return TextRenderConfig.builder()
                .maxLength(config.getMaxLength())
                .placeholder(config.getPlaceholder())
                .multiLine(multiLine)
                .build();
    }

    /**
     * 转换多行文本类型配置
     */
    public static TextRenderConfig convertText(MultiLineTextFieldConfig config, boolean multiLine) {
        return TextRenderConfig.builder()
                .maxLength(config.getMaxLength())
                .placeholder(config.getPlaceholder())
                .multiLine(multiLine)
                .build();
    }

    /**
     * 转换 Markdown 类型配置
     */
    public static MarkdownRenderConfig convertMarkdown(MarkdownFieldConfig config) {
        return MarkdownRenderConfig.builder()
                .maxLength(config.getMaxLength())
                .placeholder(config.getPlaceholder())
                .build();
    }

    /**
     * 转换附件类型配置
     */
    public static AttachmentRenderConfig convertAttachment(AttachmentFieldConfig config) {
        return AttachmentRenderConfig.builder()
                .allowedFileTypes(config.getAllowedFileTypes())
                .maxFileSize(config.getMaxFileSize())
                .maxFileCount(config.getMaxFileCount())
                .build();
    }

    /**
     * 转换关联类型配置
     */
    public static LinkRenderConfig convertLink(LinkFieldConfig config) {
        return LinkRenderConfig.builder()
                .multiple(Boolean.TRUE.equals(config.getMultiple()))
                .build();
    }

    /**
     * 转换架构层级类型配置
     */
    public static StructureRenderConfig convertStructure(StructureFieldConfig config) {
        String structureIdStr = config.getStructureId() != null
                ? config.getStructureId().value()
                : null;
        return StructureRenderConfig.builder()
                .structureId(structureIdStr)
                .leafOnly(config.isLeafOnly())
                .build();
    }

    /**
     * 转换网页链接类型配置
     */
    public static WebUrlRenderConfig convertWebUrl(WebUrlFieldConfig config) {
        return WebUrlRenderConfig.builder()
                .validateUrl(config.isValidateUrl())
                .showPreview(config.isShowPreview())
                .build();
    }
}
