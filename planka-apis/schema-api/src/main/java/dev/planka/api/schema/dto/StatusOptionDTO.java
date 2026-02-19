package dev.planka.api.schema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 价值流状态选项 DTO
 * <p>
 * 用于状态下拉选择，只包含必要的显示信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusOptionDTO {
    /**
     * 状态 ID
     */
    private String id;

    /**
     * 状态名称
     */
    private String name;

    /**
     * 阶段类型：TODO, IN_PROGRESS, DONE, CANCELLED
     */
    private String stepKind;
}
