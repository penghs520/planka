package dev.planka.view.executor;

import dev.planka.api.card.CardServiceClient;
import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.card.renderconfig.*;
import dev.planka.api.card.request.CardPageQueryRequest;
import dev.planka.api.schema.dto.inheritance.FieldConfigListWithSource;
import dev.planka.api.schema.service.FieldConfigQueryService;
import dev.planka.api.view.request.ViewDataRequest;
import dev.planka.api.view.response.ColumnMeta;
import dev.planka.api.view.response.GroupedCardData;
import dev.planka.api.view.response.ListViewDataResponse;
import dev.planka.api.view.response.PageInfo;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.BuiltinField;
import dev.planka.domain.schema.definition.fieldconfig.FieldType;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.domain.schema.definition.stream.StatusConfig;
import dev.planka.domain.schema.definition.stream.StepConfig;
import dev.planka.domain.schema.definition.stream.StepStatusKind;
import dev.planka.domain.schema.definition.stream.ValueStreamDefinition;
import dev.planka.domain.schema.definition.view.ListViewDefinition;
import dev.planka.infra.cache.schema.query.ValueStreamCacheQuery;
import dev.planka.view.converter.ViewToQueryConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 列表视图执行器
 * <p>
 * 负责执行列表视图的数据查询，包括：
 * - 普通列表查询（分页）
 * - 分组查询（按指定字段分组）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ListViewExecutor implements ViewExecutor<ListViewDefinition, ListViewDataResponse> {

    private static final String VIEW_TYPE = "LIST";
    private static final String TITLE_FIELD_ID = "title";
    private static final String TITLE_FIELD_NAME = "标题";

    private final CardServiceClient cardServiceClient;
    private final FieldConfigQueryService fieldConfigQueryService;
    private final ViewToQueryConverter queryConverter;
    private final ValueStreamCacheQuery valueStreamCacheQuery;

    @Override
    public String getViewType() {
        return VIEW_TYPE;
    }

    @Override
    public ListViewDataResponse execute(ListViewDefinition viewDef,
                                         ViewDataRequest request,
                                         String operatorId) {
        // 获取字段配置映射
        Map<String, FieldConfig> fieldConfigMap = getFieldConfigMap(viewDef.getCardTypeId().value());

        // 构建列元数据
        List<ColumnMeta> columns = buildColumns(viewDef, fieldConfigMap);

        // 获取价值流状态选项（用于 $statusId 字段显示和编辑）
        List<ListViewDataResponse.StatusOption> statusOptions = getStatusOptions(viewDef.getCardTypeId());

        // 判断是否分组查询
        ListViewDataResponse response;
        if (viewDef.getGroupBy() != null && !viewDef.getGroupBy().isEmpty()) {
            response = executeGroupedQuery(viewDef, request, operatorId, columns);
        } else {
            // 普通分页查询
            response = executeFlatQuery(viewDef, request, operatorId, columns);
        }

        // 设置状态选项
        response.setStatusOptions(statusOptions);
        return response;
    }

    /**
     * 获取字段配置映射
     */
    private Map<String, FieldConfig> getFieldConfigMap(String cardTypeId) {
        Map<String, FieldConfig> fieldConfigMap = new HashMap<>();

        try {
            Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId);
            if (result.isSuccess() && result.getData() != null) {
                for (FieldConfig fieldConfig : result.getData().getFields()) {
                    fieldConfigMap.put(fieldConfig.getFieldId().value(), fieldConfig);
                }
            }
        } catch (Exception e) {
            log.warn("获取字段配置映射失败: {}", e.getMessage());
        }

        return fieldConfigMap;
    }

    /**
     * 执行普通分页查询
     */
    private ListViewDataResponse executeFlatQuery(ListViewDefinition viewDef,
                                                   ViewDataRequest request,
                                                   String operatorId,
                                                   List<ColumnMeta> columns) {
        // 1. 转换为卡片查询请求
        CardPageQueryRequest queryRequest = queryConverter.convert(viewDef, request, operatorId);

        // 2. 调用 card-service 查询
        Result<PageResult<CardDTO>> result = cardServiceClient.pageQuery(operatorId, queryRequest);

        if (!result.isSuccess()) {
            log.error("查询卡片数据失败: {}", result.getMessage());
            throw new RuntimeException("查询卡片数据失败: " + result.getMessage());
        }

        PageResult<CardDTO> pageResult = result.getData();

        // 3. 构建响应
        PageInfo pageInfo = PageInfo.of(
                pageResult.getPage(),
                pageResult.getSize(),
                pageResult.getTotal()
        );

        return ListViewDataResponse.flat(
                viewDef.getId().value(),
                viewDef.getName(),
                viewDef.getCardTypeId().value(),
                columns,
                pageResult.getContent(),
                pageInfo
        );
    }

    /**
     * 执行分组查询
     * <p>
     * 分组查询采用懒加载策略：
     * - 首次只返回分组摘要（分组值 + 数量）
     * - 用户展开分组时再加载具体数据
     */
    private ListViewDataResponse executeGroupedQuery(ListViewDefinition viewDef,
                                                      ViewDataRequest request,
                                                      String operatorId,
                                                      List<ColumnMeta> columns) {
        // 如果指定了 groupValue，查询该分组的具体数据
        if (request != null && request.getGroupValue() != null) {
            return executeGroupDataQuery(viewDef, request, operatorId, columns);
        }

        // 否则返回分组摘要
        // TODO: 实现分组摘要查询，目前先返回空分组列表
        List<GroupedCardData> groups = new ArrayList<>();

        return ListViewDataResponse.grouped(
                viewDef.getId().value(),
                viewDef.getName(),
                viewDef.getCardTypeId().value(),
                columns,
                groups
        );
    }

    /**
     * 查询指定分组的数据
     */
    private ListViewDataResponse executeGroupDataQuery(ListViewDefinition viewDef,
                                                        ViewDataRequest request,
                                                        String operatorId,
                                                        List<ColumnMeta> columns) {
        // TODO: 构建分组条件并查询
        // 目前先返回空结果
        return ListViewDataResponse.flat(
                viewDef.getId().value(),
                viewDef.getName(),
                viewDef.getCardTypeId().value(),
                columns,
                new ArrayList<>(),
                PageInfo.of(0, 20, 0)
        );
    }

    /**
     * 构建列元数据
     * <p>
     * 始终包含"标题"列作为第一列
     */
    private List<ColumnMeta> buildColumns(ListViewDefinition viewDef, Map<String, FieldConfig> fieldConfigMap) {
        List<ColumnMeta> columns = new ArrayList<>();

        // 始终添加标题列作为第一列
        columns.add(buildTitleColumn());

        // 如果没有配置额外列，返回只有标题的列表
        if (viewDef.getColumnConfigs() == null || viewDef.getColumnConfigs().isEmpty()) {
            return columns;
        }

        // 按配置构建额外列（跳过标题列，因为已经添加过了）
        for (ListViewDefinition.ColumnConfig config : viewDef.getColumnConfigs()) {
            if (config.isVisible() && !TITLE_FIELD_ID.equals(config.getFieldId())) {
                columns.add(toColumnMeta(config, fieldConfigMap));
            }
        }

        return columns;
    }

    /**
     * 构建标题列（内置列，始终可编辑）
     */
    private ColumnMeta buildTitleColumn() {
        return ColumnMeta.builder()
                .fieldId(TITLE_FIELD_ID)
                .title(TITLE_FIELD_NAME)
                .fieldType(FieldType.TEXT.getCode())
                .width(200)
                .frozen(true)
                .visible(true)
                .sortable(true)
                .editable(true)
                .required(true)
                .renderConfig(TextRenderConfig.builder()
                        .maxLength(500)
                        .multiLine(false)
                        .build())
                .build();
    }

    /**
     * 将列配置转换为列元数据
     */
    private ColumnMeta toColumnMeta(ListViewDefinition.ColumnConfig config, Map<String, FieldConfig> fieldConfigMap) {
        String fieldId = config.getFieldId();

        // 处理内置字段
        if (BuiltinField.isBuiltinField(fieldId)) {
            return buildBuiltinColumnMeta(config);
        }

        // 处理自定义字段
        FieldConfig fieldConfig = fieldConfigMap.get(fieldId);

        // 获取字段名称，如果找不到则使用 fieldId
        String fieldName = fieldConfig != null ? fieldConfig.getName() : fieldId;

        // 计算是否可编辑：非只读且非系统字段
        boolean editable = fieldConfig != null
                && !Boolean.TRUE.equals(fieldConfig.getReadOnly())
                && !fieldConfig.isSystemField();

        // 获取是否必填
        boolean required = fieldConfig != null && Boolean.TRUE.equals(fieldConfig.getRequired());

        // 获取字段类型
        String fieldType = resolveFieldType(fieldConfig);

        // 使用公共转换器构建渲染配置
        FieldRenderConfig renderConfig = FieldRenderConfigConverter.convert(fieldConfig);

        return ColumnMeta.builder()
                .fieldId(fieldId)
                .title(fieldName)
                .fieldType(fieldType)
                .width(config.getWidth() != null ? config.getWidth() : 150)
                .frozen(config.isFrozen())
                .visible(config.isVisible())
                .sortable(true)
                .editable(editable)
                .required(required)
                .builtin(false)
                .renderConfig(renderConfig)
                .build();
    }

    /**
     * 构建内置字段的列元数据
     */
    private ColumnMeta buildBuiltinColumnMeta(ListViewDefinition.ColumnConfig config) {
        String fieldId = config.getFieldId();
        BuiltinField builtinField = BuiltinField.fromFieldId(fieldId);

        if (builtinField == null) {
            // 未知的内置字段，返回默认配置
            return ColumnMeta.builder()
                    .fieldId(fieldId)
                    .title(fieldId)
                    .fieldType(FieldType.TEXT.getCode())
                    .width(config.getWidth() != null ? config.getWidth() : 150)
                    .frozen(config.isFrozen())
                    .visible(config.isVisible())
                    .sortable(true)
                    .editable(false)
                    .required(false)
                    .builtin(true)
                    .build();
        }

        return ColumnMeta.builder()
                .fieldId(fieldId)
                .title(builtinField.getDisplayName())
                .fieldType(builtinField.getFieldType().getCode())
                .width(config.getWidth() != null ? config.getWidth() : 150)
                .frozen(config.isFrozen())
                .visible(config.isVisible())
                .sortable(builtinField.isSortable())
                .editable(builtinField.isEditable())
                .required(false)
                .builtin(true)
                .renderConfig(buildBuiltinRenderConfig(builtinField))
                .build();
    }

    /**
     * 构建内置字段的渲染配置
     */
    private FieldRenderConfig buildBuiltinRenderConfig(BuiltinField field) {
        return switch (field) {
            case CREATED_AT, UPDATED_AT, ARCHIVED_AT, DISCARDED_AT ->
                    DateRenderConfig.builder().dateFormat("DATETIME").build();
            case CARD_STYLE ->
                    EnumRenderConfig.builder()
                            .multiSelect(false)
                            .options(List.of(
                                    EnumOptionDTO.builder().id("ACTIVE").label("进行中").color("#52c41a").enabled(true).build(),
                                    EnumOptionDTO.builder().id("ARCHIVED").label("已归档").color("#8c8c8c").enabled(true).build(),
                                    EnumOptionDTO.builder().id("DISCARDED").label("已丢弃").color("#ff4d4f").enabled(true).build()
                            ))
                            .build();
            case STATUS_ID ->
                    // 状态选项通过 statusOptions 字段提供，这里返回空选项
                    EnumRenderConfig.builder().multiSelect(false).options(List.of()).build();
            case CODE ->
                    TextRenderConfig.builder().multiLine(false).build();
        };
    }

    /**
     * 解析字段类型
     */
    private String resolveFieldType(FieldConfig fieldConfig) {
        if (fieldConfig == null) {
            return FieldType.TEXT.getCode();
        }
        FieldType fieldType = fieldConfig.getFieldType();
        return fieldType != null ? fieldType.getCode() : FieldType.TEXT.getCode();
    }

    /**
     * 获取价值流状态选项
     * <p>
     * 从价值流定义中提取所有状态，转换为 StatusOption 列表
     *
     * @param cardTypeId 卡片类型ID
     * @return 状态选项列表，如果获取失败则返回空列表
     */
    private List<ListViewDataResponse.StatusOption> getStatusOptions(CardTypeId cardTypeId) {
        try {
            Optional<ValueStreamDefinition> result = valueStreamCacheQuery.getValueStreamByCardTypeId(cardTypeId);
            if (result.isEmpty()) {
                log.debug("获取价值流定义失败或为空: cardTypeId={}", cardTypeId);
                return List.of();
            }

            ValueStreamDefinition valueStream = result.get();
            if (valueStream.getStepList() == null || valueStream.getStepList().isEmpty()) {
                return List.of();
            }

            List<ListViewDataResponse.StatusOption> options = new ArrayList<>();
            for (StepConfig step : valueStream.getStepList()) {
                if (step.getStatusList() != null) {
                    // 获取阶段类型
                    String stepKind = step.getKind() != null ? step.getKind().name() : StepStatusKind.IN_PROGRESS.name();
                    for (StatusConfig status : step.getStatusList()) {
                        options.add(new ListViewDataResponse.StatusOption(
                                status.getId().value(),
                                status.getName(),
                                stepKind
                        ));
                    }
                }
            }
            return options;
        } catch (Exception e) {
            log.warn("获取价值流状态选项失败: cardTypeId={}, error={}", cardTypeId, e.getMessage());
            return List.of();
        }
    }
}
