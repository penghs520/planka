package dev.planka.schema.service.common;

import dev.planka.api.schema.dto.SchemaChangelogDTO;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.schema.changelog.SemanticChange;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.infra.cache.card.CardCacheService;
import dev.planka.infra.cache.card.model.CardBasicInfo;
import dev.planka.schema.mapper.SchemaChangelogMapper;
import dev.planka.schema.model.SchemaChangelogEntity;
import dev.planka.schema.service.common.diff.SchemaDiffService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Schema 变更日志服务
 * <p>
 * 性能优化点：
 * 1. 使用 org_id 字段直接查询，避免 JOIN
 * 2. 关键字搜索使用数据库 LIKE 查询，避免全量加载
 * 3. 合并批量填充逻辑，减少数据库和远程调用次数
 */
@Slf4j
@Service
public class SchemaChangeLogService {

    private final SchemaChangelogMapper changelogMapper;
    private final SchemaDiffService diffService;
    private final SchemaAssembler assembler;
    private final SchemaCommonService schemaCommonService;
    private final SchemaQuery schemaQuery;
    private final CardCacheService cardCacheService;

    public SchemaChangeLogService(SchemaChangelogMapper changelogMapper,
                                  SchemaDiffService diffService,
                                  SchemaAssembler assembler,
                                  SchemaCommonService schemaCommonService,
                                  SchemaQuery schemaQuery, CardCacheService cardCacheService) {
        this.changelogMapper = changelogMapper;
        this.diffService = diffService;
        this.assembler = assembler;
        this.schemaCommonService = schemaCommonService;
        this.schemaQuery = schemaQuery;
        this.cardCacheService = cardCacheService;
    }

    /**
     * 分页获取变更历史（支持关键字搜索）
     */
    public Result<PageResult<SchemaChangelogDTO>> getChangelogWithPaging(String schemaId, int page, int size, String keyword) {
        return getChangelogWithPaging(schemaId, page, size, keyword, false, null);
    }

    /**
     * 分页获取变更历史（支持关键字搜索和附属Schema查询）
     */
    public Result<PageResult<SchemaChangelogDTO>> getChangelogWithPaging(
            String schemaId, int page, int size, String keyword, boolean includeChildren) {
        return getChangelogWithPaging(schemaId, page, size, keyword, includeChildren, null);
    }

    /**
     * 分页获取变更历史（支持关键字搜索、附属Schema查询和操作人筛选）
     */
    public Result<PageResult<SchemaChangelogDTO>> getChangelogWithPaging(
            String schemaId, int page, int size, String keyword, boolean includeChildren, String changedBy) {

        List<String> schemaIds = collectSchemaIds(schemaId, includeChildren);
        int offset = page * size;

        long total;
        List<SchemaChangelogEntity> logs;

        // 使用通用筛选方法处理所有筛选条件组合
        total = changelogMapper.countBySchemaIdsWithFilters(schemaIds, keyword, changedBy);
        if (total == 0) {
            return Result.success(PageResult.empty(page, size));
        }
        logs = changelogMapper.findBySchemaIdsWithFilters(schemaIds, keyword, changedBy, offset, size);

        List<SchemaChangelogDTO> dtos = logs.stream().map(assembler::toChangelogDTO).collect(Collectors.toList());

        // 批量填充所有名称（合并为一次调用）
        batchFillAllNames(dtos);

        return Result.success(PageResult.of(dtos, page, size, total));
    }

    /**
     * 收集需要查询的所有 Schema ID（包括主 Schema 和附属 Schema）
     */
    private List<String> collectSchemaIds(String schemaId, boolean includeChildren) {
        List<String> schemaIds = new ArrayList<>();
        schemaIds.add(schemaId);

        if (includeChildren) {
            List<SchemaDefinition<?>> children = schemaQuery.queryByBelongTo(schemaId, null);
            for (SchemaDefinition<?> child : children) {
                schemaIds.add(child.getId().value());
            }
            log.debug("Found {} children schemas for schemaId={}", children.size(), schemaId);
        }

        return schemaIds;
    }

    /**
     * 按组织ID分页获取全局变更历史（支持关键字搜索、Schema类型筛选和操作人筛选）
     * <p>
     * 优化点：
     * 1. 直接使用 org_id 字段查询，无需 JOIN
     * 2. 关键字搜索使用数据库 LIKE 查询，避免全量加载
     * 3. 合并批量填充逻辑，减少查询次数
     *
     * @param orgId      组织ID
     * @param page       页码（从0开始）
     * @param size       每页数量
     * @param keyword    搜索关键字（可选，匹配变更摘要）
     * @param schemaType Schema类型（可选）
     * @param changedBy  操作人ID（可选）
     */
    public Result<PageResult<SchemaChangelogDTO>> getGlobalChangelogWithPaging(
            String orgId, int page, int size, String keyword, String schemaType, String changedBy) {

        int offset = page * size;

        long total;
        List<SchemaChangelogEntity> logs;

        // 使用通用查询方法处理各种筛选条件组合
        total = changelogMapper.countByFilters(orgId, schemaType, keyword, changedBy);
        if (total == 0) {
            return Result.success(PageResult.empty(page, size));
        }
        logs = changelogMapper.findByFiltersWithPaging(orgId, schemaType, keyword, changedBy, offset, size);

        List<SchemaChangelogDTO> dtos = logs.stream().map(assembler::toChangelogDTO).collect(Collectors.toList());

        // 批量填充所有名称（合并为一次调用）
        batchFillAllNames(dtos);

        return Result.success(PageResult.of(dtos, page, size, total));
    }

    /**
     * 批量填充所有名称（合并操作人、Schema名称、语义变更目标名称的查询）
     * <p>
     * 优化点：将原来的3次独立查询合并，减少数据库和远程调用次数
     */
    private void batchFillAllNames(List<SchemaChangelogDTO> changelogs) {
        if (changelogs == null || changelogs.isEmpty()) {
            return;
        }

        // 1. 收集所有需要查询的 ID
        Set<String> operatorCardIds = new HashSet<>();
        Set<String> schemaIds = new HashSet<>();

        for (SchemaChangelogDTO changelog : changelogs) {
            // 操作人卡片ID
            if (changelog.getChangedBy() != null && !changelog.getChangedBy().isEmpty()) {
                operatorCardIds.add(changelog.getChangedBy());
            }
            // Schema ID
            if (changelog.getSchemaId() != null) {
                schemaIds.add(changelog.getSchemaId());
            }
            // 语义变更中的目标ID
            if (changelog.getChangeDetail() != null && changelog.getChangeDetail().getSemanticChanges() != null) {
                for (SemanticChange semanticChange : changelog.getChangeDetail().getSemanticChanges()) {
                    String targetId = semanticChange.getTargetId();
                    if (targetId != null && targetId.equals(semanticChange.getTargetName())) {
                        schemaIds.add(targetId);
                    }
                }
            }
        }

        // 2. 批量查询操作人名称（远程调用）
        Map<String, String> operatorNameMap = queryOperatorNames(operatorCardIds);

        // 3. 批量查询 Schema 名称（一次数据库查询）
        Map<String, String> schemaNameMap = querySchemaNames(schemaIds);

        // 4. 填充名称到 DTO
        for (SchemaChangelogDTO changelog : changelogs) {
            // 填充操作人名称
            String operatorName = operatorNameMap.get(changelog.getChangedBy());
            if (operatorName != null) {
                changelog.setChangedByName(operatorName);
            }

            // 填充 Schema 名称
            String schemaName = schemaNameMap.get(changelog.getSchemaId());
            if (schemaName != null) {
                changelog.setSchemaName(schemaName);
            }

            // 填充语义变更目标名称并重新生成摘要
            fillSemanticChangeTargetNamesAndUpdateSummary(changelog, schemaNameMap);
        }
    }

    /**
     * 批量查询操作人名称
     */
    private Map<String, String> queryOperatorNames(Set<String> operatorCardIds) {
        Map<String, String> result = new HashMap<>();
        if (operatorCardIds.isEmpty()) {
            return result;
        }

        try {
            Map<CardId, CardBasicInfo> basicInfos = cardCacheService.getBasicInfoByIds(operatorCardIds.stream().map(CardId::of).collect(Collectors.toSet()));
            basicInfos.forEach((id, basicInfo) -> {
                result.put(id.value(), basicInfo.title().getDisplayValue());
            });
        } catch (Exception e) {
            log.error("Error querying operator names", e);
        }

        return result;
    }

    /**
     * 批量查询 Schema 名称
     */
    private Map<String, String> querySchemaNames(Set<String> schemaIds) {
        Map<String, String> result = new HashMap<>();
        if (schemaIds.isEmpty()) {
            return result;
        }

        Result<List<SchemaDefinition<?>>> schemaResult = schemaCommonService.getByIdsWithDeleted(new ArrayList<>(schemaIds));
        if (schemaResult.isSuccess() && schemaResult.getData() != null) {
            for (SchemaDefinition<?> schema : schemaResult.getData()) {
                result.put(schema.getId().value(), schema.getName());
            }
        }

        return result;
    }

    /**
     * 填充语义变更目标名称并更新摘要
     */
    private void fillSemanticChangeTargetNamesAndUpdateSummary(SchemaChangelogDTO changelog, Map<String, String> schemaNameMap) {
        if (changelog.getChangeDetail() == null || changelog.getChangeDetail().getSemanticChanges() == null) {
            return;
        }

        boolean hasUpdate = false;
        for (SemanticChange semanticChange : changelog.getChangeDetail().getSemanticChanges()) {
            String targetId = semanticChange.getTargetId();
            if (targetId != null && schemaNameMap.containsKey(targetId)) {
                semanticChange.setTargetName(schemaNameMap.get(targetId));
                hasUpdate = true;
            }
        }

        // 如果有更新名称，重新生成changeSummary
        if (hasUpdate && "UPDATE".equals(changelog.getAction())) {
            String schemaName = extractSchemaNameFromSnapshot(changelog.getAfterSnapshot());
            String detailText = diffService.generateSummaryText(changelog.getChangeDetail());
            if (detailText != null && !detailText.isEmpty()) {
                changelog.setChangeSummary("更新了 " + schemaName + "：" + detailText);
            }
        }
    }

    /**
     * 从快照中提取Schema名称
     */
    private String extractSchemaNameFromSnapshot(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return "";
        }
        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(snapshot);
            return node.path("name").asText("");
        } catch (Exception e) {
            return "";
        }
    }
}
