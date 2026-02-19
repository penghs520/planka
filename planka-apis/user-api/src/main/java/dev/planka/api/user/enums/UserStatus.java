package dev.planka.api.user.enums;

/**
 * 用户状态
 */
public enum UserStatus {
    /**
     * 待激活（新用户首次需要设置密码）
     */
    PENDING_ACTIVATION,

    /**
     * 正常
     */
    ACTIVE,

    /**
     * 已禁用
     */
    DISABLED,

    /**
     * 已锁定（登录失败次数过多）
     */
    LOCKED
}
