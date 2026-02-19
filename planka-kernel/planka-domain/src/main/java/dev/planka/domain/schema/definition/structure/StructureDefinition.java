package dev.planka.domain.schema.definition.structure;

import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.StructureId;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;

/**
 * 架构线定义
 * <p>
 * 架构线是一条多级关联的树形结构，例如：部落->小队->小组。
 * 架构线可以用于生成架构属性定义，当一个卡片类型新建一个架构属性时，
 * 需要指定一条架构线，并且选择当前卡片类型与架构线上每个节点类型的关联匹配关系。
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StructureDefinition extends AbstractSchemaDefinition<StructureId> {

    /** 架构线层级列表（有序，从根到叶） */
    @Setter
    @JsonProperty("levels")
    private List<StructureLevel> levels;

    /** 是否系统内置 */
    @Setter
    @JsonProperty("systemStructure")
    private boolean systemStructure = false;

    @JsonCreator
    public StructureDefinition(@JsonProperty("id") StructureId id, @JsonProperty("orgId") String orgId, @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.STRUCTURE_DEFINITION;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.STRUCTURE_DEFINITION;
    }

    @Override
    public SchemaId belongTo() {
        return null;
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of();
    }

    @Override
    protected StructureId newId() {
        return StructureId.generate();
    }
}
