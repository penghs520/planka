package dev.planka.card.service.flowrecord;

import dev.planka.domain.stream.FlowRecordType;
import dev.planka.domain.stream.StatusWorkType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流动记录实体
 * <p>
 * 注意：此实体对应动态创建的分表，不使用 @TableName 注解
 */
@Data
public class FlowRecordEntity {

    /**
     * 唯一标识（雪花算法）
     */
    private Long id;

    /**
     * 卡片ID
     */
    private Long cardId;

    /**
     * 卡片类型ID
     */
    private String cardTypeId;

    /**
     * 价值流ID
     */
    private String streamId;

    /**
     * 阶段ID
     */
    private String stepId;

    /**
     * 状态ID
     */
    private String statusId;

    /**
     * 状态工作类型（0=等待，1=工作中）
     */
    private Integer statusWorkType;

    /**
     * 记录类型枚举
     */
    private String recordType;

    /**
     * 事件发生时间（毫秒精度）
     */
    private LocalDateTime eventTime;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 获取状态工作类型枚举
     */
    public StatusWorkType getStatusWorkTypeEnum() {
        return statusWorkType != null ? StatusWorkType.fromCode(statusWorkType) : null;
    }

    /**
     * 设置状态工作类型枚举
     */
    public void setStatusWorkTypeEnum(StatusWorkType type) {
        this.statusWorkType = type != null ? type.getCode() : null;
    }

    /**
     * 获取记录类型枚举
     */
    public FlowRecordType getRecordTypeEnum() {
        return recordType != null ? FlowRecordType.valueOf(recordType) : null;
    }

    /**
     * 设置记录类型枚举
     */
    public void setRecordTypeEnum(FlowRecordType type) {
        this.recordType = type != null ? type.name() : null;
    }
}
