package dev.planka.domain.schema.definition.formula;

import dev.planka.domain.field.FieldId;
import dev.planka.domain.formula.FormulaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 时间点公式定义
 * <p>
 * 用于获取卡片的时间点信息，支持多种数据源。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TimePointFormulaDefinition extends AbstractFormulaDefinition {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.TIME_POINT_FORMULA_DEFINITION;
    }

    /**
     * 数据源类型
     */
    @JsonProperty("sourceType")
    private TimePointSourceType sourceType;

    /**
     * 源日期字段ID（当 sourceType 为 CUSTOM_DATE_FIELD 时必填）
     */
    @JsonProperty("sourceFieldId")
    private FieldId sourceFieldId;

    /**
     * 价值流ID（当 sourceType 为 STATUS_ENTER_TIME, STATUS_EXIT_TIME, CURRENT_STATUS_ENTER_TIME 时必填）
     */
    @JsonProperty("streamId")
    private StreamId streamId;

    /**
     * 价值流状态ID（当 sourceType 为 STATUS_ENTER_TIME 或 STATUS_EXIT_TIME 时必填）
     */
    @JsonProperty("statusId")
    private StatusId statusId;

    @JsonCreator
    public TimePointFormulaDefinition(
            @JsonProperty("id") FormulaId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        // id 可以为 null，AbstractSchemaDefinition 构造函数会自动生成新 ID
        super(id, orgId, name);
    }

    @Override
    public void validate() {
        super.validate();
        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType 不能为空");
        }

        // 根据 sourceType 校验必填字段
        switch (sourceType) {
            case CUSTOM_DATE_FIELD:
                if (sourceFieldId == null) {
                    throw new IllegalArgumentException("当 sourceType 为 CUSTOM_DATE_FIELD 时，sourceFieldId 不能为空");
                }
                break;
            case STATUS_ENTER_TIME:
            case STATUS_EXIT_TIME:
                if (streamId == null) {
                    throw new IllegalArgumentException("当 sourceType 为 " + sourceType + " 时，streamId 不能为空");
                }
                if (statusId == null) {
                    throw new IllegalArgumentException("当 sourceType 为 " + sourceType + " 时，statusId 不能为空");
                }
                break;
            case CURRENT_STATUS_ENTER_TIME:
                if (streamId == null) {
                    throw new IllegalArgumentException("当 sourceType 为 CURRENT_STATUS_ENTER_TIME 时，streamId 不能为空");
                }
                break;
            case CARD_CREATED_TIME:
            case CARD_UPDATED_TIME:
                // 不需要额外字段
                break;
            default:
                throw new IllegalArgumentException("不支持的数据源类型: " + sourceType);
        }
    }
}
