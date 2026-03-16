package cn.planka.notification.mapper;

import cn.planka.notification.model.SystemNotificationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统通知 Mapper
 */
@Mapper
public interface SystemNotificationMapper extends BaseMapper<SystemNotificationEntity> {
}
