package dev.planka.view.controller;

import dev.planka.api.view.ViewDataClient;
import dev.planka.api.view.request.ViewDataRequest;
import dev.planka.api.view.request.ViewPreviewRequest;
import dev.planka.api.view.response.GroupedCardData;
import dev.planka.api.view.response.ViewDataResponse;
import dev.planka.common.result.Result;
import dev.planka.view.service.ViewDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 视图数据控制器
 * <p>
 * 实现 ViewDataClient 接口，提供视图数据查询的 REST API。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/view-data")
@RequiredArgsConstructor
public class ViewDataController implements ViewDataClient {

    private final ViewDataService viewDataService;

    @Override
    @PostMapping("/{viewId}")
    public Result<ViewDataResponse> queryByViewId(
            @PathVariable("viewId") String viewId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody ViewDataRequest request) {
        log.debug("查询视图数据, viewId={}, operatorId={}", viewId, operatorId);
        return viewDataService.queryByViewId(viewId, request, operatorId);
    }

    @Override
    @PostMapping("/{viewId}/groups")
    public Result<List<GroupedCardData>> queryGroups(
            @PathVariable("viewId") String viewId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody ViewDataRequest request) {
        log.debug("查询视图分组, viewId={}, operatorId={}", viewId, operatorId);
        return viewDataService.queryGroups(viewId, request, operatorId);
    }

    @Override
    @PostMapping("/preview")
    public Result<ViewDataResponse> preview(
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody ViewPreviewRequest request) {
        log.debug("预览视图数据, operatorId={}", operatorId);
        return viewDataService.preview(
                request.getViewDefinition(),
                request.getDataRequestOrDefault(),
                operatorId
        );
    }
}
