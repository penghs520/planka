package dev.planka.common.page;

import dev.planka.common.constant.KanbanConstants;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页请求基类
 */
@Getter
@Setter
public class PageRequest{


    /** 页码（从0开始） */
    private int page = KanbanConstants.DEFAULT_PAGE;

    /** 每页大小 */
    private int size = KanbanConstants.DEFAULT_PAGE_SIZE;

    /** 排序字段 */
    private List<SortField> sorts;

    public PageRequest() {
        this.sorts = new ArrayList<>();
    }

    public PageRequest(int page, int size) {
        this.page = Math.max(0, page);
        this.size = Math.min(Math.max(1, size), KanbanConstants.MAX_PAGE_SIZE);
        this.sorts = new ArrayList<>();
    }

    /**
     * 添加排序字段
     */
    public PageRequest addSort(String field, SortDirection direction) {
        if (this.sorts == null) {
            this.sorts = new ArrayList<>();
        }
        this.sorts.add(new SortField(field, direction));
        return this;
    }

    /**
     * 获取偏移量
     */
    public int getOffset() {
        return page * size;
    }


}
