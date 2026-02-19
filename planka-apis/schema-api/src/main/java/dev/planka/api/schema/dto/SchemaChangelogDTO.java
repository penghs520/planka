package dev.planka.api.schema.dto;

import dev.planka.domain.schema.changelog.ChangeDetail;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Schema 变更日志数据传输对象
 */
@Getter
@Setter
public class SchemaChangelogDTO {

    /** 日志ID */
    private Long id;

    /** Schema ID */
    private String schemaId;

    /** Schema 名称（用于显示附属 Schema 的变更时区分来源） */
    private String schemaName;

    /** Schema 类型 */
    private String schemaType;

    /** 操作类型（CREATE/UPDATE/DELETE） */
    private String action;

    /** 变更后的版本号 */
    private int contentVersion;

    /** 变更前快照（JSON） */
    private String beforeSnapshot;

    /** 变更后快照（JSON） */
    private String afterSnapshot;

    /** 变更摘要（人可读） */
    private String changeSummary;

    /** 结构化变更详情 */
    private ChangeDetail changeDetail;

    /** 变更时间 */
    private LocalDateTime changedAt;

    /** 变更人ID */
    private String changedBy;

    /** 变更人名称 */
    private String changedByName;

    /** 分布式追踪ID */
    private String traceId;
}
