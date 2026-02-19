package dev.planka.domain.history.source;

import dev.planka.domain.history.OperationSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * 定时任务执行来源
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ScheduledTaskOperationSource implements OperationSource {

    public static final String TYPE = "SCHEDULED_TASK";
    private static final String MESSAGE_KEY = "history.source.scheduled";

    /**
     * 定时任务ID
     */
    private final String taskId;

    /**
     * 定时任务名称
     */
    private final String taskName;

    @JsonCreator
    public ScheduledTaskOperationSource(
            @JsonProperty("taskId") String taskId,
            @JsonProperty("taskName") String taskName) {
        this.taskId = taskId;
        this.taskName = taskName;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getMessageKey() {
        return MESSAGE_KEY;
    }
}
