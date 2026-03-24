package cn.planka.domain.schema.definition.cascaderelation;

import cn.planka.domain.schema.SchemaId;
import cn.planka.domain.schema.SchemaSubType;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.CascadeRelationId;
import cn.planka.domain.schema.definition.AbstractSchemaDefinition;
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
 * 架构线可以用于生成架构属性定义，当一个__PLANKA_EINST__新建一个架构属性时，
 * 需要指定一条架构线，并且选择当前__PLANKA_EINST__与架构线上每个节点类型的关联匹配关系。
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CascadeRelationDefinition extends AbstractSchemaDefinition<CascadeRelationId> {

    /** 架构线层级列表（有序，从根到叶） */
    @Setter
    @JsonProperty("levels")
    private List<CascadeRelationLevel> levels;

    /** 是否系统内置 */
    @Setter
    @JsonProperty("systemCascadeRelation")
    private boolean systemCascadeRelation = false;

    @JsonCreator
    public CascadeRelationDefinition(@JsonProperty("id") CascadeRelationId id, @JsonProperty("orgId") String orgId, @JsonProperty("name") String name) {
        super(id, orgId, name);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CASCADE_RELATION_DEFINITION;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.CASCADE_RELATION_DEFINITION;
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
    protected CascadeRelationId newId() {
        return CascadeRelationId.generate();
    }
}
