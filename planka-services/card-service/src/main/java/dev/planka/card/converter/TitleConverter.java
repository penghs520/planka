package dev.planka.card.converter;

import dev.planka.domain.card.CardTitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import planka.graph.driver.proto.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 卡片标题转换器
 * <p>
 * 负责在 domain CardTitle 和 proto Title 之间进行转换
 */
public class TitleConverter {

    private static final Logger logger = LoggerFactory.getLogger(TitleConverter.class);

    /**
     * 转换 CardTitle 为 proto Title
     */
    public static Title toProto(CardTitle cardTitle) {
        if (cardTitle == null) {
            return Title.getDefaultInstance();
        }

        Title.Builder builder = Title.newBuilder();

        if (cardTitle instanceof CardTitle.PureTitle pureTitle) {
            builder.setPure(PureTitle.newBuilder()
                    .setValue(pureTitle.getValue() != null ? pureTitle.getValue() : "")
                    .build());
        } else if (cardTitle instanceof CardTitle.JointTitle jointTitle) {
            JointTitle.Builder jointBuilder = JointTitle.newBuilder();
            jointBuilder.setName(jointTitle.getValue() != null ? jointTitle.getValue() : "");
            jointBuilder.setArea(toProtoTitleJointArea(jointTitle.getArea()));

            if (jointTitle.getMultiParts() != null) {
                for (CardTitle.JointParts parts : jointTitle.getMultiParts()) {
                    JointTitleParts.Builder partsBuilder = JointTitleParts.newBuilder();
                    if (parts.parts() != null) {
                        for (CardTitle.JointPart part : parts.parts()) {
                            partsBuilder.addParts(JointTitlePart.newBuilder()
                                    .setName(part.name() != null ? part.name() : "")
                                    .build());
                        }
                    }
                    jointBuilder.addMultiParts(partsBuilder.build());
                }
            }
            builder.setJoint(jointBuilder.build());
        } else {
            // 未知类型，返回空标题
            logger.warn("未知的CardTitle类型: {}", cardTitle.getClass().getName());
            builder.setPure(PureTitle.newBuilder().setValue("").build());
        }

        return builder.build();
    }

    /**
     * 从 proto Title 转换为 CardTitle
     */
    public static CardTitle fromProto(Title protoTitle) {
        if (protoTitle == null) {
            return null;
        }

        if (protoTitle.hasPure()) {
            String value = protoTitle.getPure().getValue();
            if (value.isEmpty()) {
                return null;
            }
            return CardTitle.pure(value);
        } else if (protoTitle.hasJoint()) {
            JointTitle joint = protoTitle.getJoint();
            List<CardTitle.JointParts> multiParts = new ArrayList<>();

            for (JointTitleParts protoPartsGroup : joint.getMultiPartsList()) {
                List<CardTitle.JointPart> parts = new ArrayList<>();
                for (JointTitlePart protoPart : protoPartsGroup.getPartsList()) {
                    parts.add(new CardTitle.JointPart(protoPart.getName()));
                }
                multiParts.add(new CardTitle.JointParts(parts));
            }

            return CardTitle.joint(joint.getName(), fromProtoTitleJointArea(joint.getArea()), multiParts);
        }

        return null;
    }

    /**
     * 从 proto Title Map 转换为 CardTitle Map
     */
    public static Map<String, CardTitle> fromProtoMap(Map<String, Title> protoTitleMap) {
        if (protoTitleMap == null || protoTitleMap.isEmpty()) {
            return Collections.emptyMap();
        }

        return protoTitleMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> fromProto(entry.getValue())
                ));
    }

    /**
     * 转换 JointArea 为 proto TitleJointArea
     */
    private static TitleJointArea toProtoTitleJointArea(CardTitle.JointArea area) {
        if (area == null) {
            return TitleJointArea.SUFFIX;
        }
        return switch (area) {
            case PREFIX -> TitleJointArea.PREFIX;
            case SUFFIX -> TitleJointArea.SUFFIX;
        };
    }

    /**
     * 从 proto TitleJointArea 转换为 domain JointArea
     */
    private static CardTitle.JointArea fromProtoTitleJointArea(TitleJointArea protoArea) {
        if (protoArea == null) {
            return CardTitle.JointArea.SUFFIX;
        }
        return switch (protoArea) {
            case PREFIX -> CardTitle.JointArea.PREFIX;
            case SUFFIX, UNRECOGNIZED -> CardTitle.JointArea.SUFFIX;
        };
    }
}
