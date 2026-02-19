package dev.planka.domain.outsourcing;

import dev.planka.domain.schema.SchemaId;
import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.AbstractSchemaDefinition;
import dev.planka.domain.schema.definition.condition.Condition;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * 考勤配置 Schema 定义
 *
 * 作为组织级别的配置，继承 AbstractSchemaDefinition
 */
@Getter
@Setter
public class OutsourcingConfig extends AbstractSchemaDefinition<OutsourcingConfigId> {

    /**
     * 时间统计折算单位（Hour/Minute）
     */
    @JsonProperty("durationUnit")
    private DurationUnit durationUnit = DurationUnit.MINUTE;

    /**
     * 小数位数（≥0）
     */
    @JsonProperty("decimalScale")
    private Integer decimalScale = 0;

    /**
     * 成员卡片类型ID（可选）
     * 指定参与考勤的成员卡片类型
     */
    @JsonProperty("memberCardTypeId")
    private String memberCardTypeId;

    /**
     * 成员筛选条件（可选）
     * 用于进一步筛选该卡片类型下的成员卡片
     */
    @JsonProperty("memberFilter")
    private Condition memberFilter;

    /**
     * 卡片考勤必填
     */
    @JsonProperty("cardAttendanceRequired")
    private Boolean cardAttendanceRequired = false;

    /**
     * 签到配置
     */
    @JsonProperty("attendanceConf")
    private AttendanceConf attendanceConf;

    /**
     * 请假配置
     */
    @JsonProperty("leaveConf")
    private LeaveConf leaveConf;

    /**
     * 加班配置
     */
    @JsonProperty("overtimeConf")
    private OvertimeConf overtimeConf;

    /**
     * 补卡配置
     */
    @JsonProperty("attendanceChangeConf")
    private AttendanceChangeConf attendanceChangeConf;

    /**
     * 结算配置
     */
    @JsonProperty("settlementConf")
    private SettlementConf settlementConf;

    public OutsourcingConfig(OutsourcingConfigId id, String orgId, String name) {
        super(id, orgId, name);
    }

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.OUTSOURCING_CONFIG;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.OUTSOURCING_CONFIG;
    }

    @Override
    public SchemaId belongTo() {
        return null; // 组织级别配置，不属于其他 Schema
    }

    @Override
    public Set<SchemaId> secondKeys() {
        return Set.of(); // 无二级索引
    }

    @Override
    protected OutsourcingConfigId newId() {
        return OutsourcingConfigId.generate();
    }

    @Override
    public void validate() {
        super.validate();

        if (decimalScale != null && decimalScale < 0) {
            throw new IllegalArgumentException("decimalScale 必须 ≥ 0");
        }

        // 校验子配置
        if (attendanceConf != null) {
            attendanceConf.validate();
        }
        if (leaveConf != null) {
            leaveConf.validate();
        }
        if (overtimeConf != null) {
            overtimeConf.validate();
        }
        if (attendanceChangeConf != null) {
            attendanceChangeConf.validate();
        }
        if (settlementConf != null) {
            settlementConf.validate();
        }
    }
}
