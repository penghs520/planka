package dev.planka.user.mapper;

import dev.planka.user.model.RefreshTokenEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 刷新令牌Mapper
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshTokenEntity> {

    /**
     * 根据token哈希查询
     */
    @Select("SELECT * FROM sys_refresh_token WHERE token_hash = #{tokenHash} AND revoked = 0 AND expires_at > NOW()")
    RefreshTokenEntity selectByTokenHash(String tokenHash);

    /**
     * 撤销用户所有token
     */
    @Update("UPDATE sys_refresh_token SET revoked = 1 WHERE user_id = #{userId} AND revoked = 0")
    int revokeAllByUserId(String userId);
}
