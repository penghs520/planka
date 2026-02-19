package dev.planka.api.view;

import dev.planka.api.view.request.ViewDataRequest;
import dev.planka.api.view.request.ViewPreviewRequest;
import dev.planka.api.view.response.GroupedCardData;
import dev.planka.api.view.response.ViewDataResponse;
import dev.planka.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * 视图数据服务客户端
 * <p>
 * 提供视图数据查询的 API 接口，前端通过此接口获取视图数据，
 * 而不是直接调用 card-api。
 */
@FeignClient(name = "view-service", contextId = "viewDataClient", path = "/api/v1/view-data")
public interface ViewDataClient {

    /**
     * 根据视图ID查询数据
     *
     * @param viewId     视图ID
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request    查询请求
     * @return 视图数据
     */
    @PostMapping("/{viewId}")
    Result<ViewDataResponse> queryByViewId(
            @PathVariable("viewId") String viewId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody ViewDataRequest request);

    /**
     * 查询视图的分组摘要
     * <p>
     * 当视图配置了分组时，可以先查询分组摘要（分组值 + 数量），
     * 然后按需展开加载具体分组的数据。
     *
     * @param viewId     视图ID
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request    查询请求
     * @return 分组摘要列表
     */
    @PostMapping("/{viewId}/groups")
    Result<List<GroupedCardData>> queryGroups(
            @PathVariable("viewId") String viewId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody ViewDataRequest request);

    /**
     * 预览视图数据
     * <p>
     * 用于在配置视图时预览数据效果，传入视图定义（未持久化）进行查询。
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param request    预览请求（包含视图定义和查询参数）
     * @return 视图数据
     */
    @PostMapping("/preview")
    Result<ViewDataResponse> preview(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody ViewPreviewRequest request);
}
