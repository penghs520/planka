package cn.planka.domain.schema.definition.workflow;

import java.util.List;

/**
 * 基于角色的选择器
 */
public record RoleBasedSelector(
    List<String> roleIds
) implements ApproverSelector {
}
