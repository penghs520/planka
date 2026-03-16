package cn.planka.api.schema.vo.notification;

import cn.planka.domain.notification.DefinitionParameter;
import cn.planka.domain.notification.TemplateType;
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

    /** 模板类型（内置/自定义） */
    private TemplateType templateType;

    /** 定义参数 */
    private DefinitionParameter definitionParameter;

    /** 所属卡片类型ID（兼容字段，仅当定义参数为卡片类型时有效） */
    private String cardTypeId;

    /** 所属卡片类型名称（兼容字段，仅当定义参数为卡片类型时有效） */
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

    /** 通知内容 */
    private NotificationContentVO content;

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

        /** 多选择器列表（推荐） */
        private List<SelectorItemVO> selectors;

        /** 选择类型（向后兼容） */
        private String selectorType;

        /** 固定成员ID列表 */
        private List<String> memberIds;

        /** 字段ID */
        private String fieldId;

        /** 字段ID列表（多选） */
        private List<String> fieldIds;

        /** 是否包含系统人员 */
        private boolean includeSystemUsers;

        /** 系统人员ID列表 */
        private List<String> systemUserIds;
    }

    /**
     * 选择器项 VO（多选择器模式）
     */
    @Data
    @Builder
    public static class SelectorItemVO {

        /** 选择类型 */
        private String selectorType;

        /** 固定成员ID列表 */
        private List<String> memberIds;

        /** 字段ID */
        private String fieldId;

        /** 来源标识：OPERATOR-操作人属性, CARD-卡片字段 */
        private String source;
    }

    /**
     * 通知内容 VO
     */
    @Data
    @Builder
    public static class NotificationContentVO {

        /** 内容类型：SHORT-短内容, LONG-长内容 */
        private String type;

        /** 文本模板（短内容时使用） */
        private String textTemplate;

        /** 富文本模板（长内容时使用） */
        private String richTextTemplate;

        /** 抄送人选择器（长内容时使用） */
        private RecipientSelectorVO ccSelector;
    }
}
