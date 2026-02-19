package dev.planka.schema.controller;

import dev.planka.api.schema.dto.StatusOptionDTO;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.definition.stream.StatusConfig;
import dev.planka.domain.schema.definition.stream.StepConfig;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.schema.service.stream.ValueStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 价值流 REST 控制器
 * <p>
 * 提供价值流定义的查询接口。
 * 创建、更新、删除等通用操作使用 SchemaCommonController。
 */
@RestController
@RequestMapping("/api/v1/schemas/value-streams")
@RequiredArgsConstructor
public class ValueStreamController {

    private final ValueStreamService valueStreamService;

    /**
     * 根据卡片类型ID获取价值流定义
     * <p>
     * 一个卡片类型仅允许创建一个价值流，不存在则返回null
     *
     * @param cardTypeId 卡片类型ID
     * @return 价值流定义
     */
    @GetMapping("/by-card-type/{cardTypeId}")
    public Result<ValueStreamDefinition> getByCardType(
            @PathVariable("cardTypeId") String cardTypeId) {
        return valueStreamService.getByCardTypeId(cardTypeId);
    }

    /**
     * 获取价值流状态选项列表
     * <p>
     * 返回精简的状态选项，用于状态下拉选择
     *
     * @param cardTypeId 卡片类型ID
     * @return 状态选项列表
     */
    @GetMapping("/status-options/{cardTypeId}")
    public Result<List<StatusOptionDTO>> getStatusOptions(
            @PathVariable("cardTypeId") String cardTypeId) {

        Result<ValueStreamDefinition> valueStreamResult = valueStreamService.getByCardTypeId(cardTypeId);
        if (!valueStreamResult.isSuccess()) {
            return Result.failure(valueStreamResult.getCode(), valueStreamResult.getMessage());
        }

        ValueStreamDefinition valueStream = valueStreamResult.getData();
        if (valueStream == null || valueStream.getStepList() == null) {
            return Result.success(List.of());
        }

        List<StatusOptionDTO> options = new ArrayList<>();
        for (StepConfig step : valueStream.getStepList()) {
            if (step.getStatusList() != null) {
                String stepKind = step.getKind() != null ? step.getKind().name() : "IN_PROGRESS";
                for (StatusConfig status : step.getStatusList()) {
                    options.add(StatusOptionDTO.builder()
                            .id(status.getId().value())
                            .name(status.getName())
                            .stepKind(stepKind)
                            .build());
                }
            }
        }

        return Result.success(options);
    }
}
