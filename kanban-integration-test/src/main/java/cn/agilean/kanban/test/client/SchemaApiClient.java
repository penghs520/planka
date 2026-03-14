package cn.agilean.kanban.test.client;

import cn.agilean.kanban.api.schema.dto.SchemaChangelogDTO;
import cn.agilean.kanban.api.schema.dto.SchemaReferenceSummaryDTO;
import cn.agilean.kanban.api.schema.request.CreateSchemaRequest;
import cn.agilean.kanban.api.schema.request.UpdateSchemaRequest;
import cn.agilean.kanban.common.result.PageResult;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.schema.definition.SchemaDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Schema API 客户端
 * <p>
 * 使用 RestTemplate 调用 Gateway-service 的 Schema 接口
 */
@Component
public class SchemaApiClient {

    private static final Logger log = LoggerFactory.getLogger(SchemaApiClient.class);

    private final RestTemplate restTemplate;
    private final String gatewayBaseUrl;

    public SchemaApiClient(RestTemplate restTemplate,
                           @Value("${gateway.base-url}") String gatewayBaseUrl) {
        this.restTemplate = restTemplate;
        this.gatewayBaseUrl = gatewayBaseUrl;
    }

    private <T> T logAndReturn(String method, String url, Result<T> result) {
        log.info(">>> {} {} => success={}, code={}, message={}",
                method, url, result.isSuccess(), result.getCode(), result.getMessage());
        if (!result.isSuccess()) {
            log.warn("    Response data: {}", result.getData());
        }
        return (T) result;
    }

    // ==================== 基础 CRUD ====================

    /**
     * 根据ID获取 Schema
     */
    public Result<SchemaDefinition> getById(String schemaId) {
        String url = gatewayBaseUrl + "/api/v1/schemas/{schemaId}";
        ResponseEntity<Result<SchemaDefinition>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                schemaId
        );
        return response.getBody();
    }

    /**
     * 根据ID列表批量获取 Schema
     */
    public Result<List<SchemaDefinition>> getByIds(List<String> schemaIds) {
        String url = gatewayBaseUrl + "/api/v1/schemas/batch";
        HttpEntity<List<String>> request = new HttpEntity<>(schemaIds, createJsonHeaders());
        ResponseEntity<Result<List<SchemaDefinition>>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    /**
     * 创建 Schema
     */
    public Result<SchemaDefinition> create(String orgId, CreateSchemaRequest createRequest) {
        String url = gatewayBaseUrl + "/api/v1/schemas";
        HttpHeaders headers = createJsonHeaders();
        headers.set("X-Org-Id", orgId);
        HttpEntity<CreateSchemaRequest> request = new HttpEntity<>(createRequest, headers);
        log.info(">>> POST {} with orgId={}, definition={}", url, orgId,
                createRequest.getDefinition() != null ? createRequest.getDefinition().getClass().getSimpleName() : "null");
        ResponseEntity<Result<SchemaDefinition>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {}
        );
        Result<SchemaDefinition> result = response.getBody();
        log.info("<<< POST {} => success={}, code={}, message={}",
                url, result.isSuccess(), result.getCode(), result.getMessage());
        if (!result.isSuccess()) {
            log.warn("    Error response: {}", result);
        }
        return result;
    }

    /**
     * 更新 Schema
     */
    public Result<SchemaDefinition> update(String schemaId, UpdateSchemaRequest updateRequest) {
        String url = gatewayBaseUrl + "/api/v1/schemas/{schemaId}";
        HttpEntity<UpdateSchemaRequest> request = new HttpEntity<>(updateRequest, createJsonHeaders());
        ResponseEntity<Result<SchemaDefinition>> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                request,
                new ParameterizedTypeReference<>() {},
                schemaId
        );
        return response.getBody();
    }

    /**
     * 删除 Schema（软删除）
     */
    public Result<Void> delete(String schemaId) {
        String url = gatewayBaseUrl + "/api/v1/schemas/{schemaId}";
        ResponseEntity<Result<Void>> response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<>() {},
                schemaId
        );
        return response.getBody();
    }

    // ==================== 状态变更 ====================

    /**
     * 启用 Schema
     */
    public Result<Void> activate(String schemaId) {
        String url = gatewayBaseUrl + "/api/v1/schemas/{schemaId}/activate";
        ResponseEntity<Result<Void>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(createJsonHeaders()),
                new ParameterizedTypeReference<>() {},
                schemaId
        );
        return response.getBody();
    }

    /**
     * 停用 Schema
     */
    public Result<Void> disable(String schemaId) {
        String url = gatewayBaseUrl + "/api/v1/schemas/{schemaId}/disable";
        ResponseEntity<Result<Void>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(createJsonHeaders()),
                new ParameterizedTypeReference<>() {},
                schemaId
        );
        return response.getBody();
    }

    // ==================== 按条件查询 ====================

    /**
     * 按组织ID和类型分页查询 Schema 列表
     */
    public Result<PageResult<SchemaDefinition>> listByOrgAndType(String orgId, String type, int page, int size) {
        String url = UriComponentsBuilder.fromHttpUrl(gatewayBaseUrl + "/api/v1/schemas")
                .queryParam("type", type)
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();
        HttpHeaders headers = createJsonHeaders();
        headers.set("X-Org-Id", orgId);
        HttpEntity<?> request = new HttpEntity<>(headers);
        ResponseEntity<Result<PageResult<SchemaDefinition>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    /**
     * 按业务二级键查询 Schema 列表
     */
    public Result<List<SchemaDefinition>> getBySecondKey(String secondKey, String type) {
        String url = UriComponentsBuilder.fromHttpUrl(gatewayBaseUrl + "/api/v1/schemas/by-second-key/{secondKey}")
                .queryParam("type", type)
                .buildAndExpand(secondKey)
                .toUriString();
        ResponseEntity<Result<List<SchemaDefinition>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    /**
     * 按所属 Schema ID 查询
     */
    public Result<List<SchemaDefinition>> getByBelongTo(String belongTo) {
        String url = gatewayBaseUrl + "/api/v1/schemas/by-belong-to/{belongTo}";
        ResponseEntity<Result<List<SchemaDefinition>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                belongTo
        );
        return response.getBody();
    }

    // ==================== 引用关系 ====================

    /**
     * 获取 Schema 的引用摘要
     */
    public Result<SchemaReferenceSummaryDTO> getReferenceSummary(String schemaId) {
        String url = gatewayBaseUrl + "/api/v1/schemas/{schemaId}/reference-summary";
        ResponseEntity<Result<SchemaReferenceSummaryDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                schemaId
        );
        return response.getBody();
    }

    // ==================== 变更历史 ====================

    /**
     * 获取 Schema 的变更历史
     */
    public Result<List<SchemaChangelogDTO>> getChangelog(String schemaId, int limit) {
        String url = UriComponentsBuilder.fromHttpUrl(gatewayBaseUrl + "/api/v1/schemas/{schemaId}/changelog")
                .queryParam("limit", limit)
                .buildAndExpand(schemaId)
                .toUriString();
        ResponseEntity<Result<List<SchemaChangelogDTO>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody();
    }

    // ==================== 辅助方法 ====================

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
