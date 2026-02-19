package dev.planka.schema.mapper;

import dev.planka.schema.model.SchemaEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Schema Mapper 接口
 */
@Mapper
public interface SchemaMapper extends BaseMapper<SchemaEntity> {

    /**
     * 按组织ID和类型查询（排除已删除）
     */
    @Select("SELECT * FROM schema_definition WHERE org_id = #{orgId} AND schema_type = #{schemaType} AND state != 'DELETED' ORDER BY created_at DESC")
    List<SchemaEntity> findAllByOrgIdAndType(@Param("orgId") String orgId, @Param("schemaType") String schemaType);

    /**
     * 按组织ID和类型分页查询（排除已删除）
     */
    @Select("SELECT * FROM schema_definition WHERE org_id = #{orgId} AND schema_type = #{schemaType} AND state != 'DELETED' ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<SchemaEntity> findByOrgIdAndTypePaged(@Param("orgId") String orgId, @Param("schemaType") String schemaType, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计组织下某类型的数量（排除已删除）
     */
    @Select("SELECT COUNT(*) FROM schema_definition WHERE org_id = #{orgId} AND schema_type = #{schemaType} AND state != 'DELETED'")
    long countByOrgIdAndType(@Param("orgId") String orgId, @Param("schemaType") String schemaType);

    /**
     * 按所属Schema ID查询
     */
    @Select("SELECT * FROM schema_definition WHERE belong_to = #{belongTo} AND state != 'DELETED'")
    List<SchemaEntity> findByBelongTo(@Param("belongTo") String belongTo);

    /**
     * 批量查询
     */
    @Select("<script>" +
            "SELECT * FROM schema_definition WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND state != 'DELETED'" +
            "</script>")
    List<SchemaEntity> selectByIds(@Param("ids") List<String> ids);

    /**
     * 批量查询（包含已删除）
     * <p>
     * 主要用于操作历史等需要显示已删除 Schema 名称的场景
     */
    @Select("<script>" +
            "SELECT * FROM schema_definition WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<SchemaEntity> selectByIdsWithDeleted(@Param("ids") List<String> ids);
}
