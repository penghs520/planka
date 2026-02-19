package dev.planka.card.converter;

import dev.planka.api.card.request.Yield;
import dev.planka.api.card.request.YieldField;
import dev.planka.api.card.request.YieldLink;
import dev.planka.domain.link.LinkFieldIdUtils;
import dev.planka.domain.link.LinkPosition;
import planka.graph.driver.proto.query.YieldedField;
import planka.graph.driver.proto.query.YieldedLink;

/**
 * Yield 转换器
 * 将 card-api 的 Yield 转换为 zgraph protobuf 的 Yield
 */
public class YieldConverter {

    private YieldConverter() {
    }

    /**
     * 转换为 protobuf Yield
     */
    public static planka.graph.driver.proto.query.Yield toProto(Yield yield) {
        if (yield == null) {
            return planka.graph.driver.proto.query.Yield.getDefaultInstance();
        }

        planka.graph.driver.proto.query.Yield.Builder builder = planka.graph.driver.proto.query.Yield.newBuilder();

        // 转换 YieldField
        if (yield.getField() != null) {
            builder.setYieldedField(toProtoYieldedField(yield.getField()));
        }

        // 转换 YieldLink 列表
        if (yield.getLinks() != null) {
            for (YieldLink link : yield.getLinks()) {
                builder.addYieldedLinks(toProtoYieldedLink(link));
            }
        }

        return builder.build();
    }

    /**
     * 转换 YieldField 为 YieldedField
     */
    private static YieldedField toProtoYieldedField(YieldField field) {
        if (field == null) {
            return YieldedField.getDefaultInstance();
        }

        YieldedField.Builder builder = YieldedField.newBuilder();
        builder.setContainsAllCustomField(field.isAllFields());
        builder.setContainsDesc(field.isIncludeDescription());

        if (field.getFieldIds() != null && !field.isAllFields()) {
            builder.addAllCustomFields(field.getFieldIds());
        }

        return builder.build();
    }

    /**
     * 转换 YieldLink 为 YieldedLink
     */
    private static YieldedLink toProtoYieldedLink(YieldLink link) {
        if (link == null) {
            return YieldedLink.getDefaultInstance();
        }

        YieldedLink.Builder builder = YieldedLink.newBuilder();

        // 从 linkFieldId 解析 linkTypeId 和 position，转换为 PathNode
        if (link.getLinkFieldId() != null) {
            String linkTypeId = LinkFieldIdUtils.getLinkTypeId(link.getLinkFieldId());
            LinkPosition position = LinkFieldIdUtils.getPosition(link.getLinkFieldId());
            builder.setPathNode(PathConverter.toProtoPathNode(linkTypeId, position));
        }

        // 递归转换 targetYield
        if (link.getTargetYield() != null) {
            if (link.getTargetYield().getField() != null) {
                builder.setYieldedField(toProtoYieldedField(link.getTargetYield().getField()));
            }
            if (link.getTargetYield().getLinks() != null) {
                for (YieldLink nestedLink : link.getTargetYield().getLinks()) {
                    builder.addNextYieldedLink(toProtoYieldedLink(nestedLink));
                }
            }
        }

        return builder.build();
    }

    /**
     * 从 protobuf PathNode 提取 linkFieldId
     */
    public static String fromProtoPathNode(planka.graph.driver.proto.common.PathNode pathNode) {
        if (pathNode == null) {
            return null;
        }
        String linkTypeId = pathNode.getLtId();
        LinkPosition position = PathConverter.fromProtoLinkPositionString(pathNode.getPosition());
        return LinkFieldIdUtils.build(linkTypeId, position);
    }
}
