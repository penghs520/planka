package dev.planka.card.service.structure;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.dto.StructureNodeDTO;
import dev.planka.card.repository.CardRepository;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardStyle;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.fieldconfig.StructureFieldConfig;
import dev.planka.domain.schema.definition.structure.StructureDefinition;
import dev.planka.domain.schema.definition.structure.StructureLevel;
import dev.planka.infra.cache.schema.SchemaCacheService;
import dev.planka.api.card.request.*;
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
public class StructureOptionsService {

    private static final Logger logger = LoggerFactory.getLogger(StructureOptionsService.class);

    private final SchemaCacheService schemaCacheService;
    private final CardRepository cardRepository;

    public StructureOptionsService(SchemaCacheService schemaCacheService,
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
    public Result<List<StructureNodeDTO>> queryOptions(StructureOptionsRequest request,
                                                       String orgId, String operatorId) {
        String structureFieldId = request.getStructureFieldId();

        // 1. 获取架构属性定义
        Optional<SchemaDefinition<?>> defOpt = schemaCacheService.getById(structureFieldId);
        if (defOpt.isEmpty() || !(defOpt.get() instanceof StructureFieldConfig structureFieldDef)) {
            return Result.failure("STRUCTURE_FIELD_NOT_FOUND", "架构属性定义不存在: " + structureFieldId);
        }

        // 2. 获取架构线定义
        String structureId = structureFieldDef.getStructureId().value();
        Optional<SchemaDefinition<?>> structureDefOpt = schemaCacheService.getById(structureId);
        if (structureDefOpt.isEmpty() || !(structureDefOpt.get() instanceof StructureDefinition structureDef)) {
            return Result.failure("STRUCTURE_NOT_FOUND", "架构线定义不存在: " + structureId);
        }

        List<StructureLevel> levels = structureDef.getLevels();
        if (levels == null || levels.isEmpty()) {
            return Result.success(List.of());
        }

        // 3. 查询所有层级的卡片
        Map<Integer, List<CardDTO>> levelCards = queryAllLevelCards(levels, orgId, operatorId);

        // 4. 构建树形结构
        List<StructureNodeDTO> tree = buildTree(levels, levelCards);

        return Result.success(tree);
    }

    /**
     * 查询所有层级的卡片
     * <p>
     * 非根层级需要查询父关联关系，用于构建树形结构
     */
    private Map<Integer, List<CardDTO>> queryAllLevelCards(List<StructureLevel> levels,
                                                            String orgId, String operatorId) {
        Map<Integer, List<CardDTO>> result = new HashMap<>();

        for (StructureLevel level : levels) {
            List<String> cardTypeIds = level.cardTypeIds().stream()
                    .map(CardTypeId::value)
                    .toList();

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
        scope.setCardStyles(List.of(CardStyle.ACTIVE));
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
    private List<StructureNodeDTO> buildTree(List<StructureLevel> levels,
                                              Map<Integer, List<CardDTO>> levelCards) {
        if (levels.isEmpty()) {
            return List.of();
        }

        int maxLevel = levels.size() - 1;

        // 按层级从小到大排序
        List<StructureLevel> sortedLevels = levels.stream()
                .sorted(Comparator.comparingInt(StructureLevel::index))
                .toList();

        // 构建节点映射 (cardId -> node)
        Map<String, StructureNodeDTO> nodeMap = new HashMap<>();

        // 先创建所有节点
        for (StructureLevel level : sortedLevels) {
            List<CardDTO> cards = levelCards.getOrDefault(level.index(), List.of());
            boolean isLeaf = level.index() == maxLevel;

            for (CardDTO card : cards) {
                StructureNodeDTO node = StructureNodeDTO.builder()
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
            StructureLevel level = sortedLevels.get(i);
            LinkFieldId parentLinkFieldId = level.parentLinkFieldId();
            if (parentLinkFieldId == null) {
                continue;
            }

            List<CardDTO> cards = levelCards.getOrDefault(level.index(), List.of());
            for (CardDTO card : cards) {
                String parentId = findParentCardId(card, parentLinkFieldId.value());
                if (parentId != null) {
                    StructureNodeDTO parentNode = nodeMap.get(parentId);
                    StructureNodeDTO childNode = nodeMap.get(card.getId().asStr());
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
