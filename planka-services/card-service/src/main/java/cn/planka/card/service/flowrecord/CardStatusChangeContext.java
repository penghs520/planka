package cn.planka.card.service.flowrecord;

import cn.planka.domain.card.CardId;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.stream.StatusId;
import cn.planka.domain.stream.StatusWorkType;
import cn.planka.domain.stream.StepId;
import cn.planka.domain.stream.StreamId;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 卡片状态变更上下文
 * <p>
 * 封装状态变更所需的所有信息，用于生成流动记录
 */
@Getter
@Builder
public class CardStatusChangeContext {

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
     * 原阶段ID
     */
    private final StepId fromStepId;

    /**
     * 原状态ID
     */
    private final StatusId fromStatusId;

    /**
     * 原状态工作类型
     */
    private final StatusWorkType fromStatusWorkType;

    /**
     * 目标阶段ID
     */
    private final StepId toStepId;

    /**
     * 目标状态ID
     */
    private final StatusId toStatusId;

    /**
     * 目标状态工作类型
     */
    private final StatusWorkType toStatusWorkType;

    /**
     * 是否为回滚操作
     */
    private final boolean rollback;

    /**
     * 事件发生时间
     */
    private final LocalDateTime eventTime;

    /**
     * 操作人ID
     */
    private final String operatorId;
}
