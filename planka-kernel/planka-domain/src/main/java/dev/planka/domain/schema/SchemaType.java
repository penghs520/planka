package dev.planka.domain.schema;

import lombok.Getter;

/**
 * Schema 类型枚举
 * <p>
 * 定义系统支持的所有 Schema 类型。
 * 后续可按需扩展更多类型。
 */
@Getter
public enum SchemaType {

    /**
     * 卡片类型定义
     */
    CARD_TYPE("卡片类型", false),

    /**
     * 卡片类型的属性配置（哪个属性在哪个卡片类型上的配置）
     */
    FIELD_CONFIG("卡片类型属性配置", true),

    /**
     * 属性定义
     */
    FIELD_DEFINITION("属性定义", false),

    /**
     * 视图定义
     */
    VIEW("视图", false),

    /**
     * 价值流定义（属于某个卡片类型）
     */
    VALUE_STREAM("价值流定义", true),

    /**
     * 关联类型定义
     */
    LINK_TYPE("关联类型", false),

    /**
     * 架构线定义
     */
    STRUCTURE_DEFINITION("架构线定义", false),

    /**
     * 业务规则定义（属于某个卡片类型）
     */
    BIZ_RULE("业务规则", true),

    /**
     * 流转策略定义（属于某个价值流）
     */
    FLOW_POLICY("流转策略", true),

    /**
     * 卡片详情页模板定义（属于某个卡片类型）
     */
    CARD_DETAIL_TEMPLATE("卡片详情页模板定义", true),

    /**
     * 卡片新建页模板定义（属于某个卡片类型）
     */
    CARD_CREATE_PAGE_TEMPLATE("卡片新建页模板定义", true),

    /**
     * 卡面定义（属于某个卡片类型）
     */
    CARD_FACE("卡面定义", true),

    /**
     * 卡片权限定义（属于某个卡片类型）
     */
    CARD_PERMISSION("权限定义", true),


    /**
     * 菜单定义
     */
    MENU("菜单定义", false),

    /**
     * 通知渠道配置（组织级别）
     */
    NOTIFICATION_CHANNEL_CONFIG("通知渠道配置", false),

    /**
     * 通知模板（属于某个卡片类型）
     */
    NOTIFICATION_TEMPLATE("通知模板", true),

    /**
     * 计算公式定义
     */
    FORMULA_DEFINITION("计算公式定义", false),

    /**
     * 卡片动作配置（属于某个卡片类型）
     */
    CARD_ACTION("卡片动作配置", true),

    /**
     * 考勤配置
     */
    OUTSOURCING_CONFIG("考勤配置", false);

    private final String displayName;
    private final boolean requiresBelongTo;

    SchemaType(String displayName, boolean requiresBelongTo) {
        this.displayName = displayName;
        this.requiresBelongTo = requiresBelongTo;
    }

    /**
     * 是否需要belongTo（组合关系的从属方）
     */
    public boolean requiresBelongTo() {
        return requiresBelongTo;
    }
}
