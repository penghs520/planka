package dev.planka.domain.history;

import lombok.Getter;

/**
 * 操作类型枚举
 * <p>
 * 定义卡片操作历史中支持的所有操作类型
 */
@Getter
public enum OperationType {

    // ==================== 卡片生命周期 ====================

    /**
     * 创建卡片
     */
    CARD_CREATED("card.created", "创建卡片"),

    /**
     * 归档卡片
     */
    CARD_ARCHIVED("card.archived", "归档卡片"),

    /**
     * 丢弃卡片
     */
    CARD_ABANDONED("card.abandoned", "丢弃卡片"),

    /**
     * 还原卡片
     */
    CARD_RESTORED("card.restored", "还原卡片"),

    /**
     * 切换卡片类型
     */
    CARD_TYPE_CHANGED("card.type_changed", "切换卡片类型"),

    // ==================== 属性变更 ====================

    /**
     * 修改标题
     */
    FIELD_TITLE_UPDATED("field.title.updated", "修改标题"),

    /**
     * 修改描述
     */
    FIELD_DESC_UPDATED("field.desc.updated", "修改描述"),

    /**
     * 修改自定义属性
     */
    FIELD_CUSTOM_UPDATED("field.custom.updated", "修改自定义属性"),

    /**
     * 添加自定义属性
     */
    FIELD_CUSTOM_ADDED("field.custom.added", "添加自定义属性"),

    /**
     * 删除自定义属性
     */
    FIELD_CUSTOM_REMOVED("field.custom.removed", "删除自定义属性"),

    /**
     * 添加标签
     */
    FIELD_TAG_ADDED("field.tag.added", "添加标签"),

    /**
     * 删除标签
     */
    FIELD_TAG_REMOVED("field.tag.removed", "删除标签"),

    // ==================== 关联操作 ====================

    /**
     * 添加关联
     */
    LINK_ADDED("link.added", "添加关联"),

    /**
     * 修改关联
     */
    LINK_UPDATED("link.updated", "修改关联"),

    /**
     * 删除关联
     */
    LINK_REMOVED("link.removed", "删除关联"),

    // ==================== 价值流操作 ====================

    /**
     * 移动卡片（状态流转）
     */
    STREAM_MOVED("stream.moved", "移动卡片"),

    /**
     * 回滚卡片
     */
    STREAM_ROLLBACK("stream.rollback", "回滚卡片"),

    /**
     * 开始工作
     */
    STREAM_WORK_STARTED("stream.work.started", "开始工作"),

    /**
     * 暂停工作
     */
    STREAM_WORK_STOPPED("stream.work.stopped", "暂停工作"),

    // ==================== 附件操作 ====================

    /**
     * 上传附件
     */
    ATTACHMENT_UPLOADED("attachment.uploaded", "上传附件"),

    /**
     * 删除附件
     */
    ATTACHMENT_DELETED("attachment.deleted", "删除附件"),

    // ==================== 阻塞操作 ====================

    /**
     * 阻塞
     */
    BLOCK_CREATED("block.created", "阻塞"),

    /**
     * 编辑阻塞
     */
    BLOCK_UPDATED("block.updated", "编辑阻塞"),

    /**
     * 解除阻塞
     */
    BLOCK_RESOLVED("block.resolved", "解除阻塞"),

    // ==================== 其他 ====================

    /**
     * 关注卡片
     */
    FOCUS_ADDED("focus.added", "关注卡片"),

    /**
     * 取消关注
     */
    FOCUS_REMOVED("focus.removed", "取消关注");

    private final String code;
    private final String defaultLabel;

    OperationType(String code, String defaultLabel) {
        this.code = code;
        this.defaultLabel = defaultLabel;
    }

    /**
     * 获取国际化消息码
     */
    public String getMessageKey() {
        return "history.operation." + code;
    }

    /**
     * 根据编码查找枚举值
     */
    public static OperationType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (OperationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
