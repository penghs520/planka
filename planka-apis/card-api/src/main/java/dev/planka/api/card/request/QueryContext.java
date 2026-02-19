package dev.planka.api.card.request;

import lombok.Data;

import java.util.Map;

/**
 * 查询上下文
 */
@Data
public class QueryContext {
    /** 组织ID */
    private String orgId;

    /** 当前操作成员ID */
    private String operatorId;

    /** 参数 */
    private Map<String, String> parameters;

    /** 是否一致性读 */
    private boolean consistentRead;
}
