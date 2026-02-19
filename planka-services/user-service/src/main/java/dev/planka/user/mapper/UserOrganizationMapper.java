package dev.planka.user.mapper;

import dev.planka.user.model.UserOrganizationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-组织关系Mapper
 */
@Mapper
public interface UserOrganizationMapper extends BaseMapper<UserOrganizationEntity> {

    /**
     * 根据用户ID查询其所有组织关系
     */
    @Select("SELECT * FROM sys_user_organization WHERE user_id = #{userId} AND status = 'ACTIVE'")
    List<UserOrganizationEntity> selectByUserId(String userId);

    /**
     * 根据组织ID查询所有成员关系
     */
    @Select("SELECT * FROM sys_user_organization WHERE org_id = #{orgId} AND status = 'ACTIVE'")
    List<UserOrganizationEntity> selectByOrgId(String orgId);

    /**
     * 查询用户在组织中的关系
     */
    @Select("SELECT * FROM sys_user_organization WHERE user_id = #{userId} AND org_id = #{orgId}")
    UserOrganizationEntity selectByUserIdAndOrgId(@Param("userId") String userId, @Param("orgId") String orgId);
}
