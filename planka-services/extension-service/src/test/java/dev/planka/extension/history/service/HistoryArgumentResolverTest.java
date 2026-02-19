package dev.planka.extension.history.service;

import dev.planka.api.card.CardServiceClient;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.history.HistoryArgument;
import dev.planka.domain.history.HistoryArgument.EnumFieldValue;
import dev.planka.domain.history.HistoryArgument.LinkFieldValue;
import dev.planka.domain.history.HistoryArgument.StructureFieldValue;
import dev.planka.domain.history.HistoryMessage;
import dev.planka.extension.history.service.SchemaNameCache.SchemaNameInfo;
import dev.planka.extension.history.vo.HistoryArgumentVO;
import dev.planka.extension.history.vo.HistoryMessageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static dev.planka.extension.history.support.HistoryTestDataBuilder.TEST_CARD_TYPE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * HistoryArgumentResolver 单元测试
 */
@ExtendWith(MockitoExtension.class)
class HistoryArgumentResolverTest {

    @Mock
    private SchemaNameCache schemaNameCache;

    @Mock
    private CardServiceClient cardServiceClient;

    private HistoryArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new HistoryArgumentResolver(schemaNameCache, cardServiceClient);
    }

    // ==================== resolve 单条消息解析测试 ====================

    @Nested
    @DisplayName("resolve - 单条消息解析")
    class ResolveTests {

        @Test
        @DisplayName("null 消息返回 null")
        void shouldReturnNullForNullMessage() {
            // When
            HistoryMessageVO result = resolver.resolve(null, TEST_CARD_TYPE_ID);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("无参数消息返回空参数列表")
        void shouldReturnEmptyArgsForNoArgs() {
            // Given
            HistoryMessage message = HistoryMessage.of("history.test");

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMessageKey()).isEqualTo("history.test");
            assertThat(result.getArgs()).isEmpty();
        }

        @Test
        @DisplayName("TextArg 直接转换，不查询")
        void shouldConvertTextArgDirectly() {
            // Given
            HistoryMessage message = HistoryMessage.of("history.test", HistoryMessage.text("测试文本"));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            assertThat(result.getArgs()).hasSize(1);
            HistoryArgumentVO arg = result.getArgs().get(0);
            assertThat(arg.getType()).isEqualTo("TEXT");
            assertThat(arg.getValue()).isEqualTo("测试文本");

            // 验证未调用任何外部服务
            verifyNoInteractions(schemaNameCache, cardServiceClient);
        }

        @Test
        @DisplayName("OperateFieldArg 查询字段名称")
        void shouldResolveOperateFieldArg() {
            // Given
            HistoryMessage message = HistoryMessage.of(
                    "history.test", HistoryMessage.operateField("field-001"));

            when(schemaNameCache.getFieldNames(Set.of("field-001")))
                    .thenReturn(Map.of("field-001", new SchemaNameInfo("优先级", false)));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            assertThat(result.getArgs()).hasSize(1);
            HistoryArgumentVO arg = result.getArgs().get(0);
            assertThat(arg.getType()).isEqualTo("OPERATE_FIELD");
            assertThat(arg.getFieldId()).isEqualTo("field-001");
            assertThat(arg.getFieldName()).isEqualTo("优先级");
            assertThat(arg.getDeleted()).isFalse();
        }

        @Test
        @DisplayName("StatusArg 查询状态名称")
        void shouldResolveStatusArg() {
            // Given
            HistoryMessage message = HistoryMessage.of(
                    "history.test", HistoryMessage.status("status-001", "待处理"));

            when(schemaNameCache.getStatusNames(TEST_CARD_TYPE_ID, Set.of("status-001")))
                    .thenReturn(Map.of("status-001", "进行中"));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            assertThat(result.getArgs()).hasSize(1);
            HistoryArgumentVO arg = result.getArgs().get(0);
            assertThat(arg.getType()).isEqualTo("STATUS");
            assertThat(arg.getStatusId()).isEqualTo("status-001");
            assertThat(arg.getStatusName()).isEqualTo("进行中");
            assertThat(arg.getDeleted()).isFalse();
        }
    }

    // ==================== resolveBatch 批量消息解析测试 ====================

    @Nested
    @DisplayName("resolveBatch - 批量消息解析")
    class ResolveBatchTests {

        @Test
        @DisplayName("null 或空列表返回空 List")
        void shouldReturnEmptyListForNullOrEmpty() {
            // When & Then
            assertThat(resolver.resolveBatch(null, TEST_CARD_TYPE_ID)).isEmpty();
            assertThat(resolver.resolveBatch(List.of(), TEST_CARD_TYPE_ID)).isEmpty();
        }

        @Test
        @DisplayName("合并多条消息的 ID 进行批量查询")
        void shouldBatchQueryForMultipleMessages() {
            // Given
            HistoryMessage msg1 = HistoryMessage.of(
                    "history.test", HistoryMessage.operateField("field-1"));
            HistoryMessage msg2 = HistoryMessage.of(
                    "history.test", HistoryMessage.operateField("field-2"));

            when(schemaNameCache.getFieldNames(argThat((Set<String> ids) ->
                    ids.contains("field-1") && ids.contains("field-2"))))
                    .thenReturn(Map.of(
                            "field-1", new SchemaNameInfo("属性1", false),
                            "field-2", new SchemaNameInfo("属性2", false)
                    ));

            // When
            List<HistoryMessageVO> results = resolver.resolveBatch(List.of(msg1, msg2), TEST_CARD_TYPE_ID);

            // Then
            assertThat(results).hasSize(2);

            // 验证只调用一次批量查询
            verify(schemaNameCache, times(1)).getFieldNames(anySet());
        }

        @Test
        @DisplayName("每条消息独立转换为 VO")
        void shouldConvertEachMessageToVO() {
            // Given
            HistoryMessage msg1 = HistoryMessage.of("history.msg1");
            HistoryMessage msg2 = HistoryMessage.of("history.msg2");

            // When
            List<HistoryMessageVO> results = resolver.resolveBatch(List.of(msg1, msg2), TEST_CARD_TYPE_ID);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getMessageKey()).isEqualTo("history.msg1");
            assertThat(results.get(1).getMessageKey()).isEqualTo("history.msg2");
        }

        @Test
        @DisplayName("包含 null 消息时跳过")
        void shouldHandleNullMessagesInList() {
            // Given
            HistoryMessage msg1 = HistoryMessage.of("history.msg1");
            List<HistoryMessage> messages = new ArrayList<>();
            messages.add(msg1);
            messages.add(null);

            // When
            List<HistoryMessageVO> results = resolver.resolveBatch(messages, TEST_CARD_TYPE_ID);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getMessageKey()).isEqualTo("history.msg1");
            assertThat(results.get(1)).isNull();
        }
    }

    // ==================== collectIds ID收集测试 ====================

    @Nested
    @DisplayName("collectIds - ID 收集")
    class CollectIdsTests {

        @Test
        @DisplayName("收集 OperateFieldArg 中的 fieldId")
        void shouldCollectOperateFieldIds() {
            // Given
            HistoryMessage message = HistoryMessage.of(
                    "history.test",
                    HistoryMessage.operateField("field-001"),
                    HistoryMessage.operateField("field-002")
            );

            when(schemaNameCache.getFieldNames(argThat((Set<String> ids) ->
                    ids.size() == 2 && ids.contains("field-001") && ids.contains("field-002"))))
                    .thenReturn(Map.of(
                            "field-001", new SchemaNameInfo("属性1", false),
                            "field-002", new SchemaNameInfo("属性2", false)
                    ));

            // When
            resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            verify(schemaNameCache).getFieldNames(argThat((Set<String> ids) ->
                    ids.contains("field-001") && ids.contains("field-002")));
        }

        @Test
        @DisplayName("区分普通属性和关联属性 ID")
        void shouldDistinguishFieldAndLinkFieldIds() {
            // Given
            String normalFieldId = "field-001";
            String linkFieldId = "linkType-1:SOURCE";

            HistoryMessage message = HistoryMessage.of(
                    "history.test",
                    HistoryMessage.operateField(normalFieldId),
                    HistoryMessage.operateField(linkFieldId)
            );

            when(schemaNameCache.getFieldNames(Set.of(normalFieldId)))
                    .thenReturn(Map.of(normalFieldId, new SchemaNameInfo("普通属性", false)));
            when(schemaNameCache.getLinkFieldNames(Set.of(linkFieldId)))
                    .thenReturn(Map.of(linkFieldId, new SchemaNameInfo("关联属性", false)));

            // When
            resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            verify(schemaNameCache).getFieldNames(Set.of(normalFieldId));
            verify(schemaNameCache).getLinkFieldNames(Set.of(linkFieldId));
        }

        @Test
        @DisplayName("收集 EnumFieldValue 中的枚举选项 ID")
        void shouldCollectEnumOptionIds() {
            // Given
            List<EnumFieldValue.EnumOption> options = List.of(
                    HistoryMessage.enumOption("opt-1", "选项1"),
                    HistoryMessage.enumOption("opt-2", "选项2")
            );
            HistoryArgument enumArg = HistoryMessage.enumFieldValue("field-001", options);
            HistoryMessage message = HistoryMessage.of("history.test", enumArg);

            when(schemaNameCache.getFieldNames(Set.of("field-001")))
                    .thenReturn(Map.of("field-001", new SchemaNameInfo("枚举属性", false)));
            when(schemaNameCache.getEnumOptionNames(eq("field-001"), argThat((Set<String> ids) ->
                    ids.contains("opt-1") && ids.contains("opt-2")), eq(false)))
                    .thenReturn(Map.of("opt-1", "高优先级", "opt-2", "低优先级"));

            // When
            resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            verify(schemaNameCache).getEnumOptionNames(eq("field-001"), anySet(), eq(false));
        }

        @Test
        @DisplayName("收集 LinkFieldValue 中的卡片 ID")
        void shouldCollectLinkedCardIds() {
            // Given
            List<LinkFieldValue.LinkedCardRef> cardRefs = List.of(
                    HistoryMessage.linkedCardRef("card-1", "cardType-1"),
                    HistoryMessage.linkedCardRef("card-2", "cardType-1")
            );
            HistoryArgument linkArg = HistoryMessage.linkFieldValue("linkType-1:SOURCE", cardRefs);
            HistoryMessage message = HistoryMessage.of("history.test", linkArg);

            when(schemaNameCache.getLinkFieldNames(anySet()))
                    .thenReturn(Map.of("linkType-1:SOURCE", new SchemaNameInfo("关联", false)));
            when(cardServiceClient.queryCardNames(eq("system"), argThat((List<String> ids) ->
                    ids.contains("card-1") && ids.contains("card-2"))))
                    .thenReturn(Result.success(Map.of(
                            "card-1", CardTitle.pure("卡片1"),
                            "card-2", CardTitle.pure("卡片2")
                    )));

            // When
            resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            verify(cardServiceClient).queryCardNames(eq("system"), anyList());
        }

        @Test
        @DisplayName("收集 StatusArg 中的状态 ID")
        void shouldCollectStatusIds() {
            // Given
            HistoryMessage message = HistoryMessage.of(
                    "history.test",
                    HistoryMessage.status("status-1", "待处理"),
                    HistoryMessage.status("status-2", "进行中")
            );

            when(schemaNameCache.getStatusNames(eq(TEST_CARD_TYPE_ID), argThat((Set<String> ids) ->
                    ids.contains("status-1") && ids.contains("status-2"))))
                    .thenReturn(Map.of("status-1", "待办", "status-2", "处理中"));

            // When
            resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            verify(schemaNameCache).getStatusNames(eq(TEST_CARD_TYPE_ID), anySet());
        }
    }

    // ==================== resolveNames 名称解析测试 ====================

    @Nested
    @DisplayName("resolveNames - 名称解析")
    class ResolveNamesTests {

        @Test
        @DisplayName("CardServiceClient 异常时继续处理，不抛出")
        void shouldContinueOnCardServiceClientError() {
            // Given
            List<LinkFieldValue.LinkedCardRef> cardRefs = List.of(
                    HistoryMessage.linkedCardRef("card-1", "cardType-1")
            );
            HistoryArgument linkArg = HistoryMessage.linkFieldValue("linkType-1:SOURCE", cardRefs);
            HistoryMessage message = HistoryMessage.of("history.test", linkArg);

            when(schemaNameCache.getLinkFieldNames(anySet()))
                    .thenReturn(Map.of("linkType-1:SOURCE", new SchemaNameInfo("关联", false)));
            when(cardServiceClient.queryCardNames(anyString(), anyList()))
                    .thenThrow(new RuntimeException("服务不可用"));

            // When & Then - 不应抛出异常
            assertThatCode(() -> resolver.resolve(message, TEST_CARD_TYPE_ID))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== toVO 参数转换测试 ====================

    @Nested
    @DisplayName("toVO - 参数转换")
    class ToVOTests {

        @Test
        @DisplayName("OperateFieldArg 标记删除状态")
        void shouldMarkDeletedForOperateFieldArg() {
            // Given
            HistoryMessage message = HistoryMessage.of(
                    "history.test", HistoryMessage.operateField("field-001"));

            when(schemaNameCache.getFieldNames(Set.of("field-001")))
                    .thenReturn(Map.of("field-001", new SchemaNameInfo("已删除属性", true)));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO arg = result.getArgs().get(0);
            assertThat(arg.getDeleted()).isTrue();
            assertThat(arg.getFieldName()).isEqualTo("已删除属性");
        }

        @Test
        @DisplayName("字段不存在时使用 fieldId 作为名称")
        void shouldUsefieldIdWhenFieldNotFound() {
            // Given
            HistoryMessage message = HistoryMessage.of(
                    "history.test", HistoryMessage.operateField("field-001"));

            when(schemaNameCache.getFieldNames(Set.of("field-001")))
                    .thenReturn(Collections.emptyMap());

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO arg = result.getArgs().get(0);
            assertThat(arg.getFieldName()).isEqualTo("field-001");
            assertThat(arg.getDeleted()).isTrue();
        }

        @Test
        @DisplayName("StatusArg 标记删除状态")
        void shouldMarkDeletedForStatusArg() {
            // Given
            HistoryMessage message = HistoryMessage.of(
                    "history.test", HistoryMessage.status("status-001", "待处理"));

            when(schemaNameCache.getStatusNames(TEST_CARD_TYPE_ID, Set.of("status-001")))
                    .thenReturn(Collections.emptyMap());

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO arg = result.getArgs().get(0);
            assertThat(arg.getDeleted()).isTrue();
        }

        @Test
        @DisplayName("状态不存在时使用存储的备份名称")
        void shouldUseBackupNameWhenStatusNotFound() {
            // Given
            HistoryMessage message = HistoryMessage.of(
                    "history.test", HistoryMessage.status("status-001", "备份状态名"));

            when(schemaNameCache.getStatusNames(TEST_CARD_TYPE_ID, Set.of("status-001")))
                    .thenReturn(Collections.emptyMap());

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO arg = result.getArgs().get(0);
            assertThat(arg.getStatusName()).isEqualTo("备份状态名");
        }
    }

    // ==================== convertFieldValueArgToVO 转换测试 ====================

    @Nested
    @DisplayName("convertFieldValueArgToVO - FieldValueArg 转换")
    class ConvertFieldValueArgToVOTests {

        @Test
        @DisplayName("TextFieldValue 转换正确")
        void shouldConvertTextFieldValue() {
            // Given
            HistoryArgument arg = HistoryMessage.textFieldValue("field-001", "文本内容");
            HistoryMessage message = HistoryMessage.of("history.test", arg);

            when(schemaNameCache.getFieldNames(Set.of("field-001")))
                    .thenReturn(Map.of("field-001", new SchemaNameInfo("文本属性", false)));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO vo = result.getArgs().get(0);
            assertThat(vo.getType()).isEqualTo("FIELD_VALUE_TEXT");
            assertThat(vo.getDisplayValue()).isEqualTo("文本内容");
        }

        @Test
        @DisplayName("NumberFieldValue 格式化为 BigDecimal 字符串")
        void shouldFormatNumberFieldValue() {
            // Given
            HistoryArgument arg = HistoryMessage.numberFieldValue("field-001", new BigDecimal("123.45"));
            HistoryMessage message = HistoryMessage.of("history.test", arg);

            when(schemaNameCache.getFieldNames(Set.of("field-001")))
                    .thenReturn(Map.of("field-001", new SchemaNameInfo("数字属性", false)));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO vo = result.getArgs().get(0);
            assertThat(vo.getType()).isEqualTo("FIELD_VALUE_NUMBER");
            assertThat(vo.getDisplayValue()).isEqualTo("123.45");
        }

        @Test
        @DisplayName("DateFieldValue 格式化日期")
        void shouldFormatDateFieldValue() {
            // Given
            HistoryArgument arg = HistoryMessage.dateFieldValue("field-001", LocalDate.of(2024, 6, 15));
            HistoryMessage message = HistoryMessage.of("history.test", arg);

            when(schemaNameCache.getFieldNames(Set.of("field-001")))
                    .thenReturn(Map.of("field-001", new SchemaNameInfo("日期属性", false)));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO vo = result.getArgs().get(0);
            assertThat(vo.getType()).isEqualTo("FIELD_VALUE_DATE");
            assertThat(vo.getDisplayValue()).isEqualTo("2024-06-15");
        }

        @Test
        @DisplayName("DateTimeFieldValue 格式化日期时间")
        void shouldFormatDateTimeFieldValue() {
            // Given
            HistoryArgument arg = HistoryMessage.dateTimeFieldValue(
                    "field-001", LocalDateTime.of(2024, 6, 15, 14, 30, 0));
            HistoryMessage message = HistoryMessage.of("history.test", arg);

            when(schemaNameCache.getFieldNames(Set.of("field-001")))
                    .thenReturn(Map.of("field-001", new SchemaNameInfo("时间属性", false)));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO vo = result.getArgs().get(0);
            assertThat(vo.getType()).isEqualTo("FIELD_VALUE_DATETIME");
            assertThat(vo.getDisplayValue()).contains("2024-06-15");
        }

        @Test
        @DisplayName("EnumFieldValue 解析选项名称并用逗号连接")
        void shouldJoinEnumOptionNames() {
            // Given
            List<EnumFieldValue.EnumOption> options = List.of(
                    HistoryMessage.enumOption("opt-1", "备份1"),
                    HistoryMessage.enumOption("opt-2", "备份2")
            );
            HistoryArgument arg = HistoryMessage.enumFieldValue("field-001", options);
            HistoryMessage message = HistoryMessage.of("history.test", arg);

            when(schemaNameCache.getFieldNames(Set.of("field-001")))
                    .thenReturn(Map.of("field-001", new SchemaNameInfo("枚举属性", false)));
            when(schemaNameCache.getEnumOptionNames(eq("field-001"), anySet(), eq(false)))
                    .thenReturn(Map.of("opt-1", "高", "opt-2", "中"));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO vo = result.getArgs().get(0);
            assertThat(vo.getType()).isEqualTo("FIELD_VALUE_ENUM");
            assertThat(vo.getDisplayValue()).contains("高").contains("中");
        }

        @Test
        @DisplayName("StructureFieldValue 解析路径并用 / 连接")
        void shouldJoinStructurePathWithSlash() {
            // Given
            List<StructureFieldValue.StructureNode> path = List.of(
                    HistoryMessage.structureNode("node-1", "研发中心"),
                    HistoryMessage.structureNode("node-2", "前端组")
            );
            HistoryArgument arg = HistoryMessage.structureFieldValue("field-001", path);
            HistoryMessage message = HistoryMessage.of("history.test", arg);

            when(schemaNameCache.getFieldNames(Set.of("field-001")))
                    .thenReturn(Map.of("field-001", new SchemaNameInfo("部门", false)));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO vo = result.getArgs().get(0);
            assertThat(vo.getType()).isEqualTo("FIELD_VALUE_STRUCTURE");
            assertThat(vo.getDisplayValue()).isEqualTo("研发中心 / 前端组");
        }

        @Test
        @DisplayName("LinkFieldValue 解析卡片标题并用顿号连接")
        void shouldJoinCardTitlesWithChineseComma() {
            // Given
            List<LinkFieldValue.LinkedCardRef> cardRefs = List.of(
                    HistoryMessage.linkedCardRef("card-1", "cardType-1"),
                    HistoryMessage.linkedCardRef("card-2", "cardType-1")
            );
            HistoryArgument arg = HistoryMessage.linkFieldValue("linkType-1:SOURCE", cardRefs);
            HistoryMessage message = HistoryMessage.of("history.test", arg);

            when(schemaNameCache.getLinkFieldNames(Set.of("linkType-1:SOURCE")))
                    .thenReturn(Map.of("linkType-1:SOURCE", new SchemaNameInfo("父需求", false)));
            when(cardServiceClient.queryCardNames(eq("system"), anyList()))
                    .thenReturn(Result.success(Map.of(
                            "card-1", CardTitle.pure("需求A"),
                            "card-2", CardTitle.pure("需求B")
                    )));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO vo = result.getArgs().get(0);
            assertThat(vo.getType()).isEqualTo("FIELD_VALUE_LINK");
            assertThat(vo.getDisplayValue()).isEqualTo("需求A、需求B");
        }

        @Test
        @DisplayName("LinkFieldValue 包含卡片引用列表")
        void shouldIncludeCardRefs() {
            // Given
            List<LinkFieldValue.LinkedCardRef> cardRefs = List.of(
                    HistoryMessage.linkedCardRef("card-1", "cardType-1")
            );
            HistoryArgument arg = HistoryMessage.linkFieldValue("linkType-1:SOURCE", cardRefs);
            HistoryMessage message = HistoryMessage.of("history.test", arg);

            when(schemaNameCache.getLinkFieldNames(anySet()))
                    .thenReturn(Map.of("linkType-1:SOURCE", new SchemaNameInfo("关联", false)));
            when(cardServiceClient.queryCardNames(eq("system"), anyList()))
                    .thenReturn(Result.success(Map.of(
                            "card-1", CardTitle.pure("卡片1")
                    )));

            // When
            HistoryMessageVO result = resolver.resolve(message, TEST_CARD_TYPE_ID);

            // Then
            HistoryArgumentVO vo = result.getArgs().get(0);
            assertThat(vo.getCards()).hasSize(1);
            assertThat(vo.getCards().get(0).cardId()).isEqualTo("card-1");
        }
    }
}
