package dev.planka.schema.service.cardtype;

import dev.planka.api.schema.dto.inheritance.FieldConfigListWithSource;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.CardCreatePageTemplateId;
import dev.planka.domain.schema.EntityState;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.fieldconfig.*;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;
import dev.planka.domain.schema.definition.template.CardCreatePageTemplateDefinition;
import dev.planka.domain.schema.definition.template.create.CreatePageFieldItemConfig;
import dev.planka.schema.dto.CreatePageFormVO;
import dev.planka.schema.dto.CreatePageFormVO.*;
import dev.planka.schema.dto.CreatePageTemplateListItemVO;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaQuery;
import dev.planka.api.schema.service.FieldConfigQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 卡片新建页模板服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardCreatePageTemplateService {

    private final SchemaRepository schemaRepository;
    private final SchemaQuery schemaQuery;
    private final FieldConfigQueryService fieldConfigQueryService;

    /**
     * 查询模板列表
     *
     * @param orgId      组织ID
     * @param cardTypeId 卡片类型ID（可选）
     * @return 模板列表
     */
    public Result<List<CreatePageTemplateListItemVO>> list(String orgId, String cardTypeId) {
        List<SchemaDefinition<?>> definitions;

        if (cardTypeId != null && !cardTypeId.isEmpty()) {
            // 按卡片类型筛选
            definitions = schemaQuery.queryBySecondKey(CardTypeId.of(cardTypeId), SchemaType.CARD_CREATE_PAGE_TEMPLATE);
        } else {
            // 查询组织下所有模板
            definitions = schemaQuery.queryAll(orgId, SchemaType.CARD_CREATE_PAGE_TEMPLATE);
        }

        List<CreatePageTemplateListItemVO> voList = definitions.stream()
                .filter(CardCreatePageTemplateDefinition.class::isInstance)
                .map(CardCreatePageTemplateDefinition.class::cast)
                .map(this::toListItemVO)
                .toList();

        return Result.success(voList);
    }

    /**
     * 根据卡片类型ID获取模板列表
     *
     * @param cardTypeId 卡片类型ID
     * @return 模板定义列表
     */
    public Result<List<CardCreatePageTemplateDefinition>> getByCardType(String cardTypeId) {
        List<SchemaDefinition<?>> definitions = schemaQuery.queryBySecondKey(CardTypeId.of(cardTypeId), SchemaType.CARD_CREATE_PAGE_TEMPLATE);

        List<CardCreatePageTemplateDefinition> templates = definitions.stream()
                .filter(CardCreatePageTemplateDefinition.class::isInstance)
                .map(CardCreatePageTemplateDefinition.class::cast)
                .toList();

        return Result.success(templates);
    }

    /**
     * 获取卡片类型的默认新建页模板
     *
     * @param cardTypeId 卡片类型ID
     * @return 默认模板定义，如果不存在则返回null
     */
    public Result<CardCreatePageTemplateDefinition> getDefaultByCardType(String cardTypeId) {
        List<SchemaDefinition<?>> definitions = schemaQuery.queryBySecondKey(CardTypeId.of(cardTypeId), SchemaType.CARD_CREATE_PAGE_TEMPLATE);

        CardCreatePageTemplateDefinition defaultTemplate = definitions.stream()
                .filter(CardCreatePageTemplateDefinition.class::isInstance)
                .map(CardCreatePageTemplateDefinition.class::cast)
                .filter(CardCreatePageTemplateDefinition::isDefault)
                .findFirst()
                .orElse(null);

        return Result.success(defaultTemplate);
    }

    /**
     * 复制模板
     */
    @Transactional
    public Result<CardCreatePageTemplateDefinition> copy(String templateId, String operatorId, String newName) {
        SchemaDefinition<?> source = schemaRepository.findById(templateId).orElse(null);
        if (source == null) {
            return Result.failure("TEMPLATE_NOT_FOUND", "模板不存在");
        }
        if (!(source instanceof CardCreatePageTemplateDefinition sourceTemplate)) {
            return Result.failure("INVALID_TEMPLATE_TYPE", "无效的模板类型");
        }

        CardCreatePageTemplateDefinition copy = new CardCreatePageTemplateDefinition(
                CardCreatePageTemplateId.generate(),
                sourceTemplate.getOrgId(),
                newName
        );
        copy.setDescription(sourceTemplate.getDescription());
        copy.setCardTypeId(sourceTemplate.getCardTypeId());
        copy.setSystemTemplate(false);
        copy.setDefault(false);
        copy.setEnabled(true);

        initializeMetadata(copy, operatorId);
        schemaRepository.save(copy);

        return Result.success(copy);
    }

    private void initializeMetadata(CardCreatePageTemplateDefinition definition, String operatorId) {
        definition.setState(EntityState.ACTIVE);
        definition.setContentVersion(1);
        definition.setStructureVersion("1.0.0");
        LocalDateTime now = LocalDateTime.now();
        definition.setCreatedAt(now);
        definition.setCreatedBy(operatorId);
        definition.setUpdatedAt(now);
        definition.setUpdatedBy(operatorId);
    }

    /**
     * 转换为列表项 VO
     */
    private CreatePageTemplateListItemVO toListItemVO(CardCreatePageTemplateDefinition definition) {
        CreatePageTemplateListItemVO vo = new CreatePageTemplateListItemVO();
        vo.setId(definition.getId().value());
        vo.setOrgId(definition.getOrgId());
        vo.setName(definition.getName());
        vo.setDescription(definition.getDescription());
        vo.setCardTypeId(definition.getCardTypeId() != null ? definition.getCardTypeId().value() : null);
        vo.setSystemTemplate(definition.isSystemTemplate());
        vo.setEnabled(definition.isEnabled());
        vo.setDefault(definition.isDefault());
        vo.setContentVersion(definition.getContentVersion());
        vo.setCreatedAt(definition.getCreatedAt());
        vo.setUpdatedAt(definition.getUpdatedAt());
        return vo;
    }

    /**
     * 设置为默认模板，同时取消该卡片类型下其他模板的默认状态
     */
    @Transactional
    public Result<Void> setDefault(String templateId, String operatorId) {
        SchemaDefinition<?> target = schemaRepository.findById(templateId).orElse(null);
        if (target == null) {
            return Result.failure("TEMPLATE_NOT_FOUND", "模板不存在");
        }
        if (!(target instanceof CardCreatePageTemplateDefinition targetTemplate)) {
            return Result.failure("INVALID_TEMPLATE_TYPE", "无效的模板类型");
        }

        if (targetTemplate.isDefault()) {
            return Result.success();
        }

        List<SchemaDefinition<?>> sameTypeTemplates = schemaQuery.queryBySecondKey(
                targetTemplate.getCardTypeId(),
                SchemaType.CARD_CREATE_PAGE_TEMPLATE
        );

        LocalDateTime now = LocalDateTime.now();
        for (SchemaDefinition<?> def : sameTypeTemplates) {
            if (def instanceof CardCreatePageTemplateDefinition template && template.isDefault()) {
                updateTemplateDefaultStatus(template, false, operatorId, now);
            }
        }

        updateTemplateDefaultStatus(targetTemplate, true, operatorId, now);
        return Result.success();
    }

    private void updateTemplateDefaultStatus(CardCreatePageTemplateDefinition template, boolean isDefault,
                                             String operatorId, LocalDateTime now) {
        template.setDefault(isDefault);
        template.setUpdatedBy(operatorId);
        template.setUpdatedAt(now);
        schemaRepository.save(template);
    }

    // ==================== 表单运行时 API ====================

    /**
     * 获取新建页表单配置
     * <p>
     * 聚合模板布局、字段配置和默认值，返回完整的表单配置供前端渲染。
     *
     * @param cardTypeId 卡片类型ID
     * @return 表单配置 VO
     */
    public Result<CreatePageFormVO> getForm(String cardTypeId) {
        // 1. 获取卡片类型信息
        SchemaDefinition<?> cardTypeDef = schemaRepository.findById(cardTypeId).orElse(null);
        if (!(cardTypeDef instanceof EntityCardType cardType)) {
            return Result.failure("CARD_TYPE_NOT_FOUND", "卡片类型不存在");
        }

        // 2. 获取默认模板（如果不存在，使用虚拟默认模板）
        CardCreatePageTemplateDefinition template = getDefaultTemplateOrNull(cardTypeId);

        // 3. 通过 FieldConfigQueryService 获取完整字段配置（包含继承和默认值）
        Result<FieldConfigListWithSource> fieldListResult = fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId);
        if (!fieldListResult.isSuccess()) {
            return Result.failure(fieldListResult.getCode(), fieldListResult.getMessage());
        }

        // 4. 构建字段配置映射（fieldId -> config）
        Map<String, FieldConfig> fieldConfigMap = new HashMap<>();
        for (FieldConfig config : fieldListResult.getData().getFields()) {
            fieldConfigMap.put(config.getFieldId().value(), config);
        }

        // 5. 构建 FormVO
        CreatePageFormVO formVO = new CreatePageFormVO();
        formVO.setCardTypeId(cardTypeId);
        formVO.setCardTypeName(cardType.getName());

        if (template != null) {
            formVO.setTemplateId(template.getId().value());
            formVO.setTemplateName(template.getName());
            formVO.setFields(buildFieldVOList(template.getFieldItems(), fieldConfigMap));
        } else {
            // 虚拟默认模板：由后端填充标题字段
            List<CreatePageFieldVO> defaultFieldVos = buildDefaultFieldVos(fieldConfigMap);
            formVO.setFields(defaultFieldVos);
        }

        return Result.success(formVO);
    }

    private List<CreatePageFieldVO> buildDefaultFieldVos(Map<String, FieldConfig> fieldConfigMap) {
        List<CreatePageFieldVO> defaultFieldVos = new ArrayList<>();
        CreatePageFieldVO $title = createBuiltinFieldVO("$title", 100);
        if ($title != null) {
            defaultFieldVos.add($title);
        }
        return defaultFieldVos;
    }

    private CardCreatePageTemplateDefinition getDefaultTemplateOrNull(String cardTypeId) {
        List<SchemaDefinition<?>> definitions = schemaQuery.queryBySecondKey(
                CardTypeId.of(cardTypeId), SchemaType.CARD_CREATE_PAGE_TEMPLATE);

        return definitions.stream()
                .filter(CardCreatePageTemplateDefinition.class::isInstance)
                .map(CardCreatePageTemplateDefinition.class::cast)
                .filter(CardCreatePageTemplateDefinition::isDefault)
                .findFirst()
                .orElse(null);
    }

    private List<CreatePageFieldVO> buildFieldVOList(
            List<CreatePageFieldItemConfig> fieldItems,
            Map<String, FieldConfig> fieldConfigMap) {

        List<CreatePageFieldVO> result = new ArrayList<>();

        for (CreatePageFieldItemConfig item : fieldItems) {
            String fieldId = item.getFieldId();
            int widthPercent = item.getWidthPercent() != null ? item.getWidthPercent() : 50;

            FieldConfig config = fieldConfigMap.get(fieldId);
            if (config == null) {
                // 内置字段（如 $title）可能不在 FieldConfig 中，需要特殊处理
                CreatePageFieldVO builtinVO = createBuiltinFieldVO(fieldId, widthPercent);
                if (builtinVO != null) {
                    result.add(builtinVO);
                    continue;
                }
                log.warn("Field config not found for fieldId: {}", fieldId);
                continue;
            }

            CreatePageFieldVO vo = convertFieldConfigToVO(config, widthPercent);
            if (vo != null) {
                result.add(vo);
            }
        }

        return result;
    }

    /**
     * 为内置字段创建默认 VO
     */
    private CreatePageFieldVO createBuiltinFieldVO(String fieldId, int widthPercent) {
        if ("$title".equals(fieldId)) {
            TextFieldVO vo = new TextFieldVO();
            vo.setFieldId(fieldId);
            vo.setName("标题");
            vo.setWidthPercent(widthPercent);
            vo.setRequired(true);
            vo.setReadOnly(false);
            vo.setTextType("SINGLE_LINE");
            return vo;
        }
        // 可以根据需要添加更多内置字段的处理
        return null;
    }

    /**
     * 将 FieldConfig 转换为对应的 VO
     */
    private CreatePageFieldVO convertFieldConfigToVO(FieldConfig config, int widthPercent) {
        FieldType fieldType = config.getFieldType();
        if (fieldType == null) {
            return null;
        }

        return switch (fieldType) {
            case TEXT -> convertTextFieldConfigToVO((SingleLineTextFieldConfig) config, widthPercent);
            case TEXTAREA -> convertTextAreaFieldConfigToVO((MultiLineTextFieldConfig) config, widthPercent);
            case MARKDOWN -> convertMarkdownFieldConfigToVO((MarkdownFieldConfig) config, widthPercent);
            case NUMBER -> convertNumberFieldConfigToVO((NumberFieldConfig) config, widthPercent);
            case DATE -> convertDateFieldConfigToVO((DateFieldConfig) config, widthPercent);
            case ENUM -> convertEnumFieldConfigToVO((EnumFieldConfig) config, widthPercent);
            case ATTACHMENT -> convertAttachmentFieldConfigToVO((AttachmentFieldConfig) config, widthPercent);
            case WEB_URL -> convertWebUrlFieldConfigToVO((WebUrlFieldConfig) config, widthPercent);
            case STRUCTURE -> convertStructureFieldConfigToVO((StructureFieldConfig) config, widthPercent);
            case LINK -> convertLinkFieldConfigToVO((LinkFieldConfig) config, widthPercent);
            default -> null;
        };
    }

    // ==================== FieldConfig 转 VO 方法 ====================

    private TextFieldVO convertTextFieldConfigToVO(SingleLineTextFieldConfig config, int widthPercent) {
        TextFieldVO vo = new TextFieldVO();
        setCommonFieldProperties(vo, config, widthPercent);
        vo.setTextType("SINGLE_LINE");
        vo.setMaxLength(config.getMaxLength());
        vo.setDefaultValue(config.getDefaultValue());
        vo.setPlaceholder(config.getPlaceholder());
        return vo;
    }

    private TextFieldVO convertTextAreaFieldConfigToVO(MultiLineTextFieldConfig config, int widthPercent) {
        TextFieldVO vo = new TextFieldVO();
        setCommonFieldProperties(vo, config, widthPercent);
        vo.setTextType("MULTI_LINE");
        vo.setMaxLength(config.getMaxLength());
        vo.setDefaultValue(config.getDefaultValue());
        vo.setPlaceholder(config.getPlaceholder());
        return vo;
    }

    private TextFieldVO convertMarkdownFieldConfigToVO(MarkdownFieldConfig config, int widthPercent) {
        TextFieldVO vo = new TextFieldVO();
        setCommonFieldProperties(vo, config, widthPercent);
        vo.setTextType("MARKDOWN");
        vo.setMaxLength(config.getMaxLength());
        vo.setDefaultValue(config.getDefaultValue());
        vo.setPlaceholder(config.getPlaceholder());
        return vo;
    }

    private NumberFieldVO convertNumberFieldConfigToVO(NumberFieldConfig config, int widthPercent) {
        NumberFieldVO vo = new NumberFieldVO();
        setCommonFieldProperties(vo, config, widthPercent);
        vo.setMinValue(config.getMinValue());
        vo.setMaxValue(config.getMaxValue());
        vo.setPrecision(config.getPrecision());
        vo.setUnit(config.getUnit());
        vo.setShowThousandSeparator(config.isShowThousandSeparator());
        vo.setDefaultValue(config.getDefaultValue());
        return vo;
    }

    private DateFieldVO convertDateFieldConfigToVO(DateFieldConfig config, int widthPercent) {
        DateFieldVO vo = new DateFieldVO();
        setCommonFieldProperties(vo, config, widthPercent);
        vo.setDateFormat(config.getDateFormat() != null ? config.getDateFormat().name() : null);
        vo.setUseNowAsDefault(config.isUseNowAsDefault());
        return vo;
    }

    private EnumFieldVO convertEnumFieldConfigToVO(EnumFieldConfig config, int widthPercent) {
        EnumFieldVO vo = new EnumFieldVO();
        setCommonFieldProperties(vo, config, widthPercent);
        // 转换枚举选项为 VO 格式
        if (config.getOptions() != null) {
            List<EnumOptionVO> optionVOs = config.getOptions().stream()
                    .map(EnumOptionVO::from)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            vo.setOptions(optionVOs);
            log.info("转换枚举字段选项: fieldId={}, name={}, optionsCount={}",
                    config.getFieldId().value(), config.getName(), optionVOs.size());
            // 打印前3个选项用于调试
            optionVOs.stream().limit(3).forEach(opt ->
                log.info("  选项: id={}, label={}, enabled={}", opt.getId(), opt.getLabel(), opt.isEnabled())
            );
        } else {
            log.warn("枚举字段配置缺少选项: fieldId={}, name={}", config.getFieldId().value(), config.getName());
        }
        vo.setMultiSelect(config.isMultiSelect());
        vo.setDefaultOptionIds(config.getDefaultOptionIds());
        return vo;
    }

    private AttachmentFieldVO convertAttachmentFieldConfigToVO(AttachmentFieldConfig config, int widthPercent) {
        AttachmentFieldVO vo = new AttachmentFieldVO();
        setCommonFieldProperties(vo, config, widthPercent);
        vo.setAllowedFileTypes(config.getAllowedFileTypes());
        vo.setMaxFileSize(config.getMaxFileSize());
        vo.setMaxFileCount(config.getMaxFileCount());
        return vo;
    }

    private WebUrlFieldVO convertWebUrlFieldConfigToVO(WebUrlFieldConfig config, int widthPercent) {
        WebUrlFieldVO vo = new WebUrlFieldVO();
        setCommonFieldProperties(vo, config, widthPercent);
        vo.setValidateUrl(config.isValidateUrl());
        vo.setShowPreview(config.isShowPreview());
        vo.setDefaultUrl(config.getDefaultUrl());
        vo.setDefaultLinkText(config.getDefaultLinkText());
        return vo;
    }

    private StructureFieldVO convertStructureFieldConfigToVO(StructureFieldConfig config, int widthPercent) {
        StructureFieldVO vo = new StructureFieldVO();
        setCommonFieldProperties(vo, config, widthPercent);
        vo.setStructureId(config.getStructureId() != null ? config.getStructureId().value() : null);
        vo.setLeafOnly(config.isLeafOnly());
        return vo;
    }

    private LinkFieldVO convertLinkFieldConfigToVO(LinkFieldConfig config, int widthPercent) {
        LinkFieldVO vo = new LinkFieldVO();
        setCommonFieldProperties(vo, config, widthPercent);
        vo.setMultiple(Boolean.TRUE.equals(config.getMultiple()));
        return vo;
    }

    private void setCommonFieldProperties(CreatePageFieldVO vo, FieldConfig config, int widthPercent) {
        vo.setFieldId(config.getFieldId().value());
        vo.setName(config.getName());
        vo.setWidthPercent(widthPercent);
        vo.setRequired(Boolean.TRUE.equals(config.getRequired()));
        vo.setReadOnly(Boolean.TRUE.equals(config.getReadOnly()));
    }

}

