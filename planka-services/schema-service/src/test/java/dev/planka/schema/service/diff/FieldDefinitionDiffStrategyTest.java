package dev.planka.schema.service.diff;

import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.changelog.ChangeDetail;
import dev.planka.domain.schema.changelog.FieldChange;
import dev.planka.domain.schema.changelog.SemanticChange;
import dev.planka.domain.schema.changelog.SemanticChangeType;
import dev.planka.domain.schema.definition.fieldconfig.EnumFieldConfig;
import dev.planka.schema.service.common.diff.FieldDefinitionDiffStrategy;
import dev.planka.schema.service.common.diff.JsonDiffHelper;
import dev.planka.schema.service.common.diff.SchemaDiffService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FieldDefinitionDiffStrategy 单元测试
 */
@DisplayName("属性定义差异比较策略测试")
class FieldDefinitionDiffStrategyTest {

    private FieldDefinitionDiffStrategy strategy;
    private SchemaDiffService diffService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        JsonDiffHelper diffHelper = new JsonDiffHelper();
        strategy = new FieldDefinitionDiffStrategy(diffHelper);
        diffService = new SchemaDiffService(objectMapper, List.of(strategy));
    }

    /**
     * 创建枚举属性配置
     */
    private EnumFieldConfig createEnumField(String id, String name, List<EnumFieldConfig.EnumOptionDefinition> options) {
        EnumFieldConfig field = new EnumFieldConfig(
                new FieldConfigId(id),
                "org-1",
                name,
                null,
                new FieldId("field-" + id),
                false
        );
        field.setOptions(options);
        return field;
    }

    /**
     * 序列化为JSON字符串
     */
    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * 解析JSON为JsonNode
     */
    private JsonNode toJsonNode(Object obj) throws Exception {
        String json = toJson(obj);
        return objectMapper.readTree(json);
    }

    @Nested
    @DisplayName("枚举属性测试")
    class EnumFieldTests {

        @Test
        @DisplayName("检测枚举选项的启用状态变更")
        void diff_enumOptionEnabledChange_detectsChange() throws Exception {
            // 变更前：选项启用
            List<EnumFieldConfig.EnumOptionDefinition> beforeOptions = new ArrayList<>();
            beforeOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "高", "HIGH", true, "#ff0000", 1));
            beforeOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-2", "中", "MEDIUM", true, "#ffff00", 2));
            EnumFieldConfig before = createEnumField("field-1", "优先级", beforeOptions);

            // 变更后：opt-1 禁用
            List<EnumFieldConfig.EnumOptionDefinition> afterOptions = new ArrayList<>();
            afterOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "高", "HIGH", false, "#ff0000", 1));
            afterOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-2", "中", "MEDIUM", true, "#ffff00", 2));
            EnumFieldConfig after = createEnumField("field-1", "优先级", afterOptions);

            JsonNode beforeNode = toJsonNode(before);
            JsonNode afterNode = toJsonNode(after);

            ChangeDetail detail = strategy.diff(beforeNode, afterNode);

            assertThat(detail).isNotNull();
            assertThat(detail.getSemanticChanges()).hasSize(1);

            SemanticChange change = detail.getSemanticChanges().get(0);
            assertThat(change.getCategory()).isEqualTo("ENUM_OPTION");
            assertThat(change.getOperation()).isEqualTo(SemanticChangeType.MODIFIED);
            assertThat(change.getTargetId()).isEqualTo("opt-1");
            assertThat(change.getTargetName()).isEqualTo("高");

            // 验证详细字段变更
            assertThat(change.getDetails()).isNotNull();
            assertThat(change.getDetails()).isNotEmpty();

            // 查找启用状态变更
            FieldChange enabledChange = change.getDetails().stream()
                    .filter(fc -> "enabled".equals(fc.getFieldPath()))
                    .findFirst()
                    .orElse(null);

            assertThat(enabledChange).isNotNull();
            assertThat(enabledChange.getFieldLabel()).isEqualTo("启用状态");

            // 测试生成摘要文本
            String summary = diffService.generateSummaryText(detail);
            System.out.println("摘要: " + summary);
            assertThat(summary).contains("修改了枚举选项");
            assertThat(summary).contains("高");
            assertThat(summary).contains("启用状态");
        }

        @Test
        @DisplayName("检测枚举选项的颜色变更")
        void diff_enumOptionColorChange_detectsChange() throws Exception {
            List<EnumFieldConfig.EnumOptionDefinition> beforeOptions = new ArrayList<>();
            beforeOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "待处理", "TODO", true, "#cccccc", 1));
            EnumFieldConfig before = createEnumField("field-1", "状态", beforeOptions);

            List<EnumFieldConfig.EnumOptionDefinition> afterOptions = new ArrayList<>();
            afterOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "待处理", "TODO", true, "#0000ff", 1));
            EnumFieldConfig after = createEnumField("field-1", "状态", afterOptions);

            JsonNode beforeNode = toJsonNode(before);
            JsonNode afterNode = toJsonNode(after);

            ChangeDetail detail = strategy.diff(beforeNode, afterNode);

            assertThat(detail.getSemanticChanges()).hasSize(1);

            SemanticChange change = detail.getSemanticChanges().get(0);
            assertThat(change.getOperation()).isEqualTo(SemanticChangeType.MODIFIED);
            assertThat(change.getDetails()).isNotEmpty();

            FieldChange colorChange = change.getDetails().stream()
                    .filter(fc -> "color".equals(fc.getFieldPath()))
                    .findFirst()
                    .orElse(null);

            assertThat(colorChange).isNotNull();
            assertThat(colorChange.getFieldLabel()).isEqualTo("颜色");
            assertThat(colorChange.getOldValue()).isEqualTo("#cccccc");
            assertThat(colorChange.getNewValue()).isEqualTo("#0000ff");

            // 测试生成摘要文本
            String summary = diffService.generateSummaryText(detail);
            System.out.println("摘要: " + summary);
            assertThat(summary).contains("颜色");
        }

        @Test
        @DisplayName("检测枚举选项的标签变更")
        void diff_enumOptionLabelChange_detectsChange() throws Exception {
            List<EnumFieldConfig.EnumOptionDefinition> beforeOptions = new ArrayList<>();
            beforeOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "需求", "REQ", true, "#00ff00", 1));
            EnumFieldConfig before = createEnumField("field-1", "类型", beforeOptions);

            List<EnumFieldConfig.EnumOptionDefinition> afterOptions = new ArrayList<>();
            afterOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "业务需求", "REQ", true, "#00ff00", 1));
            EnumFieldConfig after = createEnumField("field-1", "类型", afterOptions);

            JsonNode beforeNode = toJsonNode(before);
            JsonNode afterNode = toJsonNode(after);

            ChangeDetail detail = strategy.diff(beforeNode, afterNode);

            assertThat(detail.getSemanticChanges()).hasSize(1);

            SemanticChange change = detail.getSemanticChanges().get(0);
            assertThat(change.getOperation()).isEqualTo(SemanticChangeType.MODIFIED);

            FieldChange labelChange = change.getDetails().stream()
                    .filter(fc -> "label".equals(fc.getFieldPath()))
                    .findFirst()
                    .orElse(null);

            assertThat(labelChange).isNotNull();
            assertThat(labelChange.getFieldLabel()).isEqualTo("显示名称");
            assertThat(labelChange.getOldValue()).isEqualTo("需求");
            assertThat(labelChange.getNewValue()).isEqualTo("业务需求");

            // 测试生成摘要文本
            String summary = diffService.generateSummaryText(detail);
            System.out.println("摘要: " + summary);
            assertThat(summary).contains("显示名称");
        }

        @Test
        @DisplayName("检测新增枚举选项")
        void diff_enumOptionAdded_detectsAddition() throws Exception {
            List<EnumFieldConfig.EnumOptionDefinition> beforeOptions = new ArrayList<>();
            beforeOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "高", "HIGH", true, "#ff0000", 1));
            EnumFieldConfig before = createEnumField("field-1", "优先级", beforeOptions);

            List<EnumFieldConfig.EnumOptionDefinition> afterOptions = new ArrayList<>();
            afterOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "高", "HIGH", true, "#ff0000", 1));
            afterOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-2", "低", "LOW", true, "#00ff00", 2));
            EnumFieldConfig after = createEnumField("field-1", "优先级", afterOptions);

            JsonNode beforeNode = toJsonNode(before);
            JsonNode afterNode = toJsonNode(after);

            ChangeDetail detail = strategy.diff(beforeNode, afterNode);

            assertThat(detail.getSemanticChanges()).hasSize(1);

            SemanticChange change = detail.getSemanticChanges().get(0);
            assertThat(change.getCategory()).isEqualTo("ENUM_OPTION");
            assertThat(change.getOperation()).isEqualTo(SemanticChangeType.ADDED);
            assertThat(change.getTargetId()).isEqualTo("opt-2");
            assertThat(change.getTargetName()).isEqualTo("低");

            // 测试生成摘要文本
            String summary = diffService.generateSummaryText(detail);
            System.out.println("摘要: " + summary);
            assertThat(summary).contains("添加了枚举选项");
            assertThat(summary).contains("低");
        }

        @Test
        @DisplayName("检测删除枚举选项")
        void diff_enumOptionRemoved_detectsRemoval() throws Exception {
            List<EnumFieldConfig.EnumOptionDefinition> beforeOptions = new ArrayList<>();
            beforeOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "高", "HIGH", true, "#ff0000", 1));
            beforeOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-2", "低", "LOW", true, "#00ff00", 2));
            EnumFieldConfig before = createEnumField("field-1", "优先级", beforeOptions);

            List<EnumFieldConfig.EnumOptionDefinition> afterOptions = new ArrayList<>();
            afterOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "高", "HIGH", true, "#ff0000", 1));
            EnumFieldConfig after = createEnumField("field-1", "优先级", afterOptions);

            JsonNode beforeNode = toJsonNode(before);
            JsonNode afterNode = toJsonNode(after);

            ChangeDetail detail = strategy.diff(beforeNode, afterNode);

            assertThat(detail.getSemanticChanges()).hasSize(1);

            SemanticChange change = detail.getSemanticChanges().get(0);
            assertThat(change.getCategory()).isEqualTo("ENUM_OPTION");
            assertThat(change.getOperation()).isEqualTo(SemanticChangeType.REMOVED);
            assertThat(change.getTargetId()).isEqualTo("opt-2");
            assertThat(change.getTargetName()).isEqualTo("低");

            // 测试生成摘要文本
            String summary = diffService.generateSummaryText(detail);
            System.out.println("摘要: " + summary);
            assertThat(summary).contains("移除了枚举选项");
            assertThat(summary).contains("低");
        }

        @Test
        @DisplayName("同时检测多个枚举选项变更")
        void diff_multipleEnumOptionChanges_detectsAllChanges() throws Exception {
            List<EnumFieldConfig.EnumOptionDefinition> beforeOptions = new ArrayList<>();
            beforeOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "高", "HIGH", true, "#ff0000", 1));
            beforeOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-2", "中", "MEDIUM", true, "#ffff00", 2));
            EnumFieldConfig before = createEnumField("field-1", "优先级", beforeOptions);

            // opt-1 颜色变更, opt-2 删除, opt-3 新增
            List<EnumFieldConfig.EnumOptionDefinition> afterOptions = new ArrayList<>();
            afterOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "高", "HIGH", true, "#ff00ff", 1));
            afterOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-3", "低", "LOW", true, "#00ff00", 2));
            EnumFieldConfig after = createEnumField("field-1", "优先级", afterOptions);

            JsonNode beforeNode = toJsonNode(before);
            JsonNode afterNode = toJsonNode(after);

            ChangeDetail detail = strategy.diff(beforeNode, afterNode);

            assertThat(detail.getSemanticChanges()).hasSize(3);

            // 验证新增
            SemanticChange addedChange = detail.getSemanticChanges().stream()
                    .filter(c -> c.getOperation() == SemanticChangeType.ADDED)
                    .findFirst()
                    .orElse(null);
            assertThat(addedChange).isNotNull();
            assertThat(addedChange.getTargetId()).isEqualTo("opt-3");

            // 验证删除
            SemanticChange removedChange = detail.getSemanticChanges().stream()
                    .filter(c -> c.getOperation() == SemanticChangeType.REMOVED)
                    .findFirst()
                    .orElse(null);
            assertThat(removedChange).isNotNull();
            assertThat(removedChange.getTargetId()).isEqualTo("opt-2");

            // 验证修改
            SemanticChange modifiedChange = detail.getSemanticChanges().stream()
                    .filter(c -> c.getOperation() == SemanticChangeType.MODIFIED)
                    .findFirst()
                    .orElse(null);
            assertThat(modifiedChange).isNotNull();
            assertThat(modifiedChange.getTargetId()).isEqualTo("opt-1");
            assertThat(modifiedChange.getDetails()).isNotEmpty();

            // 测试生成摘要文本
            String summary = diffService.generateSummaryText(detail);
            System.out.println("摘要: " + summary);
            assertThat(summary).contains("添加了枚举选项");
            assertThat(summary).contains("移除了枚举选项");
            assertThat(summary).contains("修改了枚举选项");
        }

        @Test
        @DisplayName("使用SchemaDiffService.diff方法测试完整流程")
        void diffService_enumOptionChange_generatesCorrectSummary() throws Exception {
            List<EnumFieldConfig.EnumOptionDefinition> beforeOptions = new ArrayList<>();
            beforeOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "高", "HIGH", true, "#ff0000", 1));
            EnumFieldConfig before = createEnumField("field-1", "优先级", beforeOptions);

            List<EnumFieldConfig.EnumOptionDefinition> afterOptions = new ArrayList<>();
            afterOptions.add(new EnumFieldConfig.EnumOptionDefinition("opt-1", "高", "HIGH", false, "#ff0000", 1));
            EnumFieldConfig after = createEnumField("field-1", "优先级", afterOptions);

            String beforeJson = toJson(before);
            String afterJson = toJson(after);

            ChangeDetail detail = diffService.diff(beforeJson, afterJson);

            assertThat(detail).isNotNull();
            assertThat(detail.getSemanticChanges()).hasSize(1);

            String summary = diffService.generateSummaryText(detail);
            System.out.println("完整流程摘要: " + summary);

            // 验证摘要包含详细变更信息
            assertThat(summary).contains("修改了枚举选项");
            assertThat(summary).contains("高");
            assertThat(summary).contains("启用状态");
        }
    }
}
