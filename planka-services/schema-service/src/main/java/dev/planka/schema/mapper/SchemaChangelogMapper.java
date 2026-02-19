package dev.planka.schema.mapper;

import dev.planka.schema.model.SchemaChangelogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Schema 变更日志 Mapper 接口
 */
@Mapper
public interface SchemaChangelogMapper extends BaseMapper<SchemaChangelogEntity> {

    /**
     * 按Schema ID查询变更日志（按时间倒序）
     */
    @Select("SELECT * FROM schema_changelog WHERE schema_id = #{schemaId} ORDER BY changed_at DESC LIMIT #{limit}")
    List<SchemaChangelogEntity> findBySchemaId(@Param("schemaId") String schemaId, @Param("limit") int limit);

    /**
     * 按Schema ID分页查询变更日志（按时间倒序）
     */
    @Select("SELECT * FROM schema_changelog WHERE schema_id = #{schemaId} ORDER BY changed_at DESC LIMIT #{size} OFFSET #{offset}")
    List<SchemaChangelogEntity> findBySchemaIdWithPaging(@Param("schemaId") String schemaId, @Param("offset") int offset, @Param("size") int size);

    /**
     * 统计Schema变更日志数量
     */
    @Select("SELECT COUNT(*) FROM schema_changelog WHERE schema_id = #{schemaId}")
    long countBySchemaId(@Param("schemaId") String schemaId);

    /**
     * 按Schema ID查询全部变更日志（按时间倒序）
     */
    @Select("SELECT * FROM schema_changelog WHERE schema_id = #{schemaId} ORDER BY changed_at DESC")
    List<SchemaChangelogEntity> findAllBySchemaId(@Param("schemaId") String schemaId);

    /**
     * 按多个Schema ID分页查询变更日志（按时间倒序）
     */
    @Select("<script>" +
            "SELECT * FROM schema_changelog WHERE schema_id IN " +
            "<foreach collection='schemaIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY changed_at DESC LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<SchemaChangelogEntity> findBySchemaIdsWithPaging(
            @Param("schemaIds") List<String> schemaIds,
            @Param("offset") int offset,
            @Param("size") int size);

    /**
     * 统计多个Schema的变更日志数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM schema_changelog WHERE schema_id IN " +
            "<foreach collection='schemaIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    long countBySchemaIds(@Param("schemaIds") List<String> schemaIds);

    /**
     * 按多个Schema ID查询全部变更日志（按时间倒序）
     */
    @Select("<script>" +
            "SELECT * FROM schema_changelog WHERE schema_id IN " +
            "<foreach collection='schemaIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " ORDER BY changed_at DESC" +
            "</script>")
    List<SchemaChangelogEntity> findAllBySchemaIds(@Param("schemaIds") List<String> schemaIds);

    // ==================== 按多个Schema ID和关键字查询 ====================

    /**
     * 按多个Schema ID和关键字分页查询变更日志
     */
    @Select("<script>" +
            "SELECT * FROM schema_changelog WHERE schema_id IN " +
            "<foreach collection='schemaIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND change_summary LIKE CONCAT('%', #{keyword}, '%')" +
            " ORDER BY changed_at DESC LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<SchemaChangelogEntity> findBySchemaIdsAndKeywordWithPaging(
            @Param("schemaIds") List<String> schemaIds,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    /**
     * 统计多个Schema下匹配关键字的变更日志数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM schema_changelog WHERE schema_id IN " +
            "<foreach collection='schemaIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND change_summary LIKE CONCAT('%', #{keyword}, '%')" +
            "</script>")
    long countBySchemaIdsAndKeyword(@Param("schemaIds") List<String> schemaIds, @Param("keyword") String keyword);

    // ==================== 按多个Schema ID通用筛选查询 ====================

    /**
     * 按多个Schema ID通用筛选查询（支持 keyword、changedBy 的任意组合）
     */
    @Select("<script>" +
            "SELECT * FROM schema_changelog WHERE schema_id IN " +
            "<foreach collection='schemaIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "<if test='keyword != null and keyword != \"\"'> AND change_summary LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='changedBy != null and changedBy != \"\"'> AND changed_by = #{changedBy}</if>" +
            " ORDER BY changed_at DESC LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<SchemaChangelogEntity> findBySchemaIdsWithFilters(
            @Param("schemaIds") List<String> schemaIds,
            @Param("keyword") String keyword,
            @Param("changedBy") String changedBy,
            @Param("offset") int offset,
            @Param("size") int size);

    /**
     * 统计多个Schema ID通用筛选条件下的变更日志数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM schema_changelog WHERE schema_id IN " +
            "<foreach collection='schemaIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "<if test='keyword != null and keyword != \"\"'> AND change_summary LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='changedBy != null and changedBy != \"\"'> AND changed_by = #{changedBy}</if>" +
            "</script>")
    long countBySchemaIdsWithFilters(
            @Param("schemaIds") List<String> schemaIds,
            @Param("keyword") String keyword,
            @Param("changedBy") String changedBy);

    // ==================== 通用筛选查询（支持多条件组合） ====================

    /**
     * 通用筛选查询（支持 schemaType、keyword、changedBy 的任意组合）
     */
    @Select("<script>" +
            "SELECT * FROM schema_changelog WHERE org_id = #{orgId}" +
            "<if test='schemaType != null and schemaType != \"\"'> AND schema_type = #{schemaType}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND change_summary LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='changedBy != null and changedBy != \"\"'> AND changed_by = #{changedBy}</if>" +
            " ORDER BY changed_at DESC LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<SchemaChangelogEntity> findByFiltersWithPaging(
            @Param("orgId") String orgId,
            @Param("schemaType") String schemaType,
            @Param("keyword") String keyword,
            @Param("changedBy") String changedBy,
            @Param("offset") int offset,
            @Param("size") int size);

    /**
     * 统计通用筛选条件下的变更日志数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM schema_changelog WHERE org_id = #{orgId}" +
            "<if test='schemaType != null and schemaType != \"\"'> AND schema_type = #{schemaType}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND change_summary LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='changedBy != null and changedBy != \"\"'> AND changed_by = #{changedBy}</if>" +
            "</script>")
    long countByFilters(
            @Param("orgId") String orgId,
            @Param("schemaType") String schemaType,
            @Param("keyword") String keyword,
            @Param("changedBy") String changedBy);
}
