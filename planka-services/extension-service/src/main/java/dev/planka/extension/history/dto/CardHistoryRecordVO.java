package dev.planka.extension.history.dto;

import dev.planka.domain.history.CardHistoryRecord;
import dev.planka.domain.history.OperationSource;
import dev.planka.domain.history.OperationType;
import dev.planka.extension.history.vo.HistoryMessageVO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 卡片操作历史记录 VO
 * <p>
 * 用于前端展示，包含操作人名称等显示信息
 */
@Data
@Builder
public class CardHistoryRecordVO {

    /**
     * 历史记录ID
     */
    private String id;

    /**
     * 卡片ID
     */
    private Long cardId;

    /**
     * 操作类型
     */
    private OperationType operationType;

    /**
     * 操作人ID（成员卡片ID）
     */
    private String operatorId;

    /**
     * 操作人名称（从成员卡片标题获取）
     */
    private String operatorName;

    /**
     * 操作人IP
     */
    private String operatorIp;

    /**
     * 操作来源
     */
    private OperationSource operationSource;

    /**
     * 国际化消息（已填充显示名称）
     */
    private HistoryMessageVO message;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 从领域模型转换为 VO
     *
     * @param record       领域模型
     * @param operatorName 操作人名称（可为null）
     * @param messageVO    已解析的消息 VO
     * @return VO
     */
    public static CardHistoryRecordVO from(
            CardHistoryRecord record,
            String operatorName,
            HistoryMessageVO messageVO) {
        return CardHistoryRecordVO.builder()
                .id(record.id().value())
                .cardId(record.cardId())
                .operationType(record.operationType())
                .operatorId(record.operatorId())
                .operatorName(operatorName != null ? operatorName : record.operatorId())
                .operatorIp(record.operatorIp())
                .operationSource(record.operationSource())
                .message(messageVO)
                .traceId(record.traceId())
                .createdAt(record.createdAt())
                .build();
    }
}
