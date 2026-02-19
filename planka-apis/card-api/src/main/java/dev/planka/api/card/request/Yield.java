package dev.planka.api.card.request;

import dev.planka.domain.link.Path;
import lombok.Data;

import java.util.List;

/**
 * 查询返回定义
 */
@Data
public class Yield {
    /**
     * 当前卡片需要返回的字段
     */
    private YieldField field;

    /**
     * 需要返回的关联卡
     */
    private List<YieldLink> links;

    public Yield setLinks(List<YieldLink> links){
        this.links = links;
        return this;
    }

    /**
     * @return 返回包括返回了全部属性（不包含关联）的Yield实例
     */
    public static Yield all() {
        Yield yield = new Yield();
        yield.setField(YieldField.all());
        return yield;
    }

    /**
     * @return 返回只包含基本信息（id、title、typeId等）的Yield实例，用于关联卡片的简要展示
     */
    public static Yield basic() {
        Yield yield = new Yield();
        yield.setField(YieldField.basic());
        return yield;
    }

    /**
     * 根据关联路径构建嵌套 Yield
     * <p>
     * 从最内层向外层构建：最内层关联卡片返回全部字段，中间层只返回基本字段。
     *
     * @param path 关联路径，linkNodes 中每个元素是 linkFieldId
     * @return 包含嵌套关联查询的 Yield
     */
    public static Yield forLinkPath(Path path) {
        List<String> linkNodes = path.linkNodes();
        Yield innerYield = Yield.all();

        for (int i = linkNodes.size() - 1; i >= 0; i--) {
            YieldLink link = new YieldLink();
            link.setLinkFieldId(linkNodes.get(i));
            link.setTargetYield(innerYield);

            innerYield = new Yield();
            innerYield.setField(YieldField.basic());
            innerYield.setLinks(List.of(link));
        }

        return innerYield;
    }
}
