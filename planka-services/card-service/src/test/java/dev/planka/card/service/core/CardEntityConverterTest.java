package dev.planka.card.service.core;

import dev.planka.api.card.request.CreateCardRequest;
import dev.planka.card.model.CardEntity;
import dev.planka.card.repository.CardRepository;
import dev.planka.card.service.sequence.SequenceSegmentService;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.card.OrgId;
import dev.planka.domain.schema.definition.cardtype.CodeGenerationRule;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.infra.cache.schema.query.CardTypeCacheQuery;
import dev.planka.infra.cache.schema.query.ValueStreamCacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardEntityConverterTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private SequenceSegmentService sequenceSegmentService;
    @Mock
    private ValueStreamCacheQuery valueStreamCacheQuery;
    @Mock
    private CardTypeCacheQuery cardTypeCacheQuery;
    @Mock
    private TitleValueResolver titleValueResolver;

    private CardEntityConverter cardEntityConverter;

    @BeforeEach
    void setUp() {
        cardEntityConverter = new CardEntityConverter(
                cardRepository,
                sequenceSegmentService,
                valueStreamCacheQuery,
                cardTypeCacheQuery,
                titleValueResolver
        );
    }

    @Test
    void toCardEntityForCreate_shouldGenerateCustomCode_whenRuleIsPresent() {
        // Given
        String orgIdValue = "org-1";
        OrgId orgId = new OrgId(orgIdValue);
        CardTypeId typeId = new CardTypeId("type-1");

        CreateCardRequest request = new CreateCardRequest(
                orgId,
                typeId,
                CardTitle.pure("Test Card"),
                "Description",
                null
        );

        EntityCardType cardType = new EntityCardType(typeId, orgIdValue, "Type Name");
        CodeGenerationRule rule = new CodeGenerationRule();
        rule.setPrefix("TEST-");
        rule.setDateFormat("yyyyMM");
        rule.setDateSequenceConnector("-");
        rule.setSequenceLength(4);
        cardType.setCodeGenerationRule(rule);

        when(cardTypeCacheQuery.getById(typeId)).thenReturn(Optional.of(cardType));
        when(sequenceSegmentService.getNextCodeInOrg(orgIdValue)).thenReturn(100L);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String expectedKey = "TEST-" + dateStr + "-";
        when(sequenceSegmentService.getNextCode(eq(orgIdValue), eq(typeId), eq(expectedKey))).thenReturn(1L);

        // When
        CardEntity cardEntity = cardEntityConverter.toCardEntityForCreate(request);

        // Then
        assertThat(cardEntity).isNotNull();
        assertThat(cardEntity.getCustomCode()).isEqualTo(expectedKey + "0001");

        verify(sequenceSegmentService).getNextCode(eq(orgIdValue), eq(typeId), eq(expectedKey));
    }

    @Test
    void toCardEntityForCreate_shouldHandleCustomCodeWithoutDate_whenDateFormatIsEmpty() {
        // Given
        String orgIdValue = "org-1";
        OrgId orgId = new OrgId(orgIdValue);
        CardTypeId typeId = new CardTypeId("type-1");

        CreateCardRequest request = new CreateCardRequest(
                orgId,
                typeId,
                CardTitle.pure("Test Card"),
                "Description",
                null
        );

        EntityCardType cardType = new EntityCardType(typeId, orgIdValue, "Type Name");
        CodeGenerationRule rule = new CodeGenerationRule();
        rule.setPrefix("PRJ-");
        rule.setDateFormat(null); // No date
        rule.setSequenceLength(3);
        cardType.setCodeGenerationRule(rule);

        when(cardTypeCacheQuery.getById(typeId)).thenReturn(Optional.of(cardType));
        when(sequenceSegmentService.getNextCodeInOrg(orgIdValue)).thenReturn(100L);

        String expectedKey = "PRJ-";
        when(sequenceSegmentService.getNextCode(eq(orgIdValue), eq(typeId), eq(expectedKey))).thenReturn(42L);

        // When
        CardEntity cardEntity = cardEntityConverter.toCardEntityForCreate(request);

        // Then
        assertThat(cardEntity).isNotNull();
        assertThat(cardEntity.getCustomCode()).isEqualTo("PRJ-042");

        verify(sequenceSegmentService).getNextCode(eq(orgIdValue), eq(typeId), eq(expectedKey));
    }

    @Test
    void toCardEntityForCreate_shouldHandleCustomCodeWithoutPrefix_whenPrefixIsNull() {
        // Given
        String orgIdValue = "org-1";
        OrgId orgId = new OrgId(orgIdValue);
        CardTypeId typeId = new CardTypeId("type-1");

        CreateCardRequest request = new CreateCardRequest(
                orgId,
                typeId,
                CardTitle.pure("Test Card"),
                "Description",
                null
        );

        EntityCardType cardType = new EntityCardType(typeId, orgIdValue, "Type Name");
        CodeGenerationRule rule = new CodeGenerationRule();
        rule.setPrefix(null);
        rule.setDateFormat("yyyy");
        rule.setDateSequenceConnector("-");
        rule.setSequenceLength(4);
        cardType.setCodeGenerationRule(rule);

        when(cardTypeCacheQuery.getById(typeId)).thenReturn(Optional.of(cardType));
        when(sequenceSegmentService.getNextCodeInOrg(orgIdValue)).thenReturn(100L);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String expectedKey = dateStr + "-";
        when(sequenceSegmentService.getNextCode(eq(orgIdValue), eq(typeId), eq(expectedKey))).thenReturn(5L);

        // When
        CardEntity cardEntity = cardEntityConverter.toCardEntityForCreate(request);

        // Then
        assertThat(cardEntity).isNotNull();
        assertThat(cardEntity.getCustomCode()).isEqualTo(expectedKey + "0005");

        verify(sequenceSegmentService).getNextCode(eq(orgIdValue), eq(typeId), eq(expectedKey));
    }

    @Test
    void toCardEntityForCreate_shouldNotGenerateCustomCode_whenRuleIsNull() {
        // Given
        String orgIdValue = "org-1";
        OrgId orgId = new OrgId(orgIdValue);
        CardTypeId typeId = new CardTypeId("type-1");
        CreateCardRequest request = new CreateCardRequest(
                orgId,
                typeId,
                CardTitle.pure("Test Card"),
                "Description",
                null
        );

        EntityCardType cardType = new EntityCardType(typeId, orgIdValue, "Type Name");
        cardType.setCodeGenerationRule(null);

        when(cardTypeCacheQuery.getById(typeId)).thenReturn(Optional.of(cardType));
        when(sequenceSegmentService.getNextCodeInOrg(orgIdValue)).thenReturn(100L);

        // When
        CardEntity cardEntity = cardEntityConverter.toCardEntityForCreate(request);

        // Then
        assertThat(cardEntity).isNotNull();
        assertThat(cardEntity.getCustomCode()).isNull();
    }

    @Test
    void toCardEntityForCreate_shouldNotGenerateCustomCode_whenCardTypeNotFound() {
        // Given
        String orgIdValue = "org-1";
        OrgId orgId = new OrgId(orgIdValue);
        CardTypeId typeId = new CardTypeId("type-1");
        CreateCardRequest request = new CreateCardRequest(
                orgId,
                typeId,
                CardTitle.pure("Test Card"),
                "Description",
                null
        );

        when(cardTypeCacheQuery.getById(typeId)).thenReturn(Optional.empty());
        when(sequenceSegmentService.getNextCodeInOrg(orgIdValue)).thenReturn(100L);

        // When
        CardEntity cardEntity = cardEntityConverter.toCardEntityForCreate(request);

        // Then
        assertThat(cardEntity).isNotNull();
        assertThat(cardEntity.getCustomCode()).isNull();
    }
}
