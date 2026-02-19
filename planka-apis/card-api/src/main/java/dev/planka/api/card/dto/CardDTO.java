package dev.planka.api.card.dto;

import dev.planka.domain.card.*;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.link.Path;
import dev.planka.domain.stream.StatusId;
import dev.planka.domain.stream.StreamId;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 统一的卡片表示（DTO形态）。
 */
@Getter
@Setter
public class CardDTO {

    // ==================== 标识信息 ====================

    /**
     * 卡片ID
     */
    private CardId id;

    /**
     * 卡片内置编号
     */
    private long codeInOrg;

    /**
     * 卡片自定义编号
     */
    private String customCode;

    /**
     * 组织ID
     */
    private OrgId orgId;

    /**
     * 卡片类型ID
     */
    private CardTypeId typeId;

    // ==================== 基本信息 ====================

    /**
     * 卡片标题
     */
    private CardTitle title;

    /**
     * 卡片描述
     */
    private CardDescription description;

    // ==================== 状态信息 ====================

    /**
     * 生命周期状态
     */
    private CardStyle cardStyle;

    /**
     * 价值流定义ID
     */
    private StreamId streamId;

    /**
     * 价值流状态ID
     */
    private StatusId statusId;

    // ==================== 属性值存储 ====================

    /**
     * 自定义属性值 - 使用 fieldId 作为 key，FieldValue 自身也包含 fieldId
     */
    private Map<String/* fieldId */, FieldValue<?>> fieldValues;

    /**
     * 关联卡片
     * <p>
     * key 为 linkFieldId，格式为 "{linkTypeId}:{SOURCE|TARGET}"
     */
    private Map<String/* linkFieldId */, Set<CardDTO>> linkedCards;

    // ==================== 审计信息 ====================

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 丢弃时间
     */
    private LocalDateTime abandonedAt;

    /**
     * 归档时间
     */
    private LocalDateTime archivedAt;


    public String getCode() {
        if (customCode != null) {
            return customCode;
        }
        return String.valueOf(codeInOrg);
    }

    /**
     * 沿关联路径获取末端关联卡片
     * <p>
     * 逐层遍历 linkedCards，沿 path 的每个 linkFieldId 向下查找，返回最终一层的所有卡片。
     *
     * @param path 关联路径
     * @return 末端关联卡片集合，路径中断时返回空集合
     */
    public Set<CardDTO> getLinkedCards(Path path) {
        List<CardDTO> currentLevel = List.of(this);
        for (String linkFieldId : path.linkNodes()) {
            List<CardDTO> nextLevel = new ArrayList<>();
            for (CardDTO current : currentLevel) {
                if (current.linkedCards != null) {
                    Set<CardDTO> linked = current.linkedCards.get(linkFieldId);
                    if (linked != null) {
                        nextLevel.addAll(linked);
                    }
                }
            }
            if (nextLevel.isEmpty()) {
                return Collections.emptySet();
            }
            currentLevel = nextLevel;
        }
        return new LinkedHashSet<>(currentLevel);
    }
}
