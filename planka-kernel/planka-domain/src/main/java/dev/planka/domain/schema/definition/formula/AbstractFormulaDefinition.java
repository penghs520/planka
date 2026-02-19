package dev.planka.domain.schema.definition.formula;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.formula.FormulaId;
import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.planka.domain.schema.definition.SchemaDefinition;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 计算公式定义抽象基类
 * <p>
 * 所有公式定义的公共基类，包含公式的基本信息。
 * <p>
 * 注意：Jackson 多态注解已集中配置在 {@link SchemaDefinition} 接口上。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class AbstractFormulaDefinition extends AbstractSchemaDefinition<FormulaId> {

    /**
     * 公式编码（唯一标识，可以为空，仅在数据同步场景需要使用）
     */
    @JsonProperty("code")
    protected String code;

    /**
     * 关联的卡片类型ID列表
     * <p>
     * 可以关联单个实体类型（EntityCardType）
     * 或关联多个属性集（AbstractCardType）
     */
    @JsonProperty("cardTypeIds")
    private List<CardTypeId> cardTypeIds;

    protected AbstractFormulaDefinition(FormulaId id, String orgId, String name) {
        super(id, orgId, name);
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.FORMULA_DEFINITION;
    }

    @Override
    public SchemaId belongTo() {
        return null;  // 公式定义没有所属关系
    }

    @Override
    public Set<SchemaId> secondKeys() {
        if (cardTypeIds == null) {
            return Set.of();
        }
        return new HashSet<>(cardTypeIds);
    }

    @Override
    protected FormulaId newId() {
        return FormulaId.generate();
    }

    @Override
    public void validate() {
        super.validate();
        // 基本校验：cardTypeIds 如果存在，不能为空列表
        // 详细的卡片类型存在性和类型检查（抽象/具体）在 Service 层的生命周期处理器中完成
        if (cardTypeIds != null && cardTypeIds.isEmpty()) {
            throw new IllegalArgumentException("cardTypeIds 不能为空列表");
        }
    }
}
