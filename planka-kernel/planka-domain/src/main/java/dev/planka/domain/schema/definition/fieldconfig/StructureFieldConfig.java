package dev.planka.domain.schema.definition.fieldconfig;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.StructureId;
import dev.planka.domain.schema.definition.structure.StructureLevelBinding;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 架构层级属性的卡片类型级别配置
 * <p>
 * structureId 和 levelBindings 从父类型继承，
 * 在 Config 中只读，不可覆盖。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class StructureFieldConfig extends FieldConfig {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.STRUCTURE_FIELD;
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.STRUCTURE;
    }

    /** 关联的架构线ID（从父类型继承） */
    @JsonProperty("structureId")
    private StructureId structureId;

    /** 与架构线各层级的关联绑定配置（从父类型继承） */
    @JsonProperty("levelBindings")
    private List<StructureLevelBinding> levelBindings;

    /** 是否只能选择叶子节点 */
    @JsonProperty("leafOnly")
    private boolean leafOnly = false;

    /** 最大选择层级 */
    @JsonProperty("maxLevel")
    private Integer maxLevel;

    @JsonCreator
    public StructureFieldConfig(
            @JsonProperty("id") FieldConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("fieldId") FieldId fieldId,
            @JsonProperty("systemField") boolean systemField) {
        super(id, orgId, name, cardTypeId, fieldId, systemField);
    }

}
