package cn.planka.domain.schema.definition.fieldconfig;

import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.field.FieldConfigId;
import cn.planka.domain.field.FieldId;
import cn.planka.domain.schema.SchemaSubType;
import cn.planka.domain.schema.CascadeRelationId;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationLevelBinding;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 级联属性的__PLANKA_EINST__级别配置
 * <p>
 * cascadeRelationId 和 levelBindings 从父类型继承，
 * 在 Config 中只读，不可覆盖。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CascadeFieldConfig extends FieldConfig {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CASCADE_FIELD;
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.CASCADE;
    }

    /** 关联的架构线ID（从父类型继承） */
    @JsonProperty("cascadeRelationId")
    private CascadeRelationId cascadeRelationId;

    /** 与架构线各层级的关联绑定配置（从父类型继承） */
    @JsonProperty("levelBindings")
    private List<CascadeRelationLevelBinding> levelBindings;

    /** 是否只能选择叶子节点 */
    @JsonProperty("leafOnly")
    private boolean leafOnly = false;

    /** 最大选择层级 */
    @JsonProperty("maxLevel")
    private Integer maxLevel;

    @JsonCreator
    public CascadeFieldConfig(
            @JsonProperty("id") FieldConfigId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name,
            @JsonProperty("cardTypeId") CardTypeId cardTypeId,
            @JsonProperty("fieldId") FieldId fieldId,
            @JsonProperty("systemField") boolean systemField) {
        super(id, orgId, name, cardTypeId, fieldId, systemField);
    }

}
