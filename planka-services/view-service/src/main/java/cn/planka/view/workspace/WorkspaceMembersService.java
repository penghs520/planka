package cn.planka.view.workspace;

import cn.planka.api.card.CardServiceClient;
import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.request.*;
import cn.planka.api.user.UserServiceContract;
import cn.planka.api.user.dto.MemberCardDirectoryEnrichmentDTO;
import cn.planka.api.user.request.MemberCardIdsRequest;
import cn.planka.api.view.response.WorkspaceMemberRowDTO;
import cn.planka.common.result.PageResult;
import cn.planka.common.result.Result;
import cn.planka.common.util.SystemSchemaIds;
import cn.planka.domain.card.CardCycle;
import cn.planka.domain.field.TextFieldValue;
import cn.planka.domain.schema.definition.condition.Condition;
import cn.planka.domain.schema.definition.condition.TitleConditionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作区成员目录：成员卡片分页 + user-service 补齐角色与上次登录
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMembersService {

    private static final int FETCH_BATCH_SIZE = 200;
    private static final int ENRICH_BATCH_SIZE = 200;

    private static final String SORT_NAME = "name";
    private static final String SORT_EMAIL = "email";
    private static final String SORT_JOINED = "joined";
    private static final String SORT_LAST_SEEN = "lastSeen";

    private final CardServiceClient cardServiceClient;
    private final UserServiceContract userServiceContract;

    public Result<PageResult<WorkspaceMemberRowDTO>> listWorkspaceMembers(
            String orgId,
            String operatorMemberCardId,
            int page,
            int size,
            String keyword,
            String sort,
            String order) {

        if (orgId == null || orgId.isBlank()) {
            return Result.failure("ORG_MISSING", "缺少组织上下文");
        }
        if (operatorMemberCardId == null || operatorMemberCardId.isBlank()) {
            return Result.failure("OPERATOR_MISSING", "缺少成员卡片上下文");
        }

        int pageOneBased = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int pageNumZeroBased = pageOneBased - 1;

        String sortKey = normalizeSortKey(sort);
        SortWay sortWay = parseSortWay(order);

        String memberCardTypeId = SystemSchemaIds.memberCardTypeId(orgId);
        String emailFieldId = SystemSchemaIds.memberEmailFieldId(orgId);
        String teamLinkFieldId = SystemSchemaIds.teamMemberLinkTypeId(orgId) + ":TARGET";

        CardPageQueryRequest queryRequest = buildBaseMemberQuery(
                orgId, operatorMemberCardId, memberCardTypeId, keyword,
                emailFieldId, teamLinkFieldId);

        if (SORT_LAST_SEEN.equals(sortKey)) {
            return listSortedByLastSeen(
                    orgId,
                    operatorMemberCardId,
                    queryRequest,
                    emailFieldId,
                    teamLinkFieldId,
                    pageOneBased,
                    pageSize,
                    sortWay);
        }

        Sort cardSort = buildCardSort(sortKey, emailFieldId, sortWay);
        SortAndPage sortAndPage = new SortAndPage();
        Page pageParam = new Page();
        pageParam.setPageNum(pageNumZeroBased);
        pageParam.setPageSize(pageSize);
        sortAndPage.setPage(pageParam);
        sortAndPage.setSorts(List.of(cardSort));
        queryRequest.setSortAndPage(sortAndPage);

        Result<PageResult<CardDTO>> cardResult = cardServiceClient.pageQuery(operatorMemberCardId, queryRequest);
        if (!cardResult.isSuccess()) {
            log.warn("成员目录卡片查询失败: {}", cardResult.getMessage());
            return Result.failure(cardResult.getCode(), cardResult.getMessage());
        }

        PageResult<CardDTO> cardPage = cardResult.getData();
        List<CardDTO> cards = cardPage.getContent() != null ? cardPage.getContent() : List.of();

        List<WorkspaceMemberRowDTO> rows = buildRows(
                orgId, cards, emailFieldId, teamLinkFieldId);

        PageResult<WorkspaceMemberRowDTO> pageResult = new PageResult<>(
                rows,
                pageOneBased,
                pageSize,
                cardPage.getTotal());
        return Result.success(pageResult);
    }

    /**
     * 上次在线来自 user-service，图库无法按该字段分页排序：拉齐当前筛选下的全部成员卡片后内存排序再分页。
     */
    private Result<PageResult<WorkspaceMemberRowDTO>> listSortedByLastSeen(
            String orgId,
            String operatorMemberCardId,
            CardPageQueryRequest queryRequest,
            String emailFieldId,
            String teamLinkFieldId,
            int pageOneBased,
            int pageSize,
            SortWay sortWay) {

        Sort stableTitleSort = buildCardSort(SORT_NAME, emailFieldId, SortWay.ASC);
        List<CardDTO> allCards = new ArrayList<>();
        int batchPage = 0;
        long total = -1;

        while (true) {
            SortAndPage sortAndPage = new SortAndPage();
            Page pageParam = new Page();
            pageParam.setPageNum(batchPage);
            pageParam.setPageSize(FETCH_BATCH_SIZE);
            sortAndPage.setPage(pageParam);
            sortAndPage.setSorts(List.of(stableTitleSort));
            queryRequest.setSortAndPage(sortAndPage);

            Result<PageResult<CardDTO>> cardResult = cardServiceClient.pageQuery(operatorMemberCardId, queryRequest);
            if (!cardResult.isSuccess()) {
                log.warn("成员目录卡片查询失败: {}", cardResult.getMessage());
                return Result.failure(cardResult.getCode(), cardResult.getMessage());
            }
            PageResult<CardDTO> cardPage = cardResult.getData();
            if (total < 0) {
                total = cardPage.getTotal();
            }
            List<CardDTO> chunk = cardPage.getContent() != null ? cardPage.getContent() : List.of();
            if (chunk.isEmpty()) {
                break;
            }
            allCards.addAll(chunk);
            if (chunk.size() < FETCH_BATCH_SIZE) {
                break;
            }
            batchPage++;
        }

        List<WorkspaceMemberRowDTO> rows = buildRows(orgId, allCards, emailFieldId, teamLinkFieldId);

        Comparator<WorkspaceMemberRowDTO> byLastLogin = Comparator.comparing(
                WorkspaceMemberRowDTO::getLastLoginAt,
                sortWay == SortWay.DESC
                        ? Comparator.nullsLast(Comparator.reverseOrder())
                        : Comparator.nullsLast(Comparator.naturalOrder()));
        rows.sort(byLastLogin);

        int from = (pageOneBased - 1) * pageSize;
        if (from >= rows.size()) {
            return Result.success(new PageResult<>(List.of(), pageOneBased, pageSize, total >= 0 ? total : rows.size()));
        }
        int to = Math.min(from + pageSize, rows.size());
        List<WorkspaceMemberRowDTO> pageRows = new ArrayList<>(rows.subList(from, to));

        long totalOut = total >= 0 ? total : rows.size();
        return Result.success(new PageResult<>(pageRows, pageOneBased, pageSize, totalOut));
    }

    private static String normalizeSortKey(String sort) {
        if (sort == null || sort.isBlank()) {
            return SORT_NAME;
        }
        String s = sort.trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case SORT_EMAIL, SORT_JOINED, SORT_LAST_SEEN -> s;
            default -> SORT_NAME;
        };
    }

    private static SortWay parseSortWay(String order) {
        if (order != null && order.trim().equalsIgnoreCase("desc")) {
            return SortWay.DESC;
        }
        return SortWay.ASC;
    }

    private static Sort buildCardSort(String sortKey, String emailFieldId, SortWay way) {
        Sort sort = new Sort();
        SortField sortField = new SortField();
        switch (sortKey) {
            case SORT_EMAIL -> sortField.setFieldId(emailFieldId);
            case SORT_JOINED -> sortField.setFieldId("createdAt");
            default -> sortField.setFieldId("title");
        }
        sort.setSortField(sortField);
        sort.setSortWay(way);
        return sort;
    }

    private CardPageQueryRequest buildBaseMemberQuery(
            String orgId,
            String operatorMemberCardId,
            String memberCardTypeId,
            String keyword,
            String emailFieldId,
            String teamLinkFieldId) {

        CardPageQueryRequest queryRequest = new CardPageQueryRequest();

        QueryContext queryContext = new QueryContext();
        queryContext.setOrgId(orgId);
        queryContext.setOperatorId(operatorMemberCardId);
        queryRequest.setQueryContext(queryContext);

        QueryScope queryScope = new QueryScope();
        queryScope.setCardTypeIds(List.of(memberCardTypeId));
        queryScope.setCardCycles(List.of(CardCycle.ACTIVE));
        queryRequest.setQueryScope(queryScope);

        if (keyword != null && !keyword.isBlank()) {
            TitleConditionItem titleCondition = new TitleConditionItem(
                    new TitleConditionItem.TitleSubject(null),
                    new TitleConditionItem.TitleOperator.Contains(keyword.trim())
            );
            queryRequest.setCondition(Condition.of(titleCondition));
        }

        YieldField yieldField = new YieldField();
        yieldField.setAllFields(false);
        yieldField.setFieldIds(Set.of(emailFieldId));

        YieldLink teamLink = new YieldLink();
        teamLink.setLinkFieldId(teamLinkFieldId);
        Yield teamTargetYield = new Yield();
        teamTargetYield.setField(YieldField.basic());
        teamLink.setTargetYield(teamTargetYield);

        Yield yield = new Yield();
        yield.setField(yieldField);
        yield.setLinks(List.of(teamLink));
        queryRequest.setYield(yield);

        return queryRequest;
    }

    private List<WorkspaceMemberRowDTO> buildRows(
            String orgId,
            List<CardDTO> cards,
            String emailFieldId,
            String teamLinkFieldId) {

        List<String> memberCardIds = cards.stream()
                .map(c -> c.getId().asStr())
                .toList();

        Map<String, MemberCardDirectoryEnrichmentDTO> enrichmentByCardId =
                enrichInBatches(orgId, memberCardIds);

        List<WorkspaceMemberRowDTO> rows = new ArrayList<>();
        for (CardDTO card : cards) {
            String cardId = card.getId().asStr();
            MemberCardDirectoryEnrichmentDTO en = enrichmentByCardId.get(cardId);

            String name = "";
            if (card.getTitle() != null) {
                name = card.getTitle().getDisplayValue();
            }

            String email = null;
            var fv = card.getFieldValue(emailFieldId);
            if (fv instanceof TextFieldValue tv) {
                email = tv.getValue();
            }

            List<String> teamNames = extractTeamNames(card, teamLinkFieldId);
            LocalDateTime lastLogin = en != null ? en.lastLoginAt() : null;

            rows.add(new WorkspaceMemberRowDTO(
                    cardId,
                    name,
                    email,
                    teamNames,
                    en != null ? en.role() : null,
                    card.getCreatedAt(),
                    lastLogin));
        }
        return rows;
    }

    private Map<String, MemberCardDirectoryEnrichmentDTO> enrichInBatches(String orgId, List<String> memberCardIds) {
        Map<String, MemberCardDirectoryEnrichmentDTO> enrichmentByCardId = new HashMap<>();
        if (memberCardIds.isEmpty()) {
            return enrichmentByCardId;
        }
        for (int i = 0; i < memberCardIds.size(); i += ENRICH_BATCH_SIZE) {
            int to = Math.min(i + ENRICH_BATCH_SIZE, memberCardIds.size());
            List<String> sub = memberCardIds.subList(i, to);
            Result<List<MemberCardDirectoryEnrichmentDTO>> enrichResult =
                    userServiceContract.enrichMembersByMemberCards(orgId, new MemberCardIdsRequest(sub));
            if (enrichResult.isSuccess() && enrichResult.getData() != null) {
                for (MemberCardDirectoryEnrichmentDTO e : enrichResult.getData()) {
                    enrichmentByCardId.put(e.memberCardId(), e);
                }
            } else {
                log.warn("成员目录 user-service 补齐失败: {}", enrichResult != null ? enrichResult.getMessage() : "null");
            }
        }
        return enrichmentByCardId;
    }

    private static List<String> extractTeamNames(CardDTO card, String teamLinkFieldId) {
        Set<CardDTO> teams = card.getLinkedCards(teamLinkFieldId);
        if (teams == null || teams.isEmpty()) {
            return List.of();
        }
        return teams.stream()
                .map(c -> c.getTitle() != null ? c.getTitle().getDisplayValue() : "")
                .filter(s -> !s.isBlank())
                .sorted()
                .toList();
    }
}
