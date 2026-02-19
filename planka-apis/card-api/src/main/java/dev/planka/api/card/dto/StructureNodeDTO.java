package dev.planka.api.card.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 架构节点 DTO
 * <p>
 * 表示架构线中的一个节点（卡片），用于架构属性编辑器的树形展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructureNodeDTO {

    /**
     * 节点ID（卡片ID）
     */
    private String id;

    /**
     * 节点名称（卡片标题）
     */
    private String name;

    /**
     * 层级索引（0为根层级）
     */
    private int levelIndex;

    /**
     * 层级名称（如"部落"、"小队"）
     */
    private String levelName;

    /**
     * 是否为叶子节点
     */
    private boolean leaf;

    /**
     * 子节点列表
     */
    private List<StructureNodeDTO> children;
}
