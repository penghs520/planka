package dev.planka.domain.stream;

/**
 * 流动记录类型枚举
 * <p>
 * 卡片在价值流中移动时产生的记录类型：
 * <ul>
 *   <li>正常移动：LEAVE(原状态) + ENTER(新状态)</li>
 *   <li>回滚移动：ROLLBACK_LEAVE(原状态) + ROLLBACK_ENTER(目标状态)</li>
 * </ul>
 */
public enum FlowRecordType {

    /**
     * 正常进入状态
     */
    ENTER,

    /**
     * 正常离开状态
     */
    LEAVE,

    /**
     * 回滚进入（回到之前的状态）
     */
    ROLLBACK_ENTER,

    /**
     * 回滚离开（从当前状态回退）
     */
    ROLLBACK_LEAVE;

    /**
     * 是否为进入类型（ENTER 或 ROLLBACK_ENTER）
     */
    public boolean isEntry() {
        return this == ENTER || this == ROLLBACK_ENTER;
    }

    /**
     * 是否为回滚类型（ROLLBACK_ENTER 或 ROLLBACK_LEAVE）
     */
    public boolean isRollback() {
        return this == ROLLBACK_ENTER || this == ROLLBACK_LEAVE;
    }
}
