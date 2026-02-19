package dev.planka.common.page;

import lombok.Getter;
import lombok.Setter;

/**
 * 排序字段
 */
@Setter
@Getter
public class SortField {

    private String field;
    private SortDirection direction;

    public SortField() {
    }

    public SortField(String field, SortDirection direction) {
        this.field = field;
        this.direction = direction;
    }

}
