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
 * 日期汇集公式定义
 * <p>
 * 用于从关联卡片的某个日期字段中汇集最早、最晚时间。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DateCollectionFormulaDefinition extends AbstractFormulaDefinition {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION;
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
     * 关联卡片中的源日期字段ID（必须是日期类型字段）
     */
    @JsonProperty("sourceFieldId")
    private FieldId sourceFieldId;

    /**
     * 汇集方式
     */
    @JsonProperty("aggregationType")
    private DateAggregationType aggregationType;

    /**
     * 过滤条件（JSON格式，用于过滤关联卡片）
     * <p>
     * 只有符合条件的关联卡片才会参与汇集
     */
    @JsonProperty("filterCondition")
    private String filterCondition;

    @JsonCreator
    public DateCollectionFormulaDefinition(
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
        if (sourceFieldId == null) {
            throw new IllegalArgumentException("sourceFieldId 不能为空");
        }
        if (aggregationType == null) {
            throw new IllegalArgumentException("aggregationType 不能为空");
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
