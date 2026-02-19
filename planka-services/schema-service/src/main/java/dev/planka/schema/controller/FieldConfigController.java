package dev.planka.schema.controller;

import dev.planka.api.schema.dto.inheritance.FieldConfigListWithSource;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.api.schema.service.FieldConfigQueryService;
import dev.planka.schema.service.fieldconfig.FieldConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 属性配置 REST 控制器
 * <p>
 * 提供卡片类型的属性配置查询和保存接口。
 * <p>
 * 继承优先级：自身 > 显式父类 > 任意卡属性集
 */
@RestController
@RequestMapping("/api/v1/schemas/field-configs")
@RequiredArgsConstructor
public class FieldConfigController {

    private final FieldConfigService fieldConfigService;
    private final FieldConfigQueryService fieldConfigQueryService;

    /**
     * 获取卡片类型属性配置列表
     * <p>
     * 返回属性配置列表，包含继承的属性和关联属性，并标记多继承时的配置冲突。
     *
     * @param cardTypeId 卡片类型ID
     */
    @GetMapping("/with-source/{cardTypeId}")
    public Result<FieldConfigListWithSource> getFieldConfigs(@PathVariable("cardTypeId") String cardTypeId) {
        return fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId);
    }

    /**
     * 保存单个属性配置
     * <p>
     * 保存卡片类型的单个属性配置。
     *
     * @param cardTypeId 卡片类型ID
     * @param operatorId 操作人ID
     * @param config     属性配置
     */
    @PostMapping("/{cardTypeId}")
    public Result<?> saveFieldConfig(
            @PathVariable("cardTypeId") String cardTypeId,
            @RequestHeader("X-Member-Card-Id") String operatorId,
            @RequestBody FieldConfig config) {
        return fieldConfigService.saveFieldConfig(cardTypeId, operatorId, config);
    }

    /**
     * 删除属性配置（恢复为从父类型继承）
     *
     * @param fieldConfigId 属性配置ID
     */
    @DeleteMapping("/{cardTypeId}/{fieldConfigId}")
    public Result<Void> deleteFieldConfig(
            @PathVariable("fieldConfigId") String fieldConfigId, @RequestHeader("X-Member-Card-Id") String operatorId) {
        return fieldConfigService.deleteFieldConfig(fieldConfigId, operatorId);
    }
}
