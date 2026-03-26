package cn.planka.domain.schema.definition.workflow;

/**
 * 审批模式
 */
public enum ApprovalMode {
    /**
     * 或签：任意一人通过即可
     */
    ANY_ONE,

    /**
     * 会签：所有人都需要通过
     */
    ALL_REQUIRED
}
