package dev.planka.api.schema.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Schema 引用摘要 DTO
 * <p>
 * 包含一个 Schema 的所有引用关系：
 * - outgoing: 该 Schema 引用了哪些其他 Schema
 * - incoming: 该 Schema 被哪些其他 Schema 引用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaReferenceSummaryDTO {

    /** Schema ID */
    private String schemaId;

    /** Schema 名称 */
    private String schemaName;

    /** Schema 类型 */
    private String schemaType;

    /** 出向引用：该 Schema 引用了哪些 Schema */
    private List<ReferenceNodeDTO> outgoing;

    /** 入向引用：该 Schema 被哪些 Schema 引用 */
    private List<ReferenceNodeDTO> incoming;
}
