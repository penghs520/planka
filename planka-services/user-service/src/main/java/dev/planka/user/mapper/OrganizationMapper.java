package dev.planka.user.mapper;

import dev.planka.user.model.OrganizationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 组织Mapper
 */
@Mapper
public interface OrganizationMapper extends BaseMapper<OrganizationEntity> {
}
