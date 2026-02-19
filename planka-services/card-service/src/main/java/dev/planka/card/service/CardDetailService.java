package dev.planka.card.service;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.dto.CardDetailResponse;
import dev.planka.api.card.request.Yield;
import dev.planka.api.card.request.YieldField;
import dev.planka.api.card.request.YieldLink;
import dev.planka.api.schema.dto.inheritance.FieldConfigListWithSource;
import dev.planka.api.schema.service.FieldConfigQueryService;
import dev.planka.card.service.core.CardService;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkFieldIdUtils;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.stream.StatusConfig;
import dev.planka.domain.schema.definition.stream.StepConfig;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.domain.schema.definition.template.CardDetailTemplateDefinition;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.template.detail.DetailHeaderConfig;
import dev.planka.domain.schema.definition.template.detail.FieldItemConfig;
import dev.planka.domain.schema.definition.template.detail.SectionConfig;
import dev.planka.domain.schema.definition.template.detail.TabConfig;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StatusWorkType;
import dev.planka.infra.cache.schema.SchemaCacheService;
import dev.planka.infra.cache.schema.query.CardDetailTemplateCacheQuery;
import dev.planka.infra.cache.schema.query.ValueStreamCacheQuery;
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

    private final CardService cardService;
    private final FieldConfigQueryService fieldConfigQueryService;
    private final FieldControlService fieldControlService;
    private final FieldRenderConfigService fieldRenderConfigService;
    private final SchemaCacheService schemaCacheService;
    private final ValueStreamCacheQuery valueStreamCacheQuery;
    private final CardDetailTemplateCacheQuery cardDetailTemplateCacheQuery;

    public CardDetailService(CardService cardService,
                             FieldConfigQueryService fieldConfigQueryService,
                             FieldControlService fieldControlService,
                             FieldRenderConfigService fieldRenderConfigService,
                             SchemaCacheService schemaCacheService,
                             ValueStreamCacheQuery valueStreamCacheQuery, CardDetailTemplateCacheQuery cardDetailTemplateCacheQuery) {
        this.cardService = cardService;
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
            Result<CardDTO> basicCardResult = cardService.findById(cardId, Yield.basic(), operatorCardId);
            if (!basicCardResult.isSuccess()) {
                return Result.failure(basicCardResult.getCode(), basicCardResult.getMessage());
            }
            CardTypeId cardTypeId = basicCardResult.getData().getTypeId();

            // 2. 获取详情模板（领域模型）
            CardDetailTemplateDefinition templateDef = getTemplateDefinition(cardTypeId);

            // 3. 根据模板构建 Yield
            Yield yield = buildYieldFromTemplate(templateDef);

            // 4. 用完整的 Yield 获取卡片数据（包含关联卡片）
            Result<CardDTO> cardResult = cardService.findById(cardId, yield, operatorCardId);
            if (!cardResult.isSuccess()) {
                return Result.failure(cardResult.getCode(), cardResult.getMessage());
            }
            CardDTO card = cardResult.getData();

            // 5. 转换模板为 DTO
            CardDetailResponse.DetailTemplateDTO template = templateDef != null ? convertToDTO(templateDef) : createDefaultTemplate();

            // 6. 获取字段配置（领域模型）- 获取该卡片类型的所有字段配置
            List<FieldConfig> domainFieldConfigs =
                    getDomainFieldConfigs(cardTypeId.value());

            // 7. 转换为渲染元数据（与列表视图统一的 FieldRenderConfig）
            // 注意：这里返回所有字段的渲染元数据，不仅仅是模板中的字段
            // 这样前端可以正确显示所有字段的名称，即使某些字段不在模板中
            List<CardDetailResponse.FieldRenderMetaDTO> fieldRenderMetas =
                    fieldRenderConfigService.toRenderMetas(domainFieldConfigs);

            // 调试日志：输出字段渲染元数据
            logger.debug("Card detail - cardId: {}, cardTypeId: {}", cardId, cardTypeId);
            logger.debug("Field render metas count: {}", fieldRenderMetas.size());
            for (CardDetailResponse.FieldRenderMetaDTO meta : fieldRenderMetas) {
                logger.debug("  Field: id={}, name={}", meta.getFieldId(), meta.getName());
            }
            logger.debug("Card fieldValues keys: {}", card.getFieldValues() != null ? card.getFieldValues().keySet() : "null");

            // 8. 计算字段控制配置（必填/只读）
            Map<String, CardDetailResponse.FieldControlDTO> fieldControls =
                    fieldControlService.computeFieldControls(domainFieldConfigs, operatorId);

            // 9. 获取卡片类型信息
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
     * 获取详情模板定义（领域模型）
     */
    private CardDetailTemplateDefinition getTemplateDefinition(CardTypeId cardTypeId) {
        try {
            List<CardDetailTemplateDefinition> templates = cardDetailTemplateCacheQuery.getByCardTypeId(cardTypeId);
            if (CollectionUtils.isEmpty(templates)) {
                return null;
            }
            // 优先选择默认模板
            return templates.stream()
                    .filter(CardDetailTemplateDefinition::isDefault)
                    .findFirst()
                    .orElse(templates.get(0));
        } catch (Exception e) {
            logger.warn("获取详情模板失败: {}", e.getMessage());
        }
        return null;
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
     * 获取领域模型字段配置（用于新的服务调用）
     */
    private List<FieldConfig> getDomainFieldConfigs(String cardTypeId) {
        try {
            logger.info("开始获取字段配置，cardTypeId: {}", cardTypeId);

            // 调用 schema-service 获取卡片类型的字段配置列表
            Result<FieldConfigListWithSource> result =
                    fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId);

            logger.info("字段配置查询结果: success={}, hasData={}",
                    result.isSuccess(),
                    result.getData() != null);

            if (result.isSuccess() && result.getData() != null && result.getData().getFields() != null) {
                logger.info("成功获取 {} 个字段配置", result.getData().getFields().size());
                return result.getData().getFields();
            }

            logger.warn("获取字段配置返回空结果，cardTypeId: {}, message: {}",
                    cardTypeId, result.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("获取字段配置失败，cardTypeId: {}", cardTypeId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 创建默认模板
     */
    private CardDetailResponse.DetailTemplateDTO createDefaultTemplate() {
        // 基本属性区域（内置字段）
        List<CardDetailResponse.FieldItemConfigDTO> basicFieldItems = Arrays.asList(
                CardDetailResponse.FieldItemConfigDTO.builder()
                        .fieldConfigId("$createdAt")
                        .widthPercent(25)
                        .customLabel("创建时间")
                        .build(),
                CardDetailResponse.FieldItemConfigDTO.builder()
                        .fieldConfigId("$updatedAt")
                        .widthPercent(25)
                        .customLabel("更新时间")
                        .build(),
                CardDetailResponse.FieldItemConfigDTO.builder()
                        .fieldConfigId("$description")
                        .widthPercent(100)
                        .customLabel("详情描述")
                        .build()
        );

        CardDetailResponse.SectionConfigDTO basicSection = CardDetailResponse.SectionConfigDTO.builder()
                .sectionId("basic_section")
                .name("基本属性")
                .collapsed(false)
                .collapsible(false)
                .fieldItems(basicFieldItems)
                .build();


        // 基础信息标签页
        CardDetailResponse.TabConfigDTO basicInfoTab = CardDetailResponse.TabConfigDTO.builder()
                .tabId("basic_info")
                .tabType("SYSTEM")
                .name("基础信息")
                .systemTabType("BASIC_INFO")
                .fieldRowSpacing("NORMAL")
                .sections(Collections.singletonList(basicSection))
                .build();

        // 评论标签页
        CardDetailResponse.TabConfigDTO commentTab = CardDetailResponse.TabConfigDTO.builder()
                .tabId("comment")
                .tabType("SYSTEM")
                .name("评论")
                .systemTabType("COMMENT")
                .fieldRowSpacing("NORMAL")
                .build();

        // 操作记录标签页
        CardDetailResponse.TabConfigDTO activityLogTab = CardDetailResponse.TabConfigDTO.builder()
                .tabId("activity_log")
                .tabType("SYSTEM")
                .name("操作记录")
                .systemTabType("ACTIVITY_LOG")
                .fieldRowSpacing("NORMAL")
                .build();

        // 默认头部配置
        CardDetailResponse.HeaderConfigDTO header = CardDetailResponse.HeaderConfigDTO.builder()
                .showTypeIcon(true)
                .showCardNumber(true)
                .showStatus(true)
                .quickActionFieldIds(Collections.emptyList())
                .build();

        return CardDetailResponse.DetailTemplateDTO.builder()
                .id("default_template")
                .name("默认模板")
                .header(header)
                .tabs(Arrays.asList(basicInfoTab, commentTab, activityLogTab))
                .build();
    }

    /**
     * 获取卡片类型信息
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
                        logger.warn("获取卡片类型信息失败，cardTypeId: {}", cardTypeId);
                        return CardDetailResponse.CardTypeInfoDTO.builder().id(cardTypeId).build();
                    });
        } catch (Exception e) {
            logger.warn("获取卡片类型信息失败: {}", e.getMessage());
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
    private String getStatusColorByStepKind(String stepKind, StatusWorkType workType) {
        if (stepKind != null) {
            return switch (stepKind) {
                case "TODO" -> "#faad14";       // 待办 - 橙色
                case "IN_PROGRESS" -> "#69b1ff"; // 进行中 - 浅蓝色
                case "DONE" -> "#52c41a";        // 完成 - 绿色
                case "CANCELLED" -> "#8c8c8c";   // 取消 - 灰色
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
