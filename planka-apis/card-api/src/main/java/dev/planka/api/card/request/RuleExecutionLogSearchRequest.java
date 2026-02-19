package dev.planka.api.card.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则执行日志搜索请求
 */
@Getter
@Setter
public class RuleExecutionLogSearchRequest {

    /** 规则ID列表（可选） */
    private List<String> ruleIds;

    /** 执行状态列表：SUCCESS/FAILED/SKIPPED（可选） */
    private List<String> statuses;

    /** 开始时间（可选） */
    private LocalDateTime startTime;

    /** 结束时间（可选） */
    private LocalDateTime endTime;

    /** 页码（从1开始） */
    private int page = 1;

    /** 每页大小 */
    private int size = 20;

    /** 是否升序排列（false=最新在前） */
    private boolean sortAsc = false;

    /**
     * 获取偏移量
     */
    public int getOffset() {
        return (Math.max(1, page) - 1) * size;
    }
}
