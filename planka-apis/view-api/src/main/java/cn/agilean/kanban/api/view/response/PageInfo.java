package dev.planka.api.view.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo {

    /**
     * 当前页码（从0开始）
     */
    private int page;

    /**
     * 每页大小
     */
    private int size;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    /**
     * 根据总数和分页参数计算分页信息
     */
    public static PageInfo of(int page, int size, long total) {
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return PageInfo.builder()
                .page(page)
                .size(size)
                .total(total)
                .totalPages(totalPages)
                .hasPrevious(page > 0)
                .hasNext(page < totalPages - 1)
                .build();
    }
}
