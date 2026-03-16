package cn.planka.notification.model;

import cn.planka.domain.notification.NotificationTemplateDefinition;
import lombok.Builder;
import lombok.Data;

/**
 * 应用后的模板
 * 包含解析后的标题和内容
 */
@Data
@Builder
public class AppliedTemplate {
    /**
     * 原始模板定义
     */
    private NotificationTemplateDefinition template;

    /**
     * 解析后的标题
     */
    private String title;

    /**
     * 解析后的短内容
     */
    private String content;

    /**
     * 解析后的长内容（富文本）
     */
    private String richContent;
}
