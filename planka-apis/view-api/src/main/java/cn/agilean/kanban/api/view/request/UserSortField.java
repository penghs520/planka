package dev.planka.api.view.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户自定义排序字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSortField {

    /**
     * 字段ID
     */
    private String fieldId;

    /**
     * 排序方向
     */
    private SortDirection direction;

    public enum SortDirection {
        ASC,
        DESC
    }

    public static UserSortField asc(String fieldId) {
        return new UserSortField(fieldId, SortDirection.ASC);
    }

    public static UserSortField desc(String fieldId) {
        return new UserSortField(fieldId, SortDirection.DESC);
    }
}
