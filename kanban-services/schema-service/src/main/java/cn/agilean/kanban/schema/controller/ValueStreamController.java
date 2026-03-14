package cn.agilean.kanban.schema.controller;

import cn.agilean.kanban.api.schema.dto.StatusOptionDTO;
import cn.agilean.kanban.api.schema.request.UpdateSchemaRequest;
import cn.agilean.kanban.common.result.Result;
import cn.agilean.kanban.domain.schema.definition.stream.StatusConfig;
import cn.agilean.kanban.domain.schema.definition.stream.StepConfig;
import cn.agilean.kanban.domain.schema.definition.stream.ValueStreamDefinition;
import cn.agilean.kanban.event.card.CardMigrationRequestedEvent;
import cn.agilean.kanban.schema.service.common.SchemaCommonService;
import cn.agilean.kanban.schema.service.stream.ValueStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static final String CARD_EVENT_TOPIC = "kanban-card-events";

    private final ValueStreamService valueStreamService;
    private final SchemaCommonService schemaCommonService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
        String streamId = valueStream.getId().value();
        for (StepConfig step : valueStream.getStepList()) {
            if (step.getStatusList() != null) {
                String stepKind = step.getKind() != null ? step.getKind().name() : "IN_PROGRESS";
                for (StatusConfig status : step.getStatusList()) {
                    options.add(StatusOptionDTO.builder()
                            .id(status.getId().value())
                            .name(status.getName())
                            .stepKind(stepKind)
                            .streamId(streamId)
                            .build());
                }
            }
        }

        return Result.success(options);
    }

    /**
     * 删除状态并迁移卡片
     * <p>
     * 发送卡片迁移请求事件，由 card-service 执行实际的迁移操作，然后删除该状态
     *
     * @param valueStreamId 价值流ID
     * @param statusId 要删除的状态ID
     * @param targetStatusId 目标状态ID
     * @param orgId 组织ID
     * @param operatorId 操作人ID
     * @return 操作结果
     */
    @DeleteMapping("/{valueStreamId}/status/{statusId}")
    public Result<Void> deleteStatusWithMigration(
            @PathVariable String valueStreamId,
            @PathVariable String statusId,
            @RequestParam String targetStatusId,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {

        // 1. 获取价值流定义
        Result<ValueStreamDefinition> valueStreamResult = valueStreamService.getById(valueStreamId);
        if (!valueStreamResult.isSuccess() || valueStreamResult.getData() == null) {
            return Result.failure(valueStreamResult.getCode(), "价值流不存在");
        }

        ValueStreamDefinition valueStream = valueStreamResult.getData();

        // 2. 发送卡片迁移请求事件
        CardMigrationRequestedEvent event = new CardMigrationRequestedEvent(
                orgId, operatorId, null, null, valueStream.getCardTypeId().value());
        event.withMigrationInfo(valueStreamId, Map.of(statusId, targetStatusId), false);
        publishCardMigrationEvent(event);

        // 3. 从价值流定义中删除该状态，创建新的 ValueStreamDefinition
        List<StepConfig> updatedStepList = new ArrayList<>();
        for (StepConfig step : valueStream.getStepList()) {
            List<StatusConfig> updatedStatusList = step.getStatusList().stream()
                    .filter(status -> !status.getId().value().equals(statusId))
                    .toList();
            StepConfig updatedStep = new StepConfig();
            updatedStep.setId(step.getId());
            updatedStep.setName(step.getName());
            updatedStep.setDesc(step.getDesc());
            updatedStep.setKind(step.getKind());
            updatedStep.setSortOrder(step.getSortOrder());
            updatedStep.setStatusList(updatedStatusList);
            updatedStepList.add(updatedStep);
        }

        ValueStreamDefinition updatedValueStream = new ValueStreamDefinition(
                valueStream.getId(),
                valueStream.getOrgId(),
                valueStream.getName(),
                valueStream.getCardTypeId(),
                updatedStepList
        );

        // 4. 保存价值流定义
        UpdateSchemaRequest updateRequest = new UpdateSchemaRequest();
        updateRequest.setDefinition(updatedValueStream);
        Result<?> updateResult = schemaCommonService.update(valueStreamId, operatorId, updateRequest);
        if (!updateResult.isSuccess()) {
            return Result.failure(updateResult.getCode(), "更新价值流定义失败: " + updateResult.getMessage());
        }

        return Result.success();
    }

    /**
     * 删除阶段并迁移卡片
     * <p>
     * 根据状态迁移映射发送卡片迁移请求事件，由 card-service 执行实际的迁移操作，然后删除该阶段
     *
     * @param valueStreamId 价值流ID
     * @param stepId 要删除的阶段ID
     * @param statusMigrationMap 状态迁移映射：源状态ID -> 目标状态ID
     * @param orgId 组织ID
     * @param operatorId 操作人ID
     * @return 操作结果
     */
    @DeleteMapping("/{valueStreamId}/step/{stepId}")
    public Result<Void> deleteStepWithMigration(
            @PathVariable String valueStreamId,
            @PathVariable String stepId,
            @RequestBody Map<String, String> statusMigrationMap,
            @RequestHeader("X-Org-Id") String orgId,
            @RequestHeader("X-Member-Card-Id") String operatorId) {

        // 1. 获取价值流定义
        Result<ValueStreamDefinition> valueStreamResult = valueStreamService.getById(valueStreamId);
        if (!valueStreamResult.isSuccess() || valueStreamResult.getData() == null) {
            return Result.failure(valueStreamResult.getCode(), "价值流不存在");
        }

        ValueStreamDefinition valueStream = valueStreamResult.getData();

        // 2. 发送卡片迁移请求事件
        CardMigrationRequestedEvent event = new CardMigrationRequestedEvent(
                orgId, operatorId, null, null, valueStream.getCardTypeId().value());
        event.withMigrationInfo(valueStreamId, statusMigrationMap, true);
        publishCardMigrationEvent(event);

        // 3. 从价值流定义中删除该阶段，创建新的 ValueStreamDefinition
        List<StepConfig> updatedStepList = valueStream.getStepList().stream()
                .filter(step -> !step.getId().value().equals(stepId))
                .toList();

        ValueStreamDefinition updatedValueStream = new ValueStreamDefinition(
                valueStream.getId(),
                valueStream.getOrgId(),
                valueStream.getName(),
                valueStream.getCardTypeId(),
                updatedStepList
        );

        // 4. 保存价值流定义
        UpdateSchemaRequest updateRequest = new UpdateSchemaRequest();
        updateRequest.setDefinition(updatedValueStream);
        Result<?> updateResult = schemaCommonService.update(valueStreamId, operatorId, updateRequest);
        if (!updateResult.isSuccess()) {
            return Result.failure(updateResult.getCode(), "更新价值流定义失败: " + updateResult.getMessage());
        }

        return Result.success();
    }

    /**
     * 发布卡片迁移请求事件
     */
    private void publishCardMigrationEvent(CardMigrationRequestedEvent event) {
        if (kafkaTemplate == null) {
            return;
        }
        kafkaTemplate.send(CARD_EVENT_TOPIC, event.getOrgId(), event);
    }
}
