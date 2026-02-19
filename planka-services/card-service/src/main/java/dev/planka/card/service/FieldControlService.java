package dev.planka.card.service;

import dev.planka.api.card.dto.CardDetailResponse.FieldControlDTO;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段控制服务
 * <p>
 * 计算卡片详情页字段的控制配置（可编辑/只读、必填级别）。
 * <p>
 * 当前实现基于字段配置的 required/readOnly 属性。
 * 未来可扩展为整合权限管理体系，支持基于角色/用户的动态控制。
 */
@Service
public class FieldControlService {

    /** 内置字段前缀 */
    private static final String BUILTIN_PREFIX = "$";

    /**
     * 计算字段控制配置
     *
     * @param fieldConfigs 字段配置列表
     * @param currentUserId 当前用户 ID（预留，用于未来权限扩展）
     * @return 字段控制配置映射（key: fieldId, value: 控制配置）
     */
    public Map<String, FieldControlDTO> computeFieldControls(
            List<FieldConfig> fieldConfigs,
            String currentUserId) {

        Map<String, FieldControlDTO> controls = new HashMap<>();

        for (FieldConfig config : fieldConfigs) {
            controls.put(config.getFieldId().value(), computeSingleFieldControl(config, currentUserId));
        }

        // 添加常见内置字段的控制配置
        addBuiltinFieldControls(controls);

        return controls;
    }

    /**
     * 计算单个字段的控制配置
     */
    private FieldControlDTO computeSingleFieldControl(
            FieldConfig config,
            String currentUserId) {

        boolean editable = true;
        String readOnlyReasonType = null;
        String readOnlyReasonText = null;
        String requiredLevel = null;
        String requiredReasonText = null;

        // 1. 内置字段始终只读
        if (isBuiltinField(config.getFieldId().value())) {
            editable = false;
            readOnlyReasonType = "BUILTIN_FIELD";
            readOnlyReasonText = "内置字段不可编辑";
        }
        // 2. 字段配置为只读
        else if (Boolean.TRUE.equals(config.getReadOnly())) {
            editable = false;
            readOnlyReasonType = "FIELD_CONFIG";
            readOnlyReasonText = "此字段已被配置为只读";
        }
        // 3. TODO: 未来扩展权限检查
        // else if (!permissionService.canEditField(currentUserId, config)) {
        //     editable = false;
        //     readOnlyReasonType = "PERMISSION_DENIED";
        //     readOnlyReasonText = "您没有编辑此字段的权限";
        // }

        // 必填配置
        if (Boolean.TRUE.equals(config.getRequired())) {
            // 默认为 HINT（仅提示），未来可从 FieldConfig 读取配置的级别
            requiredLevel = "HINT";
            requiredReasonText = "此字段为必填项";
        }

        return FieldControlDTO.builder()
                .editable(editable)
                .readOnlyReasonType(readOnlyReasonType)
                .readOnlyReasonText(readOnlyReasonText)
                .requiredLevel(requiredLevel)
                .requiredReasonText(requiredReasonText)
                .build();
    }

    /**
     * 添加常见内置字段的控制配置
     * <p>
     * 内置字段（如创建时间、更新时间等）始终为只读
     */
    private void addBuiltinFieldControls(Map<String, FieldControlDTO> controls) {
        String[] builtinFields = {
                "$createdAt", "$updatedAt", "$archivedAt", "$discardedAt",
                "$cardStyle", "$statusId", "$code", "$description"
        };

        FieldControlDTO builtinControl = FieldControlDTO.builder()
                .editable(false)
                .readOnlyReasonType("BUILTIN_FIELD")
                .readOnlyReasonText("内置字段不可编辑")
                .build();

        for (String fieldId : builtinFields) {
            if (!controls.containsKey(fieldId)) {
                controls.put(fieldId, builtinControl);
            }
        }
    }

    /**
     * 判断是否为内置字段
     * <p>
     * 内置字段以 $ 开头
     */
    private boolean isBuiltinField(String fieldId) {
        if (fieldId == null) {
            return false;
        }
        return fieldId.startsWith(BUILTIN_PREFIX);
    }
}
