package dev.planka.schema.service.condition;

import dev.planka.api.card.CardServiceClient;
import dev.planka.api.schema.dto.EnumOptionDTO;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.fieldconfig.EnumFieldConfig;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.stream.StatusConfig;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.schema.dto.condition.CardDisplayInfo;
import dev.planka.schema.dto.condition.ConditionDisplayInfo;
import dev.planka.schema.service.common.SchemaCommonService;
import dev.planka.schema.service.linktype.LinkFieldHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 条件显示信息服务
 * <p>
 * 提供批量解析 Condition 中所有需要显示名称的 ID 的功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConditionDisplayInfoService {

    private final SchemaCommonService schemaCommonService;
    private final LinkFieldHelper linkFieldHelper;
    private final CardServiceClient cardServiceClient;

    /**
     * 获取条件的显示信息
     * <p>
     * 遍历 Condition 树，收集所有需要解析的 ID，然后批量查询各类信息。
     *
     * @param condition 条件定义
     * @return 显示信息
     */
    public Result<ConditionDisplayInfo> getDisplayInfo(Condition condition) {
        if (condition == null || condition.isEmpty()) {
            return Result.success(ConditionDisplayInfo.empty());
        }

        // 1. 收集所有需要解析的 ID
        ConditionIdCollector collector = new ConditionIdCollector();
        collector.collect(condition);

        // 2. 批量解析各类信息
        Map<String, String> fieldNames = resolveFieldNames(collector.getFieldIds());
        Map<String, String> linkFieldNames = resolveLinkFieldNames(collector.getLinkFieldIds());
        Map<String, List<EnumOptionDTO>> enumOptions = resolveEnumOptions(collector.getEnumFieldIds());
        Map<String, CardDisplayInfo> cards = resolveCardInfos(collector.getCardIds());
        Map<String, String> statusNames = resolveStatusNames(collector.getStreamStatusMap());

        // 3. 组装返回
        return Result.success(new ConditionDisplayInfo(
                fieldNames,
                linkFieldNames,
                enumOptions,
                cards,
                statusNames
        ));
    }

    /**
     * 解析字段名称
     * <p>
     * 直接根据 fieldId 查询 FieldConfig 获取名称
     */
    private Map<String, String> resolveFieldNames(Set<String> fieldIds) {
        if (fieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();

        // 批量获取 FieldConfig
        Result<List<SchemaDefinition<?>>> schemasResult = schemaCommonService.getByIds(new ArrayList<>(fieldIds));
        if (schemasResult.isSuccess() && schemasResult.getData() != null) {
            for (SchemaDefinition<?> schema : schemasResult.getData()) {
                if (schema instanceof FieldConfig fieldConfig) {
                    result.put(fieldConfig.getId().value(), fieldConfig.getName());
                }
            }
        }

        return result;
    }

    /**
     * 解析关联属性名称（批量）
     */
    private Map<String, String> resolveLinkFieldNames(Set<String> linkFieldIds) {
        if (linkFieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return linkFieldHelper.getDisplayNames(linkFieldIds);
    }

    /**
     * 解析枚举选项
     * <p>
     * 直接根据枚举字段 ID 查询 EnumFieldConfig 获取选项
     */
    private Map<String, List<EnumOptionDTO>> resolveEnumOptions(Set<String> enumFieldIds) {
        if (enumFieldIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<EnumOptionDTO>> result = new HashMap<>();

        // 批量获取 FieldConfig
        Result<List<SchemaDefinition<?>>> schemasResult = schemaCommonService.getByIds(new ArrayList<>(enumFieldIds));
        if (schemasResult.isSuccess() && schemasResult.getData() != null) {
            for (SchemaDefinition<?> schema : schemasResult.getData()) {
                if (schema instanceof EnumFieldConfig enumConfig) {
                    String fieldId = enumConfig.getId().value();
                    if (enumConfig.getOptions() != null) {
                        List<EnumOptionDTO> options = enumConfig.getOptions().stream()
                                .map(opt -> new EnumOptionDTO(opt.id(), opt.label(), opt.color(), opt.enabled()))
                                .collect(Collectors.toList());
                        result.put(fieldId, options);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 解析卡片信息（批量）
     * <p>
     * 调用卡片服务批量获取卡片的标题信息。
     *
     * @param cardIds 卡片ID集合
     * @return cardId -> CardDisplayInfo 的映射
     */
    private Map<String, CardDisplayInfo> resolveCardInfos(Set<String> cardIds) {
        if (cardIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // 调用卡片服务批量获取卡片名称
            // 使用系统用户身份调用（传空字符串表示系统调用）
            Result<Map<String, CardTitle>> namesResult = cardServiceClient.queryCardNames(
                    "system",
                    new ArrayList<>(cardIds)
            );

            if (!namesResult.isSuccess() || namesResult.getData() == null) {
                log.warn("Failed to query card names: {}", namesResult.getMessage());
                return Collections.emptyMap();
            }

            // 转换为 CardDisplayInfo
            Map<String, CardDisplayInfo> result = new HashMap<>();
            for (Map.Entry<String, CardTitle> entry : namesResult.getData().entrySet()) {
                String cardId = entry.getKey();
                CardTitle title = entry.getValue();
                String displayTitle = title != null ? title.getDisplayValue() : cardId;
                // code 暂时使用空字符串，因为 queryCardNames 不返回 code
                result.put(cardId, new CardDisplayInfo(cardId, "", displayTitle));
            }

            return result;
        } catch (Exception e) {
            log.warn("Failed to query card names from card service", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 解析状态名称（批量）
     * <p>
     * 根据 streamId -> Set<statusId> 的映射，批量查询价值流定义，
     * 然后从中提取状态名称。
     *
     * @param streamStatusMap streamId -> Set<statusId> 的映射
     * @return statusId -> statusName 的映射
     */
    private Map<String, String> resolveStatusNames(Map<String, Set<String>> streamStatusMap) {
        if (streamStatusMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 批量查询价值流定义
        Set<String> streamIds = streamStatusMap.keySet();
        Result<List<SchemaDefinition<?>>> schemasResult = schemaCommonService.getByIds(new ArrayList<>(streamIds));

        if (!schemasResult.isSuccess() || schemasResult.getData() == null) {
            return Collections.emptyMap();
        }

        // 2. 收集价值流定义
        Map<String, ValueStreamDefinition> valueStreamMap = new HashMap<>();

        for (SchemaDefinition<?> schema : schemasResult.getData()) {
            if (schema instanceof ValueStreamDefinition valueStream) {
                valueStreamMap.put(valueStream.getId().value(), valueStream);
            }
        }

        // 3. 构建 statusId -> statusName 映射
        Map<String, String> result = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : streamStatusMap.entrySet()) {
            String streamId = entry.getKey();
            Set<String> statusIdsToResolve = new HashSet<>(entry.getValue());

            ValueStreamDefinition valueStream = valueStreamMap.get(streamId);
            if (valueStream != null) {
                resolveFromValueStream(valueStream, statusIdsToResolve, result);
            }
        }

        return result;
    }

    /**
     * 从价值流定义中解析状态名称
     */
    private void resolveFromValueStream(ValueStreamDefinition valueStream,
                                     Set<String> statusIdsToResolve,
                                     Map<String, String> result) {
        if (valueStream.getStepList() == null) {
            return;
        }

        valueStream.getStepList().stream()
                .filter(step -> step.getStatusList() != null)
                .flatMap(step -> step.getStatusList().stream())
                .forEach(status -> resolveStatus(status, statusIdsToResolve, result));
    }

    /**
     * 解析单个状态配置
     */
    private void resolveStatus(StatusConfig statusConfig,
                               Set<String> statusIdsToResolve,
                               Map<String, String> result) {
        if (statusIdsToResolve.contains(statusConfig.getId().value())) {
            result.put(statusConfig.getId().value(), statusConfig.getName());
            statusIdsToResolve.remove(statusConfig.getId().value());
        }
    }
}
