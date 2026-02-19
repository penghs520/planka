package dev.planka.schema.controller;

import dev.planka.common.result.Result;
import dev.planka.schema.dto.condition.ConditionDisplayInfo;
import dev.planka.schema.dto.condition.GetConditionDisplayInfoRequest;
import dev.planka.schema.service.condition.ConditionDisplayInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 条件 REST 控制器
 * <p>
 * 提供条件相关的查询接口。
 */
@RestController
@RequestMapping("/api/v1/conditions")
@RequiredArgsConstructor
public class ConditionController {

    private final ConditionDisplayInfoService conditionDisplayInfoService;

    /**
     * 获取条件的显示信息
     * <p>
     * 批量解析 Condition 中所有需要显示名称的 ID，返回 ID -> Name 映射。
     * 包含：字段名称、关联属性名称、枚举选项、卡片信息、状态名称。
     *
     * @param request 请求参数
     * @return 条件显示信息
     */
    @PostMapping("/display-info")
    public Result<ConditionDisplayInfo> getDisplayInfo(
            @Valid @RequestBody GetConditionDisplayInfoRequest request) {
        return conditionDisplayInfoService.getDisplayInfo(request.condition());
    }
}
