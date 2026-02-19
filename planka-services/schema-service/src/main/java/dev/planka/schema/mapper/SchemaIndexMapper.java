package dev.planka.schema.mapper;

import dev.planka.schema.model.SchemaIndexEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Schema 二级索引 Mapper 接口
 */
@Mapper
public interface SchemaIndexMapper extends BaseMapper<SchemaIndexEntity> {

    /**
     * 按索引类型和键值查询Schema ID列表
     */
    @Select("SELECT schema_id FROM schema_index WHERE index_type = #{indexType} AND index_key = #{indexKey}")
    List<String> findSchemaIdsByTypeAndKey(@Param("indexType") String indexType, @Param("indexKey") String indexKey);

    /**
     * 按Schema ID查询所有索引
     */
    @Select("SELECT * FROM schema_index WHERE schema_id = #{schemaId}")
    List<SchemaIndexEntity> findBySchemaId(@Param("schemaId") String schemaId);

    /**
     * 删除Schema的所有索引
     */
    @Delete("DELETE FROM schema_index WHERE schema_id = #{schemaId}")
    int deleteBySchemaId(@Param("schemaId") String schemaId);

    /**
     * 查询全量索引数据
     */
    @Select("SELECT * FROM schema_index")
    List<SchemaIndexEntity> findAll();
}
