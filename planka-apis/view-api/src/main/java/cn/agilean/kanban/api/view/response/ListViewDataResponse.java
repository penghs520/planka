package dev.planka.api.view.response;

import dev.planka.api.card.dto.CardDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 列表视图数据响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ListViewDataResponse extends ViewDataResponse {

    private static final String VIEW_TYPE = "LIST";

    /**
     * 关联的卡片类型 ID
     */
    private String cardTypeId;

    /**
     * 列元数据（列ID、列名、列类型、宽度等）
     */
    private List<ColumnMeta> columns;

    /**
     * 卡片数据列表（非分组模式）
     */
    private List<CardDTO> cards;

    /**
     * 分组数据（分组模式）
     */
    private List<GroupedCardData> groups;

    /**
     * 是否分组展示
     */
    private boolean grouped;

    /**
     * 分页信息
     */
    private PageInfo pageInfo;

    /**
     * 价值流状态选项列表
     * <p>
     * 用于 $statusId 内置字段的渲染，提供状态ID到名称和颜色的映射
     */
    private List<StatusOption> statusOptions;

    @Override
    public String getViewType() {
        return VIEW_TYPE;
    }

    /**
     * 价值流状态选项
     */
    @Data
    public static class StatusOption {
        /**
         * 状态ID
         */
        private String id;

        /**
         * 状态名称
         */
        private String name;

        /**
         * 阶段类型：TODO, IN_PROGRESS, DONE, CANCELLED
         */
        private String stepKind;

        public StatusOption() {
        }

        public StatusOption(String id, String name, String stepKind) {
            this.id = id;
            this.name = name;
            this.stepKind = stepKind;
        }
    }

    /**
     * 创建非分组的列表视图响应
     */
    public static ListViewDataResponse flat(
            String viewId,
            String viewName,
            String cardTypeId,
            List<ColumnMeta> columns,
            List<CardDTO> cards,
            PageInfo pageInfo) {
        ListViewDataResponse response = new ListViewDataResponse();
        response.setViewId(viewId);
        response.setViewName(viewName);
        response.setCardTypeId(cardTypeId);
        response.setColumns(columns);
        response.setCards(cards);
        response.setGrouped(false);
        response.setPageInfo(pageInfo);
        return response;
    }

    /**
     * 创建分组的列表视图响应
     */
    public static ListViewDataResponse grouped(
            String viewId,
            String viewName,
            String cardTypeId,
            List<ColumnMeta> columns,
            List<GroupedCardData> groups) {
        ListViewDataResponse response = new ListViewDataResponse();
        response.setViewId(viewId);
        response.setViewName(viewName);
        response.setCardTypeId(cardTypeId);
        response.setColumns(columns);
        response.setGroups(groups);
        response.setGrouped(true);
        return response;
    }
}
