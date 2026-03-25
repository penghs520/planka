package cn.planka.domain.schema.template.detail;

import cn.planka.common.util.SystemSchemaIds;
import cn.planka.domain.schema.CardDetailTemplateId;
import cn.planka.domain.schema.definition.fieldconfig.FieldConfig;
import cn.planka.domain.schema.definition.fieldconfig.FieldType;
import cn.planka.domain.schema.definition.template.CardDetailTemplateDefinition;
import cn.planka.domain.schema.definition.template.detail.DetailHeaderConfig;
import cn.planka.domain.schema.definition.template.detail.FieldItemConfig;
import cn.planka.domain.schema.definition.template.detail.SectionConfig;
import cn.planka.domain.schema.definition.template.detail.TabConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 未配置详情页模板时，根据字段列表生成与 {@code CardDetailService} 运行时一致的默认模板（仅内存，未持久化）。
 */
public final class DefaultCardDetailTemplateBuilder {

    private DefaultCardDetailTemplateBuilder() {
    }

    public static CardDetailTemplateDefinition build(List<FieldConfig> fieldConfigs) {
        CardDetailTemplateDefinition template = new CardDetailTemplateDefinition(
                CardDetailTemplateId.of("default_template"),
                null,
                "默认模板");
        template.setDefault(true);
        template.setSystemTemplate(true);

        DetailHeaderConfig header = new DetailHeaderConfig();
        header.setShowTypeIcon(true);
        header.setShowCardNumber(true);
        header.setShowStatus(true);
        template.setHeader(header);

        List<FieldItemConfig> fieldItems = new ArrayList<>();
        int currentRowWidth = 0;

        FieldItemConfig createdAtItem = new FieldItemConfig();
        createdAtItem.setFieldConfigId("$createdAt");
        createdAtItem.setWidthPercent(25);
        createdAtItem.setCustomLabel("创建时间");
        createdAtItem.setStartNewRow(false);
        fieldItems.add(createdAtItem);
        currentRowWidth += 25;

        FieldItemConfig updatedAtItem = new FieldItemConfig();
        updatedAtItem.setFieldConfigId("$updatedAt");
        updatedAtItem.setWidthPercent(25);
        updatedAtItem.setCustomLabel("更新时间");
        updatedAtItem.setStartNewRow(false);
        fieldItems.add(updatedAtItem);
        currentRowWidth = 0;

        boolean isFirstField = true;
        if (fieldConfigs != null && !fieldConfigs.isEmpty()) {
            for (FieldConfig fieldConfig : fieldConfigs) {
                String fieldId = fieldConfig.getFieldId().value();

                if (isArchiverOrDiscarderField(fieldId)) {
                    continue;
                }

                FieldType fieldType = fieldConfig.getFieldType();
                boolean isFullWidth = isFullWidthFieldType(fieldType);
                int widthPercent = isFullWidth ? 100 : 25;

                boolean startNewRow = isFirstField || (currentRowWidth + widthPercent) > 100;
                if (startNewRow) {
                    currentRowWidth = 0;
                }

                FieldItemConfig item = new FieldItemConfig();
                item.setFieldConfigId(fieldId);
                item.setWidthPercent(widthPercent);
                item.setStartNewRow(startNewRow);
                fieldItems.add(item);

                currentRowWidth += widthPercent;
                if (currentRowWidth >= 100) {
                    currentRowWidth = 0;
                }
                isFirstField = false;
            }
        }

        FieldItemConfig descriptionItem = new FieldItemConfig();
        descriptionItem.setFieldConfigId("$description");
        descriptionItem.setWidthPercent(100);
        descriptionItem.setCustomLabel("描述");
        descriptionItem.setStartNewRow(true);
        fieldItems.add(descriptionItem);

        SectionConfig section = new SectionConfig();
        section.setSectionId("basic_section");
        section.setName(null);
        section.setCollapsed(false);
        section.setCollapsible(true);
        section.setFieldItems(fieldItems);

        TabConfig basicInfoTab = new TabConfig();
        basicInfoTab.setTabId("basic_info");
        basicInfoTab.setTabType(TabConfig.TabType.SYSTEM);
        basicInfoTab.setName("基础信息");
        basicInfoTab.setSystemTabType(TabConfig.SystemTabType.BASIC_INFO);
        basicInfoTab.setFieldRowSpacing(TabConfig.FieldRowSpacing.NORMAL);
        basicInfoTab.setSections(Collections.singletonList(section));

        TabConfig commentTab = new TabConfig();
        commentTab.setTabId("comment");
        commentTab.setTabType(TabConfig.TabType.SYSTEM);
        commentTab.setName("评论");
        commentTab.setSystemTabType(TabConfig.SystemTabType.COMMENT);
        commentTab.setFieldRowSpacing(TabConfig.FieldRowSpacing.NORMAL);

        TabConfig activityLogTab = new TabConfig();
        activityLogTab.setTabId("activity_log");
        activityLogTab.setTabType(TabConfig.TabType.SYSTEM);
        activityLogTab.setName("操作记录");
        activityLogTab.setSystemTabType(TabConfig.SystemTabType.ACTIVITY_LOG);
        activityLogTab.setFieldRowSpacing(TabConfig.FieldRowSpacing.NORMAL);

        template.setTabs(Arrays.asList(basicInfoTab, commentTab, activityLogTab));

        return template;
    }

    private static boolean isFullWidthFieldType(FieldType fieldType) {
        return fieldType == FieldType.TEXTAREA
                || fieldType == FieldType.MARKDOWN
                || fieldType == FieldType.ATTACHMENT;
    }

    private static boolean isArchiverOrDiscarderField(String fieldId) {
        return fieldId.contains(SystemSchemaIds.LINK_ARCHIVER_PATTERN)
                || fieldId.contains(SystemSchemaIds.LINK_DISCARDER_PATTERN);
    }
}
