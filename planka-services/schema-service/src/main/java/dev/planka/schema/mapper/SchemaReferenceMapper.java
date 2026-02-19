package dev.planka.schema.mapper;

import dev.planka.schema.model.SchemaReferenceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Schema 引用关系 Mapper 接口
 */
@Mapper
public interface SchemaReferenceMapper extends BaseMapper<SchemaReferenceEntity> {

    /**
     * 按源Schema ID查询引用关系 (查询引用了谁)
     */
    @Select("SELECT * FROM schema_reference WHERE source_id = #{sourceId}")
    List<SchemaReferenceEntity> findBySourceId(@Param("sourceId") String sourceId);

    /**
     * 按目标Schema ID查询引用关系（查询被谁引用）
     */
    @Select("SELECT * FROM schema_reference WHERE target_id = #{targetId}")
    List<SchemaReferenceEntity> findByTargetId(@Param("targetId") String targetId);

    /**
     * 按目标Schema ID和引用类型查询（查询被谁引用）
     */
    @Select("SELECT * FROM schema_reference WHERE target_id = #{targetId} AND reference_type = #{referenceType}")
    List<SchemaReferenceEntity> findByTargetIdAndType(@Param("targetId") String targetId, @Param("referenceType") String referenceType);

    /**
     * 查询聚合引用的源Schema ID列表
     */
    @Select("SELECT source_id FROM schema_reference WHERE target_id = #{targetId} AND reference_type = 'AGGREGATION'")
    List<String> findAggregationSourceIds(@Param("targetId") String targetId);

    /**
     * 删除源Schema的所有引用关系
     */
    @Delete("DELETE FROM schema_reference WHERE source_id = #{sourceId}")
    int deleteBySourceId(@Param("sourceId") String sourceId);
}
