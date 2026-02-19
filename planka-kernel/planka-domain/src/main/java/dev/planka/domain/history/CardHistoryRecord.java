package dev.planka.domain.history;

import java.time.LocalDateTime;

/**
 * 卡片操作历史记录
 * <p>
 * 领域模型，用于在服务层传递历史记录数据
 */
public record CardHistoryRecord(
        CardHistoryId id,
        String orgId,
        Long cardId,
        String cardTypeId,
        OperationType operationType,
        String operatorId,
        String operatorIp,
        OperationSource operationSource,
        HistoryMessage message,
        String traceId,
        LocalDateTime createdAt
) {

    /**
     * 创建新的历史记录（不含ID，ID 由 Repository 生成）
     */
    public static CardHistoryRecord create(
            String orgId,
            Long cardId,
            String cardTypeId,
            OperationType operationType,
            String operatorId,
            String operatorIp,
            OperationSource operationSource,
            HistoryMessage message,
            String traceId) {
        return new CardHistoryRecord(
                null,
                orgId,
                cardId,
                cardTypeId,
                operationType,
                operatorId,
                operatorIp,
                operationSource,
                message,
                traceId,
                LocalDateTime.now()
        );
    }
}
