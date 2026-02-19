package dev.planka.schema.service.fieldconfig;

import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.schema.repository.SchemaRepository;
import dev.planka.schema.service.common.SchemaCommonService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 属性配置服务
 * <p>
 * 提供卡片类型属性配置的获取和保存功能。
 * <p>
 * 继承优先级（从高到低）：自身 > 显式父类 > 任意卡属性集
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FieldConfigService {

    private final SchemaCommonService schemaCommonService;
    private final SchemaRepository schemaRepository;
    private final ObjectMapper objectMapper;


    /**
     * 保存属性配置
     * <p>
     * 如果配置来自父类型（cardTypeId 不匹配），则创建新的配置属于当前卡片类型。
     * 新建配置时，id 和 fieldId 使用相同的值。
     *
     * @param cardTypeId 卡片类型ID
     * @param operatorId 操作人ID
     * @param config     属性配置
     * @return 保存结果
     */
    public Result<?> saveFieldConfig(String cardTypeId,
                                        String operatorId,
                                        FieldConfig config) {
        Result<SchemaDefinition<?>> cardTypeResult = schemaCommonService.getById(cardTypeId);
        if (cardTypeResult.isSuccess()) {
            if (cardTypeResult.getData() == null) {
                return Result.failure(CommonErrorCode.DATA_NOT_FOUND, "卡片类型不存在");
            }
        } else {
            return Result.failure(cardTypeResult.getCode(), cardTypeResult.getMessage());
        }

        if (!(cardTypeResult.getData() instanceof CardTypeDefinition cardType)) {
            return Result.failure(CommonErrorCode.BAD_REQUEST, "指定的 Schema 不是卡片类型");
        }

        // 检查配置是否属于当前卡片类型
        FieldConfig configToSave = config;
        if (config.getCardTypeId() == null || !config.getCardTypeId().value().equals(cardTypeId)) {
            // 配置来自父类型或未设置，需要创建新的配置
            configToSave = createOwnConfig(config, cardType);
            log.info("Creating own config for cardType {} from inherited config, fieldId: {}",
                    cardTypeId, config.getFieldId());
        }

        return saveSingleConfig(configToSave, cardType.getOrgId(), operatorId);
    }

    /**
     * 删除属性配置（恢复为从父类型继承）
     *
     * @param fieldConfigId 属性配置ID
     * @return 删除结果
     */
    public Result<Void> deleteFieldConfig(String fieldConfigId, String operatorId) {
        return schemaCommonService.delete(fieldConfigId, operatorId);
    }

    /**
     * 从继承的配置创建当前卡片类型的自有配置
     * <p>
     * 通过 JSON 序列化/反序列化复制配置，并更新 id 和 cardTypeId。
     * 新配置的 id 为 null（保存时会生成新的ID）。
     *
     * @param inheritedConfig 继承的配置
     * @param cardType        当前卡片类型
     * @return 新的自有配置
     */
    private FieldConfig createOwnConfig(FieldConfig inheritedConfig,
                                        CardTypeDefinition cardType) {
        try {
            // 序列化为 JSON
            ObjectNode jsonNode = objectMapper.valueToTree(inheritedConfig);

            // 清空 id（让系统生成新 ID）
            jsonNode.putNull("id");

            // 设置新的 cardTypeId
            jsonNode.put("cardTypeId", cardType.getId().value());

            // 清空审计字段，保存时会重新设置
            jsonNode.remove("createdAt");
            jsonNode.remove("createdBy");
            jsonNode.remove("updatedAt");
            jsonNode.remove("updatedBy");

            // 反序列化为新的配置对象
            return objectMapper.treeToValue(jsonNode, FieldConfig.class);
        } catch (Exception e) {
            log.error("Failed to create own config from inherited config, fieldId: {}",
                    inheritedConfig.getFieldId(), e);
            throw new RuntimeException("创建自有配置失败", e);
        }
    }

    private Result<?> saveSingleConfig(FieldConfig config, String orgId, String operatorId) {
        if (config.getCreatedAt() == null) {
            config.setCreatedAt(LocalDateTime.now());
        }
        if (config.getCreatedBy() == null) {
            config.setCreatedBy(operatorId);
        }
        if (config.getStructureVersion() == null) {
            config.setStructureVersion("1.0.0");
        }

        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(operatorId);


        if (schemaRepository.existsById(config.getId().value())) {
            UpdateSchemaRequest request = new UpdateSchemaRequest();
            request.setDefinition(config);
            return schemaCommonService.update(config.getId().value(), operatorId, request);
        } else {
            CreateSchemaRequest request = new CreateSchemaRequest();
            request.setDefinition(config);
            return schemaCommonService.create(orgId, operatorId, request);
        }
    }
}
