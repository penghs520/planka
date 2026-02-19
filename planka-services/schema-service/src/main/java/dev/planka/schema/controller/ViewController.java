package dev.planka.schema.controller;

import dev.planka.api.schema.vo.view.ViewListItemVO;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.view.ListViewDefinition;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 视图 REST 控制器
 * <p>
 * 提供视图配置的列表查询和辅助接口。
 * 基础 CRUD 操作委托给 SchemaController 处理。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/schemas/views")
@RequiredArgsConstructor
public class ViewController {

    private final SchemaRepository schemaRepository;
    private final SchemaQuery schemaQuery;

    /**
     * 查询视图列表（简化版）
     *
     * @param orgId 组织 ID（从请求头获取）
     * @return 视图列表
     */
    @GetMapping
    public Result<List<ViewListItemVO>> list(@RequestHeader("X-Org-Id") String orgId) {
        List<SchemaDefinition<?>> schemas = schemaQuery.query(orgId, SchemaType.VIEW);

        // 收集所有卡片类型ID用于批量查询名称
        Map<String, String> cardTypeNames = getCardTypeNames(schemas);

        List<ViewListItemVO> voList = schemas.stream()
                .filter(s -> s instanceof ListViewDefinition)
                .map(s -> toVO((ListViewDefinition) s, cardTypeNames))
                .collect(Collectors.toList());

        return Result.success(voList);
    }

    /**
     * 批量获取卡片类型名称映射
     */
    private Map<String, String> getCardTypeNames(List<SchemaDefinition<?>> schemas) {
        // 收集所有cardTypeId
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

        // 批量查询卡片类型
        List<SchemaDefinition<?>> cardTypes = schemaRepository.findByIds(cardTypeIds.stream().collect(Collectors.toSet()));
        return cardTypes.stream()
                .collect(Collectors.toMap(
                        s -> s.getId().value(),
                        SchemaDefinition::getName,
                        (a, b) -> a
                ));
    }

    /**
     * 将 ListViewDefinition 转换为 VO
     */
    private ViewListItemVO toVO(ListViewDefinition view, Map<String, String> cardTypeNames) {
        String cardTypeId = view.getCardTypeId() != null ? view.getCardTypeId().value() : null;

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
                .enabled(view.isEnabled())
                .contentVersion(view.getContentVersion())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .build();
    }
}
