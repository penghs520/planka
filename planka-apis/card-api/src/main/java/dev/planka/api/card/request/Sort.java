package dev.planka.api.card.request;

import lombok.Data;

/**
 * 排序项
 */
@Data
public class Sort {
    /** 排序字段 */
    private SortField sortField;

    /** 排序方式 */
    private SortWay sortWay;
}
