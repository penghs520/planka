package cn.planka.api.schema.request.notification;

import cn.planka.domain.notification.DefinitionParameter;
import cn.planka.domain.notification.TemplateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 创建通知模板请求
 */
@Getter
@Setter
public class CreateNotificationTemplateRequest {

    /** 模板名称 */
    @NotBlank(message = "模板名称不能为空")
    private String name;

    /** 模板类型（内置/自定义） */
    private TemplateType templateType = TemplateType.CUSTOM;

    /** 定义参数 */
    @NotNull(message = "定义参数不能为空")
    private DefinitionParameter definitionParameter;

    /** 触发事件类型 */
    @NotBlank(message = "触发事件不能为空")
    private String triggerEvent;

    /** 通知对象类型：MEMBER-通知人, GROUP-通知群 */
    @NotBlank(message = "通知对象类型不能为空")
    private String recipientType;

    /** 接收者选择器配置 */
    @NotNull(message = "接收者选择器不能为空")
    private RecipientSelectorRequest recipientSelector;

    /** 是否为强通知 */
    private boolean strongNotification = true;

    /** 适用的通知渠道列表 */
    @NotEmpty(message = "通知渠道不能为空")
    private List<String> channels;

    /** 通知标题模板 */
    @NotBlank(message = "标题模板不能为空")
    private String titleTemplate;

    /** 通知内容 */
    @NotNull(message = "通知内容不能为空")
    private NotificationContentRequest content;

    /**
     * 接收者选择器请求
     */
    @Getter
    @Setter
    public static class RecipientSelectorRequest {

        /** 多选择器列表（推荐使用） */
        private List<SelectorItemRequest> selectors;

        /** 选择类型：FIXED_MEMBERS-固定成员, FROM_FIELD-从字段获取, CARD_WATCHERS-卡片关注者, CURRENT_OPERATOR-当前操作人（向后兼容） */
        private String selectorType;

        /** 固定成员ID列表（selectorType=FIXED_MEMBERS时使用） */
        private List<String> memberIds;

        /** 字段ID（selectorType=FROM_FIELD时使用） */
        private String fieldId;

        /** 字段ID列表（多选） */
        private List<String> fieldIds;

        /** 是否包含系统人员（固定可选系统人员） */
        private boolean includeSystemUsers = false;

        /** 系统人员ID列表 */
        private List<String> systemUserIds;
    }

    /**
     * 选择器项请求（多选择器模式）
     */
    @Getter
    @Setter
    public static class SelectorItemRequest {

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
     * 通知内容请求
     */
    @Getter
    @Setter
    public static class NotificationContentRequest {

        /** 内容类型：SHORT-短内容, LONG-长内容 */
        @NotBlank(message = "内容类型不能为空")
        private String type;

        /** 文本模板（短内容时使用） */
        private String textTemplate;

        /** 富文本模板（长内容时使用） */
        private String richTextTemplate;

        /** 抄送人选择器（长内容时使用） */
        private RecipientSelectorRequest ccSelector;
    }
}
