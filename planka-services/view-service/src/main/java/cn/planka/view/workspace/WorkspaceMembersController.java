package cn.planka.view.workspace;

import cn.planka.api.view.response.WorkspaceMemberRowDTO;
import cn.planka.common.result.PageResult;
import cn.planka.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 工作区成员目录（无视图定义，由 view-service 编排卡片与用户域数据）
 */
@RestController
@RequestMapping("/api/v1/view-data/workspace/members")
@RequiredArgsConstructor
public class WorkspaceMembersController {

    private final WorkspaceMembersService workspaceMembersService;

    @GetMapping
    public Result<PageResult<WorkspaceMemberRowDTO>> list(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorMemberCardId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", defaultValue = "name") String sort,
            @RequestParam(name = "order", defaultValue = "asc") String order) {
        return workspaceMembersService.listWorkspaceMembers(
                orgId, operatorMemberCardId, page, size, keyword, sort, order);
    }
}
