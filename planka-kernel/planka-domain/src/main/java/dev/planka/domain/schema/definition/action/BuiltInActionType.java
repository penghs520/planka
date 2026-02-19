package dev.planka.domain.schema.definition.action;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.CardActionId;

/**
 * 内置动作类型枚举
 * <p>
 * 定义系统内置的卡片动作类型。
 */
public enum BuiltInActionType {

    /**
     * 丢弃
     * <p>
     * 将卡片状态设置为 DISCARDED
     */
    DISCARD,

    /**
     * 归档
     * <p>
     * 将卡片状态设置为 ARCHIVED
     */
    ARCHIVE,

    /**
     * 还原
     * <p>
     * 将卡片状态恢复为 ACTIVE
     */
    RESTORE,

    /**
     * 阻塞/解阻（切换）
     * <p>
     * 根据当前阻塞状态切换
     */
    BLOCK_TOGGLE,

    /**
     * 点亮/暂停（切换）
     * <p>
     * 根据当前点亮状态切换
     */
    HIGHLIGHT_TOGGLE;

    /**
     * 获取内置动作的默认名称
     */
    public String getDefaultName() {
        return switch (this) {
            case DISCARD -> "丢弃";
            case ARCHIVE -> "归档";
            case RESTORE -> "还原";
            case BLOCK_TOGGLE -> "阻塞/解阻";
            case HIGHLIGHT_TOGGLE -> "点亮/暂停";
        };
    }

    /**
     * 获取内置动作的默认类别
     */
    public ActionCategory getDefaultCategory() {
        return switch (this) {
            case DISCARD, ARCHIVE, RESTORE -> ActionCategory.LIFECYCLE;
            case BLOCK_TOGGLE, HIGHLIGHT_TOGGLE -> ActionCategory.STATE_TOGGLE;
        };
    }

    /**
     * 生成内置动作的固定 ID
     *
     * @param cardTypeId 卡片类型 ID
     * @param actionType 内置动作类型
     * @return 固定 ID，格式为 builtin:{cardTypeId}:{actionType}
     */
    public static String builtInActionId(String cardTypeId, BuiltInActionType actionType) {
        return "builtin:" + cardTypeId + ":" + actionType.name();
    }

    /**
     * 创建默认的内置动作配置
     *
     * @param cardTypeId 卡片类型ID
     * @return 默认的内置动作配置定义
     */
    public CardActionConfigDefinition createDefaultAction(CardTypeId cardTypeId) {
        String builtInId = builtInActionId(cardTypeId.value(), this);
        CardActionConfigDefinition action = new CardActionConfigDefinition(
                CardActionId.of(builtInId), null, this.getDefaultName());
        action.setCardTypeId(cardTypeId);
        action.setBuiltIn(true);
        action.setBuiltInActionType(this);
        action.setActionCategory(this.getDefaultCategory());
        action.setEnabled(true);
        action.setSortOrder(this.ordinal());
        return action;
    }
}
