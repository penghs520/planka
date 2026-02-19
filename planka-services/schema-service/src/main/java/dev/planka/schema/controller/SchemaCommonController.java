package dev.planka.schema.controller;

import dev.planka.api.schema.dto.SchemaChangelogDTO;
import dev.planka.api.schema.dto.SchemaReferenceSummaryDTO;
import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.schema.service.common.SchemaChangeLogService;
import dev.planka.schema.service.common.SchemaCommonService;
import dev.planka.schema.service.common.SchemaQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Schema REST 控制器
 */
@RestController
@RequestMapping("/api/v1/schemas/common")
@RequiredArgsConstructor
public class SchemaCommonController {

    private final SchemaCommonService schemaCommonService;
    private final SchemaChangeLogService schemaChangeLogService;
    private final SchemaQuery schemaQuery;

    // ==================== 基础 CRUD ====================

    /**
     * 根据ID获取Schema
     */
    @GetMapping("/{schemaId}")
    public Result<SchemaDefinition<?>> getById(@PathVariable("schemaId") String schemaId) {
        return schemaCommonService.getById(schemaId);
    }

    /**
     * 批量获取Schema
     */
    @PostMapping("/batch")
    public Result<List<SchemaDefinition<?>>> getByIds(@RequestBody List<String> schemaIds) {
        return schemaCommonService.getByIds(schemaIds);
    }

    /**
     * 批量获取Schema（包含已删除）
     * <p>
     * 主要用于操作历史等需要显示已删除 Schema 名称的场景
     */
    @PostMapping("/batch-with-deleted")
    public Result<List<SchemaDefinition<?>>> getByIdsWithDeleted(@RequestBody List<String> schemaIds) {
        return schemaCommonService.getByIdsWithDeleted(schemaIds);
    }

    /**
     * 创建Schema
     *
     * @param orgId      组织 ID（从请求头获取）
     * @param operatorId 操作人 ID
     * @param request    创建请求
     */
    @PostMapping
    public Result<SchemaDefinition<?>> create(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @Valid @RequestBody CreateSchemaRequest request) {
        return schemaCommonService.create(orgId, operatorId, request);
    }

    /**
     * 更新Schema
     *
     * @param schemaId   Schema ID
     * @param operatorId 操作人 ID
     * @param request    更新请求
     */
    @PutMapping("/{schemaId}")
    public Result<SchemaDefinition<?>> update(
            @PathVariable("schemaId") String schemaId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @Valid @RequestBody UpdateSchemaRequest request) {
        return schemaCommonService.update(schemaId, operatorId, request);
    }

    /**
     * 删除Schema
     *
     * @param schemaId   Schema ID
     * @param operatorId 操作人 ID
     */
    @DeleteMapping("/{schemaId}")
    public Result<Void> delete(
            @PathVariable("schemaId") String schemaId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return schemaCommonService.delete(schemaId, operatorId);
    }

    // ==================== 状态变更 ====================

    /**
     * 启用Schema
     */
    @PostMapping("/{schemaId}/activate")
    public Result<Void> activate(
            @PathVariable("schemaId") String schemaId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return schemaCommonService.activate(schemaId, operatorId);
    }

    /**
     * 停用Schema
     */
    @PostMapping("/{schemaId}/disable")
    public Result<Void> disable(
            @PathVariable("schemaId") String schemaId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return schemaCommonService.disable(schemaId, operatorId);
    }

    // ==================== 查询接口 ====================

    /**
     * 按组织和类型分页查询
     *
     * @param orgId 组织ID（从请求头获取）
     * @param type  Schema 类型
     * @param page  页码（从1开始）
     * @param size  每页数量
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<SchemaDefinition<?>>> listByOrgAndType(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam("type") SchemaType type,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return schemaCommonService.listByOrgAndType(orgId, type, page, size);
    }

    /**
     * 按二级键查询
     *
     * @param secondKey     索引值
     * @param secondKeyType 索引类型（必填）
     * @param type          目标 Schema 类型（可选）
     */
    @GetMapping("/by-second-key/{secondKey}")
    public Result<List<SchemaDefinition<?>>> getBySecondKey(
            @PathVariable("secondKey") String secondKey,
            @RequestParam("secondKeyType") SchemaType secondKeyType,
            @RequestParam(value = "type", required = false) SchemaType type) {
        return Result.success(schemaQuery.queryBySecondKey(secondKey, secondKeyType, type));
    }

    /**
     * 按所属 Schema 查询
     *
     * @param belongTo      所属 Schema ID
     * @param schemaSubType Schema 子类型（可选，用于类型过滤，如 "CARD_PERMISSION"）
     */
    @GetMapping("/by-belong-to/{belongTo}")
    public Result<List<SchemaDefinition<?>>> getByBelongTo(
            @PathVariable("belongTo") String belongTo,
            @RequestParam(value = "type", required = false) String schemaSubType) {
        return Result.success(schemaQuery.queryByBelongTo(belongTo, schemaSubType));
    }

    // ==================== 引用关系 ====================

    /**
     * 获取 Schema 的引用摘要
     */
    @GetMapping("/{schemaId}/reference-summary")
    public Result<SchemaReferenceSummaryDTO> getReferenceSummary(
            @PathVariable("schemaId") String schemaId) {
        return schemaCommonService.getReferenceSummary(schemaId);
    }

    // ==================== 变更历史 ====================

    /**
     * 获取全局变更历史（分页）
     * <p>
     * 查询组织下所有 Schema 的变更日志
     *
     * @param orgId      组织ID（从请求头获取）
     * @param page       页码（从0开始）
     * @param size       每页数量
     * @param keyword    搜索关键字（可选，匹配变更摘要、Schema名称）
     * @param schemaType Schema类型（可选，用于筛选特定类型的变更日志）
     * @param changedBy  操作人ID（可选，用于筛选特定操作人的变更日志）
     */
    @GetMapping("/changelog")
    public Result<PageResult<SchemaChangelogDTO>> getGlobalChangelog(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "schemaType", required = false) String schemaType,
            @RequestParam(name = "changedBy", required = false) String changedBy) {
        return schemaChangeLogService.getGlobalChangelogWithPaging(orgId, page, size, keyword, schemaType, changedBy);
    }

    /**
     * 获取变更历史（分页）
     *
     * @param schemaId        Schema ID
     * @param page            页码（从0开始）
     * @param size            每页数量
     * @param keyword         搜索关键字（可选，匹配变更摘要）
     * @param includeChildren 是否包含附属Schema的变更日志（可选，默认false）
     * @param changedBy       操作人ID（可选，用于筛选特定操作人的变更日志）
     */
    @GetMapping("/{schemaId}/changelog")
    public Result<PageResult<SchemaChangelogDTO>> getChangelog(
            @PathVariable("schemaId") String schemaId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "includeChildren", defaultValue = "false") boolean includeChildren,
            @RequestParam(name = "changedBy", required = false) String changedBy) {
        return schemaChangeLogService.getChangelogWithPaging(schemaId, page, size, keyword, includeChildren, changedBy);
    }

    /**
     * 还原至指定版本
     *
     * @param schemaId    Schema ID
     * @param changelogId 变更日志 ID
     * @param operatorId  操作人 ID
     */
    @PostMapping("/{schemaId}/changelog/{changelogId}/restore")
    public Result<SchemaDefinition<?>> restoreToVersion(
            @PathVariable("schemaId") String schemaId,
            @PathVariable("changelogId") Long changelogId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {
        return schemaCommonService.restoreToVersion(schemaId, changelogId, operatorId);
    }
}
