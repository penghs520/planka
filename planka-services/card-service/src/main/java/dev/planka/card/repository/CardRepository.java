package dev.planka.card.repository;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.card.model.CardEntity;
import dev.planka.common.result.PageResult;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;
import dev.planka.infra.cache.card.model.CardBasicInfo;
import dev.planka.api.card.request.*;
import planka.graph.driver.proto.write.BatchCardCommonResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 卡片仓储接口
 * 定义所有卡片数据操作
 */
public interface CardRepository {

    // ==================== 写操作 ====================

    /**
     * 创建卡片
     */
    CardId create(CardEntity cardEntity);

    /**
     * 更新卡片
     */
    void update(CardEntity cardEntity);

    /**
     * 丢弃卡片
     */
    void discard(CardId cardId, String discardReason, String operatorId);

    /**
     * 批量创建卡片
     */
    List<CardId> batchCreate(List<CardEntity> cardEntities);

    /**
     * 批量更新卡片
     */
    BatchCardCommonResponse batchUpdate(List<CardEntity> cardEntities);

    /**
     * 批量丢弃卡片
     */
    void batchDiscard(List<CardId> cardIds, String discardReason, String operatorId);

    /**
     * 批量归档卡片
     */
    void batchArchive(List<CardId> cardIds, String operatorId);

    /**
     * 批量还原卡片
     */
    void batchRestore(List<CardId> cardIds, String operatorId);

    /**
     * 归档卡片
     */
    void archive(CardId cardId, String operatorId);

    /**
     * 还原卡片
     */
    void restore(CardId cardId, String operatorId);

    /**
     * 更新卡片价值流状态
     *
     * @param cardId     卡片ID
     * @param streamId   价值流ID
     * @param statusId   目标状态ID
     * @param operatorId 操作人ID
     */
    void updateStatus(CardId cardId, StreamId streamId, StatusId statusId, String operatorId);

    /**
     * 批量更新卡片价值流状态
     *
     * @param cardIds    卡片ID列表
     * @param streamId   价值流ID
     * @param statusId   目标状态ID
     * @param operatorId 操作人ID
     */
    void batchUpdateStatus(List<CardId> cardIds, StreamId streamId, StatusId statusId, String operatorId);

    // ==================== 读操作 ====================

    /**
     * 根据ID查询卡片
     */
    Optional<CardDTO> findById(CardId cardId, Yield yield, String operatorId);

    /**
     * 根据多个ID查询卡片
     */
    List<CardDTO> findByIds(List<CardId> cardIds, Yield yield, String operatorId);

    /**
     * 查询卡片列表 (不分页)
     */
    List<CardDTO> query(CardQueryRequest request);

    /**
     * 分页查询卡片
     */
    PageResult<CardDTO> pageQuery(CardPageQueryRequest request);

    /**
     * 查询卡片ID列表
     */
    List<String> queryIds(CardIdQueryRequest request);

    /**
     * 统计卡片数量
     */
    Integer count(CardCountRequest request);

    /**
     * 批量查询卡片名称
     * <p>
     * 轻量级查询，只返回卡片ID和标题的映射，用于审计日志等场景
     *
     * @param cardIds 卡片ID列表
     * @param operatorId 操作人ID（用于权限控制）
     * @return 卡片ID到标题的映射
     */
    Map<String, CardTitle> queryCardNames(List<String> cardIds, String operatorId);

    /**
     * 批量查询卡片基础信息（用于权限校验）
     * <p>
     * 轻量级查询，只返回权限校验必需的字段，不包含完整的属性值和关联卡片
     *
     * @param cardIds 卡片ID集合
     * @return 卡片ID到基础信息的映射（不存在的卡片不在结果中）
     */
    Map<CardId, CardBasicInfo> findBasicInfoByIds(Set<CardId> cardIds);
}
