package dev.planka.domain.link;

import lombok.Getter;

/**
 * 关联位置枚举
 * <p>
 * 表示在关联关系中的位置（源端或目标端）
 */
@Getter
public enum LinkPosition {

    /**
     * 源端
     */
    SOURCE("source", "源端"),

    /**
     * 目标端
     */
    TARGET("target", "目标端");

    private final String code;
    private final String description;

    LinkPosition(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取对端位置
     */
    public LinkPosition opposite() {
        return this == SOURCE ? TARGET : SOURCE;
    }

    public static LinkPosition fromCode(String code) {
        for (LinkPosition position : values()) {
            if (position.code.equals(code)) {
                return position;
            }
        }
        throw new IllegalArgumentException("Unknown LinkPosition code: " + code);
    }
}
