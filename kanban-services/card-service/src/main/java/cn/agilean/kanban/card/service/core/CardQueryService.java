package cn.agilean.kanban.card.service.core;

import cn.agilean.kanban.api.card.dto.CardDTO;
import cn.agilean.kanban.api.card.request.CardCountRequest;
import cn.agilean.kanban.api.card.request.CardIdQueryRequest;
import cn.agilean.kanban.api.card.request.CardPageQueryRequest;
import cn.agilean.kanban.api.card.request.CardQueryRequest;
import cn.agilean.kanban.api.card.request.QueryContext;
import cn.agilean.kanban.api.card.request.QueryScope;
import cn.agilean.kanban.api.card.request.Yield;
import cn.agilean.kanban.api.card.request.YieldLink;
import cn.agilean.kanban.card.converter.StructureFieldValueBuilder;
import cn.agilean.kanban.card.repository.CardRepository;
import cn.agilean.kanban.card.service.permission.CardPermissionService;
import cn.agilean.kanban.common.exception.CommonErrorCode;
import cn.agilean.kanban.common.result.PageResult;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.card.CardId;
import cn.agilean.kanban.domain.card.CardTitle;
import cn.agilean.kanban.domain.field.FieldValue;
import cn.agilean.kanban.domain.schema.definition.fieldconfig.StructureFieldConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 卡片查询服务
 * <p>
 * 负责卡片的查询操作，包括单条查询、批量查询、分页查询等。
 */
@Service
public class CardQueryService {

    private static final Logger logger = LoggerFactory.getLogger(CardQueryService.class);

    private final CardRepository cardRepository;
    private final YieldEnhancer yieldEnhancer;
    private final CardPermissionService cardPermissionService;

    public CardQueryService(CardRepository cardRepository, YieldEnhancer yieldEnhancer,
                            CardPermissionService cardPermissionService) {
        this.cardRepository = cardRepository;
        this.yieldEnhancer = yieldEnhancer;
        this.cardPermissionService = cardPermissionService;
    }

    /**
     * 根据ID查询单个卡片
     */
    public Result<CardDTO> findById(CardId cardId, Yield yield, CardId operatorId) {
        try {
            Yield enhancedYield = yieldEnhancer.enhance(yield);
            Optional<CardDTO> cardOpt = cardRepository.findById(cardId, enhancedYield, String.valueOf(operatorId.value()));
            if (cardOpt.isEmpty()) {
                return Result.failure(CommonErrorCode.NOT_FOUND, "卡片不存在");
            }
            CardDTO cardDTO = cardOpt.get();
            fillStructureFieldValues(cardDTO, yield);
            cardPermissionService.applyFieldReadPermissions(List.of(cardDTO), operatorId);
            return Result.success(cardDTO);
        } catch (Exception e) {
            logger.error("查询卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "查询卡片失败: " + e.getMessage());
        }
    }

    /**
     * 批量查询卡片
     */
    public Result<List<CardDTO>> findByIds(List<CardId> cardIds, Yield yield, CardId operatorId) {
        try {
            Yield enhancedYield = yieldEnhancer.enhance(yield);
            List<CardDTO> cards = cardRepository.findByIds(cardIds, enhancedYield, String.valueOf(operatorId.value()));
            fillStructureFieldValuesForList(cards, yield);
            cardPermissionService.applyFieldReadPermissions(cards, operatorId);
            return Result.success(cards);
        } catch (Exception e) {
            logger.error("批量查询卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量查询卡片失败: " + e.getMessage());
        }
    }

    /**
     * 查询卡片列表
     */
    public Result<List<CardDTO>> query(CardQueryRequest request) {
        try {
            Yield originalYield = request.getYield();
            request.setYield(yieldEnhancer.enhance(originalYield));
            List<CardDTO> cards = cardRepository.query(request);
            fillStructureFieldValuesForList(cards, originalYield);
            applyFieldPermissionsFromRequest(cards, request);
            return Result.success(cards);
        } catch (Exception e) {
            logger.error("查询卡片列表失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "查询卡片列表失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询卡片
     */
    public Result<PageResult<CardDTO>> pageQuery(CardPageQueryRequest request) {
        try {
            Yield originalYield = request.getYield();
            request.setYield(yieldEnhancer.enhance(originalYield));
            PageResult<CardDTO> result = cardRepository.pageQuery(request);
            if (result.getContent() != null) {
                fillStructureFieldValuesForList(result.getContent(), originalYield);
                applyFieldPermissionsFromRequest(result.getContent(), request);
            }
            return Result.success(result);
        } catch (Exception e) {
            logger.error("分页查询卡片失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "分页查询卡片失败: " + e.getMessage());
        }
    }

    /**
     * 查询卡片ID列表
     */
    public Result<List<String>> queryIds(CardIdQueryRequest request) {
        try {
            List<String> ids = cardRepository.queryIds(request);
            return Result.success(ids);
        } catch (Exception e) {
            logger.error("查询卡片ID失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "查询卡片ID失败: " + e.getMessage());
        }
    }

    /**
     * 统计卡片数量
     */
    public Result<Integer> count(CardCountRequest request) {
        try {
            Integer count = cardRepository.count(request);
            return Result.success(count);
        } catch (Exception e) {
            logger.error("统计卡片数量失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "统计卡片数量失败: " + e.getMessage());
        }
    }

    /**
     * 批量查询卡片名称
     */
    public Result<Map<String, CardTitle>> queryCardNames(List<String> cardIds, CardId operatorId) {
        try {
            if (cardIds == null || cardIds.isEmpty()) {
                return Result.success(Map.of());
            }
            Map<String, CardTitle> nameMap = cardRepository.queryCardNames(cardIds, String.valueOf(operatorId.value()));
            return Result.success(nameMap);
        } catch (Exception e) {
            logger.error("批量查询卡片名称失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "批量查询卡片名称失败: " + e.getMessage());
        }
    }

    /**
     * 查询指定状态下的卡片数量
     *
     * @param orgId 组织ID
     * @param statusId 状态ID
     * @param streamId 价值流ID
     * @param cardTypeId 卡片类型ID
     * @return 卡片数量
     */
    public Result<Integer> countCardsByStatus(String orgId, String statusId, String streamId, String cardTypeId) {
        try {
            CardCountRequest request = new CardCountRequest();

            QueryContext queryContext = new QueryContext();
            queryContext.setOrgId(orgId);
            request.setQueryContext(queryContext);

            QueryScope queryScope = new QueryScope();
            queryScope.setCardTypeIds(List.of(cardTypeId));
            request.setQueryScope(queryScope);

            // 构建状态条件
            cn.agilean.kanban.domain.schema.definition.condition.StatusConditionItem.StatusSubject subject =
                    new cn.agilean.kanban.domain.schema.definition.condition.StatusConditionItem.StatusSubject(null, streamId);
            cn.agilean.kanban.domain.schema.definition.condition.StatusConditionItem.StatusOperator operator =
                    new cn.agilean.kanban.domain.schema.definition.condition.StatusConditionItem.StatusOperator.Equal(statusId);
            cn.agilean.kanban.domain.schema.definition.condition.StatusConditionItem statusConditionItem =
                    new cn.agilean.kanban.domain.schema.definition.condition.StatusConditionItem(subject, operator);
            cn.agilean.kanban.domain.schema.definition.condition.Condition condition =
                    cn.agilean.kanban.domain.schema.definition.condition.Condition.of(statusConditionItem);

            request.setCondition(condition);

            Integer count = cardRepository.count(request);
            return Result.success(count);
        } catch (Exception e) {
            logger.error("查询状态下卡片数量失败", e);
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "查询状态下卡片数量失败: " + e.getMessage());
        }
    }

    /**
     * 填充架构属性值
     */
    private void fillStructureFieldValues(CardDTO cardDTO, Yield yield) {
        if (yield == null || cardDTO == null) {
            return;
        }
        List<StructureFieldConfig> structureDefs = yieldEnhancer.extractStructureFieldDefs(yield);
        fillStructureFieldValuesRecursive(cardDTO, yield, structureDefs);
    }

    /**
     * 批量填充架构属性值
     */
    private void fillStructureFieldValuesForList(List<CardDTO> cards, Yield yield) {
        if (yield == null || cards == null || cards.isEmpty()) {
            return;
        }
        List<StructureFieldConfig> structureDefs = yieldEnhancer.extractStructureFieldDefs(yield);
        for (CardDTO cardDTO : cards) {
            fillStructureFieldValuesRecursive(cardDTO, yield, structureDefs);
        }
    }

    /**
     * 从请求中提取 operatorId 并应用字段读权限
     */
    private void applyFieldPermissionsFromRequest(List<CardDTO> cards, CardQueryRequest request) {
        if (cards == null || cards.isEmpty()) {
            return;
        }
        if (request.getQueryContext() != null && request.getQueryContext().getOperatorId() != null) {
            CardId operatorId = CardId.of(request.getQueryContext().getOperatorId());
            cardPermissionService.applyFieldReadPermissions(cards, operatorId);
        }
    }

    /**
     * 递归填充架构属性值
     */
    private void fillStructureFieldValuesRecursive(CardDTO cardDTO, Yield yield,
                                                   List<StructureFieldConfig> structureDefs) {
        if (yield == null || cardDTO == null) {
            return;
        }

        if (!structureDefs.isEmpty()) {
            Map<String, FieldValue<?>> structureValues = StructureFieldValueBuilder.buildAll(cardDTO, structureDefs);
            if (!structureValues.isEmpty()) {
                if (cardDTO.getFieldValues() == null) {
                    cardDTO.setFieldValues(new HashMap<>());
                }
                cardDTO.getFieldValues().putAll(structureValues);
            }
        }

        if (yield.getLinks() != null && cardDTO.getLinkedCards() != null) {
            for (YieldLink link : yield.getLinks()) {
                if (link.getTargetYield() == null) {
                    continue;
                }
                Set<CardDTO> linkedCards = cardDTO.getLinkedCards().get(link.getLinkFieldId());
                if (linkedCards != null) {
                    for (CardDTO linkedCard : linkedCards) {
                        fillStructureFieldValuesRecursive(linkedCard, link.getTargetYield(), structureDefs);
                    }
                }
            }
        }
    }
}
