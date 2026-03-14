package cn.agilean.kanban.domain.stream;

import cn.agilean.kanban.domain.schema.definition.stream.StepStatusKind;

/**
 * 状态工作类型枚举
 * <p>
 * 用于区分状态是等待类型还是工作中类型，便于度量统计：
 */
public enum StatusWorkType {

    /**
     * 等待状态
     */
    WAITING(0),

    /**
     * 工作中状态
     */
    WORKING(1);

    private final int code;

    StatusWorkType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 根据代码值获取枚举
     */
    public static StatusWorkType fromCode(int code) {
        for (StatusWorkType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown StatusWorkType code: " + code);
    }

}
