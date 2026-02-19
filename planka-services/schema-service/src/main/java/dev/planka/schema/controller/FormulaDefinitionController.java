package dev.planka.schema.controller;

import dev.planka.api.schema.vo.formuladefinition.FormulaDefinitionVO;
import dev.planka.common.result.Result;
import dev.planka.schema.service.formula.FormulaDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 计算公式定义 REST 控制器
 * <p>
 * 提供公式定义的列表查询接口，返回 VO 格式数据。
 */
@RestController
@RequestMapping("/api/v1/schemas/formula-definitions")
@RequiredArgsConstructor
public class FormulaDefinitionController {

    private final FormulaDefinitionService formulaDefinitionService;

    /**
     * 查询公式定义列表
     *
     * @param orgId   组织 ID（从请求头获取）
     * @param spaceId 空间 ID（可选，从请求头获取，但公式定义仅支持组织级配置，此参数会被忽略）
     * @return 公式定义列表
     */
    @GetMapping
    public Result<List<FormulaDefinitionVO>> list(
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader(value = "X-Space-Id", required = false) String spaceId) {
        return formulaDefinitionService.listFormulaDefinitions(orgId, spaceId);
    }
}
