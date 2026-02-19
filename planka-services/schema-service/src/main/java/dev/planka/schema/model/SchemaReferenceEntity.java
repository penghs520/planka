package dev.planka.schema.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Schema 引用关系数据库实体
 */
@Data
@TableName("schema_reference")
public class SchemaReferenceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceId;

    private String sourceType;

    private String targetId;

    private String targetType;

    private String referenceType;
}
