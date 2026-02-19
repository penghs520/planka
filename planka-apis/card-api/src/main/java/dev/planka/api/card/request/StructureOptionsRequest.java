package dev.planka.api.card.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 架构属性可选项查询请求
 * <p>
 * 用于查询架构属性编辑器中的树形可选项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructureOptionsRequest {

    /**
     * 架构属性定义ID
     */
    @NotBlank(message = "架构属性ID不能为空")
    private String structureFieldId;
}
