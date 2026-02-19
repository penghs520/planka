package dev.planka.schema.service.common.lifecycle;

import dev.planka.domain.schema.SchemaSubType;
import dev.planka.domain.schema.definition.fieldconfig.EnumFieldConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 枚举字段配置生命周期处理器
 * <p>
 * 校验规则：
 * <ul>
 *   <li>已保存的选项不能删除（只能禁用）</li>
 *   <li>已保存的选项的 value 不能修改</li>
 *   <li>label、color、enabled、order 可以修改</li>
 *   <li>可以新增选项</li>
 * </ul>
 */
@Component
public class EnumFieldDefinitionLifecycleHandler implements SchemaLifecycleHandler<EnumFieldConfig> {

    @Override
    public String getSchemaSubType() {
        return SchemaSubType.ENUM_FIELD;
    }

    @Override
    public void beforeUpdate(EnumFieldConfig oldDefinition, EnumFieldConfig newDefinition) {
        validateOptionUpdate(oldDefinition.getOptions(), newDefinition.getOptions());
    }

    /**
     * 校验枚举选项的更新
     */
    private void validateOptionUpdate(List<EnumFieldConfig.EnumOptionDefinition> oldOptions, List<EnumFieldConfig.EnumOptionDefinition> newOptions) {
        // 如果旧选项为空，无需校验
        if (oldOptions == null || oldOptions.isEmpty()) {
            return;
        }

        // 新选项不能为空（如果旧选项不为空）
        if (newOptions == null || newOptions.isEmpty()) {
            throw new IllegalArgumentException("不能删除所有已保存的枚举选项");
        }

        // 构建新选项的 id -> option 映射
        Map<String, EnumFieldConfig.EnumOptionDefinition> newOptionMap = newOptions.stream()
                .collect(Collectors.toMap(EnumFieldConfig.EnumOptionDefinition::id, o -> o));

        // 检查每个旧选项
        for (EnumFieldConfig.EnumOptionDefinition oldOption : oldOptions) {
            EnumFieldConfig.EnumOptionDefinition newOption = newOptionMap.get(oldOption.id());

            // 规则1：已保存的选项不能删除
            if (newOption == null) {
                throw new IllegalArgumentException(
                        String.format("不能删除已保存的枚举选项「%s」，请使用禁用功能", oldOption.label()));
            }

            // 规则2：已保存选项的 value 不能修改
            if (!oldOption.value().equals(newOption.value())) {
                throw new IllegalArgumentException(
                        String.format("不能修改已保存枚举选项「%s」的值，原值: %s, 新值: %s",
                                oldOption.label(), oldOption.value(), newOption.value()));
            }
        }
    }
}
