package dev.planka.api.card.request;

import lombok.Data;

import java.util.List;

/**
 * 排序和分页
 */
@Data
public class SortAndPage {
    /** 分页信息 */
    private Page page;

    /** 排序列表 */
    private List<Sort> sorts;
}
