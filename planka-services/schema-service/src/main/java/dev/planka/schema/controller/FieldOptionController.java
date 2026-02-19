package dev.planka.schema.controller;

import dev.planka.api.schema.dto.FieldOption;
import dev.planka.api.schema.dto.inheritance.CommonFieldConfigRequest;
import dev.planka.api.schema.dto.inheritance.CommonFieldOptionResponse;
import dev.planka.api.schema.dto.inheritance.MatchingLinkFieldsRequest;
import dev.planka.api.schema.dto.inheritance.MatchingLinkFieldsResponse;
import dev.planka.common.result.Result;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.schema.service.field.FieldOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 属性选项 REST 控制器
 * <p>
 * 提供卡片类型属性选项的查询接口，用于：
 * - 视图配置的列选择
 * - 筛选器的字段选择
 * - 排序字段选择
 * - 其他需要属性列表的场景
 */
@RestController
@RequestMapping("/api/v1/schemas/field-options")
@RequiredArgsConstructor
public class FieldOptionController {

    private final FieldOptionService fieldOptionService;

    /**
     * 获取单个卡片类型的属性选项列表
     * <p>
     * 这是一个便捷接口，等同于调用 POST /common 传入单个卡片类型ID。
     *
     * @param cardTypeId 卡片类型ID
     * @param fieldTypes 可选的属性类型过滤
     * @return 属性选项响应
     */
    @GetMapping("/{cardTypeId}")
    public Result<CommonFieldOptionResponse> getFieldOptions(
            @PathVariable("cardTypeId") String cardTypeId,
            @RequestParam(value = "fieldTypes", required = false) List<String> fieldTypes) {
        CommonFieldConfigRequest request = new CommonFieldConfigRequest();
        request.setCardTypeIds(List.of(cardTypeId));
        request.setFieldTypes(fieldTypes);
        return fieldOptionService.getCommonFieldConfigs(request);
    }

    /**
     * 获取卡片类型的共同属性选项
     * <p>
     * 传入一个或多个卡片类型ID，返回它们的共同属性配置（交集）。
     * 支持按属性类型过滤。
     * <p>
     * 使用场景：
     * - 传入单个卡片类型ID：获取该卡片类型的所有属性
     * - 传入多个卡片类型ID：获取这些卡片类型的共同属性
     *
     * @param request 请求参数，包含卡片类型ID列表和可选的属性类型过滤
     * @return 共同属性配置响应
     */
    @PostMapping("/common")
    public Result<CommonFieldOptionResponse> getCommonFieldOptions(
            @RequestBody CommonFieldConfigRequest request) {
        return fieldOptionService.getCommonFieldConfigs(request);
    }

    /**
     * 获取源卡片类型和目标卡片类型之间可以匹配的关联属性
     * <p>
     * 用于架构线配置场景：
     * - 源卡片类型：当前层级的卡片类型
     * - 目标卡片类型：父层级的卡片类型
     * - 返回：当前层级中能够连接到父层级的所有关联属性（包含 multiple 信息）
     * <p>
     * 匹配规则：
     * - 源卡片类型有关联属性 (linkTypeId: L1, position: SOURCE)
     * - 目标卡片类型有关联属性 (linkTypeId: L1, position: TARGET)
     * - 则这两个属性可以建立关联，返回源端的属性
     * <p>
     * 注意：返回所有匹配的关联属性（包含单选和多选），由客户端根据场景决定是否使用
     *
     * @param request 匹配请求，包含源和目标卡片类型ID列表
     * @return 匹配的关联属性响应（从源卡片类型的视角，包含 multiple 信息）
     */
    @PostMapping("/matching-links")
    public Result<MatchingLinkFieldsResponse> getMatchingLinkFields(
            @RequestBody MatchingLinkFieldsRequest request) {
        return fieldOptionService.getMatchingLinkFields(request);
    }

    /**
     * 根据关联字段ID获取级联的目标卡片类型的共同属性选项
     * <p>
     * linkFieldId 格式: {linkTypeId}:{SOURCE|TARGET}
     * 例如: 263671031548350464:SOURCE
     * <p>
     * 当关联类型的目标端有多个卡片类型时，返回这些卡片类型之间的共同属性。
     * 返回精简的 FieldOption 列表，适用于属性选择场景。
     *
     * @param linkFieldId 关联字段ID
     * @return 共同属性选项列表
     */
    @GetMapping("/by-link-field/{linkFieldId}")
    public Result<List<FieldOption>> getFieldOptionsByLinkField(
            @PathVariable("linkFieldId") LinkFieldId linkFieldId) {
        return fieldOptionService.getFieldOptionsByLinkField(linkFieldId);
    }
}
