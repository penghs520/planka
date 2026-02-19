package dev.planka.domain.schema.definition.template;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.CardFaceId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.domain.schema.definition.condition.Condition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * 卡面定义
 * <p>
 * 定义卡片在看板/列表视图中的展示样式，包括显示哪些属性、布局方式等。
 * 实体类型通过引用卡面ID来使用该定义。
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CardFaceDefinition extends AbstractSchemaDefinition<CardFaceId> {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CARD_FACE;
    }

    /** 所属卡片类型ID */
    @Setter
    @JsonProperty("cardTypeId")
    private CardTypeId cardTypeId;

    /** 是否系统内置卡面 */
    @Setter
    @JsonProperty("systemFace")
    private boolean systemFace = false;

    /** 生效条件（当卡片满足此条件时使用该卡面） */
    @Setter
    @JsonProperty("effectiveCondition")
    private Condition effectiveCondition;

    /** 优先级（数字越小优先级越高，用于多个卡面满足条件时的选择） */
    @Setter
    @JsonProperty("priority")
    private Integer priority = 100;

    /** 卡面尺寸 */
    @Setter
    @JsonProperty("size")
    private CardSize size = CardSize.MEDIUM;

    /** 头部配置 */
    @Setter
    @JsonProperty("header")
    private HeaderConfig header;

    /** 内容区配置 */
    @Setter
    @JsonProperty("content")
    private ContentConfig content;

    /** 底部配置 */
    @Setter
    @JsonProperty("footer")
    private FooterConfig footer;

    @JsonCreator
    public CardFaceDefinition(
            @JsonProperty("id") CardFaceId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.CARD_FACE;
    }

    @Override
    public SchemaId belongTo() {
        return cardTypeId;  // 卡面定义属于某个卡片类型
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of();
    }

    @Override
    protected CardFaceId newId() {
        return CardFaceId.generate();
    }

    /**
     * 卡面尺寸
     */
    public enum CardSize {
        /** 小尺寸 */
        SMALL,
        /** 中等尺寸 */
        MEDIUM,
        /** 大尺寸 */
        LARGE
    }

    /**
     * 头部配置
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeaderConfig {
        /** 是否显示卡片类型图标 */
        @JsonProperty("showTypeIcon")
        private boolean showTypeIcon = true;

        /** 是否显示卡片编号 */
        @JsonProperty("showCardNumber")
        private boolean showCardNumber = true;

        /** 标题属性配置ID */
        @JsonProperty("titleFieldConfigId")
        private String titleFieldConfigId;
    }

    /**
     * 内容区配置
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentConfig {
        /** 显示的属性配置ID列表 */
        @JsonProperty("fieldConfigIds")
        private List<String> fieldConfigIds;

        /** 最大显示属性数量 */
        @JsonProperty("maxFieldCount")
        private Integer maxFieldCount = 3;

        /** 是否显示进度条 */
        @JsonProperty("showProgress")
        private boolean showProgress = false;

        /** 进度属性配置ID */
        @JsonProperty("progressFieldConfigId")
        private String progressFieldConfigId;
    }

    /**
     * 底部配置
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FooterConfig {
        /** 是否显示负责人头像 */
        @JsonProperty("showAssigneeAvatar")
        private boolean showAssigneeAvatar = true;

        /** 负责人属性配置ID */
        @JsonProperty("assigneeFieldConfigId")
        private String assigneeFieldConfigId;

        /** 是否显示截止日期 */
        @JsonProperty("showDueDate")
        private boolean showDueDate = false;

        /** 截止日期属性配置ID */
        @JsonProperty("dueDateFieldConfigId")
        private String dueDateFieldConfigId;

        /** 是否显示标签 */
        @JsonProperty("showTags")
        private boolean showTags = false;

        /** 标签属性配置ID */
        @JsonProperty("tagFieldConfigId")
        private String tagFieldConfigId;
    }
}
