package dev.planka.user.repository;

import dev.planka.user.mapper.OrganizationMapper;
import dev.planka.user.model.OrganizationEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 组织仓储
 */
@Repository
@RequiredArgsConstructor
public class OrganizationRepository {

    private final OrganizationMapper organizationMapper;

    public Optional<OrganizationEntity> findById(String id) {
        return Optional.ofNullable(organizationMapper.selectById(id));
    }

    public void save(OrganizationEntity org) {
        if (org.getCreatedAt() == null) {
            org.setCreatedAt(LocalDateTime.now());
        }
        org.setUpdatedAt(LocalDateTime.now());

        if (organizationMapper.selectById(org.getId()) == null) {
            organizationMapper.insert(org);
        } else {
            organizationMapper.updateById(org);
        }
    }

    public void delete(String id) {
        OrganizationEntity org = organizationMapper.selectById(id);
        if (org != null) {
            org.setDeletedAt(LocalDateTime.now());
            organizationMapper.updateById(org);
        }
    }

    public Page<OrganizationEntity> findAll(int page, int size) {
        Page<OrganizationEntity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<OrganizationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(OrganizationEntity::getDeletedAt);
        wrapper.orderByDesc(OrganizationEntity::getCreatedAt);
        return organizationMapper.selectPage(pageParam, wrapper);
    }

    public List<OrganizationEntity> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return organizationMapper.selectBatchIds(ids);
    }
}
