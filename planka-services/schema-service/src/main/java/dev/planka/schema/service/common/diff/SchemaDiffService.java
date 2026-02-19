package dev.planka.schema.service.common.diff;

import dev.planka.domain.schema.changelog.ChangeDetail;
import dev.planka.domain.schema.changelog.FieldChange;
import dev.planka.domain.schema.changelog.SemanticChange;
import dev.planka.domain.schema.changelog.SemanticChangeType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Schema差异比较服务
 * <p>
 * 负责比较Schema快照并生成结构化变更详情。
 * 通过策略模式支持不同Schema类型的特化比较逻辑。
 */
@Slf4j
@Service
public class SchemaDiffService {

    private final ObjectMapper objectMapper;
    private final List<SchemaDiffStrategy> strategies;

    public SchemaDiffService(ObjectMapper objectMapper, List<SchemaDiffStrategy> strategies) {
        this.objectMapper = objectMapper;
        // 按优先级排序策略
        this.strategies = new ArrayList<>(strategies);
        this.strategies.sort(Comparator.comparingInt(SchemaDiffStrategy::getPriority));
    }

    /**
     * 比较两个Schema快照，生成变更详情
     *
     * @param beforeSnapshot 变更前的JSON快照
     * @param afterSnapshot  变更后的JSON快照
     * @return 变更详情，如果无法比较则返回null
     */
    public ChangeDetail diff(@Nullable String beforeSnapshot, @Nullable String afterSnapshot) {
        try {
            // 处理创建场景
            if (beforeSnapshot == null && afterSnapshot != null) {
                return createAddDetail(afterSnapshot);
            }

            // 处理删除场景
            if (beforeSnapshot != null && afterSnapshot == null) {
                return createDeleteDetail(beforeSnapshot);
            }

            // 两者都为空
            if (beforeSnapshot == null) {
                return null;
            }

            // 解析JSON
            JsonNode beforeNode = objectMapper.readTree(beforeSnapshot);
            JsonNode afterNode = objectMapper.readTree(afterSnapshot);

            // 获取schemaSubType并查找策略
            String schemaSubType = afterNode.path("schemaSubType").asText(null);
            SchemaDiffStrategy strategy = findStrategy(schemaSubType);

            if (strategy != null) {
                return strategy.diff(beforeNode, afterNode);
            }

            // 没有找到策略，返回基础变更信息
            return createBasicUpdateDetail(afterNode);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse snapshot JSON for diff", e);
            return null;
        }
    }

    /**
     * 生成人可读的变更摘要文本
     *
     * @param detail 变更详情
     * @return 人可读的摘要文本
     */
    public String generateSummaryText(ChangeDetail detail) {
        if (detail == null || !detail.hasChanges()) {
            return "无变更";
        }

        List<String> parts = new ArrayList<>();

        // 格式化字段变更
        if (detail.getChanges() != null) {
            for (FieldChange change : detail.getChanges()) {
                parts.add(formatFieldChange(change));
            }
        }

        // 格式化语义变更
        if (detail.getSemanticChanges() != null) {
            for (SemanticChange semantic : detail.getSemanticChanges()) {
                parts.add(formatSemanticChange(semantic));
            }
        }

        return String.join("；", parts);
    }

    /**
     * 将变更详情序列化为JSON字符串
     */
    public String serializeChangeDetail(ChangeDetail detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ChangeDetail", e);
            return null;
        }
    }

    /**
     * 从JSON字符串反序列化变更详情
     */
    public ChangeDetail deserializeChangeDetail(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ChangeDetail.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ChangeDetail", e);
            return null;
        }
    }

    private SchemaDiffStrategy findStrategy(String schemaSubType) {
        if (schemaSubType == null) {
            return null;
        }
        return strategies.stream()
                .filter(s -> s.supports(schemaSubType))
                .findFirst()
                .orElse(null);
    }

    private ChangeDetail createAddDetail(String afterSnapshot) throws JsonProcessingException {
        JsonNode afterNode = objectMapper.readTree(afterSnapshot);
        String schemaType = afterNode.path("schemaType").asText("UNKNOWN");
        String schemaSubType = afterNode.path("schemaSubType").asText("UNKNOWN");
        String name = afterNode.path("name").asText("");

        ChangeDetail detail = ChangeDetail.forCreate(schemaType, schemaSubType);
        if (!name.isEmpty()) {
            detail.addFieldChange(FieldChange.added("name", "名称", name, "string"));
        }
        return detail;
    }

    private ChangeDetail createDeleteDetail(String beforeSnapshot) throws JsonProcessingException {
        JsonNode beforeNode = objectMapper.readTree(beforeSnapshot);
        String schemaType = beforeNode.path("schemaType").asText("UNKNOWN");
        String schemaSubType = beforeNode.path("schemaSubType").asText("UNKNOWN");

        return ChangeDetail.forDelete(schemaType, schemaSubType);
    }

    private ChangeDetail createBasicUpdateDetail(JsonNode afterNode) {
        String schemaType = afterNode.path("schemaType").asText("UNKNOWN");
        String schemaSubType = afterNode.path("schemaSubType").asText("UNKNOWN");

        return ChangeDetail.forUpdate(schemaType, schemaSubType);
    }

    private String formatFieldChange(FieldChange change) {
        String label = change.getFieldLabel();
        return switch (change.getChangeType()) {
            case ADDED -> label + ": 设置为 '" + formatValue(change.getNewValue()) + "'";
            case MODIFIED -> label + ": '" + formatValue(change.getOldValue()) + "' → '" + formatValue(change.getNewValue()) + "'";
            case REMOVED -> label + ": 已清除";
        };
    }

    private String formatSemanticChange(SemanticChange change) {
        String categoryLabel = getCategoryLabel(change.getCategory());
        String baseSummary = switch (change.getOperation()) {
            case ADDED -> "添加了" + categoryLabel + " '" + change.getTargetName() + "'";
            case REMOVED -> "移除了" + categoryLabel + " '" + change.getTargetName() + "'";
            case MODIFIED -> "修改了" + categoryLabel + " '" + change.getTargetName() + "'";
            case REORDERED -> "重排序了" + categoryLabel;
        };

        // 如果有详细字段变更，追加到摘要中
        if (change.getOperation() == SemanticChangeType.MODIFIED
                && change.getDetails() != null && !change.getDetails().isEmpty()) {
            List<String> detailParts = new ArrayList<>();
            for (FieldChange detail : change.getDetails()) {
                detailParts.add(formatFieldChange(detail));
            }
            return baseSummary + "(" + String.join(", ", detailParts) + ")";
        }

        return baseSummary;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List<?> list) {
            return list.size() + "项";
        }
        String str = value.toString();
        // 截断过长的值
        if (str.length() > 50) {
            return str.substring(0, 47) + "...";
        }
        return str;
    }

    private String getCategoryLabel(String category) {
        if (category == null) {
            return "项目";
        }
        return switch (category) {
            // 枚举属性相关
            case "ENUM_ITEM", "ENUM_OPTION" -> "枚举选项";

            // 视图相关
            case "COLUMN_CONFIG" -> "列配置";
            case "SORT_FIELD" -> "排序字段";
            case "PAGE_CONFIG" -> "分页配置";
            case "FILTER_CONDITION" -> "过滤条件";

            // 卡片类型相关
            case "PARENT_TYPE" -> "继承类型";
            case "QUICK_CREATE_CONFIG" -> "快速创建配置";
            case "PERMISSION_CONFIG" -> "权限配置";
            case "CARD_OPERATION_PERMISSION" -> "卡片操作权限";
            case "FIELD_PERMISSION" -> "属性权限";
            case "ATTACHMENT_PERMISSION" -> "附件权限";

            // 关联类型相关
            case "SOURCE_CARD_TYPE" -> "源端卡片类型";
            case "TARGET_CARD_TYPE" -> "目标端卡片类型";

            default -> "项目";
        };
    }
}
