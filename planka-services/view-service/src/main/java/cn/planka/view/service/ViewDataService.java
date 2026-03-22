package cn.planka.view.service;

import cn.planka.api.view.request.ViewDataRequest;
import cn.planka.api.view.response.GroupedCardData;
import cn.planka.api.view.response.ViewDataResponse;
import cn.planka.common.exception.CommonErrorCode;
import cn.planka.common.result.Result;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.view.AbstractViewDefinition;
import cn.planka.infra.cache.schema.SchemaCacheService;
import cn.planka.view.executor.ViewExecutor;
import cn.planka.view.executor.ViewExecutorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 视图数据服务
 * <p>
 * 负责协调视图定义获取和数据查询，是 view-service 的核心服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewDataService {

    private final SchemaCacheService schemaCacheService;
    private final ViewExecutorRegistry executorRegistry;
    private final ViewDataVisibilityService viewDataVisibilityService;

    /**
     * 根据视图ID查询数据
     *
     * @param viewId     视图ID
     * @param request    查询请求
     * @param operatorId 操作人ID
     * @return 视图数据响应
     */
    public Result<ViewDataResponse> queryByViewId(String viewId,
                                                   ViewDataRequest request,
                                                   String operatorId,
                                                   String orgId,
                                                   String structureNodeId) {
        try {
            // 1. 获取视图定义
            AbstractViewDefinition viewDef = getViewDefinition(viewId);
            if (orgId != null && !orgId.equals(viewDef.getOrgId())) {
                return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "组织与视图不匹配");
            }
            if (!viewDataVisibilityService.canQuery(viewDef, operatorId, structureNodeId)) {
                return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "无权访问该视图");
            }

            // 2. 执行查询
            return executeQuery(viewDef, request, operatorId);
        } catch (IllegalArgumentException e) {
            log.warn("视图查询参数错误: {}", e.getMessage());
            return Result.failure("INVALID_ARGUMENT", e.getMessage());
        } catch (Exception e) {
            log.error("视图查询失败, viewId={}", viewId, e);
            return Result.failure("QUERY_FAILED", "视图查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询视图的分组摘要
     *
     * @param viewId     视图ID
     * @param request    查询请求
     * @param operatorId 操作人ID
     * @return 分组摘要列表
     */
    public Result<List<GroupedCardData>> queryGroups(String viewId,
                                                      ViewDataRequest request,
                                                      String operatorId) {
        // TODO: 实现分组摘要查询
        return Result.success(List.of());
    }

    /**
     * 预览视图数据
     *
     * @param viewDef    视图定义（未持久化）
     * @param request    查询请求
     * @param operatorId 操作人ID
     * @return 视图数据响应
     */
    public Result<ViewDataResponse> preview(AbstractViewDefinition viewDef,
                                             ViewDataRequest request,
                                             String operatorId,
                                             String orgId,
                                             String structureNodeId) {
        try {
            if (orgId != null && viewDef.getOrgId() != null && !orgId.equals(viewDef.getOrgId())) {
                return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "组织与视图不匹配");
            }
            if (!viewDataVisibilityService.canQuery(viewDef, operatorId, structureNodeId)) {
                return Result.failure(CommonErrorCode.OPERATION_NOT_ALLOWED, "无权预览该视图");
            }
            return executeQuery(viewDef, request, operatorId);
        } catch (Exception e) {
            log.error("视图预览失败", e);
            return Result.failure("PREVIEW_FAILED", "视图预览失败: " + e.getMessage());
        }
    }

    /**
     * 获取视图定义
     */
    private AbstractViewDefinition getViewDefinition(String viewId) {
        SchemaDefinition<?> schema = schemaCacheService.getById(viewId)
                .orElseThrow(() -> new IllegalArgumentException("获取视图定义失败: viewId=" + viewId));

        if (!(schema instanceof AbstractViewDefinition)) {
            throw new IllegalArgumentException("Schema 不是视图类型: " + viewId);
        }

        return (AbstractViewDefinition) schema;
    }

    /**
     * 执行查询
     */
    @SuppressWarnings("unchecked")
    private Result<ViewDataResponse> executeQuery(AbstractViewDefinition viewDef,
                                                   ViewDataRequest request,
                                                   String operatorId) {
        String viewType = viewDef.getViewType();

        if (!executorRegistry.supports(viewType)) {
            return Result.failure("UNSUPPORTED_VIEW_TYPE", "不支持的视图类型: " + viewType);
        }

        ViewExecutor<AbstractViewDefinition, ViewDataResponse> executor =
                executorRegistry.getExecutor(viewType);

        ViewDataResponse response = executor.execute(viewDef, request, operatorId);
        return Result.success(response);
    }
}
