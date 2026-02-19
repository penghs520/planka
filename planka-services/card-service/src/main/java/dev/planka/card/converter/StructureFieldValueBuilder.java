package dev.planka.card.converter;

import dev.planka.api.card.dto.CardDTO;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.field.StructureFieldValue;
import dev.planka.domain.field.StructureItem;
import dev.planka.domain.schema.definition.fieldconfig.StructureFieldConfig;
import dev.planka.domain.schema.definition.structure.StructureLevelBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 架构属性值构造器
 * <p>
 * 从 CardDTO 的 linkedCards 提取关联数据，按照 levelBindings 顺序构造 StructureItem 链表。
 * 关联缺失时返回部分链（next = null），而非整体返回 null。
 */
public final class StructureFieldValueBuilder {

    private StructureFieldValueBuilder() {
        // 工具类，禁止实例化
    }

    /**
     * 批量构建架构属性值
     *
     * @param cardDTO     卡片 DTO（包含 linkedCards 关联数据）
     * @param definitions 架构属性定义列表
     * @return fieldId -> FieldValue 映射
     */
    public static Map<String, FieldValue<?>> buildAll(
            CardDTO cardDTO,
            List<StructureFieldConfig> definitions) {
        Map<String, FieldValue<?>> result = new HashMap<>();
        for (StructureFieldConfig def : definitions) {
            StructureFieldValue value = build(cardDTO, def);
            if (value != null) {
                result.put(def.getId().value(), value);
            }
        }
        return result;
    }

    /**
     * 构建单个架构属性值
     *
     * @param cardDTO    卡片 DTO（包含 linkedCards 关联数据）
     * @param definition 架构属性定义
     * @return 架构属性值，即使没有关联数据也返回空的 StructureFieldValue
     */
    public static StructureFieldValue build(
            CardDTO cardDTO,
            StructureFieldConfig definition) {

        List<StructureLevelBinding> bindings = definition.getLevelBindings();
        if (bindings == null || bindings.isEmpty()) {
            return null;
        }

        // 从第一层开始构建
        StructureItem firstItem = buildStructureItem(cardDTO, bindings, 0);

        // 即使 firstItem 为 null，也返回 StructureFieldValue（表示没有关联）
        return new StructureFieldValue(
                definition.getId().value(),
                firstItem
        );
    }

    /**
     * 递归构建 StructureItem 链
     * <p>
     * 架构属性的所有层级节点都与原始卡片直接关联，因此始终从原始卡片的 linkedCards 中获取数据。
     * 关联缺失时返回 null（中断链），但已构建的节点仍然有效。
     *
     * @param originalCard 原始卡片 DTO（包含所有层级的关联数据）
     * @param bindings     层级绑定配置列表
     * @param index        当前处理的层级索引
     * @return StructureItem 节点，或 null（如果当前层级无关联数据）
     */
    private static StructureItem buildStructureItem(
            CardDTO originalCard,
            List<StructureLevelBinding> bindings,
            int index) {

        if (index >= bindings.size() || originalCard == null) {
            return null;
        }

        StructureLevelBinding binding = bindings.get(index);
        String linkFieldId = binding.linkFieldId().value();  // 取 String 值

        // 从原始卡片获取关联数据（所有层级都从原始卡片直接获取）
        Map<String, Set<CardDTO>> linkedCards = originalCard.getLinkedCards();
        if (linkedCards == null) {
            return null;  // 没有任何关联数据
        }

        Set<CardDTO> cards = linkedCards.get(linkFieldId);
        if (cards == null || cards.isEmpty()) {
            return null;  // 该层级没有关联卡片，链在此中断
        }

        // 取第一个关联卡片（架构属性应该是单选的）
        CardDTO linkedCard = cards.iterator().next();

        // 递归构建下一层（始终从原始卡片获取，而非从关联卡片）
        StructureItem nextItem = buildStructureItem(originalCard, bindings, index + 1);

        // 获取卡片标题作为节点名称
        String name = linkedCard.getTitle() != null
                ? linkedCard.getTitle().getDisplayValue()
                : "";

        return new StructureItem(
                String.valueOf(linkedCard.getId().value()),
                name,
                nextItem  // 可能为 null，表示后续层级无数据
        );
    }
}
