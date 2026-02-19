package dev.planka.domain.schema.definition;

import dev.planka.domain.schema.EntityState;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.action.CardActionConfigDefinition;
import dev.planka.domain.schema.definition.cardtype.AbstractCardType;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.fieldconfig.*;
import dev.planka.domain.schema.definition.formula.*;
import dev.planka.domain.schema.definition.fieldconfig.*;
import dev.planka.domain.schema.definition.formula.*;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;
import dev.planka.domain.schema.definition.menu.MenuGroupDefinition;
import dev.planka.domain.schema.definition.permission.PermissionConfigDefinition;
import dev.planka.domain.schema.definition.rule.BizRuleDefinition;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.domain.schema.definition.structure.StructureDefinition;
import dev.planka.domain.schema.definition.template.CardDetailTemplateDefinition;
import dev.planka.domain.schema.definition.template.CardCreatePageTemplateDefinition;
import dev.planka.domain.schema.definition.template.CardFaceDefinition;
import dev.planka.domain.schema.definition.view.ListViewDefinition;
import dev.planka.domain.notification.NotificationChannelConfigDefinition;
import dev.planka.domain.notification.NotificationTemplateDefinition;
import dev.planka.domain.outsourcing.OutsourcingConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Set;

/**
 * Schema 定义基类接口
 * <p>
 * 所有 Schema 定义类型的公共接口。
 * 使用 Jackson 多态序列化支持，通过 "schemaSubType" 字段区分具体字段类型。
 * 所有具体子类型都在此处集中声明，使其能够反序列化到所有子类
 * <p>
 * 注意：由于 Java sealed interface 要求 permits 中的类必须在同一个包中，
 * 这里使用普通接口以支持更好的包结构组织。
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "schemaSubType", visible = true)
@JsonSubTypes({
        // ==================== 卡片类型定义 ====================
        @JsonSubTypes.Type(value = AbstractCardType.class, name = SchemaSubType.TRAIT_CARD_TYPE),
        @JsonSubTypes.Type(value = EntityCardType.class, name = SchemaSubType.ENTITY_CARD_TYPE),

        // ==================== 属性配置 ====================
        @JsonSubTypes.Type(value = SingleLineTextFieldConfig.class, name = SchemaSubType.TEXT_FIELD),
        @JsonSubTypes.Type(value = MultiLineTextFieldConfig.class, name = SchemaSubType.MULTI_LINE_TEXT_FIELD),
        @JsonSubTypes.Type(value = MarkdownFieldConfig.class, name = SchemaSubType.MARKDOWN_FIELD),
        @JsonSubTypes.Type(value = NumberFieldConfig.class, name = SchemaSubType.NUMBER_FIELD),
        @JsonSubTypes.Type(value = DateFieldConfig.class, name = SchemaSubType.DATE_FIELD),
        @JsonSubTypes.Type(value = EnumFieldConfig.class, name = SchemaSubType.ENUM_FIELD),
        @JsonSubTypes.Type(value = AttachmentFieldConfig.class, name = SchemaSubType.ATTACHMENT_FIELD),
        @JsonSubTypes.Type(value = WebUrlFieldConfig.class, name = SchemaSubType.WEB_URL_FIELD),
        @JsonSubTypes.Type(value = StructureFieldConfig.class, name = SchemaSubType.STRUCTURE_FIELD),
        @JsonSubTypes.Type(value = LinkFieldConfig.class, name = SchemaSubType.LINK_FIELD),

        // ==================== 视图定义 ====================
        @JsonSubTypes.Type(value = ListViewDefinition.class, name = SchemaSubType.LIST_VIEW),

        // ==================== 菜单定义 ====================
        @JsonSubTypes.Type(value = MenuGroupDefinition.class, name = SchemaSubType.MENU_GROUP),

        // ==================== 架构线定义 ====================
        @JsonSubTypes.Type(value = StructureDefinition.class, name = SchemaSubType.STRUCTURE_DEFINITION),

        // ==================== 其他 Schema 定义 ====================
        @JsonSubTypes.Type(value = ValueStreamDefinition.class, name = SchemaSubType.VALUE_STREAM),
        @JsonSubTypes.Type(value = LinkTypeDefinition.class, name = SchemaSubType.LINK_TYPE),
        @JsonSubTypes.Type(value = CardDetailTemplateDefinition.class, name = SchemaSubType.CARD_DETAIL_TEMPLATE),
        @JsonSubTypes.Type(value = CardCreatePageTemplateDefinition.class, name = SchemaSubType.CARD_CREATE_PAGE_TEMPLATE),
        @JsonSubTypes.Type(value = CardFaceDefinition.class, name = SchemaSubType.CARD_FACE),
        @JsonSubTypes.Type(value = PermissionConfigDefinition.class, name = SchemaSubType.CARD_PERMISSION),
        @JsonSubTypes.Type(value = BizRuleDefinition.class, name = SchemaSubType.BIZ_RULE),


        // ==================== 计算公式定义 ====================
        @JsonSubTypes.Type(value = TimePointFormulaDefinition.class, name = SchemaSubType.TIME_POINT_FORMULA_DEFINITION),
        @JsonSubTypes.Type(value = TimeRangeFormulaDefinition.class, name = SchemaSubType.TIME_RANGE_FORMULA_DEFINITION),
        @JsonSubTypes.Type(value = DateCollectionFormulaDefinition.class, name = SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION),
        @JsonSubTypes.Type(value = CardCollectionFormulaDefinition.class, name = SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION),
        @JsonSubTypes.Type(value = NumberCalculationFormulaDefinition.class, name = SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION),

        // ==================== 卡片动作 ====================
        @JsonSubTypes.Type(value = CardActionConfigDefinition.class, name = SchemaSubType.CARD_ACTION_CONFIG),

        // ==================== 考勤配置 ====================
        @JsonSubTypes.Type(value = OutsourcingConfig.class, name = SchemaSubType.OUTSOURCING_CONFIG),

        // ==================== 通知配置 ====================
        @JsonSubTypes.Type(value = NotificationChannelConfigDefinition.class, name = SchemaSubType.NOTIFICATION_CHANNEL_CONFIG),
        @JsonSubTypes.Type(value = NotificationTemplateDefinition.class, name = SchemaSubType.NOTIFICATION_TEMPLATE),
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface SchemaDefinition<ID extends SchemaId> {

    /**
     * 获取 Schema 子类型标识
     */
    String getSchemaSubType();

    /**
     * 获取 Schema 类型
     */
    SchemaType getSchemaType();

    /**
     * 获取元素ID
     */
    ID getId();

    /**
     * 获取组织ID
     */
    String getOrgId();

    /**
     * 获取元素状态
     */
    EntityState getState();

    /**
     * 获取名称
     */
    String getName();

    /**
     * 获取描述
     */
    String getDescription();

    SchemaId belongTo();

    /**
     * 获取二级索引键
     * <p>
     * 返回用于构建二级索引的 SchemaId 集合。
     * 例如 FieldConfig 返回其关联的 CardTypeId 集合。
     *
     * @return 二级索引键集合，无索引时返回空集合
     */
    Set<SchemaId> secondKeys();

    /**
     * 验证定义是否有效
     *
     * @throws IllegalArgumentException 如果定义无效
     */
    default void validate() {

    }
}
