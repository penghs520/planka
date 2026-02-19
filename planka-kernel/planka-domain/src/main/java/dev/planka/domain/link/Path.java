package dev.planka.domain.link;

import dev.planka.common.util.AssertUtils;

import java.util.List;

/**
 * 关联路径
 * <p>
 * 表示通过多层关联访问卡片的路径，每个节点是一个 linkFieldId（格式 "ltId:SOURCE/TARGET"）
 */
public record Path(List<String/* linkFieldId */> linkNodes) {

    public Path {
        AssertUtils.notEmpty(linkNodes, "linkNodes of Path can't be empty");
    }
}
