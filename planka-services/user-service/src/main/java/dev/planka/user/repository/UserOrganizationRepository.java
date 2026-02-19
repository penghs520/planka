package dev.planka.user.repository;

import dev.planka.user.mapper.UserOrganizationMapper;
import dev.planka.user.model.UserOrganizationEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户-组织关系仓储
 */
@Repository
@RequiredArgsConstructor
public class UserOrganizationRepository {

    private final UserOrganizationMapper userOrganizationMapper;

    public Optional<UserOrganizationEntity> findById(String id) {
        return Optional.ofNullable(userOrganizationMapper.selectById(id));
    }

    public Optional<UserOrganizationEntity> findByUserIdAndOrgId(String userId, String orgId) {
        return Optional.ofNullable(userOrganizationMapper.selectByUserIdAndOrgId(userId, orgId));
    }

    public List<UserOrganizationEntity> findByUserId(String userId) {
        return userOrganizationMapper.selectByUserId(userId);
    }

    public List<UserOrganizationEntity> findByOrgId(String orgId) {
        return userOrganizationMapper.selectByOrgId(orgId);
    }

    public Page<UserOrganizationEntity> findByOrgId(String orgId, int page, int size) {
        Page<UserOrganizationEntity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserOrganizationEntity::getOrgId, orgId);
        wrapper.eq(UserOrganizationEntity::getStatus, "ACTIVE");
        wrapper.orderByDesc(UserOrganizationEntity::getJoinedAt);
        return userOrganizationMapper.selectPage(pageParam, wrapper);
    }

    public void save(UserOrganizationEntity entity) {
        if (entity.getJoinedAt() == null) {
            entity.setJoinedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());

        if (userOrganizationMapper.selectById(entity.getId()) == null) {
            userOrganizationMapper.insert(entity);
        } else {
            userOrganizationMapper.updateById(entity);
        }
    }

    public void delete(String id) {
        userOrganizationMapper.deleteById(id);
    }

    public void deleteByOrgId(String orgId) {
        LambdaQueryWrapper<UserOrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserOrganizationEntity::getOrgId, orgId);
        userOrganizationMapper.delete(wrapper);
    }
}
