package dev.planka.api.schema.request.notification;

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

    /** 所属卡片类型ID */
    @NotBlank(message = "卡片类型不能为空")
    private String cardTypeId;

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

    /** 短内容模板（用于 IM/系统通知） */
    private String shortContent;

    /** 长内容模板（用于邮件） */
    private String longContent;

    /** 优先级（数字越小优先级越高） */
    private int priority = 100;

    /**
     * 接收者选择器请求
     */
    @Getter
    @Setter
    public static class RecipientSelectorRequest {

        /** 选择类型：FIXED_MEMBERS-固定成员, FROM_FIELD-从字段获取, CARD_WATCHERS-卡片关注者, CURRENT_OPERATOR-当前操作人 */
        @NotBlank(message = "选择类型不能为空")
        private String selectorType;

        /** 固定成员ID列表（selectorType=FIXED_MEMBERS时使用） */
        private List<String> memberIds;

        /** 字段ID（selectorType=FROM_FIELD时使用） */
        private String fieldId;

        /** 是否包含系统人员（固定可选系统人员） */
        private boolean includeSystemUsers = false;

        /** 系统人员ID列表 */
        private List<String> systemUserIds;
    }
}
