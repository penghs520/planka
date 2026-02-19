package dev.planka.schema.service.field;

import dev.planka.api.schema.dto.EnumOptionDTO;
import dev.planka.api.schema.dto.FieldOption;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkFieldId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.schema.definition.fieldconfig.*;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;
import dev.planka.api.schema.dto.inheritance.*;
import dev.planka.api.schema.service.FieldConfigQueryService;
import dev.planka.schema.service.linktype.LinkFieldHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 属性选项服务
 * <p>
 * 提供获取卡片类型共同属性选项的功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldOptionService {

    private final FieldConfigQueryService fieldConfigQueryService;
    private final LinkFieldHelper linkFieldHelper;

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
    public Result<List<FieldOption>> getFieldOptionsByLinkField(LinkFieldId linkFieldId) {
        // 检查关联类型是否存在
        if (!linkFieldHelper.exists(linkFieldId)) {
            return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "关联类型不存在: " + linkFieldId.getLinkTypeId());
        }

        // 获取目标卡片类型列表
        List<CardTypeId> targetCardTypeIds = linkFieldHelper.getTargetCardTypeIds(linkFieldId);

        if (targetCardTypeIds.isEmpty()) {
            // 没有限制卡片类型，返回空列表
            return Result.success(Collections.emptyList());
        }

        if (targetCardTypeIds.size() == 1) {
            // 只有一个目标卡片类型，直接返回该卡片类型的属性
            return getFieldOptionsForCardType(targetCardTypeIds.get(0).value());
        }

        // 多个目标卡片类型，获取共同属性
        return getCommonFieldOptionsForCardTypes(
                targetCardTypeIds.stream().map(CardTypeId::value).collect(Collectors.toList())
        );
    }

    /**
     * 获取单个卡片类型的属性选项
     */
    private Result<List<FieldOption>> getFieldOptionsForCardType(String cardTypeId) {
        Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId);
        if (!result.isSuccess()) {
            return Result.failure(result.getCode(), result.getMessage());
        }

        List<FieldOption> options = result.getData().getFields().stream()
                .map(this::toFieldOption)
                .collect(Collectors.toList());

        return Result.success(options);
    }

    /**
     * 获取多个卡片类型的共同属性选项
     */
    private Result<List<FieldOption>> getCommonFieldOptionsForCardTypes(List<String> cardTypeIds) {
        // 获取每个卡片类型的属性
        Map<String, List<FieldConfig>> cardTypeFieldsMap = new HashMap<>();
        for (String cardTypeId : cardTypeIds) {
            Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId);
            if (!result.isSuccess()) {
                log.warn("Failed to get field configs for card type: {}", cardTypeId);
                continue;
            }
            cardTypeFieldsMap.put(cardTypeId, result.getData().getFields());
        }

        if (cardTypeFieldsMap.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 计算共同属性（按 fieldId + linkPosition 取交集）
        Set<String> commonFieldKeys = null;
        for (List<FieldConfig> fields : cardTypeFieldsMap.values()) {
            Set<String> fieldKeys = fields.stream()
                    .map(this::buildFieldKey)
                    .collect(Collectors.toSet());
            if (commonFieldKeys == null) {
                commonFieldKeys = new HashSet<>(fieldKeys);
            } else {
                commonFieldKeys.retainAll(fieldKeys);
            }
        }

        if (commonFieldKeys == null || commonFieldKeys.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 从第一个卡片类型获取共同属性的配置
        List<FieldConfig> firstCardTypeFields = cardTypeFieldsMap.values().iterator().next();
        Set<String> finalCommonFieldKeys = commonFieldKeys;
        List<FieldOption> options = firstCardTypeFields.stream()
                .filter(f -> finalCommonFieldKeys.contains(buildFieldKey(f)))
                .map(this::toFieldOption)
                .collect(Collectors.toList());

        return Result.success(options);
    }

    /**
     * 获取多个卡片类型的共同属性配置
     * <p>
     * 传入多个卡片类型ID，返回它们的共同属性配置（交集）。
     * 支持按属性类型过滤。
     *
     * @param request 请求参数，包含卡片类型ID列表和可选的属性类型过滤
     * @return 共同属性配置响应
     */
    public Result<CommonFieldOptionResponse> getCommonFieldConfigs(CommonFieldConfigRequest request) {
        List<String> cardTypeIds = request.getCardTypeIds();
        List<String> fieldTypes = request.getFieldTypes();

        if (cardTypeIds == null || cardTypeIds.isEmpty()) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "卡片类型ID列表不能为空");
        }

        // 获取每个卡片类型的属性配置
        Map<String, List<FieldConfig>> cardTypeFieldsMap = new HashMap<>();
        for (String cardTypeId : cardTypeIds) {
            Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId);
            if (!result.isSuccess()) {
                return Result.failure(result.getCode(), result.getMessage());
            }
            cardTypeFieldsMap.put(cardTypeId, result.getData().getFields());
        }

        // 计算共同属性（按 fieldId + linkPosition 取交集）
        // 对于关联属性，需要同时考虑 fieldId 和 linkPosition
        Set<String> commonFieldKeys = null;
        for (List<FieldConfig> fields : cardTypeFieldsMap.values()) {
            Set<String> fieldKeys = fields.stream()
                    .map(this::buildFieldKey)
                    .collect(Collectors.toSet());
            if (commonFieldKeys == null) {
                commonFieldKeys = new HashSet<>(fieldKeys);
            } else {
                commonFieldKeys.retainAll(fieldKeys);
            }
        }

        if (commonFieldKeys == null || commonFieldKeys.isEmpty()) {
            return Result.success(CommonFieldOptionResponse.builder()
                    .fields(Collections.emptyList())
                    .build());
        }

        // 从第一个卡片类型获取共同属性的配置（作为参考）
        List<FieldConfig> firstCardTypeFields = cardTypeFieldsMap.get(cardTypeIds.get(0));
        Set<String> finalCommonFieldKeys = commonFieldKeys;
        List<FieldConfig> commonFields = firstCardTypeFields.stream()
                .filter(f -> finalCommonFieldKeys.contains(buildFieldKey(f)))
                .collect(Collectors.toList());

        // 按属性类型过滤
        if (fieldTypes != null && !fieldTypes.isEmpty()) {
            Set<String> fieldTypeSet = new HashSet<>(fieldTypes);
            commonFields = commonFields.stream()
                    .filter(f -> fieldTypeSet.contains(getFieldType(f)))
                    .collect(Collectors.toList());
        }

        // 转换为 FieldOption
        List<FieldOption> summaryFields = commonFields.stream()
                .map(this::toFieldOption)
                .collect(Collectors.toList());

        return Result.success(CommonFieldOptionResponse.builder()
                .fields(summaryFields)
                .build());
    }

    /**
     * 构建属性的唯一标识（关联属性需要包含方向）
     */
    private String buildFieldKey(FieldConfig config) {
        if (config instanceof LinkFieldConfig linkConfig) {
            return config.getFieldId().value() + ":" + linkConfig.getLinkPosition().name();
        }
        return config.getFieldId().value();
    }

    /**
     * 将 FieldConfig 转换为 FieldOption
     * <p>
     * 对于关联属性，id 字段格式为 "{linkTypeId}:{SOURCE|TARGET}"，已包含方向信息。
     * 对于枚举属性，会填充 enumItems 字段。
     * 对于关联属性，会填充 targetCardTypeIds 字段。
     */
    private FieldOption toFieldOption(FieldConfig config) {
        FieldOption.FieldOptionBuilder builder = FieldOption.builder()
                .id(config.getFieldId().value())
                .name(config.getName())
                .fieldType(getFieldType(config))
                .sortOrder(config.getSortOrder())
                .required(Boolean.TRUE.equals(config.getRequired()))
                .code(config.getCode())
                .systemField(config.isSystemField());

        // 枚举属性填充 enumOptions
        if (config instanceof EnumFieldConfig enumConfig && enumConfig.getOptions() != null) {
            List<EnumOptionDTO> enumOptions = enumConfig.getOptions().stream()
                    .filter(EnumFieldConfig.EnumOptionDefinition::enabled)
                    .map(opt -> EnumOptionDTO.builder()
                            .id(opt.id())
                            .label(opt.label())
                            .color(opt.color())
                            .enabled(opt.enabled())
                            .build())
                    .collect(Collectors.toList());
            builder.enumOptions(enumOptions);
        }

        // 架构属性填充 structureId
        if (config instanceof StructureFieldConfig structureConfig && structureConfig.getStructureId() != null) {
            builder.structureId(structureConfig.getStructureId().value());
        }

        // 关联属性填充 targetCardTypeIds 和 multiple
        if (config instanceof LinkFieldConfig linkConfig) {
            LinkFieldId linkFieldId = new LinkFieldId(linkConfig.getFieldId().value());
            List<CardTypeId> targetCardTypeIds = linkFieldHelper.getTargetCardTypeIds(linkFieldId);
            if (!targetCardTypeIds.isEmpty()) {
                builder.targetCardTypeIds(
                        targetCardTypeIds.stream()
                                .map(CardTypeId::value)
                                .collect(Collectors.toList())
                );
            }
            builder.multiple(linkConfig.getMultiple());
        }

        return builder.build();
    }

    /**
     * 获取属性配置的类型标识
     */
    private String getFieldType(FieldConfig config) {
        if (config instanceof SingleLineTextFieldConfig) return "SINGLE_LINE_TEXT";
        if (config instanceof MultiLineTextFieldConfig) return "MULTI_LINE_TEXT";
        if (config instanceof MarkdownFieldConfig) return "MARKDOWN";
        if (config instanceof NumberFieldConfig) return "NUMBER";
        if (config instanceof DateFieldConfig) return "DATE";
        if (config instanceof EnumFieldConfig) return "ENUM";
        if (config instanceof AttachmentFieldConfig) return "ATTACHMENT";
        if (config instanceof WebUrlFieldConfig) return "WEB_URL";
        if (config instanceof StructureFieldConfig) return "STRUCTURE";
        if (config instanceof LinkFieldConfig) return "LINK";
        return "UNKNOWN";
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
    public Result<MatchingLinkFieldsResponse> getMatchingLinkFields(MatchingLinkFieldsRequest request) {
        List<String> sourceCardTypeIds = request.getSourceCardTypeIds();
        List<String> targetCardTypeIds = request.getTargetCardTypeIds();

        if (sourceCardTypeIds == null || sourceCardTypeIds.isEmpty()) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "源卡片类型ID列表不能为空");
        }
        if (targetCardTypeIds == null || targetCardTypeIds.isEmpty()) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "目标卡片类型ID列表不能为空");
        }

        // 获取源卡片类型的所有关联属性（当前层级）
        Set<String> sourceLinkKeys = new HashSet<>();
        Map<String, LinkFieldConfig> sourceLinkFieldsMap = new HashMap<>();
        for (String cardTypeId : sourceCardTypeIds) {
            //TODO 应该提供一个只查询LinkFieldConfig的接口，这样性能更好，避免在外面再过滤
            Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId);
            if (!result.isSuccess()) {
                return Result.failure(result.getCode(), result.getMessage());
            }
            List<FieldConfig> fields = result.getData().getFields();
            for (FieldConfig field : fields) {
                if (field instanceof LinkFieldConfig linkConfig) {
                    String key = linkConfig.getLinkTypeId().value() + ":" + linkConfig.getLinkPosition().name();
                    sourceLinkKeys.add(key);
                    sourceLinkFieldsMap.putIfAbsent(key, linkConfig);
                }
            }
        }

        // 获取目标卡片类型的所有关联属性（父层级）
        Set<String> targetLinkKeys = new HashSet<>();
        for (String cardTypeId : targetCardTypeIds) {
            Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId);
            if (!result.isSuccess()) {
                return Result.failure(result.getCode(), result.getMessage());
            }
            List<FieldConfig> fields = result.getData().getFields();
            for (FieldConfig field : fields) {
                if (field instanceof LinkFieldConfig linkConfig) {
                    String key = linkConfig.getLinkTypeId().value() + ":" + linkConfig.getLinkPosition().name();
                    targetLinkKeys.add(key);
                }
            }
        }

        // 匹配：找到 linkTypeId 相同但 position 相反的关联属性
        // 返回所有匹配的关联属性，包含 multiple 信息，由客户端根据场景决定是否使用
        List<MatchingLinkFieldDTO> matchingFields = new ArrayList<>();
        for (Map.Entry<String, LinkFieldConfig> entry : sourceLinkFieldsMap.entrySet()) {
            LinkFieldConfig sourceLink = entry.getValue();
            String linkTypeId = sourceLink.getLinkTypeId().value();
            LinkPosition sourcePosition = sourceLink.getLinkPosition();

            // 获取相反的 position
            LinkPosition oppositePosition = sourcePosition == LinkPosition.SOURCE
                    ? LinkPosition.TARGET
                    : LinkPosition.SOURCE;

            String targetKey = linkTypeId + ":" + oppositePosition.name();

            // 如果目标侧存在相反方向的关联属性，则匹配成功
            if (targetLinkKeys.contains(targetKey)) {
                matchingFields.add(toMatchingLinkFieldDTO(sourceLink));
            }
        }

        return Result.success(MatchingLinkFieldsResponse.builder()
                .fields(matchingFields)
                .build());
    }

    /**
     * 将 LinkFieldConfig 转换为 MatchingLinkFieldDTO
     */
    private MatchingLinkFieldDTO toMatchingLinkFieldDTO(LinkFieldConfig config) {
        // 获取属性名称：优先使用 config.getName()，如果为空则从 LinkType 获取
        String name = config.getName();
        if (name == null || name.isBlank()) {
            LinkFieldId linkFieldId = new LinkFieldId(config.getFieldId().value());
            name = linkFieldHelper.getDisplayName(linkFieldId);
        }

        return MatchingLinkFieldDTO.builder()
                .id(config.getId().value())
                .name(name)
                .linkTypeId(config.getLinkTypeId().value())
                .linkPosition(config.getLinkPosition())
                .multiple(config.getMultiple())
                .code(config.getCode())
                .systemField(config.isSystemField())
                .sortOrder(config.getSortOrder())
                .build();
    }
}
