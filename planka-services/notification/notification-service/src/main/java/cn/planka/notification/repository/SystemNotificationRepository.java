package cn.planka.notification.repository;

import cn.planka.notification.mapper.SystemNotificationMapper;
import cn.planka.notification.model.SystemNotificationEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统通知仓储
 */
@Repository
@RequiredArgsConstructor
public class SystemNotificationRepository {

    private final SystemNotificationMapper mapper;

    /**
     * 保存通知
     */
    public void save(SystemNotificationEntity entity) {
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        if (entity.getIsRead() == null) {
            entity.setIsRead(false);
        }
        mapper.insert(entity);
    }

    /**
     * 根据ID查询
     */
    public SystemNotificationEntity findById(String id) {
        return mapper.selectById(id);
    }

    /**
     * 查询用户的通知列表（分页）
     */
    public Page<SystemNotificationEntity> findByUserId(String userId, int page, int size) {
        LambdaQueryWrapper<SystemNotificationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemNotificationEntity::getUserId, userId)
                .orderByDesc(SystemNotificationEntity::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 查询用户的未读通知
     */
    public List<SystemNotificationEntity> findUnreadByUserId(String userId) {
        LambdaQueryWrapper<SystemNotificationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemNotificationEntity::getUserId, userId)
                .eq(SystemNotificationEntity::getIsRead, false)
                .orderByDesc(SystemNotificationEntity::getCreatedAt);
        return mapper.selectList(wrapper);
    }

    /**
     * 统计用户未读通知数量
     */
    public long countUnreadByUserId(String userId) {
        LambdaQueryWrapper<SystemNotificationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemNotificationEntity::getUserId, userId)
                .eq(SystemNotificationEntity::getIsRead, false);
        return mapper.selectCount(wrapper);
    }

    /**
     * 标记为已读
     */
    public void markAsRead(String id) {
        SystemNotificationEntity entity = mapper.selectById(id);
        if (entity != null && !entity.getIsRead()) {
            entity.setIsRead(true);
            entity.setReadAt(LocalDateTime.now());
            mapper.updateById(entity);
        }
    }

    /**
     * 批量标记为已读
     */
    public void markAllAsRead(String userId) {
        LambdaQueryWrapper<SystemNotificationEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemNotificationEntity::getUserId, userId)
                .eq(SystemNotificationEntity::getIsRead, false);

        SystemNotificationEntity update = new SystemNotificationEntity();
        update.setIsRead(true);
        update.setReadAt(LocalDateTime.now());

        mapper.update(update, wrapper);
    }
}
