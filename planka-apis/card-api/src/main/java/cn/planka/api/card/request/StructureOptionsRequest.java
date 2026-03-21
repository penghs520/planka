package cn.planka.api.card.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 架构树形可选项查询请求
 * <p>
 * 二选一：{@code structureFieldId}（属性编辑器）或 {@code structureId}（工作区侧栏等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructureOptionsRequest {

    /**
     * 架构属性定义 ID（与 structureId 互斥）
     */
    private String structureFieldId;

    /**
     * 架构线定义 ID（与 structureFieldId 互斥）
     */
    private String structureId;

    @AssertTrue(message = "须且仅能指定 structureFieldId 或 structureId 之一")
    public boolean isExactlyOneStructureQueryKey() {
        boolean hasField = structureFieldId != null && !structureFieldId.isBlank();
        boolean hasStructure = structureId != null && !structureId.isBlank();
        return hasField != hasStructure;
    }
}
