package cn.planka.api.card.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 级联树形可选项查询请求
 * <p>
 * 二选一：{@code cascadeFieldId}（属性编辑器）或 {@code cascadeRelationId}（工作区侧栏等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CascadeFieldOptionsRequest {

    /**
     * 级联属性定义 ID（与 cascadeRelationId 互斥）
     */
    private String cascadeFieldId;

    /**
     * 级联关系定义 ID（与 cascadeFieldId 互斥）
     */
    private String cascadeRelationId;

    @AssertTrue(message = "须且仅能指定 cascadeFieldId 或 cascadeRelationId 之一")
    public boolean isExactlyOneCascadeFieldQueryKey() {
        boolean hasField = cascadeFieldId != null && !cascadeFieldId.isBlank();
        boolean hasCascadeRelation = cascadeRelationId != null && !cascadeRelationId.isBlank();
        return hasField != hasCascadeRelation;
    }
}
