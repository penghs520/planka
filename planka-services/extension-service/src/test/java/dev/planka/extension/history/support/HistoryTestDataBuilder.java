package dev.planka.extension.history.support;

import dev.planka.domain.card.CardTitle;
import dev.planka.domain.field.EnumFieldValue;
import dev.planka.domain.field.NumberFieldValue;
import dev.planka.domain.field.TextFieldValue;
import dev.planka.domain.history.*;
import dev.planka.domain.history.source.UserOperationSource;
import dev.planka.extension.history.service.CardHistoryService.RecordHistoryCommand;
import dev.planka.extension.history.service.CardHistoryService.SearchHistoryQuery;
import dev.planka.event.card.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作历史测试数据构建器
 */
public class HistoryTestDataBuilder {

    // ==================== 常量 ====================
    public static final String TEST_ORG_ID = "test-org-001";
    public static final String TEST_CARD_TYPE_ID = "1234567890123456789";
    public static final Long TEST_CARD_ID = 1001L;
    public static final String TEST_OPERATOR_ID = "operator-001";
    public static final String TEST_OPERATOR_IP = "192.168.1.100";
    public static final String TEST_TRACE_ID = "trace-001";
    public static final String TEST_FIELD_ID = "field-001";
    public static final String TEST_STATUS_ID = "status-001";

    // ==================== RecordHistoryCommand ====================

    public static RecordHistoryCommand createCommand() {
        return createCommand(OperationType.CARD_CREATED);
    }

    public static RecordHistoryCommand createCommand(OperationType operationType) {
        return new RecordHistoryCommand(
                TEST_ORG_ID,
                TEST_CARD_ID,
                TEST_CARD_TYPE_ID,
                operationType,
                TEST_OPERATOR_ID,
                TEST_OPERATOR_IP,
                UserOperationSource.INSTANCE,
                HistoryMessage.of("history.test"),
                TEST_TRACE_ID
        );
    }

    public static RecordHistoryCommand createCommand(OperationType operationType, HistoryMessage message) {
        return new RecordHistoryCommand(
                TEST_ORG_ID,
                TEST_CARD_ID,
                TEST_CARD_TYPE_ID,
                operationType,
                TEST_OPERATOR_ID,
                TEST_OPERATOR_IP,
                UserOperationSource.INSTANCE,
                message,
                TEST_TRACE_ID
        );
    }

    // ==================== SearchHistoryQuery ====================

    public static SearchHistoryQuery createSearchQuery() {
        return new SearchHistoryQuery(
                TEST_CARD_TYPE_ID,
                TEST_CARD_ID,
                null,
                null,
                null,
                null,
                null,
                false,
                1,
                10
        );
    }

    public static SearchHistoryQuery createSearchQuery(
            List<OperationType> operationTypes,
            List<String> operatorIds,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return new SearchHistoryQuery(
                TEST_CARD_TYPE_ID,
                TEST_CARD_ID,
                operationTypes,
                operatorIds,
                null,
                startTime,
                endTime,
                false,
                1,
                10
        );
    }

    // ==================== CardHistoryRecord ====================

    public static CardHistoryRecord createRecord() {
        return createRecord(OperationType.CARD_CREATED);
    }

    public static CardHistoryRecord createRecord(OperationType operationType) {
        return new CardHistoryRecord(
                new CardHistoryId("history-001"),
                TEST_ORG_ID,
                TEST_CARD_ID,
                TEST_CARD_TYPE_ID,
                operationType,
                TEST_OPERATOR_ID,
                TEST_OPERATOR_IP,
                UserOperationSource.INSTANCE,
                HistoryMessage.of("history.test"),
                TEST_TRACE_ID,
                LocalDateTime.now()
        );
    }

    public static CardHistoryRecord createRecord(OperationType operationType, HistoryMessage message) {
        return new CardHistoryRecord(
                new CardHistoryId("history-001"),
                TEST_ORG_ID,
                TEST_CARD_ID,
                TEST_CARD_TYPE_ID,
                operationType,
                TEST_OPERATOR_ID,
                TEST_OPERATOR_IP,
                UserOperationSource.INSTANCE,
                message,
                TEST_TRACE_ID,
                LocalDateTime.now()
        );
    }

    public static CardHistoryRecord createRecordWithId(String historyId, String operatorId) {
        return new CardHistoryRecord(
                new CardHistoryId(historyId),
                TEST_ORG_ID,
                TEST_CARD_ID,
                TEST_CARD_TYPE_ID,
                OperationType.CARD_CREATED,
                operatorId,
                TEST_OPERATOR_IP,
                UserOperationSource.INSTANCE,
                HistoryMessage.of("history.test"),
                TEST_TRACE_ID,
                LocalDateTime.now()
        );
    }

    // ==================== HistoryMessage ====================

    public static HistoryMessage createSimpleMessage() {
        return HistoryMessage.of("history.test");
    }

    public static HistoryMessage createMessageWithText(String value) {
        return HistoryMessage.of("history.test", HistoryMessage.text(value));
    }

    public static HistoryMessage createFieldUpdateMessage(String fieldId, String oldValue, String newValue) {
        return HistoryMessage.of(
                "history.field.updated",
                HistoryMessage.operateField(fieldId),
                HistoryMessage.textFieldValue(fieldId, oldValue),
                HistoryMessage.textFieldValue(fieldId, newValue)
        );
    }

    public static HistoryMessage createStatusChangeMessage(String fromStatusId, String fromStatusName,
                                                            String toStatusId, String toStatusName) {
        return HistoryMessage.of(
                "history.stream.moved",
                HistoryMessage.status(fromStatusId, fromStatusName),
                HistoryMessage.status(toStatusId, toStatusName)
        );
    }

    public static HistoryMessage createEnumFieldMessage(String fieldId, List<String> optionIds) {
        List<HistoryArgument.EnumFieldValue.EnumOption> options = optionIds.stream()
                .map(id -> HistoryMessage.enumOption(id, id))
                .toList();
        return HistoryMessage.of(
                "history.field.updated",
                HistoryMessage.operateField(fieldId),
                HistoryMessage.enumFieldValue(fieldId, options)
        );
    }

    public static HistoryMessage createLinkFieldMessage(String linkFieldId, List<String> cardIds) {
        List<HistoryArgument.LinkFieldValue.LinkedCardRef> cardRefs = cardIds.stream()
                .map(cardId -> HistoryMessage.linkedCardRef(cardId, TEST_CARD_TYPE_ID))
                .toList();
        return HistoryMessage.of(
                "history.link.added",
                HistoryMessage.operateField(linkFieldId),
                HistoryMessage.linkFieldValue(linkFieldId, cardRefs)
        );
    }

    // ==================== HistoryArgument ====================

    public static HistoryArgument textArg(String value) {
        return HistoryMessage.text(value);
    }

    public static HistoryArgument operateFieldArg(String fieldId) {
        return HistoryMessage.operateField(fieldId);
    }

    public static HistoryArgument statusArg(String statusId, String statusName) {
        return HistoryMessage.status(statusId, statusName);
    }

    public static HistoryArgument textFieldValueArg(String fieldId, String value) {
        return HistoryMessage.textFieldValue(fieldId, value);
    }

    public static HistoryArgument numberFieldValueArg(String fieldId, BigDecimal value) {
        return HistoryMessage.numberFieldValue(fieldId, value);
    }

    // ==================== Card Events ====================

    public static CardCreatedEvent createCardCreatedEvent() {
        CardCreatedEvent event = new CardCreatedEvent(
                TEST_ORG_ID, TEST_OPERATOR_ID, TEST_OPERATOR_IP, TEST_TRACE_ID,
                TEST_CARD_TYPE_ID, String.valueOf(TEST_CARD_ID));
        event.setTitle(CardTitle.pure("测试卡片"));
        return event;
    }

    public static CardCreatedEvent createCardCreatedEvent(String title) {
        CardCreatedEvent event = new CardCreatedEvent(
                TEST_ORG_ID, TEST_OPERATOR_ID, TEST_OPERATOR_IP, TEST_TRACE_ID,
                TEST_CARD_TYPE_ID, String.valueOf(TEST_CARD_ID));
        event.setTitle(title != null ? CardTitle.pure(title) : null);
        return event;
    }

    public static CardArchivedEvent createCardArchivedEvent() {
        return new CardArchivedEvent(
                TEST_ORG_ID, TEST_OPERATOR_ID, TEST_OPERATOR_IP, TEST_TRACE_ID,
                TEST_CARD_TYPE_ID, String.valueOf(TEST_CARD_ID));
    }

    public static CardAbandonedEvent createCardAbandonedEvent() {
        return createCardAbandonedEvent("测试丢弃原因");
    }

    public static CardAbandonedEvent createCardAbandonedEvent(String reason) {
        return new CardAbandonedEvent(
                TEST_ORG_ID, TEST_OPERATOR_ID, TEST_OPERATOR_IP, TEST_TRACE_ID,
                TEST_CARD_TYPE_ID, String.valueOf(TEST_CARD_ID), reason);
    }

    public static CardRestoredEvent createCardRestoredEvent() {
        return new CardRestoredEvent(
                TEST_ORG_ID, TEST_OPERATOR_ID, TEST_OPERATOR_IP, TEST_TRACE_ID,
                TEST_CARD_TYPE_ID, String.valueOf(TEST_CARD_ID));
    }

    public static CardMovedEvent createCardMovedEvent(boolean rollback) {
        CardMovedEvent event = new CardMovedEvent(
                TEST_ORG_ID, TEST_OPERATOR_ID, TEST_OPERATOR_IP, TEST_TRACE_ID,
                TEST_CARD_TYPE_ID, String.valueOf(TEST_CARD_ID));
        event.withStatusChange(
                "step-1", "status-1", "待处理", "WAIT",
                "step-2", "status-2", "处理中", "WORKING",
                rollback
        );
        return event;
    }

    public static CardUpdatedEvent createCardUpdatedEvent() {
        return new CardUpdatedEvent(
                TEST_ORG_ID, TEST_OPERATOR_ID, TEST_OPERATOR_IP, TEST_TRACE_ID,
                TEST_CARD_TYPE_ID, String.valueOf(TEST_CARD_ID));
    }

    public static CardUpdatedEvent createCardUpdatedEventWithTitleChange(String oldTitle, String newTitle) {
        CardUpdatedEvent event = createCardUpdatedEvent();
        event.withTitleChange(oldTitle, newTitle);
        return event;
    }

    public static CardUpdatedEvent createCardUpdatedEventWithDescChange(String oldDesc, String newDesc) {
        CardUpdatedEvent event = createCardUpdatedEvent();
        event.withDescriptionChange(oldDesc, newDesc);
        return event;
    }

    public static CardUpdatedEvent createCardUpdatedEventWithFieldChange(String fieldId,
                                                                          String oldValue, String newValue) {
        CardUpdatedEvent event = createCardUpdatedEvent();
        event.addFieldChange(fieldId,
                new TextFieldValue(fieldId, oldValue),
                new TextFieldValue(fieldId, newValue));
        return event;
    }

    public static CardUpdatedEvent createCardUpdatedEventWithNumberFieldChange(String fieldId,
                                                                                 Double oldValue, Double newValue) {
        CardUpdatedEvent event = createCardUpdatedEvent();
        event.addFieldChange(fieldId,
                new NumberFieldValue(fieldId, oldValue),
                new NumberFieldValue(fieldId, newValue));
        return event;
    }

    public static CardUpdatedEvent createCardUpdatedEventWithEnumFieldChange(String fieldId,
                                                                               List<String> oldValues, List<String> newValues) {
        CardUpdatedEvent event = createCardUpdatedEvent();
        event.addFieldChange(fieldId,
                new EnumFieldValue(fieldId, oldValues),
                new EnumFieldValue(fieldId, newValues));
        return event;
    }

    public static CardLinkUpdatedEvent createCardLinkUpdatedEvent(String linkFieldId, boolean isInitiator) {
        CardLinkUpdatedEvent event = new CardLinkUpdatedEvent(
                TEST_ORG_ID, TEST_OPERATOR_ID, TEST_OPERATOR_IP, TEST_TRACE_ID,
                TEST_CARD_TYPE_ID, String.valueOf(TEST_CARD_ID));
        event.setLinkFieldId(linkFieldId);
        event.setInitiator(isInitiator);
        return event;
    }

    public static CardLinkUpdatedEvent createCardLinkUpdatedEventWithAddedCards(
            String linkFieldId, List<CardLinkUpdatedEvent.LinkedCardRef> addedCards) {
        CardLinkUpdatedEvent event = createCardLinkUpdatedEvent(linkFieldId, true);
        event.setAddedCards(addedCards);
        return event;
    }

    public static CardLinkUpdatedEvent.LinkedCardRef linkedCardRef(String cardId, String cardTypeId) {
        return new CardLinkUpdatedEvent.LinkedCardRef(cardId, null, cardTypeId);
    }

    public static CardLinkUpdatedEvent.LinkedCardRef linkedCardRef(String cardId, String cardTitle, String cardTypeId) {
        return new CardLinkUpdatedEvent.LinkedCardRef(cardId, cardTitle, cardTypeId);
    }
}
