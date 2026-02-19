package dev.planka.domain.schema.definition.permission;

import dev.planka.domain.expression.TextExpressionTemplate;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.definition.condition.Condition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 权限定义
 * <p>
 * 定义卡片类型级别的权限配置，包括操作权限、属性权限、附件权限等。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PermissionConfig {

    /**
     * 卡片操作权限列表
     */
    private List<CardOperationPermission> cardOperations;

    /**
     * 属性级别权限列表
     */
    private List<FieldPermission> fieldPermissions;

    /**
     * 附件权限列表
     */
    private List<AttachmentPermission> attachmentPermissions;

    /**
     * 卡片操作权限
     * <p>
     * 采用白名单模式：满足任一卡片条件 AND 满足任一操作人条件 → 有权限
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CardOperationPermission {
        /**
         * 操作类型
         */
        @JsonProperty("operation")
        private CardOperation operation;

        /**
         * 卡片条件列表（多个条件为"或"关系，满足任一即可）
         * 如果列表为空，表示不限制卡片范围
         */
        @JsonProperty("cardConditions")
        private List<Condition> cardConditions;

        /**
         * 操作人条件列表（多个条件为"或"关系，满足任一即可）
         * 如果列表为空，表示不限制操作人
         */
        @JsonProperty("operatorConditions")
        private List<Condition> operatorConditions;

        /**
         * 权限不通过时的提示信息
         */
        @JsonProperty("alertMessage")
        private TextExpressionTemplate alertMessage;
    }

    /**
     * 属性级别权限
     * <p>
     * 采用白名单模式：满足任一卡片条件 AND 满足任一操作人条件 → 有权限
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldPermission {

        /**
         * 属性操作类型
         */
        @JsonProperty("operation")
        private FieldOperation operation;

        /**
         * 属性配置ID列表
         */
        @JsonProperty("fieldIds")
        private List<FieldId> fieldIds;

        /**
         * 卡片条件列表（多个条件为"或"关系，满足任一即可）
         * 如果列表为空，表示不限制卡片范围
         */
        @JsonProperty("cardConditions")
        private List<Condition> cardConditions;

        /**
         * 操作人条件列表（多个条件为"或"关系，满足任一即可）
         * 如果列表为空，表示不限制操作人
         */
        @JsonProperty("operatorConditions")
        private List<Condition> operatorConditions;

        /**
         * 权限不通过时的提示信息
         */
        @JsonProperty("alertMessage")
        private TextExpressionTemplate alertMessage;
    }


    /**
     * 附件属性权限
     * <p>
     * 采用白名单模式：满足任一卡片条件 AND 满足任一操作人条件 → 有权限
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttachmentPermission {

        /**
         * 附件操作类型
         */
        @JsonProperty("attachmentOperation")
        private AttachmentOperation attachmentOperation;

        /**
         * 属性配置ID列表（针对附件类型的属性）
         */
        @JsonProperty("fieldIds")
        private List<FieldId> fieldIds;

        /**
         * 卡片条件列表（多个条件为"或"关系，满足任一即可）
         * 如果列表为空，表示不限制卡片范围
         */
        @JsonProperty("cardConditions")
        private List<Condition> cardConditions;

        /**
         * 操作人条件列表（多个条件为"或"关系，满足任一即可）
         * 如果列表为空，表示不限制操作人
         */
        @JsonProperty("operatorConditions")
        private List<Condition> operatorConditions;

        /**
         * 权限不通过时的提示信息
         */
        @JsonProperty("alertMessage")
        private TextExpressionTemplate alertMessage;
    }

    /**
     * 卡片操作类型
     */
    public enum CardOperation {
        /**
         * 创建卡片
         */
        CREATE,
        /**
         * 查看卡片
         */
        READ,
        /**
         * 编辑卡片
         */
        EDIT,
        /**
         * 移动卡片（改变状态）
         */
        MOVE,
        /**
         * 回退卡片（改变状态）
         */
        ROLLBACK,
        /**
         * 归档卡片
         */
        ARCHIVE,
        /**
         * 丢弃卡片
         */
        DISCARD,
    }

    /**
     * 属性操作类型
     */
    public enum FieldOperation {
        /**
         * 查看属性
         */
        READ,
        /**
         * 脱敏查看
         */
        DESENSITIZED_READ,
        /**
         * 编辑属性
         */
        EDIT,
    }

    /**
     * 附件操作类型
     */
    public enum AttachmentOperation {
        /**
         * 上传
         */
        UPLOAD,
        /**
         * 下载
         */
        DOWNLOAD,
        /**
         * 编辑
         */
        EDIT,
        /**
         * 预览
         */
        PREVIEW,
        /**
         * 删除
         */
        DELETE,
    }
}
