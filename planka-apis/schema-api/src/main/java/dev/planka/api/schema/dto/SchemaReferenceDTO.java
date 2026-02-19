package dev.planka.api.schema.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Schema 引用关系数据传输对象
 */
@Getter
@Setter
public class SchemaReferenceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 引用关系ID */
    private Long id;

    /** 源 Schema ID */
    private String sourceId;

    /** 源 Schema 类型 */
    private String sourceType;

    /** 源 Schema 名称 */
    private String sourceName;

    /** 目标 Schema ID */
    private String targetId;

    /** 目标 Schema 类型 */
    private String targetType;

    /** 目标 Schema 名称 */
    private String targetName;

    /** 引用类型（COMPOSITION/AGGREGATION） */
    private String referenceType;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
