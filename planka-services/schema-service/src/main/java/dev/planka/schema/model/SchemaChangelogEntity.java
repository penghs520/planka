package dev.planka.schema.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Schema 变更日志数据库实体
 */
@Data
@TableName("schema_changelog")
public class SchemaChangelogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String schemaId;

    private String orgId;

    private String schemaType;

    private String action;

    private Integer contentVersion;

    private String beforeSnapshot;

    private String afterSnapshot;

    private String changeSummary;

    /**
     * 结构化变更详情（JSON格式）
     */
    private String changeDetail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime changedAt;

    private String changedBy;

    private String traceId;
}
