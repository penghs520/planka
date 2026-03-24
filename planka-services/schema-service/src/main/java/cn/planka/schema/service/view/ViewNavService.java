package cn.planka.schema.service.view;

import cn.planka.api.schema.vo.view.ViewListItemVO;
import cn.planka.domain.schema.SchemaType;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.view.ListViewDefinition;
import cn.planka.domain.schema.definition.view.ViewVisibilityScope;
import cn.planka.schema.repository.SchemaRepository;
import cn.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 当前用户可见的视图导航列表（工作区 / 架构节点）。
 */
@Service
@RequiredArgsConstructor
public class ViewNavService {

    private final SchemaQuery schemaQuery;
    private final SchemaRepository schemaRepository;
    private final ViewNavVisibilityChecker viewNavVisibilityChecker;

    public List<ViewListItemVO> listNav(String orgId, String operatorMemberCardId, String cascadeRelationNodeId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.VIEW);
        Map<String, String> cardTypeNames = getCardTypeNames(schemas);
        return schemas.stream()
                .filter(s -> s instanceof ListViewDefinition)
                .map(s -> (ListViewDefinition) s)
                .filter(v -> isNavVisible(v, orgId, operatorMemberCardId, cascadeRelationNodeId))
                .map(v -> toNavVo(v, cardTypeNames))
                .collect(Collectors.toList());
    }

    private boolean isNavVisible(ListViewDefinition view, String orgId, String operatorMemberCardId, String cascadeRelationNodeId) {
        return viewNavVisibilityChecker.isVisibleForNav(view, orgId, operatorMemberCardId, cascadeRelationNodeId);
    }

    private Map<String, String> getCardTypeNames(List<SchemaDefinition<?>> schemas) {
        List<String> cardTypeIds = schemas.stream()
                .filter(s -> s instanceof ListViewDefinition)
                .map(s -> ((ListViewDefinition) s).getCardTypeId())
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .distinct()
                .collect(Collectors.toList());
        if (cardTypeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SchemaDefinition<?>> cardTypes = schemaRepository.findByIds(cardTypeIds.stream().collect(Collectors.toSet()));
        return cardTypes.stream()
                .collect(Collectors.toMap(s -> s.getId().value(), SchemaDefinition::getName, (a, b) -> a));
    }

    private ViewListItemVO toNavVo(ListViewDefinition view, Map<String, String> cardTypeNames) {
        String cardTypeId = view.getCardTypeId() != null ? view.getCardTypeId().value() : null;
        ViewVisibilityScope scope = view.getEffectiveViewVisibilityScope();
        return ViewListItemVO.builder()
                .id(view.getId().value())
                .orgId(view.getOrgId())
                .name(view.getName())
                .description(view.getDescription())
                .viewType(view.getViewType())
                .schemaSubType(view.getSchemaSubType())
                .cardTypeId(cardTypeId)
                .cardTypeName(cardTypeId != null ? cardTypeNames.get(cardTypeId) : null)
                .columnCount(view.getColumnConfigs() != null ? view.getColumnConfigs().size() : 0)
                .defaultView(view.isDefaultView())
                .shared(view.isShared())
                .viewVisibilityScope(scope != null ? scope.name() : null)
                .visibleTeamCardIds(view.getVisibleTeamCardIds())
                .visibleCascadeRelationNodeIds(view.getVisibleCascadeRelationNodeIds())
                .createdBy(view.getCreatedBy())
                .enabled(view.isEnabled())
                .contentVersion(view.getContentVersion())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .build();
    }
}
