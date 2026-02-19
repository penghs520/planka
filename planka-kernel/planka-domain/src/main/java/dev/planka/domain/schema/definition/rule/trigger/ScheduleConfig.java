package dev.planka.domain.schema.definition.rule.trigger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

/**
 * 定时触发配置
 * <p>
 * 用于 ON_SCHEDULE 类型的规则触发。
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScheduleConfig {

    /**
     * 调度类型
     */
    @JsonProperty("scheduleType")
    private ScheduleType scheduleType;

    /**
     * Cron表达式（scheduleType=CRON时使用）
     * <p>
     * 标准6位 Cron: 秒 分 时 日 月 周
     */
    @JsonProperty("cronExpression")
    private String cronExpression;

    /**
     * 执行时间（scheduleType=DAILY/WEEKLY/MONTHLY时使用）
     */
    @JsonProperty("executeTime")
    private LocalTime executeTime;

    /**
     * 执行星期（scheduleType=WEEKLY时使用）
     * <p>
     * 1=周一, 7=周日
     */
    @JsonProperty("daysOfWeek")
    private List<Integer> daysOfWeek;

    /**
     * 执行日期（scheduleType=MONTHLY时使用）
     * <p>
     * 1-31，支持负数表示倒数（-1=最后一天）
     */
    @JsonProperty("daysOfMonth")
    private List<Integer> daysOfMonth;

    /**
     * 时区
     */
    @JsonProperty("timezone")
    private String timezone = "Asia/Shanghai";

    /**
     * 调度类型枚举
     */
    public enum ScheduleType {
        /** 自定义 Cron */
        CRON,
        /** 每天执行 */
        DAILY,
        /** 每周执行 */
        WEEKLY,
        /** 每月执行 */
        MONTHLY
    }

    /**
     * 创建每日定时配置
     */
    public static ScheduleConfig daily(LocalTime time) {
        ScheduleConfig config = new ScheduleConfig();
        config.setScheduleType(ScheduleType.DAILY);
        config.setExecuteTime(time);
        return config;
    }

    /**
     * 创建 Cron 定时配置
     */
    public static ScheduleConfig cron(String expression) {
        ScheduleConfig config = new ScheduleConfig();
        config.setScheduleType(ScheduleType.CRON);
        config.setCronExpression(expression);
        return config;
    }
}
