package dev.planka.domain.outsourcing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * 补卡配置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceChangeConf {

    /**
     * 补卡次数（≥0）
     */
    @JsonProperty("count")
    private Integer count = 5;

    /**
     * 补卡窗口（≥0）
     */
    @JsonProperty("window")
    private Integer window = 10;

    /**
     * 窗口单位（CALENDAR_DAY/BUSINESS_DAY）
     */
    @JsonProperty("windowUnit")
    private WindowUnit windowUnit = WindowUnit.CALENDAR_DAY;

    /**
     * 是否允许补非工作日的考勤
     */
    @JsonProperty("allowWeekendOrHoliday")
    private Boolean allowWeekendOrHoliday = false;

    /**
     * 签入时间限制
     */
    @JsonProperty("signIn")
    private TimeLimit signIn;

    /**
     * 签出时间限制
     */
    @JsonProperty("signOut")
    private TimeLimit signOut;

    public void validate() {
        if (count != null && count < 0) {
            throw new IllegalArgumentException("补卡次数必须 ≥ 0");
        }
        if (window != null && window < 0) {
            throw new IllegalArgumentException("补卡窗口必须 ≥ 0");
        }

        if (signIn != null) {
            signIn.validate();
        }
        if (signOut != null) {
            signOut.validate();
        }
    }

    /**
     * 时间限制
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeLimit {
        @JsonProperty("start")
        private String start = "00:00";

        @JsonProperty("end")
        private String end = "23:59";

        public void validate() {
            LocalTime startTime = parseTime(start, "start");
            LocalTime endTime = parseTime(end, "end");

            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("时间限制的 end 必须晚于 start");
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
}
