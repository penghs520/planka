package dev.planka.card.service;

import dev.planka.api.card.dto.CardDetailResponse.FieldRenderMetaDTO;
import dev.planka.api.card.renderconfig.FieldRenderConfigConverter;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字段渲染配置服务
 * <p>
 * 负责将领域模型的 FieldConfig 转换为统一的 FieldRenderConfig（与列表视图兼容）。
 */
@Service
public class FieldRenderConfigService {

    /**
     * 将字段配置列表转换为渲染元数据列表
     *
     * @param fieldConfigs 字段配置列表
     * @return 渲染元数据列表
     */
    public List<FieldRenderMetaDTO> toRenderMetas(List<FieldConfig> fieldConfigs) {
        return fieldConfigs.stream()
                .map(this::toRenderMeta)
                .collect(Collectors.toList());
    }

    /**
     * 转换单个字段配置为渲染元数据
     */
    private FieldRenderMetaDTO toRenderMeta(FieldConfig config) {
        return FieldRenderMetaDTO.builder()
                .fieldId(config.getFieldId().value())
                .name(config.getName())
                .renderConfig(FieldRenderConfigConverter.convert(config))
                .build();
    }
}
