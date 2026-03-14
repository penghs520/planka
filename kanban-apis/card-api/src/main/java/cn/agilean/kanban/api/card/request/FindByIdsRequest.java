package cn.agilean.kanban.api.card.request;

import cn.agilean.kanban.domain.card.CardId;
import lombok.Data;

import java.util.List;

/**
 * 根据ID列表查询请求
 */
@Data
public class FindByIdsRequest {

    private String operatorId;

    /**
     * 卡片ID列表
     */
    private List<CardId> cardIds;

    /**
     * 返回定义
     */
    private Yield yield;
}
