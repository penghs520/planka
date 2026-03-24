package cn.planka.card.service.cardDetail;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.dto.CardDetailResponse;
import cn.planka.api.card.request.Yield;
import cn.planka.api.card.request.YieldField;
import cn.planka.api.card.request.YieldLink;
import cn.planka.api.schema.service.FieldConfigQueryService;
import cn.planka.card.service.core.CardQueryService;
import cn.planka.common.result.Result;
import cn.planka.common.util.SystemSchemaIds;
import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.link.LinkFieldIdUtils;
import cn.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import cn.planka.domain.schema.definition.stream.StatusConfig;
import cn.planka.domain.schema.definition.stream.StepConfig;
import cn.planka.domain.schema.definition.fieldconfig.FieldType;
import cn.planka.domain.schema.definition.stream.ValueStreamDefinition;
import cn.planka.domain.schema.definition.template.CardDetailTemplateDefinition;
import cn.planka.domain.schema.definition.template.detail.DetailHeaderConfig;
import cn.planka.domain.schema.definition.template.detail.FieldItemConfig;
import cn.planka.domain.schema.definition.template.detail.SectionConfig;
import cn.planka.domain.schema.definition.template.detail.TabConfig;
import cn.planka.domain.stream.StatusId;
import cn.planka.infra.cache.schema.SchemaCacheService;
import cn.planka.infra.cache.schema.query.CardDetailTemplateCacheQuery;
import cn.planka.infra.cache.schema.query.ValueStreamCacheQuery;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 卡片详情服务
 * <p>
 * 负责组装卡片详情页所需的所有数据：卡片数据、详情模板、字段渲染配置和字段控制配置
 */
@Service
public class CardDetailService {

    private static final Logger logger = LoggerFactory.getLogger(CardDetailService.class);

    private final CardQueryService cardQueryService;
    private final FieldConfigQueryService fieldConfigQueryService;
    private final FieldControlService fieldControlService;
    private final FieldRenderConfigService fieldRenderConfigService;
    private final SchemaCacheService schemaCacheService;
    private final ValueStreamCacheQuery valueStreamCacheQuery;
    private final CardDetailTemplateCacheQuery cardDetailTemplateCacheQuery;

    public CardDetailService(CardQueryService cardQueryService,
                             FieldConfigQueryService fieldConfigQueryService,
                             FieldControlService fieldControlService,
                             FieldRenderConfigService fieldRenderConfigService,
                             SchemaCacheService schemaCacheService,
                             ValueStreamCacheQuery valueStreamCacheQuery, CardDetailTemplateCacheQuery cardDetailTemplateCacheQuery) {
        this.cardQueryService = cardQueryService;
        this.fieldConfigQueryService = fieldConfigQueryService;
        this.fieldControlService = fieldControlService;
        this.fieldRenderConfigService = fieldRenderConfigService;
        this.schemaCacheService = schemaCacheService;
        this.valueStreamCacheQuery = valueStreamCacheQuery;
        this.cardDetailTemplateCacheQuery = cardDetailTemplateCacheQuery;
    }

    /**
     * 获取卡片详情（含模板、字段渲染配置和字段控制配置）
     *
     * @param cardId     卡片ID
     * @param operatorId 操作人ID
     * @return 卡片详情响应
     */
    public Result<CardDetailResponse> getCardDetail(CardId cardId, String operatorId) {
        try {
            CardId operatorCardId = CardId.of(operatorId);

            // 1. 先用简单的 Yield 获取卡片基本信息（主要是 typeId）
            Result<CardDTO> basicCardResult = cardQueryService.findById(cardId, Yield.basic(), operatorCardId);
            if (!basicCardResult.isSuccess()) {
                return Result.failure(basicCardResult.getCode(), basicCardResult.getMessage());
            }
            CardTypeId cardTypeId = basicCardResult.getData().getTypeId();

            // 2. 获取字段配置
            List<cn.planka.domain.schema.definition.fieldconfig.FieldConfig> domainFieldConfigs =
                    fieldConfigQueryService.getFieldConfigs(cardTypeId.value());

            // 3. 获取详情模板，如果没有则创建默认模板
            CardDetailTemplateDefinition templateDef = getTemplateDefinition(cardTypeId, domainFieldConfigs);

            // 4. 根据模板构建 Yield
            Yield yield = buildYieldFromTemplate(templateDef);

            // 5. 用完整的 Yield 获取卡片数据（包含关联卡片）
            Result<CardDTO> cardResult = cardQueryService.findById(cardId, yield, operatorCardId);
            if (!cardResult.isSuccess()) {
                return Result.failure(cardResult.getCode(), cardResult.getMessage());
            }
            CardDTO card = cardResult.getData();

            // 6. 转换模板为 DTO
            CardDetailResponse.DetailTemplateDTO template = convertToDTO(templateDef);

            // 7. 转换为字段渲染元数据（与列表视图统一的 FieldRenderConfig）
            List<CardDetailResponse.FieldRenderMetaDTO> fieldRenderMetas =
                    fieldRenderConfigService.toRenderMetas(domainFieldConfigs, cardId, operatorCardId);

            // 8. 计算字段控制配置（必填/只读）
            Map<String, CardDetailResponse.FieldControlDTO> fieldControls =
                    fieldControlService.computeFieldControls(domainFieldConfigs, operatorId);

            // 9. 获取实体类型信息
            CardDetailResponse.CardTypeInfoDTO cardTypeInfo = getCardTypeInfo(cardTypeId.value());

            // 10. 获取价值流状态信息（如果卡片有 statusId）
            CardDetailResponse.ValueStreamStatusInfoDTO valueStreamStatusInfo = null;
            if (card.getStatusId() != null) {
                valueStreamStatusInfo = getValueStreamStatusInfo(
                        cardTypeId,
                        card.getStatusId()
                );
            }

            // 11. 组装响应
            CardDetailResponse response = CardDetailResponse.builder()
                    .card(card)
                    .template(template)
                    .fieldRenderMetas(fieldRenderMetas)
                    .fieldControls(fieldControls)
                    .cardTypeInfo(cardTypeInfo)
                    .valueStreamStatusInfo(valueStreamStatusInfo)
                    .build();

            return Result.success(response);
        } catch (Exception e) {
            logger.error("获取卡片详情失败", e);
            return Result.failure("INTERNAL_ERROR", "获取卡片详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取详情模板定义
     * <p>
     * 如果没有配置模板，则根据字段配置创建默认模板
     */
    private CardDetailTemplateDefinition getTemplateDefinition(
            CardTypeId cardTypeId,
            List<cn.planka.domain.schema.definition.fieldconfig.FieldConfig> fieldConfigs) {
        try {
            List<CardDetailTemplateDefinition> templates = cardDetailTemplateCacheQuery.getByCardTypeId(cardTypeId);
            if (CollectionUtils.isNotEmpty(templates)) {
                // 优先选择默认模板，如果没有设置默认则随机返回一个
                return templates.stream()
                        .filter(CardDetailTemplateDefinition::isDefault)
                        .findFirst()
                        .orElse(templates.get(0));
            }
        } catch (Exception e) {
            logger.warn("获取详情模板失败: {}", e.getMessage());
        }
        // 没有配置模板时，创建默认模板
        return createDefaultTemplateDefinition(fieldConfigs);
    }

    /**
     * 根据详情模板构建 Yield
     * <p>
     * 从模板中提取所有字段ID，区分普通字段和关联字段，构建相应的 Yield 配置
     */
    private Yield buildYieldFromTemplate(CardDetailTemplateDefinition templateDef) {
        Yield yield = new Yield();

        // 收集模板中的所有字段ID
        Set<String> fieldIds = new HashSet<>();
        List<String> linkFieldIds = new ArrayList<>();

        if (templateDef != null && templateDef.getTabs() != null) {
            for (TabConfig tab : templateDef.getTabs()) {
                if (tab.getSections() != null) {
                    for (SectionConfig section : tab.getSections()) {
                        if (section.getFieldItems() != null) {
                            for (FieldItemConfig item : section.getFieldItems()) {
                                String fieldConfigId = item.getFieldConfigId();
                                if (fieldConfigId != null && !fieldConfigId.isEmpty()) {
                                    // 判断是否为关联字段
                                    if (LinkFieldIdUtils.isValidFormat(fieldConfigId)) {
                                        linkFieldIds.add(fieldConfigId);
                                    } else {
                                        fieldIds.add(fieldConfigId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 设置普通字段
        YieldField yieldField = new YieldField();
        if (fieldIds.isEmpty() && linkFieldIds.isEmpty()) {
            // 没有模板或模板为空时，返回所有字段
            yieldField.setAllFields(true);
            yieldField.setIncludeDescription(true);
        } else {
            yieldField.setAllFields(false);
            yieldField.setFieldIds(fieldIds);
            // 如果字段中包含描述字段，设置 includeDescription
            yieldField.setIncludeDescription(fieldIds.contains("$description") || fieldIds.contains("description"));
        }
        yield.setField(yieldField);

        // 设置关联字段
        if (!linkFieldIds.isEmpty()) {
            List<YieldLink> yieldLinks = new ArrayList<>();
            for (String linkFieldId : linkFieldIds) {
                YieldLink yieldLink = new YieldLink();
                yieldLink.setLinkFieldId(linkFieldId);
                // 关联卡片只返回基本信息
                yieldLink.setTargetYield(Yield.basic());
                yieldLinks.add(yieldLink);
            }
            yield.setLinks(yieldLinks);
        }

        return yield;
    }

    /**
     * 将领域模型模板转换为 DTO
     */
    private CardDetailResponse.DetailTemplateDTO convertToDTO(CardDetailTemplateDefinition template) {
        // 转换头部配置
        CardDetailResponse.HeaderConfigDTO header = null;
        if (template.getHeader() != null) {
            DetailHeaderConfig h = template.getHeader();
            header = CardDetailResponse.HeaderConfigDTO.builder()
                    .showTypeIcon(h.isShowTypeIcon())
                    .showCardNumber(h.isShowCardNumber())
                    .showStatus(h.isShowStatus())
                    .quickActionFieldIds(h.getQuickActionFieldIds() != null ?
                            h.getQuickActionFieldIds() :
                            Collections.emptyList())
                    .build();
        }

        // 转换标签页
        List<CardDetailResponse.TabConfigDTO> tabs = new ArrayList<>();
        if (template.getTabs() != null) {
            for (TabConfig tab : template.getTabs()) {
                List<CardDetailResponse.SectionConfigDTO> sections = new ArrayList<>();
                if (tab.getSections() != null) {
                    for (SectionConfig section : tab.getSections()) {
                        List<CardDetailResponse.FieldItemConfigDTO> fieldItems = new ArrayList<>();
                        if (section.getFieldItems() != null) {
                            for (FieldItemConfig item : section.getFieldItems()) {
                                fieldItems.add(CardDetailResponse.FieldItemConfigDTO.builder()
                                        .fieldConfigId(item.getFieldConfigId())
                                        .widthPercent(item.getWidthPercent())
                                        .customLabel(item.getCustomLabel())
                                        .startNewRow(Boolean.TRUE.equals(item.getStartNewRow()))
                                        .build());
                            }
                        }
                        sections.add(CardDetailResponse.SectionConfigDTO.builder()
                                .sectionId(section.getSectionId())
                                .name(section.getName())
                                .collapsed(section.isCollapsed())
                                .collapsible(section.isCollapsible())
                                .fieldItems(fieldItems)
                                .build());
                    }
                }
                tabs.add(CardDetailResponse.TabConfigDTO.builder()
                        .tabId(tab.getTabId())
                        .tabType(tab.getTabType() != null ? tab.getTabType().name() : null)
                        .name(tab.getName())
                        .systemTabType(tab.getSystemTabType() != null ? tab.getSystemTabType().name() : null)
                        .fieldRowSpacing(tab.getFieldRowSpacing() != null ? tab.getFieldRowSpacing().name() : null)
                        .sections(sections)
                        .build());
            }
        }

        return CardDetailResponse.DetailTemplateDTO.builder()
                .id(template.getId().value())
                .name(template.getName())
                .header(header)
                .tabs(tabs)
                .build();
    }

    /**
     * 创建默认模板定义
     */
    private CardDetailTemplateDefinition createDefaultTemplateDefinition(
            List<cn.planka.domain.schema.definition.fieldconfig.FieldConfig> fieldConfigs) {
        CardDetailTemplateDefinition template = new CardDetailTemplateDefinition(
                cn.planka.domain.schema.CardDetailTemplateId.of("default_template"),
                null,
                "默认模板");
        template.setDefault(true);
        template.setSystemTemplate(true);

        // 头部配置
        DetailHeaderConfig header = new DetailHeaderConfig();
        header.setShowTypeIcon(true);
        header.setShowCardNumber(true);
        header.setShowStatus(true);
        template.setHeader(header);

        // 创建字段项配置列表
        List<FieldItemConfig> fieldItems = new ArrayList<>();
        int currentRowWidth = 0;

        // 1. 第一行：创建时间、更新时间（各25%），然后强制换行
        FieldItemConfig createdAtItem = new FieldItemConfig();
        createdAtItem.setFieldConfigId("$createdAt");
        createdAtItem.setWidthPercent(25);
        createdAtItem.setCustomLabel("创建时间");
        createdAtItem.setStartNewRow(false);
        fieldItems.add(createdAtItem);
        currentRowWidth += 25;

        FieldItemConfig updatedAtItem = new FieldItemConfig();
        updatedAtItem.setFieldConfigId("$updatedAt");
        updatedAtItem.setWidthPercent(25);
        updatedAtItem.setCustomLabel("更新时间");
        updatedAtItem.setStartNewRow(false);
        fieldItems.add(updatedAtItem);
        // 第一行结束，强制换行，后续字段从新行开始
        currentRowWidth = 0;

        // 2. 自定义字段：按字段类型排列，每行四列（25%）
        // 独占一行的类型：TEXTAREA(多行文本)、MARKDOWN、ATTACHMENT(附件)
        // 排除字段：存档人、回收人
        boolean isFirstField = true;
        if (CollectionUtils.isNotEmpty(fieldConfigs)) {
            for (cn.planka.domain.schema.definition.fieldconfig.FieldConfig fieldConfig : fieldConfigs) {
                String fieldId = fieldConfig.getFieldId().value();

                // 排除存档人和回收人字段
                if (isArchiverOrDiscarderField(fieldId)) {
                    continue;
                }

                FieldType fieldType = fieldConfig.getFieldType();
                boolean isFullWidth = isFullWidthFieldType(fieldType);
                int widthPercent = isFullWidth ? 100 : 25;

                // 判断是否需要换行：
                // - 第一个字段强制换行（从新行开始）
                // - 或者当前行剩余空间不足
                boolean startNewRow = isFirstField || (currentRowWidth + widthPercent) > 100;
                if (startNewRow) {
                    currentRowWidth = 0;
                }

                FieldItemConfig item = new FieldItemConfig();
                item.setFieldConfigId(fieldId);
                item.setWidthPercent(widthPercent);
                item.setStartNewRow(startNewRow);
                fieldItems.add(item);

                currentRowWidth += widthPercent;
                if (currentRowWidth >= 100) {
                    currentRowWidth = 0;
                }
                isFirstField = false;
            }
        }

        // 3. 最后显示描述（独占一行）
        FieldItemConfig descriptionItem = new FieldItemConfig();
        descriptionItem.setFieldConfigId("$description");
        descriptionItem.setWidthPercent(100);
        descriptionItem.setCustomLabel("描述");
        descriptionItem.setStartNewRow(true);
        fieldItems.add(descriptionItem);

        // 区域配置（不设置名称，使用 Tab 页标题作为唯一标题）
        SectionConfig section = new SectionConfig();
        section.setSectionId("basic_section");
        section.setName(null);
        section.setCollapsed(false);
        section.setCollapsible(true);
        section.setFieldItems(fieldItems);

        // 基础信息标签页
        TabConfig basicInfoTab = new TabConfig();
        basicInfoTab.setTabId("basic_info");
        basicInfoTab.setTabType(TabConfig.TabType.SYSTEM);
        basicInfoTab.setName("基础信息");
        basicInfoTab.setSystemTabType(TabConfig.SystemTabType.BASIC_INFO);
        basicInfoTab.setFieldRowSpacing(TabConfig.FieldRowSpacing.NORMAL);
        basicInfoTab.setSections(Collections.singletonList(section));

        // 评论标签页
        TabConfig commentTab = new TabConfig();
        commentTab.setTabId("comment");
        commentTab.setTabType(TabConfig.TabType.SYSTEM);
        commentTab.setName("评论");
        commentTab.setSystemTabType(TabConfig.SystemTabType.COMMENT);
        commentTab.setFieldRowSpacing(TabConfig.FieldRowSpacing.NORMAL);

        // 操作记录标签页
        TabConfig activityLogTab = new TabConfig();
        activityLogTab.setTabId("activity_log");
        activityLogTab.setTabType(TabConfig.TabType.SYSTEM);
        activityLogTab.setName("操作记录");
        activityLogTab.setSystemTabType(TabConfig.SystemTabType.ACTIVITY_LOG);
        activityLogTab.setFieldRowSpacing(TabConfig.FieldRowSpacing.NORMAL);

        template.setTabs(Arrays.asList(basicInfoTab, commentTab, activityLogTab));

        return template;
    }

    /**
     * 判断字段类型是否需要独占一行（100%宽度）
     */
    private boolean isFullWidthFieldType(FieldType fieldType) {
        return fieldType == FieldType.TEXTAREA
                || fieldType == FieldType.MARKDOWN
                || fieldType == FieldType.ATTACHMENT;
    }

    /**
     * 判断是否为存档人或回收人字段
     */
    private boolean isArchiverOrDiscarderField(String fieldId) {
        return fieldId.contains(SystemSchemaIds.LINK_ARCHIVER_PATTERN)
                || fieldId.contains(SystemSchemaIds.LINK_DISCARDER_PATTERN);
    }

    /**
     * 获取实体类型信息
     */
    private CardDetailResponse.CardTypeInfoDTO getCardTypeInfo(String cardTypeId) {
        try {
            return schemaCacheService.getById(cardTypeId)
                    .filter(schema -> schema instanceof CardTypeDefinition)
                    .map(schema -> (CardTypeDefinition) schema)
                    .map(cardType -> CardDetailResponse.CardTypeInfoDTO.builder()
                            .id(cardTypeId)
                            .name(cardType.getName())
                            .build())
                    .orElseGet(() -> {
                        logger.warn("获取实体类型信息失败，cardTypeId: {}", cardTypeId);
                        return CardDetailResponse.CardTypeInfoDTO.builder().id(cardTypeId).build();
                    });
        } catch (Exception e) {
            logger.warn("获取实体类型信息失败: {}", e.getMessage());
        }
        return CardDetailResponse.CardTypeInfoDTO.builder().id(cardTypeId).build();
    }

    /**
     * 获取价值流状态信息
     */
    private CardDetailResponse.ValueStreamStatusInfoDTO getValueStreamStatusInfo(CardTypeId cardTypeId, StatusId statusId) {
        try {
            // 获取价值流定义DTO
            Optional<ValueStreamDefinition> opt = valueStreamCacheQuery.getValueStreamByCardTypeId(cardTypeId);
            if (opt.isPresent()) {
                ValueStreamDefinition stream = opt.get();
                // 遍历阶段和状态查找匹配的状态
                for (StepConfig step : stream.getStepList()) {
                    for (StatusConfig status : step.getStatusList()) {
                        if (statusId.equals(status.getId())) {
                            String stepKind = step.getKind() != null ? step.getKind().name() : null;
                            String color = getStatusColorByStepKind(stepKind, status.getWorkType());
                            return CardDetailResponse.ValueStreamStatusInfoDTO.builder()
                                    .statusId(statusId.value())
                                    .statusName(status.getName())
                                    .stepKind(stepKind)
                                    .color(color)
                                    .build();
                        }
                    }
                }
            }
            logger.warn("获取价值流状态信息失败，cardTypeId: {}, statusId: {}", cardTypeId, statusId);
        } catch (Exception e) {
            logger.warn("获取价值流状态信息失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据阶段类型和工作类型获取状态颜色
     */
    private String getStatusColorByStepKind(String stepKind, cn.planka.domain.stream.StatusWorkType workType) {
        if (stepKind != null) {
            return switch (stepKind) {
                case "TODO" -> "#faad14";       // 待办 - 橙色
                case "IN_PROGRESS" -> "#69b1ff"; // 进行中 - 浅蓝色
                case "DONE" -> "#52c41a";        // 完成 - 绿色
                default -> "#69b1ff";            // 默认浅蓝色
            };
        }
        // 如果没有 stepKind，根据 workType 设置颜色
        if (workType != null) {
            return switch (workType) {
                case WAITING -> "#faad14";    // 等待中 - 橙色
                case WORKING -> "#69b1ff";    // 进行中 - 浅蓝色
            };
        }
        return "#69b1ff"; // 默认浅蓝色
    }

}
