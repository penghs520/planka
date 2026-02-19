package dev.planka.view.converter;

import dev.planka.api.view.request.UserSortField;
import dev.planka.api.view.request.ViewDataRequest;
import dev.planka.common.page.SortDirection;
import dev.planka.domain.card.CardStyle;
import dev.planka.domain.link.LinkFieldIdUtils;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.view.ListViewDefinition;
import dev.planka.api.card.request.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 视图定义到卡片查询请求的转换器
 */
@Component
@RequiredArgsConstructor
public class ViewToQueryConverter {

    private final ConditionMerger conditionMerger;

    /**
     * 将列表视图定义和用户请求转换为卡片分页查询请求
     *
     * @param viewDef    列表视图定义
     * @param request    用户请求
     * @param operatorId 操作人ID
     * @return 卡片分页查询请求
     */
    public CardPageQueryRequest convert(ListViewDefinition viewDef, ViewDataRequest request, String operatorId) {
        CardPageQueryRequest queryRequest = new CardPageQueryRequest();

        // 1. 设置查询上下文
        QueryContext context = buildQueryContext(viewDef, operatorId);
        queryRequest.setQueryContext(context);

        // 2. 设置查询范围（卡片类型）
        QueryScope scope = buildQueryScope(viewDef);
        queryRequest.setQueryScope(scope);

        // 3. 合并条件（视图条件 + 用户条件）
        Condition mergedCondition = conditionMerger.merge(
                viewDef.getCondition(),
                request != null ? request.getAdditionalCondition() : null
        );
        queryRequest.setCondition(mergedCondition);

        // 4. 构建 Yield（根据列配置决定返回哪些字段）
        Yield yield = buildYield(viewDef);
        queryRequest.setYield(yield);

        // 5. 设置排序和分页（用户参数优先，否则使用视图配置）
        SortAndPage sortAndPage = buildSortAndPage(viewDef, request);
        queryRequest.setSortAndPage(sortAndPage);

        return queryRequest;
    }

    private QueryContext buildQueryContext(ListViewDefinition viewDef, String operatorId) {
        QueryContext context = new QueryContext();
        context.setOrgId(viewDef.getOrgId());
        context.setOperatorId(operatorId);
        return context;
    }

    private QueryScope buildQueryScope(ListViewDefinition viewDef) {
        QueryScope scope = new QueryScope();
        scope.setCardTypeIds(List.of(viewDef.getCardTypeId().value()));
        // 默认只查活跃卡片
        scope.setCardStyles(List.of(CardStyle.ACTIVE));
        return scope;
    }

    private Yield buildYield(ListViewDefinition viewDef) {
        Yield yield = new Yield();
        YieldField field = new YieldField();

        if (viewDef.getColumnConfigs() == null || viewDef.getColumnConfigs().isEmpty()) {
            // 如果没有列配置，返回所有字段
            field.setAllFields(true);
            field.setIncludeDescription(true);
        } else {
            // 区分普通字段和关联字段
            Set<String> normalFieldIds = new HashSet<>();
            List<YieldLink> yieldLinks = new ArrayList<>();

            for (ListViewDefinition.ColumnConfig columnConfig : viewDef.getColumnConfigs()) {
                if (!columnConfig.isVisible()) {
                    continue;
                }

                String fieldId = columnConfig.getFieldId();

                // 判断是否为关联字段（格式: "{linkTypeId}:{SOURCE|TARGET}"）
                if (LinkFieldIdUtils.isValidFormat(fieldId)) {
                    // 关联字段：添加到 YieldLink 列表
                    YieldLink yieldLink = new YieldLink();
                    yieldLink.setLinkFieldId(fieldId);
                    // 关联卡片只需要返回基本信息（标题等）
                    yieldLink.setTargetYield(null);
                    yieldLinks.add(yieldLink);
                } else {
                    // 普通字段：添加到 fieldIds
                    normalFieldIds.add(fieldId);
                }
            }

            field.setFieldIds(normalFieldIds);
            field.setAllFields(false);
            field.setIncludeDescription(false);

            // 设置关联字段
            if (!yieldLinks.isEmpty()) {
                yield.setLinks(yieldLinks);
            }
        }

        yield.setField(field);
        return yield;
    }

    private SortAndPage buildSortAndPage(ListViewDefinition viewDef, ViewDataRequest request) {
        SortAndPage sortAndPage = new SortAndPage();

        // 分页
        Page page = buildPage(viewDef, request);
        sortAndPage.setPage(page);

        // 排序：用户参数优先，否则使用视图配置
        List<Sort> sorts = buildSorts(viewDef, request);
        sortAndPage.setSorts(sorts);

        return sortAndPage;
    }

    private Page buildPage(ListViewDefinition viewDef, ViewDataRequest request) {
        Page page = new Page();

        int defaultPageSize = viewDef.getPageConfig() != null
                ? viewDef.getPageConfig().getDefaultPageSize()
                : 20;

        if (request != null && request.getPage() != null) {
            page.setPageNum(request.getPage());
        } else {
            page.setPageNum(0);
        }

        if (request != null && request.getSize() != null) {
            page.setPageSize(request.getSize());
        } else {
            page.setPageSize(defaultPageSize);
        }

        return page;
    }

    private List<Sort> buildSorts(ListViewDefinition viewDef, ViewDataRequest request) {
        // 用户排序优先
        if (request != null && request.getSorts() != null && !request.getSorts().isEmpty()) {
            return convertUserSorts(request.getSorts());
        }

        // 使用视图配置的排序
        if (viewDef.getSorts() != null && !viewDef.getSorts().isEmpty()) {
            return convertViewSorts(viewDef.getSorts());
        }

        // 默认不排序
        return new ArrayList<>();
    }

    private List<Sort> convertUserSorts(List<UserSortField> userSorts) {
        return userSorts.stream()
                .map(this::convertUserSort)
                .collect(Collectors.toList());
    }

    private Sort convertUserSort(UserSortField userSort) {
        Sort sort = new Sort();

        SortField sortField = new SortField();
        sortField.setFieldId(userSort.getFieldId());
        sort.setSortField(sortField);

        sort.setSortWay(userSort.getDirection() == UserSortField.SortDirection.ASC
                ? SortWay.ASC
                : SortWay.DESC);

        return sort;
    }

    private List<Sort> convertViewSorts(List<dev.planka.common.page.SortField> viewSorts) {
        return viewSorts.stream()
                .map(this::convertViewSort)
                .collect(Collectors.toList());
    }

    private Sort convertViewSort(dev.planka.common.page.SortField viewSort) {
        Sort sort = new Sort();

        SortField sortField = new SortField();
        sortField.setFieldId(viewSort.getField());
        sort.setSortField(sortField);

        sort.setSortWay(viewSort.getDirection() == SortDirection.ASC
                ? SortWay.ASC
                : SortWay.DESC);

        return sort;
    }
}
