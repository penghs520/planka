package dev.planka.extension.comment.controller;

import dev.planka.api.user.UserServiceContract;
import dev.planka.api.user.dto.MemberDTO;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 建议控制器 - 用于 @成员 和 #卡片 自动补全
 */
@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionController {

    private final UserServiceContract userServiceContract;

    public SuggestionController(UserServiceContract userServiceContract) {
        this.userServiceContract = userServiceContract;
    }

    /**
     * 成员建议（@成员自动补全）
     */
    @GetMapping("/members")
    public Result<List<MemberSuggestion>> suggestMembers(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(required = false, defaultValue = "") String keyword) {
        Result<PageResult<MemberDTO>> result = userServiceContract.listMembers(orgId, 1, 50);
        if (!result.isSuccess() || result.getData() == null) {
            return Result.success(List.of());
        }

        List<MemberSuggestion> suggestions = result.getData().getContent().stream()
                .filter(m -> keyword.isEmpty() ||
                        (m.nickname() != null && m.nickname().toLowerCase().contains(keyword.toLowerCase())))
                .limit(10)
                .map(m -> new MemberSuggestion(m.id(), m.nickname(), m.avatar()))
                .collect(Collectors.toList());

        return Result.success(suggestions);
    }

    /**
     * 卡片建议（#卡片自动补全）
     * 暂时返回空列表，待 card-service 提供搜索接口后实现
     */
    @GetMapping("/cards")
    public Result<List<CardSuggestion>> suggestCards(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestParam(required = false, defaultValue = "") String keyword) {
        // TODO: 集成 card-service 搜索接口
        return Result.success(List.of());
    }

    public record MemberSuggestion(String id, String name, String avatar) {
    }

    public record CardSuggestion(String id, String code, String title, String cardTypeId) {
    }
}
