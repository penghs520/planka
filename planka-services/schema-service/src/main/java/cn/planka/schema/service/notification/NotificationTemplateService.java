package cn.planka.schema.service.notification;

import cn.planka.api.schema.request.CreateSchemaRequest;
import cn.planka.api.schema.request.UpdateSchemaRequest;
import cn.planka.api.schema.request.notification.CreateNotificationTemplateRequest;
import cn.planka.api.schema.request.notification.UpdateNotificationTemplateRequest;
import cn.planka.api.schema.vo.notification.NotificationTemplateVO;
import cn.planka.common.exception.CommonErrorCode;
import cn.planka.common.result.Result;
import cn.planka.domain.notification.NotificationTemplateDefinition;
import cn.planka.domain.notification.NotificationTemplateId;
import cn.planka.domain.notification.NotificationContent;
import cn.planka.domain.notification.ShortNotificationContent;
import cn.planka.domain.notification.LongNotificationContent;
import cn.planka.domain.notification.CardTypeDefinitionParameter;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.rule.BizRuleDefinition;
import cn.planka.domain.expression.TextExpressionTemplate;
import cn.planka.domain.schema.definition.rule.action.RecipientSelector;
import cn.planka.schema.repository.SchemaRepository;
import cn.planka.schema.service.common.SchemaCommonService;
import cn.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通知模板服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final SchemaRepository schemaRepository;
    private final SchemaCommonService schemaCommonService;
    private final SchemaQuery schemaQuery;

    /**
     * 触发事件名称映射
     */
    private static final Map<BizRuleDefinition.TriggerEvent, String> TRIGGER_EVENT_NAMES = Map.of(
            BizRuleDefinition.TriggerEvent.ON_CREATE, "创建时",
            BizRuleDefinition.TriggerEvent.ON_DISCARD, "丢弃时",
            BizRuleDefinition.TriggerEvent.ON_ARCHIVE, "归档时",
            BizRuleDefinition.TriggerEvent.ON_RESTORE, "还原时",
            BizRuleDefinition.TriggerEvent.ON_STATUS_MOVE, "状态前进时",
            BizRuleDefinition.TriggerEvent.ON_STATUS_ROLLBACK, "状态回退时",
            BizRuleDefinition.TriggerEvent.ON_FIELD_CHANGE, "字段变更时",
            BizRuleDefinition.TriggerEvent.ON_SCHEDULE, "定时触发"
    );

    /**
     * 查询通知模板列表
     */
    public Result<List<NotificationTemplateVO>> list(String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.NOTIFICATION_TEMPLATE);

        List<NotificationTemplateVO> voList = schemas.stream()
                .filter(s -> s instanceof NotificationTemplateDefinition)
                .map(s -> toVO((NotificationTemplateDefinition) s))
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 根据卡片类型查询通知模板列表
     */
    public Result<List<NotificationTemplateVO>> listByCardType(String orgId, String cardTypeId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.NOTIFICATION_TEMPLATE);

        List<NotificationTemplateVO> voList = schemas.stream()
                .filter(s -> s instanceof NotificationTemplateDefinition)
                .map(s -> (NotificationTemplateDefinition) s)
                .filter(t -> {
                    // 只过滤卡片类型参数的模板
                    if (t.getDefinitionParameter() instanceof CardTypeDefinitionParameter cardTypeParam) {
                        return cardTypeParam.getCardTypeId() != null
                                && cardTypeParam.getCardTypeId().value().equals(cardTypeId);
                    }
                    return false;
                })
                .map(this::toVO)
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 根据 ID 获取通知模板详情
     */
    public Result<NotificationTemplateVO> getById(String id) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(id);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "通知模板不存在: " + id);
        }

        SchemaDefinition<?> schema = schemaOpt.get();
        if (!(schema instanceof NotificationTemplateDefinition template)) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "非通知模板: " + id);
        }

        return Result.success(toVO(template));
    }

    /**
     * 创建通知模板
     */
    @Transactional
    public Result<NotificationTemplateVO> create(String orgId, String operatorId, CreateNotificationTemplateRequest request) {
        NotificationTemplateDefinition template = new NotificationTemplateDefinition(
                NotificationTemplateId.generate(),
                orgId,
                request.getName()
        );

        // 设置模板类型
        template.setTemplateType(request.getTemplateType());

        // 设置定义参数
        template.setDefinitionParameter(request.getDefinitionParameter());

        // 设置触发事件
        template.setTriggerEvent(BizRuleDefinition.TriggerEvent.valueOf(request.getTriggerEvent()));

        // 设置通知渠道
        template.setChannels(request.getChannels());

        // 设置标题模板
        template.setTitleTemplate(new TextExpressionTemplate(request.getTitleTemplate()));

        // 设置内容模板
        template.setContent(buildNotificationContent(request.getContent()));

        // 设置接收者选择器
        template.setRecipientSelector(buildRecipientSelector(request.getRecipientSelector()));

        // 设置通知对象类型
        if (request.getRecipientType() != null) {
            template.setRecipientType(cn.planka.domain.notification.RecipientType.valueOf(request.getRecipientType()));
        }

        // 设置启用状态
        template.setEnabled(true);

        CreateSchemaRequest schemaRequest = new CreateSchemaRequest();
        schemaRequest.setDefinition(template);
        Result<SchemaDefinition<?>> createResult = schemaCommonService.create(orgId, operatorId, schemaRequest);

        if (!createResult.isSuccess()) {
            return Result.failure(createResult.getCode(), createResult.getMessage());
        }

        NotificationTemplateDefinition savedTemplate = (NotificationTemplateDefinition) createResult.getData();
        log.info("NotificationTemplate created: id={}, name={}", savedTemplate.getId().value(), savedTemplate.getName());
        return Result.success(toVO(savedTemplate));
    }

    /**
     * 更新通知模板
     */
    @Transactional
    public Result<NotificationTemplateVO> update(String id, String operatorId, UpdateNotificationTemplateRequest request) {
        Optional<SchemaDefinition<?>> schemaOpt = schemaRepository.findById(id);
        if (schemaOpt.isEmpty()) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "通知模板不存在");
        }

        if (!(schemaOpt.get() instanceof NotificationTemplateDefinition template)) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "非通知模板");
        }

        // 更新基本信息
        template.setName(request.getName());
        if (request.getTemplateType() != null) {
            template.setTemplateType(request.getTemplateType());
        }
        template.setDefinitionParameter(request.getDefinitionParameter());
        template.setTriggerEvent(BizRuleDefinition.TriggerEvent.valueOf(request.getTriggerEvent()));
        template.setChannels(request.getChannels());
        template.setTitleTemplate(new TextExpressionTemplate(request.getTitleTemplate()));
        template.setContent(buildNotificationContent(request.getContent()));
        template.setRecipientSelector(buildRecipientSelector(request.getRecipientSelector()));
        // 设置通知对象类型
        if (request.getRecipientType() != null) {
            template.setRecipientType(cn.planka.domain.notification.RecipientType.valueOf(request.getRecipientType()));
        }
        template.setEnabled(request.isEnabled());

        UpdateSchemaRequest schemaRequest = new UpdateSchemaRequest();
        schemaRequest.setDefinition(template);
        schemaRequest.setExpectedVersion(request.getExpectedVersion());
        Result<SchemaDefinition<?>> updateResult = schemaCommonService.update(id, operatorId, schemaRequest);

        if (!updateResult.isSuccess()) {
            return Result.failure(updateResult.getCode(), updateResult.getMessage());
        }

        NotificationTemplateDefinition savedTemplate = (NotificationTemplateDefinition) updateResult.getData();
        log.info("NotificationTemplate updated: id={}, version={}", savedTemplate.getId().value(), savedTemplate.getContentVersion());
        return Result.success(toVO(savedTemplate));
    }

    /**
     * 删除通知模板
     */
    @Transactional
    public Result<Void> delete(String id, String operatorId) {
        return schemaCommonService.delete(id, operatorId);
    }

    /**
     * 启用通知模板
     */
    @Transactional
    public Result<Void> activate(String id, String operatorId) {
        return schemaCommonService.activate(id, operatorId);
    }

    /**
     * 停用通知模板
     */
    @Transactional
    public Result<Void> disable(String id, String operatorId) {
        return schemaCommonService.disable(id, operatorId);
    }

    /**
     * 构建接收者选择器
     */
    private RecipientSelector buildRecipientSelector(CreateNotificationTemplateRequest.RecipientSelectorRequest request) {
        if (request == null) {
            return null;
        }

        RecipientSelector selector = new RecipientSelector();

        // 优先使用多选择器模式
        if (request.getSelectors() != null && !request.getSelectors().isEmpty()) {
            List<RecipientSelector.SelectorItem> items = request.getSelectors().stream()
                    .map(itemRequest -> {
                        RecipientSelector.SelectorItem item = new RecipientSelector.SelectorItem();
                        if (itemRequest.getSelectorType() != null) {
                            item.setSelectorType(RecipientSelector.SelectorType.valueOf(itemRequest.getSelectorType()));
                        }
                        item.setMemberIds(itemRequest.getMemberIds());
                        item.setFieldId(itemRequest.getFieldId());
                        item.setSource(itemRequest.getSource());
                        return item;
                    })
                    .collect(java.util.stream.Collectors.toList());
            selector.setSelectors(items);
        } else if (request.getSelectorType() != null) {
            // 向后兼容：单选择器模式
            RecipientSelector.SelectorType selectorType = RecipientSelector.SelectorType.valueOf(request.getSelectorType());
            selector.setSelectorType(selectorType);
            selector.setMemberIds(request.getMemberIds());
            selector.setFieldId(request.getFieldId());
            selector.setFieldIds(request.getFieldIds());
        }

        return selector;
    }

    /**
     * 构建通知内容
     */
    private NotificationContent buildNotificationContent(CreateNotificationTemplateRequest.NotificationContentRequest request) {
        if (request == null) {
            return null;
        }
        if ("SHORT".equals(request.getType())) {
            return ShortNotificationContent.of(request.getTextTemplate());
        } else if ("LONG".equals(request.getType())) {
            return LongNotificationContent.of(
                    buildRecipientSelector(request.getCcSelector()),
                    request.getRichTextTemplate()
            );
        }
        throw new IllegalArgumentException("不支持的内容类型: " + request.getType());
    }

    /**
     * 将通知模板转换为 VO
     */
    private NotificationTemplateVO toVO(NotificationTemplateDefinition template) {
        // 提取卡片类型信息（兼容）
        String cardTypeId = null;
        if (template.getDefinitionParameter() instanceof CardTypeDefinitionParameter cardTypeParam) {
            cardTypeId = cardTypeParam.getCardTypeId() != null ? cardTypeParam.getCardTypeId().value() : null;
        }

        return NotificationTemplateVO.builder()
                .id(template.getId().value())
                .orgId(template.getOrgId())
                .name(template.getName())
                .templateType(template.getTemplateType())
                .definitionParameter(template.getDefinitionParameter())
                .cardTypeId(cardTypeId)
                .triggerEvent(template.getTriggerEvent() != null ? template.getTriggerEvent().name() : null)
                .triggerEventName(template.getTriggerEvent() != null ? TRIGGER_EVENT_NAMES.get(template.getTriggerEvent()) : null)
                .recipientType(template.getRecipientType() != null ? template.getRecipientType().name() : null)
                .channels(template.getChannels())
                .titleTemplate(template.getTitleTemplate() != null ? template.getTitleTemplate().template() : null)
                .content(toContentVO(template.getContent()))
                .recipientSelector(toRecipientSelectorVO(template.getRecipientSelector()))
                .enabled(template.isEnabled())
                .contentVersion(template.getContentVersion())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    /**
     * 将通知内容转换为 VO
     */
    private NotificationTemplateVO.NotificationContentVO toContentVO(NotificationContent content) {
        if (content == null) {
            return null;
        }
        if (content instanceof ShortNotificationContent shortContent) {
            return NotificationTemplateVO.NotificationContentVO.builder()
                    .type("SHORT")
                    .textTemplate(shortContent.getTextTemplate() != null ? shortContent.getTextTemplate().template() : null)
                    .build();
        } else if (content instanceof LongNotificationContent longContent) {
            return NotificationTemplateVO.NotificationContentVO.builder()
                    .type("LONG")
                    .richTextTemplate(longContent.getRichTextTemplate() != null ? longContent.getRichTextTemplate().template() : null)
                    .ccSelector(toRecipientSelectorVO(longContent.getCcSelector()))
                    .build();
        }
        return null;
    }

    /**
     * 将接收者选择器转换为 VO
     */
    private NotificationTemplateVO.RecipientSelectorVO toRecipientSelectorVO(RecipientSelector selector) {
        if (selector == null) {
            return null;
        }

        // 转换 selectors 列表
        List<NotificationTemplateVO.SelectorItemVO> selectorsVO = null;
        if (selector.getSelectors() != null && !selector.getSelectors().isEmpty()) {
            selectorsVO = selector.getSelectors().stream()
                    .map(item -> NotificationTemplateVO.SelectorItemVO.builder()
                            .selectorType(item.getSelectorType() != null ? item.getSelectorType().name() : null)
                            .memberIds(item.getMemberIds())
                            .fieldId(item.getFieldId())
                            .source(item.getSource())
                            .build())
                    .collect(java.util.stream.Collectors.toList());
        }

        return NotificationTemplateVO.RecipientSelectorVO.builder()
                .selectors(selectorsVO)
                .selectorType(selector.getSelectorType() != null ? selector.getSelectorType().name() : null)
                .memberIds(selector.getMemberIds())
                .fieldId(selector.getFieldId())
                .fieldIds(selector.getFieldIds())
                .build();
    }
}
