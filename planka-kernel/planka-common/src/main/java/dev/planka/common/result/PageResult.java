package dev.planka.common.result;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果
 *
 * @param <T> 数据类型
 */
@Setter
@Getter
public class PageResult<T> {

    /** 当前页码（从0开始） */
    private int page;

    /** 每页大小 */
    private int size;

    /** 总记录数 */
    private long total;

    /** 总页数 */
    private int totalPages;

    /** 数据列表 */
    private List<T> content;

    /** 是否有上一页 */
    private boolean hasPrevious;

    /** 是否有下一页 */
    private boolean hasNext;

    public PageResult() {
        this.content = Collections.emptyList();
    }

    public PageResult(List<T> content, int page, int size, long total) {
        this.content = content != null ? content : Collections.emptyList();
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        this.hasPrevious = page > 0;
        this.hasNext = page < totalPages - 1;
    }

    // ==================== 静态工厂方法 ====================

    public static <T> PageResult<T> of(List<T> content, int page, int size, long total) {
        return new PageResult<>(content, page, size, total);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(Collections.emptyList(), 0, 0, 0);
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(Collections.emptyList(), page, size, 0);
    }

    // ==================== Getter/Setter ====================

}
