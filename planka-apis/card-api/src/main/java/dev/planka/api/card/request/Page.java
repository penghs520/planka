package dev.planka.api.card.request;

import lombok.Data;

/**
 * 分页信息
 */
@Data
public class Page {
    /** 页码，从0开始 */
    private int pageNum;

    /** 页大小 */
    private int pageSize;
}
