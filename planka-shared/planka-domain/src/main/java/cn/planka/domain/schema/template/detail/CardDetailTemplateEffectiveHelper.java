package cn.planka.domain.schema.template.detail;

import cn.planka.domain.schema.definition.template.CardDetailTemplateDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 从已持久化的详情模板列表中选出与运行时一致的一条：优先默认，否则取第一个。
 */
public final class CardDetailTemplateEffectiveHelper {

    private CardDetailTemplateEffectiveHelper() {
    }

    public static Optional<CardDetailTemplateDefinition> selectFromList(List<CardDetailTemplateDefinition> templates) {
        if (templates == null || templates.isEmpty()) {
            return Optional.empty();
        }
        return templates.stream()
                .filter(CardDetailTemplateDefinition::isDefault)
                .findFirst()
                .or(() -> Optional.of(templates.get(0)));
    }
}
