package cn.planka.card.converter;

import cn.planka.api.card.request.QueryContext;

/**
 * 查询上下文转换器
 * 将 card-api 的 QueryContext 转换为 zgraph protobuf 的 QueryContext
 */
public class QueryContextConverter {

    private QueryContextConverter() {
    }

    /**
     * 转换为 protobuf QueryContext
     */
    public static zgraph.driver.proto.query.QueryContext toProto(QueryContext queryContext) {
        if (queryContext == null) {
            return zgraph.driver.proto.query.QueryContext.getDefaultInstance();
        }

        var builder = zgraph.driver.proto.query.QueryContext.newBuilder();

        if (queryContext.getOrgId() != null) {
            builder.setOrgId(queryContext.getOrgId());
        }
        if (queryContext.getOperatorId() != null) {
            builder.setMemberId(queryContext.getOperatorId());
        }
        if (queryContext.getParameters() != null) {
            builder.putAllParameters(queryContext.getParameters());
        }
        builder.setConsistentRead(queryContext.isConsistentRead());

        return builder.build();
    }
}
