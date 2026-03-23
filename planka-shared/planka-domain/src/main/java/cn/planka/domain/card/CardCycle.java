package cn.planka.domain.card;

import lombok.Getter;

/**
 * 卡片生命周期状态枚举
 * <p>
 */
@Getter
public enum CardCycle {

    /**
     * 活跃 - 卡片处于活跃状态
     */
    ACTIVE("活跃"),

    /**
     * 已存档 - 卡片已完成存档
     */
    ARCHIVED("已存档"),

    /**
     * 已回收 - 卡片被回收
     */
    DISCARDED("已回收");

    private final String description;

    CardCycle(String description) {
        this.description = description;
    }

    /**
     * 判断是否为活跃状态
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

}
