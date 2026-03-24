package cn.planka.card.service.cascadefield;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.api.card.dto.CascadeNodeDTO;
import cn.planka.card.repository.CardRepository;
import cn.planka.common.result.Result;
import cn.planka.domain.card.CardCycle;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.link.LinkFieldId;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.fieldconfig.CascadeFieldConfig;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationDefinition;
import cn.planka.domain.schema.definition.cascaderelation.CascadeRelationLevel;
import cn.planka.infra.cache.schema.SchemaCacheService;
import cn.planka.api.card.request.CardQueryRequest;
import cn.planka.api.card.request.QueryContext;
import cn.planka.api.card.request.QueryScope;
import cn.planka.api.card.request.CascadeFieldOptionsRequest;
import cn.planka.api.card.request.Yield;
import cn.planka.api.card.request.YieldField;
import cn.planka.api.card.request.YieldLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 架构属性可选项服务
 * <p>
 * 提供架构属性编辑器所需的树形可选项数据
 */
@Service
public class CascadeFieldOptionsService {

    private static final Logger logger = LoggerFactory.getLogger(CascadeFieldOptionsService.class);

    private final SchemaCacheService schemaCacheService;
    private final CardRepository cardRepository;

    public CascadeFieldOptionsService(SchemaCacheService schemaCacheService,
                                   CardRepository cardRepository) {
        this.schemaCacheService = schemaCacheService;
        this.cardRepository = cardRepository;
    }

    /**
     * 查询架构属性的树形可选项
     * <p>
     * 根据架构属性定义，查询整条架构线的所有节点（卡片），
     * 构建树形结构供前端编辑器展示。搜索过滤由前端完成。
     *
     * @param request    查询请求
     * @param orgId      组织ID
     * @param operatorId 操作人ID
     * @return 树形节点列表（根节点列表）
     */
    public Result<List<CascadeNodeDTO>> queryOptions(CascadeFieldOptionsRequest request,
                                                        String orgId, String operatorId) {
        if (request.getCascadeRelationId() != null && !request.getCascadeRelationId().isBlank()) {
            return queryTreeByCascadeRelationDefinitionId(request.getCascadeRelationId().trim(), orgId, operatorId);
        }

        String cascadeFieldId = request.getCascadeFieldId().trim();

        // 1. 获取架构属性定义
        Optional<SchemaDefinition<?>> defOpt = schemaCacheService.getById(cascadeFieldId);
        if (defOpt.isEmpty() || !(defOpt.get() instanceof CascadeFieldConfig cascadeFieldDef)) {
            return Result.failure("CASCADE_FIELD_NOT_FOUND", "架构属性定义不存在: " + cascadeFieldId);
        }

        // 2. 获取级联关系定义
        String sid = cascadeFieldDef.getCascadeRelationId().value();
        Optional<SchemaDefinition<?>> cascadeRelationDefOpt = schemaCacheService.getById(sid);
        if (cascadeRelationDefOpt.isEmpty() || !(cascadeRelationDefOpt.get() instanceof CascadeRelationDefinition cascadeRelationDef)) {
            return Result.failure("CASCADE_RELATION_NOT_FOUND", "级联关系定义不存在: " + sid);
        }

        return buildCascadeRelationOptionTree(cascadeRelationDef, orgId, operatorId);
    }

    private Result<List<CascadeNodeDTO>> queryTreeByCascadeRelationDefinitionId(String cascadeRelationDefinitionId,
                                                                              String orgId,
                                                                              String operatorId) {
        Optional<SchemaDefinition<?>> cascadeRelationDefOpt = schemaCacheService.getById(cascadeRelationDefinitionId);
        if (cascadeRelationDefOpt.isEmpty() || !(cascadeRelationDefOpt.get() instanceof CascadeRelationDefinition cascadeRelationDef)) {
            return Result.failure("CASCADE_RELATION_NOT_FOUND", "级联关系定义不存在: " + cascadeRelationDefinitionId);
        }
        if (!orgId.equals(cascadeRelationDef.getOrgId())) {
            return Result.failure("CASCADE_RELATION_ORG_MISMATCH", "级联关系定义不属于当前组织");
        }
        return buildCascadeRelationOptionTree(cascadeRelationDef, orgId, operatorId);
    }

    private Result<List<CascadeNodeDTO>> buildCascadeRelationOptionTree(CascadeRelationDefinition cascadeRelationDef,
                                                                 String orgId, String operatorId) {
        List<CascadeRelationLevel> levels = cascadeRelationDef.getLevels();
        if (levels == null || levels.isEmpty()) {
            return Result.success(List.of());
        }
        Map<Integer, List<CardDTO>> levelCards = queryAllLevelCards(levels, orgId, operatorId);
        List<CascadeNodeDTO> tree = buildTree(levels, levelCards);
        return Result.success(tree);
    }

    /**
     * 查询所有层级的卡片
     * <p>
     * 非根层级需要查询父关联关系，用于构建树形结构
     */
    private Map<Integer, List<CardDTO>> queryAllLevelCards(List<CascadeRelationLevel> levels,
                                                            String orgId, String operatorId) {
        Map<Integer, List<CardDTO>> result = new HashMap<>();

        for (CascadeRelationLevel level : levels) {
            List<String> cardTypeIds = List.of(level.cardTypeId().value());

            // 非根层级需要查询父关联
            String parentLinkFieldId = level.parentLinkFieldId() != null
                    ? level.parentLinkFieldId().value() : null;

            CardQueryRequest queryRequest = buildCardQueryRequest(
                    cardTypeIds, parentLinkFieldId, orgId, operatorId);
            List<CardDTO> cards = cardRepository.query(queryRequest);
            result.put(level.index(), cards);
        }

        return result;
    }

    /**
     * 构建卡片查询请求
     */
    private CardQueryRequest buildCardQueryRequest(List<String> cardTypeIds,
                                                    String parentLinkFieldId,
                                                    String orgId, String operatorId) {
        CardQueryRequest request = new CardQueryRequest();

        // 查询上下文
        QueryContext context = new QueryContext();
        context.setOrgId(orgId);
        context.setOperatorId(operatorId);
        request.setQueryContext(context);

        // 查询范围
        QueryScope scope = new QueryScope();
        scope.setCardTypeIds(cardTypeIds);
        scope.setCardCycles(List.of(CardCycle.ACTIVE));
        request.setQueryScope(scope);

        // 返回字段
        Yield yield = new Yield();
        YieldField field = new YieldField();
        field.setAllFields(false);
        yield.setField(field);

        // 如果有父关联，需要查询关联卡片
        if (parentLinkFieldId != null) {
            YieldLink link = new YieldLink();
            link.setLinkFieldId(parentLinkFieldId);
            yield.setLinks(List.of(link));
        }

        request.setYield(yield);

        return request;
    }

    /**
     * 构建树形结构
     * <p>
     * 通过父子关联关系，将平铺的卡片列表构建为树形结构
     */
    private List<CascadeNodeDTO> buildTree(List<CascadeRelationLevel> levels,
                                              Map<Integer, List<CardDTO>> levelCards) {
        if (levels.isEmpty()) {
            return List.of();
        }

        int maxLevel = levels.size() - 1;

        // 按层级从小到大排序
        List<CascadeRelationLevel> sortedLevels = levels.stream()
                .sorted(Comparator.comparingInt(CascadeRelationLevel::index))
                .toList();

        // 构建节点映射 (cardId -> node)
        Map<String, CascadeNodeDTO> nodeMap = new HashMap<>();

        // 先创建所有节点
        for (CascadeRelationLevel level : sortedLevels) {
            List<CardDTO> cards = levelCards.getOrDefault(level.index(), List.of());
            boolean isLeaf = level.index() == maxLevel;

            for (CardDTO card : cards) {
                CascadeNodeDTO node = CascadeNodeDTO.builder()
                        .id(card.getId().asStr())
                        .name(card.getTitle() != null ? card.getTitle().getDisplayValue() : "")
                        .levelIndex(level.index())
                        .levelName(level.name())
                        .leaf(isLeaf)
                        .children(isLeaf ? null : new ArrayList<>())
                        .build();
                nodeMap.put(card.getId().asStr(), node);
            }
        }

        // 构建父子关系
        for (int i = 1; i < sortedLevels.size(); i++) {
            CascadeRelationLevel level = sortedLevels.get(i);
            LinkFieldId parentLinkFieldId = level.parentLinkFieldId();
            if (parentLinkFieldId == null) {
                continue;
            }

            List<CardDTO> cards = levelCards.getOrDefault(level.index(), List.of());
            for (CardDTO card : cards) {
                String parentId = findParentCardId(card, parentLinkFieldId.value());
                if (parentId != null) {
                    CascadeNodeDTO parentNode = nodeMap.get(parentId);
                    CascadeNodeDTO childNode = nodeMap.get(card.getId().asStr());
                    if (parentNode != null && childNode != null && parentNode.getChildren() != null) {
                        parentNode.getChildren().add(childNode);
                    }
                }
            }
        }

        // 返回根节点列表
        List<CardDTO> rootCards = levelCards.getOrDefault(0, List.of());
        return rootCards.stream()
                .map(card -> nodeMap.get(card.getId().asStr()))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 查找父卡片ID
     */
    private String findParentCardId(CardDTO card, String parentLinkFieldId) {
        if (card.getLinkedCards() == null) {
            return null;
        }
        Set<CardDTO> linkedCards = card.getLinkedCards().get(parentLinkFieldId);
        if (linkedCards == null || linkedCards.isEmpty()) {
            return null;
        }
        // 取第一个（架构关系应该是单选的）
        return linkedCards.iterator().next().getId().asStr();
    }
}
