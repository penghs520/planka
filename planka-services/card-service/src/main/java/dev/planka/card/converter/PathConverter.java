package dev.planka.card.converter;

import dev.planka.domain.link.LinkFieldIdUtils;
import dev.planka.domain.link.LinkPosition;
import planka.graph.driver.proto.common.Path;
import planka.graph.driver.proto.common.PathNode;

import java.util.List;

/**
 * 路径转换器
 * 将 domain 的 Path/linkFieldId 转换为 zgraph protobuf 的 Path/PathNode
 */
public class PathConverter {

    private PathConverter() {
    }

    /**
     * 将 domain Path（linkFieldId 列表）转换为 proto Path
     */
    public static Path toProto(dev.planka.domain.link.Path path) {
        if (path == null || path.linkNodes() == null || path.linkNodes().isEmpty()) {
            return Path.getDefaultInstance();
        }

        Path.Builder builder = Path.newBuilder();
        for (String linkFieldId : path.linkNodes()) {
            builder.addNodes(toProtoPathNode(linkFieldId));
        }
        return builder.build();
    }

    /**
     * 将 linkFieldId（格式 "ltId:SOURCE"）转换为 proto PathNode
     */
    public static PathNode toProtoPathNode(String linkFieldId) {
        if (linkFieldId == null || linkFieldId.isEmpty()) {
            return PathNode.getDefaultInstance();
        }

        String linkTypeId = LinkFieldIdUtils.getLinkTypeId(linkFieldId);
        LinkPosition position = LinkFieldIdUtils.getPosition(linkFieldId);
        return toProtoPathNode(linkTypeId, position);
    }

    /**
     * 将 linkTypeId 和 position 转换为 proto PathNode
     */
    public static PathNode toProtoPathNode(String linkTypeId, LinkPosition position) {
        PathNode.Builder builder = PathNode.newBuilder();
        if (linkTypeId != null) {
            builder.setLtId(linkTypeId);
        }
        builder.setPosition(toProtoLinkPosition(position));
        return builder.build();
    }

    /**
     * 将 domain LinkPosition 转换为 proto 字符串
     * 注意：proto 中 position 是 string 类型
     */
    private static String toProtoLinkPosition(LinkPosition position) {
        if (position == null) {
            return "Src";
        }
        return switch (position) {
            case SOURCE -> "Src";
            case TARGET -> "Dest";
        };
    }

    /**
     * 将 proto LinkPosition 枚举转换为 domain LinkPosition
     */
    public static LinkPosition fromProtoLinkPosition(planka.graph.driver.proto.common.LinkPosition protoPosition) {
        return switch (protoPosition) {
            case Src -> LinkPosition.SOURCE;
            case Dest -> LinkPosition.TARGET;
            case UNRECOGNIZED -> LinkPosition.SOURCE;
        };
    }

    /**
     * 将 proto 字符串 position 转换为 domain LinkPosition
     */
    public static LinkPosition fromProtoLinkPositionString(String position) {
        if (position == null || position.isEmpty() || "Src".equals(position)) {
            return LinkPosition.SOURCE;
        }
        if ("Dest".equals(position)) {
            return LinkPosition.TARGET;
        }
        // 支持直接传入 SOURCE/TARGET
        if ("SOURCE".equals(position)) {
            return LinkPosition.SOURCE;
        }
        if ("TARGET".equals(position)) {
            return LinkPosition.TARGET;
        }
        return LinkPosition.SOURCE;
    }

    /**
     * 将 proto Path 转换为 linkFieldId 列表
     */
    public static List<String> fromProto(Path path) {
        if (path == null || path.getNodesList().isEmpty()) {
            return List.of();
        }
        return path.getNodesList().stream()
                .map(PathConverter::fromProtoPathNode)
                .toList();
    }

    /**
     * 将 proto PathNode 转换为 linkFieldId
     */
    public static String fromProtoPathNode(PathNode pathNode) {
        if (pathNode == null) {
            return null;
        }
        String linkTypeId = pathNode.getLtId();
        LinkPosition position = fromProtoLinkPositionString(pathNode.getPosition());
        return LinkFieldIdUtils.build(linkTypeId, position);
    }
}
