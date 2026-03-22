package cn.planka.card.service.action;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.CreateCardRequest;
import cn.planka.api.card.request.UpdateLinkRequest;
import cn.planka.card.repository.CardRepository;
import cn.planka.card.service.core.CardService;
import cn.planka.card.service.core.LinkCardService;
import cn.planka.common.result.Result;
import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.card.OrgId;
import cn.planka.domain.schema.CardActionId;
import cn.planka.domain.schema.definition.action.ActionCategory;
import cn.planka.domain.schema.definition.action.CardActionConfigDefinition;
import cn.planka.domain.schema.definition.action.CreateLinkedCardExecution;
import cn.planka.infra.cache.schema.query.CardActionCacheQuery;
import cn.planka.infra.expression.TextExpressionTemplateResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardActionExecutionService")
class CardActionExecutionServiceTest {

    @Mock
    private CardActionCacheQuery cardActionCacheQuery;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardService cardService;
    @Mock
    private LinkCardService linkCardService;
    @Mock
    private TextExpressionTemplateResolver templateResolver;

    private CardActionExecutionService service;

    @BeforeEach
    void setUp() {
        service = new CardActionExecutionService(
                cardActionCacheQuery,
                cardRepository,
                cardService,
                linkCardService,
                templateResolver,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("CREATE_LINKED_CARD：创建卡片并在当前卡 SOURCE 关联属性上建链")
    void execute_createLinkedCard_createsCardAndUpdatesLink() {
        CardActionId actionId = CardActionId.of("action-1");
        CardId sourceCardId = CardId.of(100L);
        CardId operatorId = CardId.of(200L);

        CreateLinkedCardExecution exec = new CreateLinkedCardExecution();
        exec.setLinkTypeId("263671031548350464");
        exec.setTargetCardTypeId("target-type-1");
        exec.setTitleTemplate("${card}");

        CardActionConfigDefinition def = new CardActionConfigDefinition(actionId, "org-1", "新建关联");
        def.setBuiltIn(false);
        def.setActionCategory(ActionCategory.CUSTOM);
        def.setExecutionType(exec);

        CardDTO card = new CardDTO();
        card.setId(sourceCardId);
        card.setOrgId(OrgId.of("org-1"));
        card.setTypeId(CardTypeId.of("employee-type"));

        when(cardActionCacheQuery.getById(actionId)).thenReturn(Optional.of(def));
        when(cardRepository.findById(eq(sourceCardId), isNull(), eq(operatorId.value())))
                .thenReturn(Optional.of(card));
        when(templateResolver.resolve(eq("${card}"), eq(sourceCardId), eq(operatorId)))
                .thenReturn("员工-张三");

        CardId newId = CardId.of(300L);
        when(cardService.create(any(CreateCardRequest.class), eq(operatorId), isNull()))
                .thenReturn(Result.success(newId));
        when(linkCardService.updateLink(
                any(UpdateLinkRequest.class), eq("org-1"), eq(operatorId.value()), isNull()))
                .thenReturn(Result.success(null));

        Result<ActionExecutionResult> result = service.execute(
                actionId, sourceCardId, operatorId, null, null, null, null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().type()).isEqualTo(ActionExecutionResult.ResultType.SUCCESS);

        ArgumentCaptor<CreateCardRequest> createCap = ArgumentCaptor.forClass(CreateCardRequest.class);
        verify(cardService).create(createCap.capture(), eq(operatorId), isNull());
        assertThat(createCap.getValue().typeId().value()).isEqualTo("target-type-1");
        assertThat(createCap.getValue().orgId().value()).isEqualTo("org-1");

        ArgumentCaptor<UpdateLinkRequest> linkCap = ArgumentCaptor.forClass(UpdateLinkRequest.class);
        verify(linkCardService).updateLink(
                linkCap.capture(), eq("org-1"), eq(operatorId.value()), isNull());
        assertThat(linkCap.getValue().getCardId()).isEqualTo("100");
        assertThat(linkCap.getValue().getLinkFieldId()).isEqualTo("263671031548350464:SOURCE");
        assertThat(linkCap.getValue().getTargetCardIds()).containsExactly("300");
    }
}
