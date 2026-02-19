package dev.planka.user.mapper;

import dev.planka.user.model.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM sys_user WHERE email = #{email} AND deleted_at IS NULL")
    UserEntity selectByEmail(String email);

    /**
     * 检查是否存在超级管理员
     */
    @Select("SELECT COUNT(*) > 0 FROM sys_user WHERE super_admin = 1 AND deleted_at IS NULL")
    boolean existsSuperAdmin();
}
