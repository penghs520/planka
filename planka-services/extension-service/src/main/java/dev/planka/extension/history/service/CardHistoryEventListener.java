package dev.planka.extension.history.service;

import dev.planka.domain.field.*;
import dev.planka.domain.history.HistoryArgument;
import dev.planka.domain.history.HistoryMessage;
import dev.planka.domain.history.OperationType;
import dev.planka.event.card.*;
import dev.planka.event.card.CardLinkUpdatedEvent.LinkedCardRef;
import dev.planka.extension.history.service.CardHistoryService.RecordHistoryCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 卡片历史事件监听器
 * <p>
 * 监听卡片相关事件，并转换为历史记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardHistoryEventListener {

    private final CardHistoryService cardHistoryService;
    private final SchemaNameCache schemaNameCache;

    /**
     * 监听卡片事件
     */
    @KafkaListener(topics = "planka-card-events", groupId = "history-service")
    public void handleCardEvent(CardEvent event) {
        try {
            if (event instanceof CardCreatedEvent e) {
                handleCardCreated(e);
            } else if (event instanceof CardArchivedEvent e) {
                handleCardArchived(e);
            } else if (event instanceof CardAbandonedEvent e) {
                handleCardAbandoned(e);
            } else if (event instanceof CardRestoredEvent e) {
                handleCardRestored(e);
            } else if (event instanceof CardMovedEvent e) {
                handleCardMoved(e);
            } else if (event instanceof CardUpdatedEvent e) {
                handleCardUpdated(e);
            } else if (event instanceof CardLinkUpdatedEvent e) {
                handleCardLinkUpdated(e);
            } else {
                log.debug("忽略未处理的事件类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("处理卡片事件失败: {}", event, e);
        }
    }

    private void handleCardCreated(CardCreatedEvent event) {
        String titleDisplay = event.getTitle() != null ? event.getTitle().getDisplayValue() : "";
        HistoryMessage message = HistoryMessage.of(
                "history.card.created",
                HistoryMessage.text(titleDisplay)
        );

        RecordHistoryCommand command = new RecordHistoryCommand(
                event.getOrgId(),
                Long.parseLong(event.getCardId()),
                event.getCardTypeId(),
                OperationType.CARD_CREATED,
                event.getOperatorId(),
                event.getSourceIp(),
                event.getOperationSource(),
                message,
                event.getTraceId()
        );

        cardHistoryService.recordHistory(command);
        log.debug("记录卡片创建历史: cardId={}", event.getCardId());
    }

    private void handleCardArchived(CardArchivedEvent event) {
        HistoryMessage message = HistoryMessage.of("history.card.archived");

        RecordHistoryCommand command = new RecordHistoryCommand(
                event.getOrgId(),
                Long.parseLong(event.getCardId()),
                event.getCardTypeId(),
                OperationType.CARD_ARCHIVED,
                event.getOperatorId(),
                event.getSourceIp(),
                event.getOperationSource(),
                message,
                event.getTraceId()
        );

        cardHistoryService.recordHistory(command);
        log.debug("记录卡片归档历史: cardId={}", event.getCardId());
    }

    private void handleCardAbandoned(CardAbandonedEvent event) {
        String reason = event.getReason();
        HistoryMessage message;
        if (reason != null && !reason.isBlank()) {
            message = HistoryMessage.of("history.card.abandoned.with_reason", HistoryMessage.text(reason));
        } else {
            message = HistoryMessage.of("history.card.abandoned");
        }

        RecordHistoryCommand command = new RecordHistoryCommand(
                event.getOrgId(),
                Long.parseLong(event.getCardId()),
                event.getCardTypeId(),
                OperationType.CARD_ABANDONED,
                event.getOperatorId(),
                event.getSourceIp(),
                event.getOperationSource(),
                message,
                event.getTraceId()
        );

        cardHistoryService.recordHistory(command);
        log.debug("记录卡片丢弃历史: cardId={}", event.getCardId());
    }

    private void handleCardRestored(CardRestoredEvent event) {
        HistoryMessage message = HistoryMessage.of("history.card.restored");

        RecordHistoryCommand command = new RecordHistoryCommand(
                event.getOrgId(),
                Long.parseLong(event.getCardId()),
                event.getCardTypeId(),
                OperationType.CARD_RESTORED,
                event.getOperatorId(),
                event.getSourceIp(),
                event.getOperationSource(),
                message,
                event.getTraceId()
        );

        cardHistoryService.recordHistory(command);
        log.debug("记录卡片还原历史: cardId={}", event.getCardId());
    }

    private void handleCardMoved(CardMovedEvent event) {
        // 根据是否回滚决定操作类型
        OperationType operationType = event.isRollback() ? OperationType.STREAM_ROLLBACK : OperationType.STREAM_MOVED;

        // 构建历史消息：存储状态ID和名称（名称作为备份）
        String messageKey = event.isRollback() ? "history.stream.rollback" : "history.stream.moved";
        HistoryMessage message = HistoryMessage.of(
                messageKey,
                HistoryMessage.status(event.getFromStatusId(), event.getFromStatusName()),
                HistoryMessage.status(event.getToStatusId(), event.getToStatusName())
        );

        RecordHistoryCommand command = new RecordHistoryCommand(
                event.getOrgId(),
                Long.parseLong(event.getCardId()),
                event.getCardTypeId(),
                operationType,
                event.getOperatorId(),
                event.getSourceIp(),
                event.getOperationSource(),
                message,
                event.getTraceId()
        );

        cardHistoryService.recordHistory(command);
        log.debug("记录卡片移动历史: cardId={}, fromStatus={}, toStatus={}, rollback={}",
                event.getCardId(), event.getFromStatusName(), event.getToStatusName(), event.isRollback());
    }

    private void handleCardUpdated(CardUpdatedEvent event) {
        // 处理标题变更
        if (event.getTitleChange() != null) {
            recordTitleChange(event, event.getTitleChange());
        }

        // 处理描述变更
        if (event.getDescriptionChange() != null) {
            recordDescriptionChange(event, event.getDescriptionChange());
        }

        // 处理自定义属性变更
        if (event.getFieldChanges() != null) {
            for (CardUpdatedEvent.FieldChange change : event.getFieldChanges()) {
                recordCustomFieldChange(event, change);
            }
        }

        log.debug("记录卡片更新历史: cardId={}", event.getCardId());
    }

    /**
     * 记录标题变更历史
     */
    private void recordTitleChange(CardUpdatedEvent event, CardUpdatedEvent.TitleChange change) {
        HistoryMessage message = HistoryMessage.of(
                "history.field.title.updated",
                HistoryMessage.text(change.getOldValue() != null ? change.getOldValue() : ""),
                HistoryMessage.text(change.getNewValue() != null ? change.getNewValue() : "")
        );

        RecordHistoryCommand command = new RecordHistoryCommand(
                event.getOrgId(),
                Long.parseLong(event.getCardId()),
                event.getCardTypeId(),
                OperationType.FIELD_TITLE_UPDATED,
                event.getOperatorId(),
                event.getSourceIp(),
                event.getOperationSource(),
                message,
                event.getTraceId()
        );

        cardHistoryService.recordHistory(command);
    }

    /**
     * 记录描述变更历史
     * <p>
     * 使用 diff 算法计算差异，只记录变化的部分，节省存储空间
     */
    private void recordDescriptionChange(CardUpdatedEvent event, CardUpdatedEvent.DescriptionChange change) {
        String oldValue = change.getOldValue() != null ? change.getOldValue() : "";
        String newValue = change.getNewValue() != null ? change.getNewValue() : "";

        HistoryMessage message = HistoryMessage.of(
                "history.field.desc.updated",
                HistoryMessage.textDiff(oldValue, newValue)
        );

        RecordHistoryCommand command = new RecordHistoryCommand(
                event.getOrgId(),
                Long.parseLong(event.getCardId()),
                event.getCardTypeId(),
                OperationType.FIELD_DESC_UPDATED,
                event.getOperatorId(),
                event.getSourceIp(),
                event.getOperationSource(),
                message,
                event.getTraceId()
        );

        cardHistoryService.recordHistory(command);
    }

    /**
     * 记录自定义属性变更历史
     * <p>
     * 根据 FieldValue 类型创建对应的多态历史参数。
     * 字段名称从 Schema 服务获取并作为备份存储。
     */
    private void recordCustomFieldChange(CardUpdatedEvent event, CardUpdatedEvent.FieldChange change) {
        String fieldId = change.getFieldId();

        // 从 Schema 服务获取字段名称作为备份
        String fieldName = schemaNameCache.getFieldName(fieldId);
        if (fieldName == null) {
            fieldName = fieldId; // 如果获取不到，使用 fieldId 作为后备
        }

        // 根据 FieldValue 类型转换为对应的历史参数
        HistoryArgument oldValueArg = convertFieldValueToArg(fieldId, change.getOldValue());
        HistoryArgument newValueArg = convertFieldValueToArg(fieldId, change.getNewValue());

        HistoryMessage message = HistoryMessage.of(
                "history.field.updated",
                HistoryMessage.operateField(fieldId),
                oldValueArg,
                newValueArg
        );

        RecordHistoryCommand command = new RecordHistoryCommand(
                event.getOrgId(),
                Long.parseLong(event.getCardId()),
                event.getCardTypeId(),
                OperationType.FIELD_CUSTOM_UPDATED,
                event.getOperatorId(),
                event.getSourceIp(),
                event.getOperationSource(),
                message,
                event.getTraceId()
        );

        cardHistoryService.recordHistory(command);
    }

    /**
     * 将 FieldValue 转换为对应的历史参数
     * <p>
     * FieldValueArg 只存储 fieldId，不存储 fieldName（由 OperateFieldArg 提供）
     */
    private HistoryArgument convertFieldValueToArg(String fieldId, FieldValue<?> fieldValue) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            // 空值使用文本类型表示
            return HistoryMessage.textFieldValue(fieldId, null);
        }

        if (fieldValue instanceof TextFieldValue textValue) {
            return HistoryMessage.textFieldValue(fieldId, textValue.getValue());
        } else if (fieldValue instanceof NumberFieldValue numberValue) {
            Double value = numberValue.getValue();
            return HistoryMessage.numberFieldValue(fieldId,
                    value != null ? BigDecimal.valueOf(value) : null);
        } else if (fieldValue instanceof DateFieldValue dateValue) {
            Long timestamp = dateValue.getValue();
            if (timestamp != null) {
                // 将时间戳转换为 LocalDateTime（使用日期时间类型以保留完整时间信息）
                java.time.LocalDateTime localDateTime = java.time.Instant.ofEpochMilli(timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
                return HistoryMessage.dateTimeFieldValue(fieldId, localDateTime);
            }
            return HistoryMessage.dateTimeFieldValue(fieldId, null);
        } else if (fieldValue instanceof EnumFieldValue enumValue) {
            List<String> optionIds = enumValue.getValue();
            if (optionIds == null || optionIds.isEmpty()) {
                return HistoryMessage.enumFieldValue(fieldId, Collections.emptyList());
            }
            // 枚举值只存储 ID，名称暂时使用 ID（后续查询时填充）
            List<HistoryArgument.EnumFieldValue.EnumOption> options = optionIds.stream()
                    .map(id -> HistoryMessage.enumOption(id, id))
                    .collect(Collectors.toList());
            return HistoryMessage.enumFieldValue(fieldId, options);
        } else if (fieldValue instanceof StructureFieldValue structureValue) {
            StructureItem item = structureValue.getValue();
            if (item == null) {
                return HistoryMessage.structureFieldValue(fieldId, Collections.emptyList());
            }
            // 将链表结构转换为路径列表
            List<HistoryArgument.StructureFieldValue.StructureNode> path = new java.util.ArrayList<>();
            StructureItem current = item;
            while (current != null) {
                path.add(HistoryMessage.structureNode(current.getId(), current.getName()));
                current = current.getNext();
            }
            return HistoryMessage.structureFieldValue(fieldId, path);
        } else {
            // 其他类型使用文本表示
            Object value = fieldValue.getValue();
            return HistoryMessage.textFieldValue(fieldId,
                    value != null ? value.toString() : null);
        }
    }

    // ==================== 关联变更事件处理 ====================

    /**
     * 处理卡片关联更新事件
     */
    private void handleCardLinkUpdated(CardLinkUpdatedEvent event) {
        String linkFieldId = event.getLinkFieldId();
        boolean isInitiator = event.isInitiator();

        // 处理添加的关联
        if (event.getAddedCards() != null && !event.getAddedCards().isEmpty()) {
            recordLinkAdded(event, linkFieldId, event.getAddedCards(), isInitiator);
        }

        // 处理删除的关联
        if (event.getRemovedCards() != null && !event.getRemovedCards().isEmpty()) {
            recordLinkRemoved(event, linkFieldId, event.getRemovedCards(), isInitiator);
        }

        log.debug("记录卡片关联更新历史: cardId={}, linkFieldId={}, initiator={}, added={}, removed={}",
                event.getCardId(), linkFieldId, isInitiator,
                event.getAddedCards() != null ? event.getAddedCards().size() : 0,
                event.getRemovedCards() != null ? event.getRemovedCards().size() : 0);
    }

    /**
     * 记录添加关联历史
     *
     * @param isInitiator 是否为主动关联方
     */
    private void recordLinkAdded(CardLinkUpdatedEvent event, String linkFieldId,
                                 List<LinkedCardRef> addedCards,
                                 boolean isInitiator) {
        // 构建关联卡片引用列表
        List<HistoryArgument.LinkFieldValue.LinkedCardRef> cardRefs = addedCards.stream()
                .map(card -> HistoryMessage.linkedCardRef(card.getCardId(), card.getCardTypeId()))
                .toList();

        // 根据是否主动方选择不同的消息 key
        // 主动方：history.link.added - "添加关联「xxx」：卡片1、卡片2"
        // 被动方：history.link.added.passive - "被添加关联「xxx」：由 卡片A"
        String messageKey = isInitiator ? "history.link.added" : "history.link.added.passive";

        HistoryMessage message = HistoryMessage.of(
                messageKey,
                HistoryMessage.operateField(linkFieldId),
                HistoryMessage.linkFieldValue(linkFieldId, cardRefs)
        );

        RecordHistoryCommand command = new RecordHistoryCommand(
                event.getOrgId(),
                Long.parseLong(event.getCardId()),
                event.getCardTypeId(),
                OperationType.LINK_ADDED,
                event.getOperatorId(),
                event.getSourceIp(),
                event.getOperationSource(),
                message,
                event.getTraceId()
        );

        cardHistoryService.recordHistory(command);
    }

    /**
     * 记录删除关联历史
     *
     * @param isInitiator 是否为主动关联方
     */
    private void recordLinkRemoved(CardLinkUpdatedEvent event, String linkFieldId,
                                   List<LinkedCardRef> removedCards,
                                   boolean isInitiator) {
        // 构建关联卡片引用列表
        List<HistoryArgument.LinkFieldValue.LinkedCardRef> cardRefs = removedCards.stream()
                .map(card -> HistoryMessage.linkedCardRef(card.getCardId(), card.getCardTypeId()))
                .toList();

        // 根据是否主动方选择不同的消息 key
        // 主动方：history.link.removed - "移除关联「xxx」：卡片1、卡片2"
        // 被动方：history.link.removed.passive - "被移除关联「xxx」：由 卡片A"
        String messageKey = isInitiator ? "history.link.removed" : "history.link.removed.passive";

        HistoryMessage message = HistoryMessage.of(
                messageKey,
                HistoryMessage.operateField(linkFieldId),
                HistoryMessage.linkFieldValue(linkFieldId, cardRefs)
        );

        RecordHistoryCommand command = new RecordHistoryCommand(
                event.getOrgId(),
                Long.parseLong(event.getCardId()),
                event.getCardTypeId(),
                OperationType.LINK_REMOVED,
                event.getOperatorId(),
                event.getSourceIp(),
                event.getOperationSource(),
                message,
                event.getTraceId()
        );

        cardHistoryService.recordHistory(command);
    }
}
