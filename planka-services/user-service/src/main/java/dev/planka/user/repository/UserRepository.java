package dev.planka.user.repository;

import dev.planka.user.mapper.UserMapper;
import dev.planka.user.model.UserEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户仓储
 */
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserMapper userMapper;

    public Optional<UserEntity> findById(String id) {
        return Optional.ofNullable(userMapper.selectById(id));
    }

    public Optional<UserEntity> findByEmail(String email) {
        return Optional.ofNullable(userMapper.selectByEmail(email));
    }

    public boolean existsByEmail(String email) {
        return userMapper.selectByEmail(email) != null;
    }

    public boolean existsSuperAdmin() {
        return userMapper.existsSuperAdmin();
    }

    public void save(UserEntity user) {
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setUpdatedAt(LocalDateTime.now());

        if (userMapper.selectById(user.getId()) == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
    }

    public Page<UserEntity> findAll(int page, int size) {
        Page<UserEntity> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(UserEntity::getDeletedAt);
        wrapper.orderByDesc(UserEntity::getCreatedAt);
        return userMapper.selectPage(pageParam, wrapper);
    }

    public List<UserEntity> findByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(ids);
    }
}
