package dev.planka.infra.expression;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.request.Yield;
import dev.planka.domain.card.CardId;

/**
 * 卡片数据提供者接口
 * <p>
 * 由 card-service 实现，为表达式解析器提供卡片数据查询能力。
 */
public interface CardDataProvider {

    /**
     * 根据卡片ID和Yield查询卡片数据
     *
     * @param cardId 卡片ID
     * @param yield  查询返回定义
     * @return 卡片数据，不存在时返回 null
     */
    CardDTO findCardById(CardId cardId, Yield yield);
}
