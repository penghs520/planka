package dev.planka.domain.stream;

import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTypeId;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 流动记录领域模型
 * <p>
 * 记录卡片在价值流中状态变化的历史，用于：
 * <ul>
 *   <li>查看卡片进入/离开某状态的时间</li>
 *   <li>计算卡片在某状态的停留时间</li>
 *   <li>支持价值流效率度量（前置时间、周期时间等）</li>
 * </ul>
 */
@Getter
@Builder
public class FlowRecord {

    /**
     * 流动记录ID
     */
    private final FlowRecordId id;

    /**
     * 卡片ID
     */
    private final CardId cardId;

    /**
     * 卡片类型ID
     */
    private final CardTypeId cardTypeId;

    /**
     * 价值流ID
     */
    private final StreamId streamId;

    /**
     * 阶段ID
     */
    private final StepId stepId;

    /**
     * 状态ID
     */
    private final StatusId statusId;

    /**
     * 状态工作类型（等待/工作中）
     */
    private final StatusWorkType statusWorkType;

    /**
     * 流动记录类型（进入/离开/回滚进入/回滚离开）
     */
    private final FlowRecordType recordType;

    /**
     * 事件发生时间
     */
    private final LocalDateTime eventTime;

    /**
     * 操作人ID
     */
    private final String operatorId;
}
