package dev.planka.extension.history.service;

import dev.planka.common.result.Result;
import dev.planka.domain.field.TextFieldValue;
import dev.planka.domain.history.CardHistoryId;
import dev.planka.domain.history.HistoryArgument;
import dev.planka.domain.history.OperationType;
import dev.planka.extension.history.service.CardHistoryService.RecordHistoryCommand;
import dev.planka.event.card.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static dev.planka.extension.history.support.HistoryTestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * CardHistoryEventListener 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CardHistoryEventListenerTest {

    @Mock
    private CardHistoryService cardHistoryService;

    @Mock
    private SchemaNameCache schemaNameCache;

    @Captor
    private ArgumentCaptor<RecordHistoryCommand> commandCaptor;

    private CardHistoryEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new CardHistoryEventListener(cardHistoryService, schemaNameCache);
        // 默认返回成功结果
        lenient().when(cardHistoryService.recordHistory(any()))
                .thenReturn(Result.success(new CardHistoryId("test-id")));
    }

    // ==================== handleCardEvent 事件分发测试 ====================

    @Nested
    @DisplayName("handleCardEvent - 事件分发")
    class HandleCardEventTests {

        @Test
        @DisplayName("CardCreatedEvent 正确分发")
        void shouldDispatchCardCreatedEvent() {
            // Given
            CardCreatedEvent event = createCardCreatedEvent();

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(argThat(cmd ->
                    cmd.operationType() == OperationType.CARD_CREATED
            ));
        }

        @Test
        @DisplayName("CardArchivedEvent 正确分发")
        void shouldDispatchCardArchivedEvent() {
            // Given
            CardArchivedEvent event = createCardArchivedEvent();

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(argThat(cmd ->
                    cmd.operationType() == OperationType.CARD_ARCHIVED
            ));
        }

        @Test
        @DisplayName("CardAbandonedEvent 正确分发")
        void shouldDispatchCardAbandonedEvent() {
            // Given
            CardAbandonedEvent event = createCardAbandonedEvent();

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(argThat(cmd ->
                    cmd.operationType() == OperationType.CARD_ABANDONED
            ));
        }

        @Test
        @DisplayName("CardRestoredEvent 正确分发")
        void shouldDispatchCardRestoredEvent() {
            // Given
            CardRestoredEvent event = createCardRestoredEvent();

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(argThat(cmd ->
                    cmd.operationType() == OperationType.CARD_RESTORED
            ));
        }

        @Test
        @DisplayName("CardMovedEvent 正确分发")
        void shouldDispatchCardMovedEvent() {
            // Given
            CardMovedEvent event = createCardMovedEvent(false);

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(argThat(cmd ->
                    cmd.operationType() == OperationType.STREAM_MOVED
            ));
        }

        @Test
        @DisplayName("CardUpdatedEvent 正确分发")
        void shouldDispatchCardUpdatedEvent() {
            // Given
            CardUpdatedEvent event = createCardUpdatedEventWithTitleChange("旧标题", "新标题");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(argThat(cmd ->
                    cmd.operationType() == OperationType.FIELD_TITLE_UPDATED
            ));
        }

        @Test
        @DisplayName("CardLinkUpdatedEvent 正确分发")
        void shouldDispatchCardLinkUpdatedEvent() {
            // Given
            CardLinkUpdatedEvent event = createCardLinkUpdatedEventWithAddedCards(
                    "linkType-1:SOURCE",
                    List.of(linkedCardRef("card-1", TEST_CARD_TYPE_ID))
            );

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(argThat(cmd ->
                    cmd.operationType() == OperationType.LINK_ADDED
            ));
        }

        @Test
        @DisplayName("处理异常不抛出，只记录日志")
        void shouldCatchExceptionAndLog() {
            // Given
            CardCreatedEvent event = createCardCreatedEvent();
            when(cardHistoryService.recordHistory(any()))
                    .thenThrow(new RuntimeException("模拟异常"));

            // When & Then - 不应抛出异常
            assertThatCode(() -> eventListener.handleCardEvent(event))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== handleCardCreated 测试 ====================

    @Nested
    @DisplayName("handleCardCreated - 卡片创建")
    class HandleCardCreatedTests {

        @Test
        @DisplayName("构建正确的 RecordHistoryCommand")
        void shouldBuildCorrectCommand() {
            // Given
            CardCreatedEvent event = createCardCreatedEvent("测试卡片");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            assertThat(cmd.orgId()).isEqualTo(TEST_ORG_ID);
            assertThat(cmd.cardId()).isEqualTo(TEST_CARD_ID);
            assertThat(cmd.cardTypeId()).isEqualTo(TEST_CARD_TYPE_ID);
            assertThat(cmd.operationType()).isEqualTo(OperationType.CARD_CREATED);
            assertThat(cmd.operatorId()).isEqualTo(TEST_OPERATOR_ID);
        }

        @Test
        @DisplayName("消息包含卡片标题")
        void shouldIncludeCardTitleInMessage() {
            // Given
            CardCreatedEvent event = createCardCreatedEvent("我的卡片标题");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            assertThat(cmd.message().getMessageKey()).isEqualTo("history.card.created");
            assertThat(cmd.message().getArgs()).hasSize(1);
            assertThat(cmd.message().getArgs().get(0)).isInstanceOf(HistoryArgument.TextArg.class);
            assertThat(((HistoryArgument.TextArg) cmd.message().getArgs().get(0)).value())
                    .isEqualTo("我的卡片标题");
        }

        @Test
        @DisplayName("标题为 null 时使用空字符串")
        void shouldUseEmptyStringWhenTitleNull() {
            // Given
            CardCreatedEvent event = createCardCreatedEvent(null);

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            assertThat(((HistoryArgument.TextArg) cmd.message().getArgs().get(0)).value())
                    .isEqualTo("");
        }
    }

    // ==================== handleCardMoved 测试 ====================

    @Nested
    @DisplayName("handleCardMoved - 卡片移动")
    class HandleCardMovedTests {

        @Test
        @DisplayName("非回滚操作使用 STREAM_MOVED 类型")
        void shouldUseStreamMovedTypeForNonRollback() {
            // Given
            CardMovedEvent event = createCardMovedEvent(false);

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            assertThat(commandCaptor.getValue().operationType()).isEqualTo(OperationType.STREAM_MOVED);
        }

        @Test
        @DisplayName("回滚操作使用 STREAM_ROLLBACK 类型")
        void shouldUseStreamRollbackTypeForRollback() {
            // Given
            CardMovedEvent event = createCardMovedEvent(true);

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            assertThat(commandCaptor.getValue().operationType()).isEqualTo(OperationType.STREAM_ROLLBACK);
        }

        @Test
        @DisplayName("消息包含起止状态 ID 和名称")
        void shouldIncludeStatusInfo() {
            // Given
            CardMovedEvent event = createCardMovedEvent(false);

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            assertThat(cmd.message().getArgs()).hasSize(2);

            HistoryArgument.StatusArg fromStatus =
                    (HistoryArgument.StatusArg) cmd.message().getArgs().get(0);
            HistoryArgument.StatusArg toStatus =
                    (HistoryArgument.StatusArg) cmd.message().getArgs().get(1);

            assertThat(fromStatus.statusId()).isEqualTo("status-1");
            assertThat(fromStatus.statusName()).isEqualTo("待处理");
            assertThat(toStatus.statusId()).isEqualTo("status-2");
            assertThat(toStatus.statusName()).isEqualTo("处理中");
        }

        @Test
        @DisplayName("回滚操作使用正确的消息 key")
        void shouldUseRollbackMessageKey() {
            // Given
            CardMovedEvent event = createCardMovedEvent(true);

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            assertThat(commandCaptor.getValue().message().getMessageKey())
                    .isEqualTo("history.stream.rollback");
        }
    }

    // ==================== handleCardUpdated 测试 ====================

    @Nested
    @DisplayName("handleCardUpdated - 卡片更新")
    class HandleCardUpdatedTests {

        @Test
        @DisplayName("标题变更产生一条历史记录")
        void shouldRecordTitleChange() {
            // Given
            CardUpdatedEvent event = createCardUpdatedEventWithTitleChange("旧标题", "新标题");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService, times(1)).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            assertThat(cmd.operationType()).isEqualTo(OperationType.FIELD_TITLE_UPDATED);
            assertThat(cmd.message().getMessageKey()).isEqualTo("history.field.title.updated");
        }

        @Test
        @DisplayName("描述变更产生一条历史记录")
        void shouldRecordDescriptionChange() {
            // Given
            CardUpdatedEvent event = createCardUpdatedEventWithDescChange("旧描述", "新描述");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService, times(1)).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            assertThat(cmd.operationType()).isEqualTo(OperationType.FIELD_DESC_UPDATED);
            assertThat(cmd.message().getMessageKey()).isEqualTo("history.field.desc.updated");
        }

        @Test
        @DisplayName("多个自定义属性变更产生多条历史记录")
        void shouldRecordMultipleFieldChanges() {
            // Given
            CardUpdatedEvent event = createCardUpdatedEvent();
            event.addFieldChange("field-1", new TextFieldValue("field-1", "旧值1"), new TextFieldValue("field-1", "新值1"));
            event.addFieldChange("field-2", new TextFieldValue("field-2", "旧值2"), new TextFieldValue("field-2", "新值2"));

            when(schemaNameCache.getFieldName("field-1")).thenReturn("属性1");
            when(schemaNameCache.getFieldName("field-2")).thenReturn("属性2");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService, times(2)).recordHistory(any());
        }

        @Test
        @DisplayName("同时变更标题、描述、属性产生多条记录")
        void shouldRecordAllChanges() {
            // Given
            CardUpdatedEvent event = createCardUpdatedEvent();
            event.withTitleChange("旧标题", "新标题");
            event.withDescriptionChange("旧描述", "新描述");
            event.addFieldChange("field-1", new TextFieldValue("field-1", "旧"), new TextFieldValue("field-1", "新"));

            when(schemaNameCache.getFieldName("field-1")).thenReturn("自定义属性");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService, times(3)).recordHistory(any());
        }

        @Test
        @DisplayName("无变更时不记录历史")
        void shouldNotRecordWhenNoChanges() {
            // Given
            CardUpdatedEvent event = createCardUpdatedEvent();
            // 不设置任何变更

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService, never()).recordHistory(any());
        }
    }

    // ==================== convertFieldValueToArg 测试 ====================

    @Nested
    @DisplayName("convertFieldValueToArg - FieldValue 转换")
    class ConvertFieldValueToArgTests {

        @Test
        @DisplayName("TextFieldValue 转换为 TextFieldValue 参数")
        void shouldConvertTextFieldValue() {
            // Given
            CardUpdatedEvent event = createCardUpdatedEventWithFieldChange(
                    "field-1", "旧文本", "新文本");
            when(schemaNameCache.getFieldName("field-1")).thenReturn("文本属性");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            // 第二个参数是旧值，第三个参数是新值
            HistoryArgument.TextFieldValue oldArg =
                    (HistoryArgument.TextFieldValue) cmd.message().getArgs().get(1);
            HistoryArgument.TextFieldValue newArg =
                    (HistoryArgument.TextFieldValue) cmd.message().getArgs().get(2);

            assertThat(oldArg.value()).isEqualTo("旧文本");
            assertThat(newArg.value()).isEqualTo("新文本");
        }

        @Test
        @DisplayName("NumberFieldValue 转换为 NumberFieldValue 参数")
        void shouldConvertNumberFieldValue() {
            // Given
            CardUpdatedEvent event = createCardUpdatedEventWithNumberFieldChange(
                    "field-1", 10.0, 20.0);
            when(schemaNameCache.getFieldName("field-1")).thenReturn("数字属性");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            HistoryArgument.NumberFieldValue oldArg =
                    (HistoryArgument.NumberFieldValue) cmd.message().getArgs().get(1);
            HistoryArgument.NumberFieldValue newArg =
                    (HistoryArgument.NumberFieldValue) cmd.message().getArgs().get(2);

            assertThat(oldArg.value()).isEqualByComparingTo("10");
            assertThat(newArg.value()).isEqualByComparingTo("20");
        }

        @Test
        @DisplayName("EnumFieldValue 转换为 EnumFieldValue 参数")
        void shouldConvertEnumFieldValue() {
            // Given
            CardUpdatedEvent event = createCardUpdatedEventWithEnumFieldChange(
                    "field-1", List.of("opt-1"), List.of("opt-2", "opt-3"));
            when(schemaNameCache.getFieldName("field-1")).thenReturn("枚举属性");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            HistoryArgument.EnumFieldValue newArg =
                    (HistoryArgument.EnumFieldValue) cmd.message().getArgs().get(2);

            assertThat(newArg.values()).hasSize(2);
            assertThat(newArg.values().get(0).optionId()).isEqualTo("opt-2");
            assertThat(newArg.values().get(1).optionId()).isEqualTo("opt-3");
        }

        @Test
        @DisplayName("null 或空 FieldValue 使用文本类型表示")
        void shouldUseTextForNullOrEmpty() {
            // Given
            CardUpdatedEvent event = createCardUpdatedEvent();
            event.addFieldChange("field-1", null, new TextFieldValue("field-1", "新值"));
            when(schemaNameCache.getFieldName("field-1")).thenReturn("属性");

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            // 旧值为 null，应该转换为 TextFieldValue
            HistoryArgument oldArg = cmd.message().getArgs().get(1);
            assertThat(oldArg).isInstanceOf(HistoryArgument.TextFieldValue.class);
        }
    }

    // ==================== handleCardLinkUpdated 测试 ====================

    @Nested
    @DisplayName("handleCardLinkUpdated - 关联更新")
    class HandleCardLinkUpdatedTests {

        @Test
        @DisplayName("添加关联产生 LINK_ADDED 记录")
        void shouldRecordLinkAdded() {
            // Given
            CardLinkUpdatedEvent event = createCardLinkUpdatedEventWithAddedCards(
                    "linkType-1:SOURCE",
                    List.of(linkedCardRef("card-1", TEST_CARD_TYPE_ID))
            );

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            assertThat(commandCaptor.getValue().operationType()).isEqualTo(OperationType.LINK_ADDED);
        }

        @Test
        @DisplayName("删除关联产生 LINK_REMOVED 记录")
        void shouldRecordLinkRemoved() {
            // Given
            CardLinkUpdatedEvent event = createCardLinkUpdatedEvent("linkType-1:SOURCE", true);
            event.setRemovedCards(List.of(linkedCardRef("card-1", TEST_CARD_TYPE_ID)));

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            assertThat(commandCaptor.getValue().operationType()).isEqualTo(OperationType.LINK_REMOVED);
        }

        @Test
        @DisplayName("主动方使用 history.link.added 消息 key")
        void shouldUseInitiatorMessageKey() {
            // Given
            CardLinkUpdatedEvent event = createCardLinkUpdatedEvent("linkType-1:SOURCE", true);
            event.setAddedCards(List.of(linkedCardRef("card-1", TEST_CARD_TYPE_ID)));

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            assertThat(commandCaptor.getValue().message().getMessageKey())
                    .isEqualTo("history.link.added");
        }

        @Test
        @DisplayName("被动方使用 history.link.added.passive 消息 key")
        void shouldUsePassiveMessageKey() {
            // Given
            CardLinkUpdatedEvent event = createCardLinkUpdatedEvent("linkType-1:TARGET", false);
            event.setAddedCards(List.of(linkedCardRef("card-1", TEST_CARD_TYPE_ID)));

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            assertThat(commandCaptor.getValue().message().getMessageKey())
                    .isEqualTo("history.link.added.passive");
        }

        @Test
        @DisplayName("同时添加和删除产生两条记录")
        void shouldRecordBothAddAndRemove() {
            // Given
            CardLinkUpdatedEvent event = createCardLinkUpdatedEvent("linkType-1:SOURCE", true);
            event.setAddedCards(List.of(linkedCardRef("card-1", TEST_CARD_TYPE_ID)));
            event.setRemovedCards(List.of(linkedCardRef("card-2", TEST_CARD_TYPE_ID)));

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService, times(2)).recordHistory(any());
        }

        @Test
        @DisplayName("无变更时不记录历史")
        void shouldNotRecordWhenNoChanges() {
            // Given
            CardLinkUpdatedEvent event = createCardLinkUpdatedEvent("linkType-1:SOURCE", true);
            // 不设置 addedCards 和 removedCards

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService, never()).recordHistory(any());
        }

        @Test
        @DisplayName("消息包含关联属性和卡片引用")
        void shouldIncludeLinkFieldAndCardRefs() {
            // Given
            CardLinkUpdatedEvent event = createCardLinkUpdatedEvent("linkType-1:SOURCE", true);
            event.setAddedCards(List.of(
                    linkedCardRef("card-1", TEST_CARD_TYPE_ID),
                    linkedCardRef("card-2", TEST_CARD_TYPE_ID)
            ));

            // When
            eventListener.handleCardEvent(event);

            // Then
            verify(cardHistoryService).recordHistory(commandCaptor.capture());
            RecordHistoryCommand cmd = commandCaptor.getValue();

            // 第一个参数是关联属性
            HistoryArgument.OperateFieldArg fieldArg =
                    (HistoryArgument.OperateFieldArg) cmd.message().getArgs().get(0);
            assertThat(fieldArg.fieldId()).isEqualTo("linkType-1:SOURCE");

            // 第二个参数是卡片引用
            HistoryArgument.LinkFieldValue linkArg =
                    (HistoryArgument.LinkFieldValue) cmd.message().getArgs().get(1);
            assertThat(linkArg.cards()).hasSize(2);
        }
    }
}
