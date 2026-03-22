package cn.planka.view.service;

import cn.planka.api.card.CardServiceClient;
import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.CardPageQueryRequest;
import cn.planka.api.card.request.Page;
import cn.planka.api.card.request.QueryContext;
import cn.planka.api.card.request.QueryScope;
import cn.planka.api.card.request.SortAndPage;
import cn.planka.common.result.Result;
import cn.planka.common.util.SystemSchemaIds;
import cn.planka.domain.card.CardCycle;
import cn.planka.domain.link.LinkFieldId;
import cn.planka.domain.link.LinkPosition;
import cn.planka.domain.schema.definition.condition.Condition;
import cn.planka.domain.schema.definition.condition.ConditionNode;
import cn.planka.domain.schema.definition.condition.LinkConditionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ViewTeamMembershipSupport {

    private final CardServiceClient cardServiceClient;

    public boolean memberInAnyTeam(String orgId, String operatorMemberCardId, Collection<String> teamCardIds) {
        if (teamCardIds == null || teamCardIds.isEmpty()) {
            return false;
        }
        Result<cn.planka.common.result.PageResult<CardDTO>> result = cardServiceClient.pageQuery(
                operatorMemberCardId,
                buildMyTeamsPageQuery(orgId, operatorMemberCardId));
        if (!result.isSuccess() || result.getData() == null || result.getData().getContent() == null) {
            return false;
        }
        for (CardDTO team : result.getData().getContent()) {
            if (team.getId() != null && teamCardIds.contains(team.getId().value())) {
                return true;
            }
        }
        return false;
    }

    private CardPageQueryRequest buildMyTeamsPageQuery(String orgId, String operatorMemberCardId) {
        LinkConditionItem linkItem = new LinkConditionItem(
                new LinkConditionItem.LinkSubject(null, LinkFieldId.of(SystemSchemaIds.teamMemberLinkTypeId(orgId), LinkPosition.SOURCE)),
                new LinkConditionItem.LinkOperator.In(
                        new LinkConditionItem.LinkValue.StaticValue(List.of(operatorMemberCardId))));
        ConditionNode root = linkItem;
        Condition condition = new Condition(root);

        CardPageQueryRequest req = new CardPageQueryRequest();
        QueryContext ctx = new QueryContext();
        ctx.setOrgId(orgId);
        ctx.setOperatorId(operatorMemberCardId);
        req.setQueryContext(ctx);
        QueryScope scope = new QueryScope();
        scope.setCardTypeIds(Collections.singletonList(SystemSchemaIds.teamCardTypeId(orgId)));
        scope.setCardCycles(List.of(CardCycle.ACTIVE));
        req.setQueryScope(scope);
        req.setCondition(condition);
        SortAndPage sap = new SortAndPage();
        Page page = new Page();
        page.setPageNum(0);
        page.setPageSize(200);
        sap.setPage(page);
        req.setSortAndPage(sap);
        return req;
    }
}
