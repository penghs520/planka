package dev.planka.api.card.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 卡片分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CardPageQueryRequest extends CardQueryRequest {
    /**
     * 排序和分页
     */
    private SortAndPage sortAndPage;
}
