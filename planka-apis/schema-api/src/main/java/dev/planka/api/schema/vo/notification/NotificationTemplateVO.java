package dev.planka.api.schema.vo.notification;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知模板 VO
 */
@Data
@Builder
public class NotificationTemplateVO {

    /** 模板 ID */
    private String id;

    /** 组织 ID */
    private String orgId;

    /** 模板名称 */
    private String name;

    /** 所属卡片类型ID */
    private String cardTypeId;

    /** 所属卡片类型名称 */
    private String cardTypeName;

    /** 触发事件类型 */
    private String triggerEvent;

    /** 触发事件显示名称 */
    private String triggerEventName;

    /** 通知对象类型：MEMBER-通知人, GROUP-通知群 */
    private String recipientType;

    /** 接收者选择器 */
    private RecipientSelectorVO recipientSelector;

    /** 是否为强通知 */
    private boolean strongNotification;

    /** 适用的通知渠道列表 */
    private List<String> channels;

    /** 通知标题模板 */
    private String titleTemplate;

    /** 短内容模板（用于 IM/系统通知） */
    private String shortContent;

    /** 长内容模板（用于邮件） */
    private String longContent;

    /** 优先级 */
    private int priority;

    /** 是否启用 */
    private boolean enabled;

    /** 内容版本号 */
    private int contentVersion;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /**
     * 接收者选择器 VO
     */
    @Data
    @Builder
    public static class RecipientSelectorVO {

        /** 选择类型 */
        private String selectorType;

        /** 固定成员ID列表 */
        private List<String> memberIds;

        /** 字段ID */
        private String fieldId;

        /** 是否包含系统人员 */
        private boolean includeSystemUsers;

        /** 系统人员ID列表 */
        private List<String> systemUserIds;
    }
}
