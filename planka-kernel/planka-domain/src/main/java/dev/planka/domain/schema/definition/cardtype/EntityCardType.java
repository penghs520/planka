package dev.planka.domain.schema.definition.cardtype;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 实体类型
 * <p>
 * 实体类型的特点：
 * <ul>
 *     <li>可以继承多个属性集，以获得共同的属性定义和关联关系定义</li>
 *     <li>可以用来创建卡片实例</li>
 * </ul>
 * <p>
 * 示例："需求"、"任务"、"缺陷"等都是实体类型，可以基于它们创建实际的卡片。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class EntityCardType extends CardTypeDefinition {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.ENTITY_CARD_TYPE;
    }

    /** 编号生成规则 */
    @JsonProperty("codeGenerationRule")
    private CodeGenerationRule codeGenerationRule;

    /** 标题组合规则 */
    @JsonProperty("titleCompositionRule")
    private TitleCompositionRule titleCompositionRule;

    /**
     * 继承的属性集ID列表（可继承多个）
     */
    @JsonProperty("parentTypeIds")
    private Set<CardTypeId> parentTypeIds;

    /**
     * 默认生效的卡面定义ID
     * <p>
     * 运行时根据卡片数据匹配第一个满足生效条件的卡面。
     */
    @JsonProperty("defaultCardFaceId")
    private String defaultCardFaceId;


    /**
     * 一键创建关联卡片配置列表
     */
    @JsonProperty("quickCreateLinkConfigs")
    private List<QuickCreateLinkConfig> quickCreateLinkConfigs;


    @JsonCreator
    public EntityCardType(
            @JsonProperty("id") CardTypeId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public Set<SchemaId> secondKeys() {
        if (CollectionUtils.isEmpty(parentTypeIds)){
            return Set.of();
        }
        return new HashSet<>(parentTypeIds);
    }

    /**
     * 一键创建关联卡片配置
     * <p>
     * 定义从当前卡片类型一键创建关联卡片的规则。
     */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuickCreateLinkConfig {
        /** 配置名称（如"拆分任务"、"创建子需求"） */
        /**
         * TODO 支持通过表达式定义名称
         */
        @JsonProperty("name")
        private String name;

        /**
         * TODO 支持通过表达式定义描述
         */
        @JsonProperty("description")
        private String description;

        /** TODO 支持给关联卡片设置默认值 */

        /**
         * 关联属性ID
         */
        @JsonProperty("linkFieldId")
        private LinkFieldId linkFieldId;

        /**
         * 限定目标卡片类型ID，为空表示允许关联关系对侧定义的全部卡片类型
         */
        @JsonProperty("targetCardTypeIdsLimit")
        private List<String> targetCardTypeIdsLimit;

    }
}

