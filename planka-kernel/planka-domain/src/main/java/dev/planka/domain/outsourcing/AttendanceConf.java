package dev.planka.domain.outsourcing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 签到配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceConf {

    /**
     * 每日工作开始时间（HH:mm）
     */
    @JsonProperty("workStart")
    private String workStart = "08:30";

    /**
     * 每日工作结束时间（HH:mm）
     */
    @JsonProperty("workEnd")
    private String workEnd = "17:30";

    /**
     * 午休开始时间（HH:mm）
     */
    @JsonProperty("lunchStart")
    private String lunchStart = "11:30";

    /**
     * 午休结束时间（HH:mm）
     */
    @JsonProperty("lunchEnd")
    private String lunchEnd = "13:30";

    /**
     * 正常出勤一天折算工作时长（小时）
     */
    @JsonProperty("workDuration")
    private Double workDuration = 8.0;

    /**
     * 工作时间是否用于工时分配
     */
    @JsonProperty("impactWm")
    private Boolean impactWm = false;

    /**
     * 可分配工作时长是否累计加班时长
     */
    @JsonProperty("accumulatedOvertime")
    private Boolean accumulatedOvertime = true;

    /**
     * 只有签入或只有签出时是否计入旷工
     */
    @JsonProperty("absenceWhenNoSignInOrOut")
    private Boolean absenceWhenNoSignInOrOut = false;

    /**
     * 校验方法
     */
    public void validate() {
        LocalTime workStartTime = parseTime(workStart, "workStart");
        LocalTime workEndTime = parseTime(workEnd, "workEnd");
        LocalTime lunchStartTime = parseTime(lunchStart, "lunchStart");
        LocalTime lunchEndTime = parseTime(lunchEnd, "lunchEnd");

        if (!workEndTime.isAfter(workStartTime)) {
            throw new IllegalArgumentException("工作结束时间必须晚于工作开始时间");
        }

        if (!lunchEndTime.isAfter(lunchStartTime)) {
            throw new IllegalArgumentException("午休结束时间必须晚于午休开始时间");
        }

        if (lunchStartTime.isBefore(workStartTime) || lunchEndTime.isAfter(workEndTime)) {
            throw new IllegalArgumentException("午休时间必须在工作时间段内");
        }

        if (workDuration <= 0) {
            throw new IllegalArgumentException("工作时长必须 > 0");
        }
    }

    private LocalTime parseTime(String time, String fieldName) {
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " 格式错误，必须为 HH:mm 格式");
        }
    }
}
