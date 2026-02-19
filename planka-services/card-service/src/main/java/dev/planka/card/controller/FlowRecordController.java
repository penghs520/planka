package dev.planka.card.controller;

import dev.planka.card.service.flowrecord.FlowRecordService;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.stream.FlowRecord;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 流动记录控制器
 * <p>
 * 提供流动记录查询 API
 */
@RestController
@RequestMapping("/api/v1/flow-records")
public class FlowRecordController {

    private final FlowRecordService flowRecordService;

    public FlowRecordController(FlowRecordService flowRecordService) {
        this.flowRecordService = flowRecordService;
    }

    /**
     * 查询卡片的流动历史
     *
     * @param streamId 价值流ID
     * @param cardId   卡片ID
     * @return 流动记录列表
     */
    @GetMapping("/history")
    public Result<List<FlowRecord>> getCardFlowHistory(
            @RequestParam("streamId") String streamId,
            @RequestParam("cardId") String cardId) {
        List<FlowRecord> records = flowRecordService.getCardFlowHistory(
                StreamId.of(streamId),
                CardId.of(cardId));
        return Result.success(records);
    }

    /**
     * 查询卡片在某状态的流动记录
     *
     * @param streamId 价值流ID
     * @param cardId   卡片ID
     * @param statusId 状态ID
     * @return 流动记录列表
     */
    @GetMapping("/status-records")
    public Result<List<FlowRecord>> getCardStatusRecords(
            @RequestParam("streamId") String streamId,
            @RequestParam("cardId") String cardId,
            @RequestParam("statusId") String statusId) {
        List<FlowRecord> records = flowRecordService.getCardStatusRecords(
                StreamId.of(streamId),
                CardId.of(cardId),
                StatusId.of(statusId));
        return Result.success(records);
    }

    /**
     * 计算卡片在某状态的停留时间
     *
     * @param streamId 价值流ID
     * @param cardId   卡片ID
     * @param statusId 状态ID
     * @return 停留时间（毫秒）
     */
    @GetMapping("/status-duration")
    public Result<Long> getStatusDuration(
            @RequestParam("streamId") String streamId,
            @RequestParam("cardId") String cardId,
            @RequestParam("statusId") String statusId) {
        Duration duration = flowRecordService.getStatusDuration(
                StreamId.of(streamId),
                CardId.of(cardId),
                StatusId.of(statusId));
        return Result.success(duration.toMillis());
    }

    /**
     * 根据时间范围查询流动记录
     *
     * @param streamId  价值流ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 流动记录列表
     */
    @GetMapping("/by-time-range")
    public Result<List<FlowRecord>> getRecordsByTimeRange(
            @RequestParam("streamId") String streamId,
            @RequestParam("startTime") LocalDateTime startTime,
            @RequestParam("endTime") LocalDateTime endTime) {
        List<FlowRecord> records = flowRecordService.getRecordsByTimeRange(
                StreamId.of(streamId),
                startTime,
                endTime);
        return Result.success(records);
    }
}
