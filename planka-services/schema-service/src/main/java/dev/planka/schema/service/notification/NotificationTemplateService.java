package dev.planka.schema.service.notification;

import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.api.schema.request.notification.CreateNotificationTemplateRequest;
import dev.planka.api.schema.request.notification.UpdateNotificationTemplateRequest;
import dev.planka.api.schema.vo.notification.NotificationTemplateVO;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.notification.NotificationTemplateDefinition;
import dev.planka.domain.notification.NotificationTemplateId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.domain.expression.TextExpressionTemplate;
import dev.planka.domain.schema.definition.rule.action.RecipientSelector;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaCommonService;
import dev.planka.schema.service.common.SchemaQuery;
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
                .filter(t -> t.getCardTypeId() != null && t.getCardTypeId().value().equals(cardTypeId))
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

        // 设置卡片类型
        template.setCardTypeId(new CardTypeId(request.getCardTypeId()));

        // 设置触发事件
        template.setTriggerEvent(BizRuleDefinition.TriggerEvent.valueOf(request.getTriggerEvent()));

        // 设置通知渠道
        template.setChannels(request.getChannels());

        // 设置标题模板
        template.setTitleTemplate(new TextExpressionTemplate(request.getTitleTemplate()));

        // 设置内容模板
        template.setShortContent(request.getShortContent());
        template.setLongContent(request.getLongContent());

        // 设置接收者选择器
        template.setRecipientSelector(buildRecipientSelector(request.getRecipientSelector()));

        // 设置优先级和启用状态
        template.setPriority(request.getPriority());
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
        template.setCardTypeId(new CardTypeId(request.getCardTypeId()));
        template.setTriggerEvent(BizRuleDefinition.TriggerEvent.valueOf(request.getTriggerEvent()));
        template.setChannels(request.getChannels());
        template.setTitleTemplate(new TextExpressionTemplate(request.getTitleTemplate()));
        template.setShortContent(request.getShortContent());
        template.setLongContent(request.getLongContent());
        template.setRecipientSelector(buildRecipientSelector(request.getRecipientSelector()));
        template.setPriority(request.getPriority());
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
        RecipientSelector.SelectorType selectorType = RecipientSelector.SelectorType.valueOf(request.getSelectorType());

        RecipientSelector selector = new RecipientSelector();
        selector.setSelectorType(selectorType);
        selector.setMemberIds(request.getMemberIds());
        selector.setFieldId(request.getFieldId());

        return selector;
    }

    /**
     * 将通知模板转换为 VO
     */
    private NotificationTemplateVO toVO(NotificationTemplateDefinition template) {
        return NotificationTemplateVO.builder()
                .id(template.getId().value())
                .orgId(template.getOrgId())
                .name(template.getName())
                .cardTypeId(template.getCardTypeId() != null ? template.getCardTypeId().value() : null)
                .triggerEvent(template.getTriggerEvent() != null ? template.getTriggerEvent().name() : null)
                .triggerEventName(template.getTriggerEvent() != null ? TRIGGER_EVENT_NAMES.get(template.getTriggerEvent()) : null)
                .channels(template.getChannels())
                .titleTemplate(template.getTitleTemplate() != null ? template.getTitleTemplate().template() : null)
                .shortContent(template.getShortContent())
                .longContent(template.getLongContent())
                .recipientSelector(toRecipientSelectorVO(template.getRecipientSelector()))
                .priority(template.getPriority())
                .enabled(template.isEnabled())
                .contentVersion(template.getContentVersion())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    /**
     * 将接收者选择器转换为 VO
     */
    private NotificationTemplateVO.RecipientSelectorVO toRecipientSelectorVO(RecipientSelector selector) {
        if (selector == null) {
            return null;
        }

        return NotificationTemplateVO.RecipientSelectorVO.builder()
                .selectorType(selector.getSelectorType() != null ? selector.getSelectorType().name() : null)
                .memberIds(selector.getMemberIds())
                .fieldId(selector.getFieldId())
                .build();
    }
}
