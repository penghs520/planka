package cn.agilean.kanban.notification.plugin;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 通知渠道定义
 * <p>
 * 描述渠道的元信息和配置字段，用于前端动态生成配置表单
 */
@Getter
@Builder
public class NotificationChannelDef {

    /**
     * 渠道 ID
     */
    private final String id;

    /**
     * 渠道名称
     */
    private final String name;

    /**
     * 渠道描述
     */
    private final String description;

    /**
     * 渠道版本
     */
    private final String version;

    /**
     * 渠道提供者
     */
    private final String provider;

    /**
     * 是否支持富文本内容
     */
    private final boolean supportsRichContent;

    /**
     * 是否支持附件
     */
    private final boolean supportsAttachment;

    /**
     * 配置字段定义列表
     * <p>
     * 用于前端动态生成配置表单
     */
    private final List<ConfigField> configFields;

    /**
     * 配置字段定义
     */
    @Getter
    @Builder
    public static class ConfigField {
        /**
         * 字段键名
         */
        private final String key;

        /**
         * 字段标签（显示名称）
         */
        private final String label;

        /**
         * 字段类型
         * <p>
         * 支持: text, password, url, number, boolean, textarea, select
         */
        private final String type;

        /**
         * 是否必填
         */
        private final boolean required;

        /**
         * 默认值
         */
        private final String defaultValue;

        /**
         * 字段描述/帮助文本
         */
        private final String description;

        /**
         * 占位符
         */
        private final String placeholder;

        /**
         * 选项列表（type=select 时使用）
         */
        private final List<SelectOption> options;

        @Getter
        @Builder
        public static class SelectOption {
            private final String value;
            private final String label;
        }
    }
}
