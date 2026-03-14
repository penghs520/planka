package cn.agilean.kanban.api.schema.service;

import cn.agilean.kanban.domain.card.CardTypeId;
import cn.agilean.kanban.domain.field.FieldId;
import cn.agilean.kanban.domain.link.LinkFieldIdUtils;
import cn.agilean.kanban.domain.link.LinkPosition;
import cn.agilean.kanban.domain.schema.definition.fieldconfig.ValueSource;
import cn.agilean.kanban.domain.schema.definition.link.LinkTypeDefinition;
import cn.agilean.kanban.domain.schema.definition.linkconfig.LinkFieldConfig;

/**
 * 属性配置工厂
 * <p>
 * 负责创建关联属性配置。
 */
class FieldConfigFactory {

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
