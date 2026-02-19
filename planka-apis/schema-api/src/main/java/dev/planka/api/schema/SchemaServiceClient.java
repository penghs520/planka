package dev.planka.api.schema;

import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.definition.SchemaDefinition;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Schema 服务契约
 * <p>
 * 定义 Schema 服务的 API 接口，同时作为 Feign 客户端供服务消费方使用。
 */
@FeignClient(name = "schema-service", contextId = "schemaServiceClient")
public interface SchemaServiceClient {

    // ==================== 基础 CRUD ====================

    /**
     * 根据ID获取 Schema
     *
     * @param schemaId Schema ID
     * @return Schema 定义
     */
    @GetMapping("/api/v1/schemas/common/{schemaId}")
    Result<SchemaDefinition<?>> getById(@PathVariable("schemaId") String schemaId);

    /**
     * 根据ID列表批量获取 Schema
     *
     * @param schemaIds Schema ID 列表
     * @return Schema 定义列表
     */
    @PostMapping("/api/v1/schemas/common/batch")
    Result<List<SchemaDefinition<?>>> getByIds(@RequestBody List<String> schemaIds);

    /**
     * 根据ID列表批量获取 Schema（包含已删除）
     * <p>
     * 主要用于操作历史等需要显示已删除 Schema 名称的场景
     *
     * @param schemaIds Schema ID 列表
     * @return Schema 定义列表（包含已删除的 Schema）
     */
    @PostMapping("/api/v1/schemas/common/batch-with-deleted")
    Result<List<SchemaDefinition<?>>> getByIdsWithDeleted(@RequestBody List<String> schemaIds);

    /**
     * 创建 Schema
     *
     * @param orgId      组织ID
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request    创建请求
     * @return 创建后的 Schema 定义
     */
    @PostMapping("/api/v1/schemas/common")
    Result<SchemaDefinition<?>> create(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody CreateSchemaRequest request);

    /**
     * 更新 Schema
     *
     * @param schemaId   Schema ID
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request    更新请求
     * @return 更新后的 Schema 定义
     */
    @PutMapping("/api/v1/schemas/common/{schemaId}")
    Result<SchemaDefinition<?>> update(
            @PathVariable("schemaId") String schemaId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody UpdateSchemaRequest request);

    /**
     * 删除 Schema（软删除）
     *
     * @param schemaId   Schema ID
     * @param operatorId 操作人ID（成员卡片ID）
     * @return 操作结果
     */
    @DeleteMapping("/api/v1/schemas/common/{schemaId}")
    Result<Void> delete(
            @PathVariable("schemaId") String schemaId,
            @RequestHeader("X-Member-Card-Id") String operatorId);

    // ==================== 状态变更 ====================

    /**
     * 启用 Schema
     *
     * @param schemaId   Schema ID
     * @param operatorId 操作人ID（成员卡片ID）
     * @return 操作结果
     */
    @PostMapping("/api/v1/schemas/common/{schemaId}/activate")
    Result<Void> activate(
            @PathVariable("schemaId") String schemaId,
            @RequestHeader("X-Member-Card-Id") String operatorId);

    /**
     * 停用 Schema
     *
     * @param schemaId   Schema ID
     * @param operatorId 操作人ID（成员卡片ID）
     * @return 操作结果
     */
    @PostMapping("/api/v1/schemas/common/{schemaId}/disable")
    Result<Void> disable(
            @PathVariable("schemaId") String schemaId,
            @RequestHeader("X-Member-Card-Id") String operatorId);

    // ==================== 按 belongTo 查询 ====================

    /**
     * 根据 belongTo 查询 Schema 列表
     * <p>
     * 用于查询隶属于某个 Schema 的所有定义，如查询某个卡片类型的权限配置。
     *
     * @param belongToId    所属 Schema ID（如 cardTypeId）
     * @param schemaSubType Schema 子类型（如 "CARD_PERMISSION"）
     * @return Schema 定义列表
     */
    @GetMapping("/api/v1/schemas/common/by-belong-to")
    Result<List<SchemaDefinition<?>>> getByBelongTo(
            @RequestParam("belongToId") String belongToId,
            @RequestParam(value = "schemaSubType", required = false) String schemaSubType);

}
