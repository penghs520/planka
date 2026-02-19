package dev.planka.schema.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * Schema 二级索引数据库实体
 */
@Data
@TableName("schema_index")
public class SchemaIndexEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String schemaId;

    private String indexType;

    private String indexKey;
}
