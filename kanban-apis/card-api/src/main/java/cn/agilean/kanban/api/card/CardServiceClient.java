package cn.agilean.kanban.api.card;

import cn.agilean.kanban.api.card.dto.BatchOperationResult;
import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.api.card.request.*;
import cn.agilean.kanban.common.result.PageResult;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.card.CardTitle;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 卡片服务 Feign 客户端接口
 * <p>
 * 使用 OpenFeign 调用 card-service 的 REST API
 */
@FeignClient(name = "card-service", contextId = "cardServiceClient", path = "/api/v1/cards")
public interface CardServiceClient {

    // ==================== 写操作 ====================

    /**
     * 创建卡片
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request 创建请求
     * @return 创建后的卡片DTO
     */
    @PostMapping
    Result<CardId> create(@RequestHeader("X-Member-Card-Id") String operatorId,
                          @RequestBody CreateCardRequest request);

    /**
     * 更新卡片
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request 更新请求
     * @return 更新后的卡片ID
     */
    @PutMapping
    Result<Void> update(@RequestHeader("X-Member-Card-Id") String operatorId,
                        @RequestBody UpdateCardRequest request);

    /**
     * 丢弃卡片 (放入回收站)
     *
     * @param cardId        卡片ID
     * @param operatorId    操作人ID（成员卡片ID）
     * @param discardReason 丢弃原因
     */
    @DeleteMapping("/{cardId}")
    Result<Void> discard(@PathVariable("cardId") String cardId,
                         @RequestHeader("X-Member-Card-Id") String operatorId,
                         @RequestParam(value = "discardReason", required = false) String discardReason);

    /**
     * 批量创建卡片
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param requests 创建请求列表
     * @return 批量操作结果（成功/失败的ID列表）
     */
    @PostMapping("/batch")
    Result<BatchOperationResult> batchCreate(@RequestHeader("X-Member-Card-Id") String operatorId,
                                             @RequestBody List<CreateCardRequest> requests);

    /**
     * 批量丢弃卡片
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request    批量丢弃请求
     */
    @DeleteMapping("/batch")
    Result<Void> batchDiscard(@RequestHeader("X-Member-Card-Id") String operatorId,
                              @RequestBody BatchOperationRequest request);

    /**
     * 批量归档卡片
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request    批量操作请求
     */
    @PostMapping("/archive")
    Result<Void> batchArchive(@RequestHeader("X-Member-Card-Id") String operatorId,
                              @RequestBody BatchOperationRequest request);

    /**
     * 批量还原卡片
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request    批量操作请求
     */
    @PostMapping("/restore")
    Result<Void> batchRestore(@RequestHeader("X-Member-Card-Id") String operatorId,
                              @RequestBody BatchOperationRequest request);

    // ==================== 读操作 ====================

    /**
     * 根据ID查询卡片
     *
     * @param cardId     卡片ID
     * @param operatorId 操作人ID（成员卡片ID）
     * @param yield      返回字段控制
     * @return 卡片DTO
     */
    @PostMapping("/{cardId}")
    Result<CardDTO> findById(@PathVariable("cardId") String cardId,
                             @RequestHeader("X-Member-Card-Id") String operatorId,
                             @RequestBody Yield yield);

    /**
     * 根据多个ID查询卡片
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request    查询请求
     * @return 卡片DTO列表
     */
    @PostMapping("/find-by-ids")
    Result<List<CardDTO>> findByIds(@RequestHeader("X-Member-Card-Id") String operatorId,
                                    @RequestBody FindByIdsRequest request);

    /**
     * 查询卡片列表 (不分页)
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request 查询请求
     * @return 卡片DTO列表
     */
    @PostMapping("/query")
    Result<List<CardDTO>> query(@RequestHeader("X-Member-Card-Id") String operatorId,
                                @RequestBody CardQueryRequest request);

    /**
     * 分页查询卡片
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request 分页查询请求
     * @return 分页结果
     */
    @PostMapping("/page-query")
    Result<PageResult<CardDTO>> pageQuery(@RequestHeader("X-Member-Card-Id") String operatorId,
                                          @RequestBody CardPageQueryRequest request);

    /**
     * 查询卡片ID列表
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request 卡片ID查询请求
     * @return 去重后的卡片ID列表
     */
    @PostMapping("/query-ids")
    Result<List<String>> queryIds(@RequestHeader("X-Member-Card-Id") String operatorId,
                                  @RequestBody CardIdQueryRequest request);

    /**
     * 统计卡片数量
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request 卡片计算查询请求
     * @return 卡片数量
     */
    @PostMapping("/count")
    Result<Integer> count(@RequestHeader("X-Member-Card-Id") String operatorId,
                          @RequestBody CardCountRequest request);

    /**
     * 批量查询卡片名称
     * <p>
     * 轻量级查询接口，只返回卡片ID和标题映射，用于审计日志、下拉框等只需要显示名称的场景
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param cardIds 卡片ID列表
     * @return 卡片ID到标题的映射 Map<cardId, CardTitle>
     */
    @PostMapping("/names")
    Result<Map<String, CardTitle>> queryCardNames(@RequestHeader("X-Member-Card-Id") String operatorId,
                                                   @RequestBody List<String> cardIds);

    /**
     * 批量更新卡片状态（用于状态迁移）
     * <p>
     * 将指定源状态下的所有卡片迁移到目标状态
     *
     * @param orgId 组织ID
     * @param sourceStatusId 源状态ID
     * @param targetStatusId 目标状态ID
     * @param streamId 价值流ID
     * @param cardTypeId 卡片类型ID
     * @param operatorId 操作人ID
     * @return 更新的卡片数量
     */
    @PostMapping("/batch-update-status")
    Result<Integer> batchUpdateCardStatus(@RequestParam("orgId") String orgId,
                                          @RequestParam("sourceStatusId") String sourceStatusId,
                                          @RequestParam("targetStatusId") String targetStatusId,
                                          @RequestParam("streamId") String streamId,
                                          @RequestParam("cardTypeId") String cardTypeId,
                                          @RequestHeader("X-Member-Card-Id") String operatorId);
}
