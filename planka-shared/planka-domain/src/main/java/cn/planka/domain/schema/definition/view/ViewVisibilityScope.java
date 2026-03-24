package cn.planka.domain.schema.definition.view;

/**
 * 视图可见性范围基数（必选其一）
 */
public enum ViewVisibilityScope {

    /** 仅创建人（成员卡）可见 */
    PRIVATE,

    /** 组织内成员可见 */
    WORKSPACE,

    /** 指定团队（团队卡）成员可见 */
    TEAMS,

    /** 指定级联关系节点上下文可见 */
    CASCADE_RELATION_NODE
}
