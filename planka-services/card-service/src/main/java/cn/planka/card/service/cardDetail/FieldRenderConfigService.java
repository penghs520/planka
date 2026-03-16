package cn.planka.card.service.cardDetail;

import cn.planka.api.card.dto.CardDetailResponse.FieldRenderMetaDTO;
import cn.planka.api.card.renderconfig.FieldRenderConfigConverter;
import cn.planka.domain.card.CardId;
import cn.planka.domain.schema.definition.fieldconfig.FieldConfig;
import cn.planka.infra.expression.TextExpressionTemplateResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字段渲染配置服务
 * <p>
 * 负责将领域模型的 FieldConfig 转换为统一的 FieldRenderConfig（与列表视图兼容）。
 */
@Service
@RequiredArgsConstructor
public class FieldRenderConfigService {

    private final TextExpressionTemplateResolver expressionResolver;

    /**
     * 将字段配置列表转换为渲染元数据列表
     *
     * @param fieldConfigs   字段配置列表
     * @param cardId         当前卡片ID（用于解析提示信息中的表达式）
     * @param memberCardId   当前操作人成员卡ID（用于解析提示信息中的表达式，可为null）
     * @return 渲染元数据列表
     */
    public List<FieldRenderMetaDTO> toRenderMetas(List<FieldConfig> fieldConfigs, CardId cardId, CardId memberCardId) {
        return fieldConfigs.stream()
                .map(config -> toRenderMeta(config, cardId, memberCardId))
                .collect(Collectors.toList());
    }

    /**
     * 转换单个字段配置为渲染元数据
     */
    private FieldRenderMetaDTO toRenderMeta(FieldConfig config, CardId cardId, CardId memberCardId) {
        // 解析提示信息中的表达式
        String resolvedHintText = expressionResolver.resolve(config.getHintText(), cardId, memberCardId);

        return FieldRenderMetaDTO.builder()
                .fieldId(config.getFieldId().value())
                .name(config.getName())
                .renderConfig(FieldRenderConfigConverter.convert(config))
                .hintText(resolvedHintText)
                .showHintBelowLabel(config.getShowHintBelowLabel())
                .showHintAsTooltip(config.getShowHintAsTooltip())
                .build();
    }
}
