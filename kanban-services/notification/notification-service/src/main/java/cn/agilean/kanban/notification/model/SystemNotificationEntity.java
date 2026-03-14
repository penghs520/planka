package cn.agilean.kanban.notification.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知实体（站内信）
 */
@Data
@TableName("system_notification")
public class SystemNotificationEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 组织ID
     */
    private String orgId;

    /**
     * 接收者用户ID
     */
    private String userId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 纯文本内容
     */
    private String content;

    /**
     * 富文本内容
     */
    private String richContent;

    /**
     * 来源类型: RULE, MANUAL
     */
    private String sourceType;

    /**
     * 来源ID（规则ID等）
     */
    private String sourceId;

    /**
     * 关联卡片ID
     */
    private String cardId;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 阅读时间
     */
    private LocalDateTime readAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
