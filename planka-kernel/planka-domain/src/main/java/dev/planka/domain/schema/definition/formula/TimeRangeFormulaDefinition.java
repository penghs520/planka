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
 * 时间段公式定义
 * <p>
 * 用于计算两个时间点之间的差值，支持多种时间点数据源。
 */
@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TimeRangeFormulaDefinition extends AbstractFormulaDefinition {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.TIME_RANGE_FORMULA_DEFINITION;
    }

    /**
     * 开始时间数据源类型
     */
    @JsonProperty("startSourceType")
    private TimePointSourceType startSourceType;

    /**
     * 开始时间字段ID（当 startSourceType 为 CUSTOM_DATE_FIELD 时必填）
     */
    @JsonProperty("startFieldId")
    private FieldId startFieldId;

    /**
     * 开始时间价值流ID（当 startSourceType 为 STATUS_ENTER_TIME, STATUS_EXIT_TIME, CURRENT_STATUS_ENTER_TIME 时必填）
     */
    @JsonProperty("startStreamId")
    private StreamId startStreamId;

    /**
     * 开始时间价值流状态ID（当 startSourceType 为 STATUS_ENTER_TIME 或 STATUS_EXIT_TIME 时必填）
     */
    @JsonProperty("startStatusId")
    private StatusId startStatusId;

    /**
     * 结束时间数据源类型
     */
    @JsonProperty("endSourceType")
    private TimePointSourceType endSourceType;

    /**
     * 结束时间字段ID（当 endSourceType 为 CUSTOM_DATE_FIELD 时必填）
     */
    @JsonProperty("endFieldId")
    private FieldId endFieldId;

    /**
     * 结束时间价值流ID（当 endSourceType 为 STATUS_ENTER_TIME, STATUS_EXIT_TIME, CURRENT_STATUS_ENTER_TIME 时必填）
     */
    @JsonProperty("endStreamId")
    private StreamId endStreamId;

    /**
     * 结束时间价值流状态ID（当 endSourceType 为 STATUS_ENTER_TIME 或 STATUS_EXIT_TIME 时必填）
     */
    @JsonProperty("endStatusId")
    private StatusId endStatusId;

    /**
     * 统计精度
     */
    @JsonProperty("precision")
    private TimeRangePrecision precision;

    @JsonCreator
    public TimeRangeFormulaDefinition(
            @JsonProperty("id") FormulaId id,
            @JsonProperty("orgId") String orgId,
            @JsonProperty("name") String name) {
        // id 可以为 null，AbstractSchemaDefinition 构造函数会自动生成新 ID
        super(id, orgId, name);
    }

    @Override
    public void validate() {
        super.validate();
        if (startSourceType == null) {
            throw new IllegalArgumentException("startSourceType 不能为空");
        }
        if (endSourceType == null) {
            throw new IllegalArgumentException("endSourceType 不能为空");
        }
        if (precision == null) {
            throw new IllegalArgumentException("precision 不能为空");
        }

        // 开始时间和结束时间不能同时为 CURRENT_TIME
        if (startSourceType == TimePointSourceType.CURRENT_TIME && endSourceType == TimePointSourceType.CURRENT_TIME) {
            throw new IllegalArgumentException("startSourceType 和 endSourceType 不能同时为 CURRENT_TIME");
        }

        // 根据 startSourceType 校验必填字段
        validateTimePointSource(startSourceType, startFieldId, startStreamId, startStatusId, "start");

        // 根据 endSourceType 校验必填字段
        validateTimePointSource(endSourceType, endFieldId, endStreamId, endStatusId, "end");
    }

    private void validateTimePointSource(TimePointSourceType sourceType, FieldId fieldId, StreamId streamId, StatusId statusId, String prefix) {
        switch (sourceType) {
            case CUSTOM_DATE_FIELD:
                if (fieldId == null) {
                    throw new IllegalArgumentException("当 " + prefix + "SourceType 为 CUSTOM_DATE_FIELD 时，" + prefix + "FieldId 不能为空");
                }
                break;
            case STATUS_ENTER_TIME:
            case STATUS_EXIT_TIME:
                if (streamId == null) {
                    throw new IllegalArgumentException("当 " + prefix + "SourceType 为 " + sourceType + " 时，" + prefix + "StreamId 不能为空");
                }
                if (statusId == null) {
                    throw new IllegalArgumentException("当 " + prefix + "SourceType 为 " + sourceType + " 时，" + prefix + "StatusId 不能为空");
                }
                break;
            case CURRENT_STATUS_ENTER_TIME:
                if (streamId == null) {
                    throw new IllegalArgumentException("当 " + prefix + "SourceType 为 CURRENT_STATUS_ENTER_TIME 时，" + prefix + "StreamId 不能为空");
                }
                break;
            case CARD_CREATED_TIME:
            case CARD_UPDATED_TIME:
            case CURRENT_TIME:
                // 不需要额外字段
                break;
            default:
                throw new IllegalArgumentException("不支持的数据源类型: " + sourceType);
        }
    }
}
