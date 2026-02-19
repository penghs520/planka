package dev.planka.api.card.request;

import dev.planka.domain.card.CardId;
import lombok.Data;

import java.util.List;

/**
 * 批量操作请求
 * <p>
 * 用于批量归档、还原、丢弃等操作
 */
@Data
public class BatchOperationRequest {
    /**
     * 卡片ID列表
     */
    private List<CardId> cardIds;

    /**
     * 丢弃原因（仅丢弃操作使用）
     */
    private String discardReason;
}
