package dev.planka.domain.card;

import lombok.Getter;

/**
 * 卡片生命周期状态枚举
 * <p>
 */
@Getter
public enum CardStyle {

    /**
     * 活跃 - 卡片处于活跃状态
     */
    ACTIVE("活跃"),

    /**
     * 已归档 - 卡片已完成归档
     */
    ARCHIVED("已归档"),

    /**
     * 已丢弃 - 卡片被丢弃
     */
    DISCARDED("已丢弃");

    private final String description;

    CardStyle(String description) {
        this.description = description;
    }

    /**
     * 判断是否为活跃状态
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

}
