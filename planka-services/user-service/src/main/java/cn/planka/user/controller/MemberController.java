package cn.planka.user.controller;

import cn.planka.api.user.dto.MemberCardDirectoryEnrichmentDTO;
import cn.planka.api.user.dto.MemberDTO;
import cn.planka.api.user.dto.MemberOptionDTO;
import cn.planka.api.user.request.AddMemberRequest;
import cn.planka.api.user.request.MemberCardIdsRequest;
import cn.planka.api.user.request.UpdateMemberRoleRequest;
import cn.planka.common.result.PageResult;
import cn.planka.common.result.Result;
import cn.planka.user.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成员控制器
 */
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 获取组织成员列表
     */
    @GetMapping
    public Result<PageResult<MemberDTO>> listMembers(
            @RequestHeader(name = "X-Org-Id") String orgId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return memberService.listMembers(orgId, page, size);
    }

    /**
     * 获取成员卡片选项列表
     * <p>
     * 用于下拉选择器等场景，支持按成员名称关键字搜索
     *
     * @param keyword 搜索关键字（可选，匹配成员卡片名称）
     */
    @GetMapping("/options")
    public Result<PageResult<MemberOptionDTO>> getMemberOptions(
            @RequestHeader(name = "X-Org-Id") String orgId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return memberService.getMemberOptions(orgId, page, size, keyword);
    }

    /**
     * 按成员卡片 ID 批量补齐目录展示字段（角色、上次登录时间）
     */
    @PostMapping("/by-member-cards")
    public Result<List<MemberCardDirectoryEnrichmentDTO>> enrichByMemberCards(
            @RequestHeader(name = "X-Org-Id") String orgId,
            @Valid @RequestBody MemberCardIdsRequest request) {
        return memberService.enrichByMemberCards(orgId, request);
    }

    /**
     * 添加成员
     */
    @PostMapping
    public Result<MemberDTO> addMember(
            @RequestHeader(name = "X-Org-Id") String orgId,
            @RequestHeader(name = "X-User-Id") String userId,
            @Valid @RequestBody AddMemberRequest request) {
        return memberService.addMember(orgId, userId, request);
    }

    /**
     * 获取成员详情
     */
    @GetMapping("/{memberId}")
    public Result<MemberDTO> getMember(@PathVariable(name = "memberId") String memberId) {
        return memberService.getMember(memberId);
    }

    /**
     * 移除成员
     */
    @DeleteMapping("/{memberId}")
    public Result<Void> removeMember(
            @PathVariable(name = "memberId") String memberId,
            @RequestHeader(name = "X-User-Id") String userId) {
        return memberService.removeMember(memberId, userId);
    }

    /**
     * 修改成员角色
     */
    @PutMapping("/{memberId}/role")
    public Result<MemberDTO> updateMemberRole(
            @PathVariable(name = "memberId") String memberId,
            @RequestHeader(name = "X-User-Id") String userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return memberService.updateMemberRole(memberId, userId, request);
    }
}
