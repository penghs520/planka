package dev.planka.api.schema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 引用节点 DTO
 * <p>
 * 表示引用图中的一个节点（被引用或引用的 Schema）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceNodeDTO {

    /** Schema ID */
    private String schemaId;

    /** Schema 名称 */
    private String schemaName;

    /** Schema 类型 */
    private String schemaType;

    /** 引用类型：COMPOSITION（组合）或 AGGREGATION（聚合） */
    private String referenceType;
}
