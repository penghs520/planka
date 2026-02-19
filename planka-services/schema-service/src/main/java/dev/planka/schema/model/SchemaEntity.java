package dev.planka.schema.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Schema 数据库实体
 */
@Data
@TableName("schema_definition")
public class SchemaEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String orgId;

    private String schemaType;

    private String name;

    private String content;

    private String state;

    private Integer contentVersion;

    private String structureVersion;

    private String belongTo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private String updatedBy;

    private LocalDateTime deletedAt;
}
