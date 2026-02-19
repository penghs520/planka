package dev.planka.extension.history.service;

import dev.planka.api.card.CardServiceClient;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.history.HistoryArgument;
import dev.planka.domain.history.HistoryMessage;
import dev.planka.domain.link.LinkFieldIdUtils;
import dev.planka.extension.history.vo.HistoryArgumentVO;
import dev.planka.extension.history.vo.HistoryMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 历史消息参数解析器
 * <p>
 * 负责将存储的 ID 解析为显示名称。
 * 优先从 Schema 获取最新名称，获取不到则使用存储的备份名称并标记已删除。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryArgumentResolver {

    private final SchemaNameCache schemaNameCache;
    private final CardServiceClient cardServiceClient;

    /**
     * 解析历史消息，填充显示名称
     */
    public HistoryMessageVO resolve(HistoryMessage message, String cardTypeId) {
        if (message == null) {
            return null;
        }

        List<HistoryArgument> args = message.getArgs();
        if (args == null || args.isEmpty()) {
            return HistoryMessageVO.builder()
                    .messageKey(message.getMessageKey())
                    .args(List.of())
                    .build();
        }

        // 收集需要解析的 ID
        ResolveContext context = collectIds(args);

        // 批量查询名称
        resolveNames(context, cardTypeId);

        // 转换为 VO
        List<HistoryArgumentVO> voList = args.stream()
                .map(arg -> toVO(arg, context))
                .collect(Collectors.toList());

        return HistoryMessageVO.builder()
                .messageKey(message.getMessageKey())
                .args(voList)
                .build();
    }

    /**
     * 批量解析历史消息
     */
    public List<HistoryMessageVO> resolveBatch(List<HistoryMessage> messages, String cardTypeId) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        // 收集所有消息中需要解析的 ID
        ResolveContext context = new ResolveContext();
        for (HistoryMessage message : messages) {
            if (message != null && message.getArgs() != null) {
                collectIds(message.getArgs(), context);
            }
        }

        // 批量查询名称
        resolveNames(context, cardTypeId);

        // 转换为 VO
        return messages.stream()
                .map(msg -> {
                    if (msg == null) {
                        return null;
                    }
                    List<HistoryArgumentVO> voList = msg.getArgs() == null ? List.of() :
                            msg.getArgs().stream()
                                    .map(arg -> toVO(arg, context))
                                    .collect(Collectors.toList());
                    return HistoryMessageVO.builder()
                            .messageKey(msg.getMessageKey())
                            .args(voList)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private ResolveContext collectIds(List<HistoryArgument> args) {
        ResolveContext context = new ResolveContext();
        collectIds(args, context);
        return context;
    }

    private void collectIds(List<HistoryArgument> args, ResolveContext context) {
        for (HistoryArgument arg : args) {
            if (arg instanceof HistoryArgument.OperateFieldArg operateFieldArg) {
                String fieldId = operateFieldArg.fieldId();
                // 区分普通属性和关联属性
                if (LinkFieldIdUtils.isValidFormat(fieldId)) {
                    context.linkFieldIds.add(fieldId);
                } else {
                    context.fieldIds.add(fieldId);
                }
            } else if (arg instanceof HistoryArgument.FieldValueArg fieldValueArg) {
                String fieldId = fieldValueArg.fieldId();
                // 区分普通属性和关联属性
                if (fieldValueArg instanceof HistoryArgument.LinkFieldValue linkValue) {
                    context.linkFieldIds.add(fieldId);
                    // 收集关联卡片 ID
                    if (linkValue.cards() != null) {
                        for (HistoryArgument.LinkFieldValue.LinkedCardRef ref : linkValue.cards()) {
                            context.cardIds.add(ref.cardId());
                        }
                    }
                } else {
                    context.fieldIds.add(fieldId);
                    // 收集枚举选项 ID
                    if (fieldValueArg instanceof HistoryArgument.EnumFieldValue enumValue && enumValue.values() != null) {
                        for (HistoryArgument.EnumFieldValue.EnumOption option : enumValue.values()) {
                            context.enumOptionIds.computeIfAbsent(fieldId, k -> new HashSet<>())
                                    .add(option.optionId());
                        }
                    }
                }
            } else if (arg instanceof HistoryArgument.StatusArg statusArg) {
                context.statusIds.add(statusArg.statusId());
            }
        }
    }

    private void resolveNames(ResolveContext context, String cardTypeId) {
        // 解析属性名称和枚举选项名称
        if (!context.fieldIds.isEmpty()) {
            Map<String, SchemaNameCache.SchemaNameInfo> fieldNames = schemaNameCache.getFieldNames(context.fieldIds);
            context.fieldNameInfoMap.putAll(fieldNames);

            for (Map.Entry<String, Set<String>> entry : context.enumOptionIds.entrySet()) {
                String fieldId = entry.getKey();
                Set<String> optionIds = entry.getValue();
                // 根据属性是否删除来决定查询方式
                SchemaNameCache.SchemaNameInfo fieldInfo = fieldNames.get(fieldId);
                boolean fieldDeleted = fieldInfo != null && fieldInfo.deleted();
                Map<String, String> optionNames = schemaNameCache.getEnumOptionNames(fieldId, optionIds, fieldDeleted);
                for (Map.Entry<String, String> optionEntry : optionNames.entrySet()) {
                    context.enumOptionNameMap.put(fieldId + ":" + optionEntry.getKey(), optionEntry.getValue());
                }
            }
        }

        // 解析关联属性名称
        if (!context.linkFieldIds.isEmpty()) {
            Map<String, SchemaNameCache.SchemaNameInfo> linkFieldNames = schemaNameCache.getLinkFieldNames(context.linkFieldIds);
            context.linkFieldNameInfoMap.putAll(linkFieldNames);
        }

        // 解析状态名称
        if (!context.statusIds.isEmpty()) {
            Map<String, String> statusNames = schemaNameCache.getStatusNames(cardTypeId, context.statusIds);
            context.statusNameMap.putAll(statusNames);
        }

        // 解析卡片标题
        if (!context.cardIds.isEmpty()) {
            try {
                Result<Map<String, CardTitle>> result = cardServiceClient.queryCardNames(
                        "system", new ArrayList<>(context.cardIds));
                if (result.isSuccess() && result.getData() != null) {
                    context.cardTitleMap.putAll(result.getData());
                }
            } catch (Exception e) {
                log.error("Failed to query card titles: cardIds={}", context.cardIds, e);
            }
        }
    }

    private HistoryArgumentVO toVO(HistoryArgument arg, ResolveContext context) {
        if (arg instanceof HistoryArgument.TextArg textArg) {
            return HistoryArgumentVO.builder()
                    .type("TEXT")
                    .value(textArg.value())
                    .build();
        } else if (arg instanceof HistoryArgument.TextDiffArg textDiffArg) {
            return convertTextDiffToVO(textDiffArg);
        } else if (arg instanceof HistoryArgument.OperateFieldArg operateFieldArg) {
            String fieldId = operateFieldArg.fieldId();
            SchemaNameCache.SchemaNameInfo nameInfo;
            // 区分普通属性和关联属性
            if (LinkFieldIdUtils.isValidFormat(fieldId)) {
                nameInfo = context.linkFieldNameInfoMap.get(fieldId);
            } else {
                nameInfo = context.fieldNameInfoMap.get(fieldId);
            }
            // 如果 nameInfo 不存在，说明属性确实不存在（被物理删除或从未存在）
            boolean deleted = nameInfo == null || nameInfo.deleted();
            return HistoryArgumentVO.builder()
                    .type("OPERATE_FIELD")
                    .fieldId(fieldId)
                    .fieldName(nameInfo != null ? nameInfo.name() : fieldId)
                    .deleted(deleted)
                    .build();
        } else if (arg instanceof HistoryArgument.FieldValueArg fieldValueArg) {
            return convertFieldValueArgToVO(fieldValueArg, context);
        } else if (arg instanceof HistoryArgument.StatusArg statusArg) {
            String resolvedName = context.statusNameMap.get(statusArg.statusId());
            boolean deleted = resolvedName == null;
            return HistoryArgumentVO.builder()
                    .type("STATUS")
                    .statusId(statusArg.statusId())
                    .statusName(resolvedName != null ? resolvedName : statusArg.statusName())
                    .deleted(deleted)
                    .build();
        }

        return HistoryArgumentVO.builder().type("UNKNOWN").build();
    }

    /**
     * 转换 TextDiffArg 为 VO
     */
    private HistoryArgumentVO convertTextDiffToVO(HistoryArgument.TextDiffArg textDiffArg) {
        List<HistoryArgumentVO.DiffHunkVO> hunkVOs = new ArrayList<>();
        if (textDiffArg.hunks() != null) {
            for (HistoryArgument.TextDiffArg.DiffHunk hunk : textDiffArg.hunks()) {
                List<HistoryArgumentVO.DiffLineVO> lineVOs = new ArrayList<>();
                if (hunk.lines() != null) {
                    for (HistoryArgument.TextDiffArg.DiffLine line : hunk.lines()) {
                        lineVOs.add(new HistoryArgumentVO.DiffLineVO(
                                line.type().name(),
                                line.content()
                        ));
                    }
                }
                hunkVOs.add(new HistoryArgumentVO.DiffHunkVO(
                        hunk.oldStart(),
                        hunk.oldCount(),
                        hunk.newStart(),
                        hunk.newCount(),
                        lineVOs
                ));
            }
        }
        return HistoryArgumentVO.builder()
                .type("TEXT_DIFF")
                .hunks(hunkVOs)
                .build();
    }

    /**
     * 转换 FieldValueArg 多态类型为 VO
     */
    private HistoryArgumentVO convertFieldValueArgToVO(HistoryArgument.FieldValueArg fieldValueArg, ResolveContext context) {
        String fieldId = fieldValueArg.fieldId();
        SchemaNameCache.SchemaNameInfo fieldInfo = context.fieldNameInfoMap.get(fieldId);
        boolean fieldDeleted = fieldInfo == null || fieldInfo.deleted();
        // FieldValueArg 不存储 fieldName，由 OperateFieldArg 提供
        // 如果 Schema 中找不到，使用 fieldId 作为后备显示
        String fieldName = fieldInfo != null ? fieldInfo.name() : fieldId;

        if (fieldValueArg instanceof HistoryArgument.TextFieldValue textValue) {
            return HistoryArgumentVO.builder()
                    .type("FIELD_VALUE_TEXT")
                    .fieldId(fieldId)
                    .fieldName(fieldName)
                    .displayValue(textValue.value())
                    .deleted(fieldDeleted)
                    .build();
        } else if (fieldValueArg instanceof HistoryArgument.NumberFieldValue numberValue) {
            String displayValue = numberValue.value() != null ? numberValue.value().toPlainString() : null;
            return HistoryArgumentVO.builder()
                    .type("FIELD_VALUE_NUMBER")
                    .fieldId(fieldId)
                    .fieldName(fieldName)
                    .displayValue(displayValue)
                    .deleted(fieldDeleted)
                    .build();
        } else if (fieldValueArg instanceof HistoryArgument.DateFieldValue dateValue) {
            String displayValue = dateValue.value() != null ? dateValue.value().toString() : null;
            return HistoryArgumentVO.builder()
                    .type("FIELD_VALUE_DATE")
                    .fieldId(fieldId)
                    .fieldName(fieldName)
                    .displayValue(displayValue)
                    .deleted(fieldDeleted)
                    .build();
        } else if (fieldValueArg instanceof HistoryArgument.DateTimeFieldValue dateTimeValue) {
            String displayValue = dateTimeValue.value() != null ? dateTimeValue.value().toString() : null;
            return HistoryArgumentVO.builder()
                    .type("FIELD_VALUE_DATETIME")
                    .fieldId(fieldId)
                    .fieldName(fieldName)
                    .displayValue(displayValue)
                    .deleted(fieldDeleted)
                    .build();
        } else if (fieldValueArg instanceof HistoryArgument.EnumFieldValue enumValue) {
            List<String> displayValues = new ArrayList<>();
            if (enumValue.values() != null) {
                for (HistoryArgument.EnumFieldValue.EnumOption option : enumValue.values()) {
                    String key = fieldId + ":" + option.optionId();
                    String resolvedName = context.enumOptionNameMap.get(key);
                    displayValues.add(resolvedName != null ? resolvedName : option.optionName());
                }
            }
            return HistoryArgumentVO.builder()
                    .type("FIELD_VALUE_ENUM")
                    .fieldId(fieldId)
                    .fieldName(fieldName)
                    .displayValue(String.join(", ", displayValues))
                    .deleted(fieldDeleted)
                    .build();
        } else if (fieldValueArg instanceof HistoryArgument.StructureFieldValue structureValue) {
            List<String> pathNames = new ArrayList<>();
            if (structureValue.path() != null) {
                for (HistoryArgument.StructureFieldValue.StructureNode node : structureValue.path()) {
                    pathNames.add(node.nodeName());
                }
            }
            return HistoryArgumentVO.builder()
                    .type("FIELD_VALUE_STRUCTURE")
                    .fieldId(fieldId)
                    .fieldName(fieldName)
                    .displayValue(String.join(" / ", pathNames))
                    .deleted(fieldDeleted)
                    .build();
        } else if (fieldValueArg instanceof HistoryArgument.LinkFieldValue linkValue) {
            // 关联属性使用 linkFieldNameInfoMap 解析名称
            SchemaNameCache.SchemaNameInfo linkFieldInfo = context.linkFieldNameInfoMap.get(fieldId);
            boolean linkFieldDeleted = linkFieldInfo == null || linkFieldInfo.deleted();
            String linkFieldName = linkFieldInfo != null ? linkFieldInfo.name() : fieldId;

            List<String> cardTitles = new ArrayList<>();
            List<HistoryArgumentVO.LinkedCardRefVO> cardRefs = new ArrayList<>();
            if (linkValue.cards() != null) {
                for (HistoryArgument.LinkFieldValue.LinkedCardRef ref : linkValue.cards()) {
                    CardTitle cardTitle = context.cardTitleMap.get(ref.cardId());
                    String displayTitle = cardTitle != null ? cardTitle.getDisplayValue() : ref.cardId();
                    cardTitles.add(displayTitle);
                    cardRefs.add(new HistoryArgumentVO.LinkedCardRefVO(
                            ref.cardId(), cardTitle, ref.cardTypeId()));
                }
            }
            return HistoryArgumentVO.builder()
                    .type("FIELD_VALUE_LINK")
                    .fieldId(fieldId)
                    .fieldName(linkFieldName)
                    .displayValue(String.join("、", cardTitles))
                    .cards(cardRefs)
                    .deleted(linkFieldDeleted)
                    .build();
        }

        return HistoryArgumentVO.builder()
                .type("FIELD_VALUE_UNKNOWN")
                .fieldId(fieldId)
                .fieldName(fieldName)
                .deleted(fieldDeleted)
                .build();
    }

    /**
     * 解析上下文
     */
    private static class ResolveContext {
        Set<String> fieldIds = new HashSet<>();
        Set<String> linkFieldIds = new HashSet<>();
        Set<String> statusIds = new HashSet<>();
        Set<String> cardIds = new HashSet<>();
        Map<String, Set<String>> enumOptionIds = new HashMap<>();

        // 属性名称信息（包含删除状态）
        Map<String, SchemaNameCache.SchemaNameInfo> fieldNameInfoMap = new HashMap<>();
        Map<String, SchemaNameCache.SchemaNameInfo> linkFieldNameInfoMap = new HashMap<>();
        Map<String, String> statusNameMap = new HashMap<>();
        Map<String, CardTitle> cardTitleMap = new HashMap<>();
        Map<String, String> enumOptionNameMap = new HashMap<>();
    }
}
