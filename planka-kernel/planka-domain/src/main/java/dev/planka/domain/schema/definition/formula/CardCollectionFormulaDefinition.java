package dev.planka.domain.schema.definition.formula;

import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.formula.FormulaId;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.domain.schema.SchemaSubType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 卡片汇集公式定义
 * <p>
 * 用于对关联卡片的个数或数值属性进行汇集。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CardCollectionFormulaDefinition extends AbstractFormulaDefinition {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION;
    }

    /**
     * 关联属性ID（格式："{linkTypeId}:{SOURCE|TARGET}"）
     */
    @JsonProperty("linkFieldId")
    private LinkFieldId linkFieldId;

    /**
     * 目标卡片类型ID列表（可选）
     * <p>
     * 如果为空，表示关联类型定义中允许的所有卡片类型
     * 如果指定，只汇集指定卡片类型的关联卡片
     */
    @JsonProperty("targetCardTypeIds")
    private List<CardTypeId> targetCardTypeIds;

    /**
     * 关联卡片中的源数值字段ID（可选）
     * <p>
     * 当汇集方式为 COUNT 或 DISTINCT_COUNT 时，可以为空（对卡片个数进行计数）
     * 当汇集方式为其他类型时，必须指定且必须是数字类型字段
     */
    @JsonProperty("sourceFieldId")
    private FieldId sourceFieldId;

    /**
     * 汇集方式
     */
    @JsonProperty("aggregationType")
    private CardAggregationType aggregationType;

    /**
     * 过滤条件（JSON格式，用于过滤关联卡片）
     * <p>
     * 只有符合条件的关联卡片才会参与汇集
     */
    @JsonProperty("filterCondition")
    private String filterCondition;

    @JsonCreator
    public CardCollectionFormulaDefinition(
            @JsonProperty("id") FormulaId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        // id 可以为 null，AbstractSchemaDefinition 构造函数会自动生成新 ID
        super(id, orgId, name);
    }

    @Override
    public void validate() {
        super.validate();
        if (linkFieldId == null) {
            throw new IllegalArgumentException("linkFieldId 不能为空");
        }
        if (aggregationType == null) {
            throw new IllegalArgumentException("aggregationType 不能为空");
        }

        // 如果汇集方式为 COUNT 或 DISTINCT_COUNT，sourceFieldId 可以为空
        // 如果汇集方式为其他类型，sourceFieldId 必须存在
        if (aggregationType != CardAggregationType.COUNT && aggregationType != CardAggregationType.DISTINCT_COUNT) {
            if (sourceFieldId == null) {
                throw new IllegalArgumentException("当 aggregationType 为 " + aggregationType + " 时，sourceFieldId 不能为空");
            }
        }

        // filterCondition 如果指定，必须是有效的 JSON 格式（这里只做基本检查，详细校验在 Service 层）
        if (filterCondition != null && !filterCondition.trim().isEmpty()) {
            // 基本格式检查：必须以 { 或 [ 开头
            String trimmed = filterCondition.trim();
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                throw new IllegalArgumentException("filterCondition 必须是有效的 JSON 格式");
            }
        }
    }
}
