package dev.planka.api.schema.service;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.link.LinkFieldIdUtils;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.schema.definition.fieldconfig.ValueSource;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;

/**
 * 属性配置工厂
 * <p>
 * 负责创建关联属性配置。
 */
public class FieldConfigFactory {

    /**
     * 从关联类型创建关联属性配置
     *
     * @param linkType   关联类型定义
     * @param cardTypeId 所属卡片类型ID
     * @param position   关联位置（SOURCE 或 TARGET）
     * @return 关联属性配置
     */
    public static LinkFieldConfig createFromLinkType(
            LinkTypeDefinition linkType,
            CardTypeId cardTypeId,
            LinkPosition position) {

        String displayName = position == LinkPosition.SOURCE
                ? linkType.getSourceName()
                : linkType.getTargetName();

        boolean multiple = position == LinkPosition.SOURCE
                ? linkType.isSourceMultiSelect()
                : linkType.isTargetMultiSelect();

        // 使用 linkTypeId:POSITION 格式作为 fieldId
        String linkFieldId = LinkFieldIdUtils.build(linkType.getId(), position);

        LinkFieldConfig config = new LinkFieldConfig(
                null,
                linkType.getOrgId(),
                displayName,
                cardTypeId,
                FieldId.of(linkFieldId),
                linkType.isSystemLinkType()  // 系统内置关联类型生成的配置也是系统内置的
        );
        config.setMultiple(multiple);
        if (linkType.isSystemInput()){
            config.setValueSource(ValueSource.SYSTEM);
            config.setReadOnly(true);
        }

        return config;
    }
}
