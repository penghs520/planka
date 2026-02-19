package dev.planka.card.service.permission.model;

import dev.planka.domain.card.CardId;

/**
 * 权限拒绝项
 * <p>
 * 用于批量权限校验时，记录被拒绝的卡片及原因
 */
public class PermissionDeniedItem {

    private final CardId cardId;
    private final String message;

    public PermissionDeniedItem(CardId cardId, String message) {
        this.cardId = cardId;
        this.message = message;
    }

    /**
     * 获取卡片ID
     */
    public CardId getCardId() {
        return cardId;
    }

    /**
     * 获取拒绝原因（来自权限配置的 alertMessage）
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "PermissionDeniedItem{" +
                "cardId=" + cardId +
                ", message='" + message + '\'' +
                '}';
    }
}
