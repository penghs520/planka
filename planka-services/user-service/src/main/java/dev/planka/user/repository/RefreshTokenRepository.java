package dev.planka.user.repository;

import dev.planka.user.mapper.RefreshTokenMapper;
import dev.planka.user.model.RefreshTokenEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 刷新令牌仓储
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final RefreshTokenMapper refreshTokenMapper;

    public Optional<RefreshTokenEntity> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(refreshTokenMapper.selectByTokenHash(tokenHash));
    }

    public void save(RefreshTokenEntity entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }

        if (refreshTokenMapper.selectById(entity.getId()) == null) {
            refreshTokenMapper.insert(entity);
        } else {
            refreshTokenMapper.updateById(entity);
        }
    }

    public void revokeAllByUserId(String userId) {
        refreshTokenMapper.revokeAllByUserId(userId);
    }

    public void revoke(String id) {
        RefreshTokenEntity entity = refreshTokenMapper.selectById(id);
        if (entity != null) {
            entity.setRevoked(true);
            refreshTokenMapper.updateById(entity);
        }
    }
}
