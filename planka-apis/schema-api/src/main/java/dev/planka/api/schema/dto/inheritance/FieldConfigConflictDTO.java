package dev.planka.api.schema.dto.inheritance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 属性配置冲突信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldConfigConflictDTO {

    /** 冲突的属性ID */
    private String fieldId;

    /** 冲突的属性名称 */
    private String fieldName;

    /** 冲突来源列表 */
    private List<ConflictingConfigDTO> conflictingSources;

    /**
     * 冲突解决状态
     * - UNRESOLVED: 未解决（需要创建自有配置覆盖）
     * - RESOLVED_BY_SELF: 已解决（自有配置已覆盖）
     */
    private String resolution;
}
