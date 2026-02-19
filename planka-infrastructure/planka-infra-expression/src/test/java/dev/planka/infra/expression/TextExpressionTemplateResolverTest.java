package dev.planka.infra.expression;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.Yield;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.expression.TextExpressionTemplate;
import dev.planka.domain.field.TextFieldValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TextExpressionTemplateResolver 单元测试")
class TextExpressionTemplateResolverTest {

    @Mock
    private CardDataProvider cardDataProvider;

    private TextExpressionTemplateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TextExpressionTemplateResolver(cardDataProvider);
    }

    @Test
    @DisplayName("纯文本模板原样返回，不查询数据")
    void resolve_returnsPlainText_whenNoExpressions() {
        String result = resolver.resolve("这是一段纯文本", CardId.of(1L), CardId.of(2L));

        assertThat(result).isEqualTo("这是一段纯文本");
        verifyNoInteractions(cardDataProvider);
    }

    @Test
    @DisplayName("null 模板返回 null")
    void resolve_returnsNull_whenTemplateIsNull() {
        String result = resolver.resolve((TextExpressionTemplate) null, CardId.of(1L), CardId.of(2L));

        assertThat(result).isNull();
        verifyNoInteractions(cardDataProvider);
    }

    @Test
    @DisplayName("空字符串模板返回空字符串")
    void resolve_returnsEmpty_whenTemplateIsEmpty() {
        String result = resolver.resolve("", CardId.of(1L), CardId.of(2L));

        assertThat(result).isEmpty();
        verifyNoInteractions(cardDataProvider);
    }

    @Test
    @DisplayName("解析 ${card.title} 替换为卡片标题")
    void resolve_replacesCardTitle() {
        CardId cardId = CardId.of(1001L);
        CardId memberCardId = CardId.of(2001L);

        CardDTO card = new CardDTO();
        card.setId(cardId);
        card.setTitle(CardTitle.pure("测试卡片"));

        when(cardDataProvider.findCardById(eq(cardId), any(Yield.class))).thenReturn(card);

        String result = resolver.resolve("卡片标题是：${card.title}", cardId, memberCardId);

        assertThat(result).isEqualTo("卡片标题是：测试卡片");
    }

    @Test
    @DisplayName("解析 ${card.code} 替换为卡片编号")
    void resolve_replacesCard() {
        CardId cardId = CardId.of(1001L);
        CardId memberCardId = CardId.of(2001L);

        CardDTO card = new CardDTO();
        card.setId(cardId);
        card.setCustomCode("C1001");
        card.setTitle(CardTitle.pure("我的卡片"));

        when(cardDataProvider.findCardById(eq(cardId), any(Yield.class))).thenReturn(card);

        String result = resolver.resolve("${card.code}已被锁定", cardId, memberCardId);

        assertThat(result).isEqualTo("C1001已被锁定");
    }

    @Test
    @DisplayName("解析 ${member} 替换为成员卡标题")
    void resolve_replacesMember() {
        CardId cardId = CardId.of(1001L);
        CardId memberCardId = CardId.of(2001L);

        CardDTO memberCard = new CardDTO();
        memberCard.setId(memberCardId);
        memberCard.setTitle(CardTitle.pure("张三"));

        when(cardDataProvider.findCardById(eq(memberCardId), any(Yield.class))).thenReturn(memberCard);

        String result = resolver.resolve("操作人：${member}", cardId, memberCardId);

        assertThat(result).isEqualTo("操作人：张三");
    }

    @Test
    @DisplayName("解析 ${system.currentDate} 替换为当前日期")
    void resolve_replacesSystemCurrentDate() {
        String expected = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String result = resolver.resolve("今天是 ${system.currentDate}", CardId.of(1L), CardId.of(2L));

        assertThat(result).isEqualTo("今天是 " + expected);
        verifyNoInteractions(cardDataProvider);
    }

    @Test
    @DisplayName("解析 ${system.currentTime} 替换为当前时间")
    void resolve_replacesSystemCurrentTime() {
        String result = resolver.resolve("当前时间：${system.currentTime}", CardId.of(1L), CardId.of(2L));

        assertThat(result).matches("当前时间：\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        verifyNoInteractions(cardDataProvider);
    }

    @Test
    @DisplayName("解析 TextExpressionTemplate 对象")
    void resolve_worksWithTextExpressionTemplate() {
        TextExpressionTemplate template = new TextExpressionTemplate("纯文本消息");

        String result = resolver.resolve(template, CardId.of(1L), CardId.of(2L));

        assertThat(result).isEqualTo("纯文本消息");
    }

    @Test
    @DisplayName("CardId 查不到数据时表达式替换为空字符串")
    void resolve_replacesWithEmpty_whenCardNotFound() {
        CardId cardId = CardId.of(1001L);
        CardId memberCardId = CardId.of(2001L);

        when(cardDataProvider.findCardById(eq(cardId), any(Yield.class))).thenReturn(null);

        String result = resolver.resolve("卡片：${card.title}", cardId, memberCardId);

        assertThat(result).isEqualTo("卡片：");
    }

    @Test
    @DisplayName("混合多种表达式同时解析")
    void resolve_handlesMultipleExpressions() {
        CardId cardId = CardId.of(1001L);
        CardId memberCardId = CardId.of(2001L);

        CardDTO card = new CardDTO();
        card.setId(cardId);
        card.setTitle(CardTitle.pure("需求A"));

        CardDTO memberCard = new CardDTO();
        memberCard.setId(memberCardId);
        memberCard.setTitle(CardTitle.pure("李四"));

        when(cardDataProvider.findCardById(eq(cardId), any(Yield.class))).thenReturn(card);
        when(cardDataProvider.findCardById(eq(memberCardId), any(Yield.class))).thenReturn(memberCard);

        String result = resolver.resolve(
            "${member} 没有权限编辑 ${card}", cardId, memberCardId);

        assertThat(result).isEqualTo("李四 没有权限编辑 需求A");
    }

    @Test
    @DisplayName("解析自定义字段值")
    void resolve_replacesCustomFieldValue() {
        CardId cardId = CardId.of(1001L);
        CardId memberCardId = CardId.of(2001L);

        CardDTO card = new CardDTO();
        card.setId(cardId);
        card.setTitle(CardTitle.pure("测试"));

        TextFieldValue priorityField = new TextFieldValue("priority", "高");
        card.setFieldValues(Map.of("priority", priorityField));

        when(cardDataProvider.findCardById(eq(cardId), any(Yield.class))).thenReturn(card);

        String result = resolver.resolve("优先级：${card.priority}", cardId, memberCardId);

        assertThat(result).isEqualTo("优先级：高");
    }

    @Test
    @DisplayName("解析关联卡片字段值 ${card.linkFieldId.fieldId}")
    @SuppressWarnings("unchecked")
    void resolve_replacesLinkedCardFieldValue() {
        CardId cardId = CardId.of(1001L);
        CardId memberCardId = CardId.of(2001L);
        CardId linkedCardId = CardId.of(3001L);

        // 创建关联卡片
        CardDTO linkedCard = new CardDTO();
        linkedCard.setId(linkedCardId);
        linkedCard.setTitle(CardTitle.pure("关联的需求"));

        // 创建当前卡片，包含关联卡片
        CardDTO card = new CardDTO();
        card.setId(cardId);
        card.setTitle(CardTitle.pure("当前任务"));
        card.setLinkedCards(Map.of("requirementLink", Set.of(linkedCard)));

        when(cardDataProvider.findCardById(eq(cardId), any(Yield.class))).thenReturn(card);

        String result = resolver.resolve("所属需求：${card.requirementLink.title}", cardId, memberCardId);

        assertThat(result).isEqualTo("所属需求：关联的需求");
    }

    @Test
    @DisplayName("未知表达式源保留原样")
    void resolve_preservesUnknownSource() {
        String result = resolver.resolve("${unknown.field}", CardId.of(1L), CardId.of(2L));

        assertThat(result).isEqualTo("${unknown.field}");
        verifyNoInteractions(cardDataProvider);
    }

    @Test
    @DisplayName("解析 ${system.currentYear} 替换为当前年份")
    void resolve_replacesSystemCurrentYear() {
        String result = resolver.resolve("今年是${system.currentYear}年", CardId.of(1L), CardId.of(2L));

        String expectedYear = String.valueOf(LocalDate.now().getYear());
        assertThat(result).isEqualTo("今年是" + expectedYear + "年");
    }

    @Test
    @DisplayName("解析 ${system.currentMonth} 替换为当前月份")
    void resolve_replacesSystemCurrentMonth() {
        String result = resolver.resolve("${system.currentMonth}月", CardId.of(1L), CardId.of(2L));

        String expectedMonth = String.valueOf(LocalDate.now().getMonthValue());
        assertThat(result).isEqualTo(expectedMonth + "月");
    }
}
