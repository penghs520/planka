package cn.planka.api.card.request;

import cn.planka.domain.link.Path;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * 合并多个 Yield，返回一个新的 Yield
     * <p>
     * 合并规则：
     * 1. 如果任意一个 Yield 的 allFields 为 true，则结果为 true
     * 2. 合并所有 fieldIds（去重）
     * 3. 如果任意一个 includeDescription 为 true，则结果为 true
     * 4. 按 linkFieldId 合并 links，相同 linkFieldId 的 targetYield 递归合并
     *
     * @param yields 要合并的 Yield 数组
     * @return 合并后的新 Yield
     */
    public static Yield merge(Yield... yields) {
        if (yields == null || yields.length == 0) {
            return Yield.basic();
        }

        Yield result = new Yield();
        YieldField resultField = new YieldField();
        result.setField(resultField);

        Set<String> fieldIds = new HashSet<>();
        Map<String, List<YieldLink>> linksByFieldId = new HashMap<>();
        boolean allFields = false;
        boolean includeDescription = false;

        for (Yield yield : yields) {
            if (yield == null) {
                continue;
            }

            // 合并字段设置
            if (yield.getField() != null) {
                YieldField field = yield.getField();
                if (field.isAllFields()) {
                    allFields = true;
                }
                if (field.isIncludeDescription()) {
                    includeDescription = true;
                }
                if (field.getFieldIds() != null) {
                    fieldIds.addAll(field.getFieldIds());
                }
            }

            // 收集 links 按 linkFieldId 分组
            if (yield.getLinks() != null) {
                for (YieldLink link : yield.getLinks()) {
                    if (link != null && link.getLinkFieldId() != null) {
                        linksByFieldId.computeIfAbsent(link.getLinkFieldId(), k -> new ArrayList<>()).add(link);
                    }
                }
            }
        }

        // 设置合并后的字段
        resultField.setAllFields(allFields);
        resultField.setIncludeDescription(includeDescription);
        resultField.setFieldIds(fieldIds);

        // 合并相同 linkFieldId 的 links
        if (!linksByFieldId.isEmpty()) {
            List<YieldLink> mergedLinks = new ArrayList<>();
            for (Map.Entry<String, List<YieldLink>> entry : linksByFieldId.entrySet()) {
                YieldLink mergedLink = new YieldLink();
                mergedLink.setLinkFieldId(entry.getKey());

                // 递归合并相同 linkFieldId 的所有 targetYield
                List<Yield> targetYields = entry.getValue().stream()
                        .map(YieldLink::getTargetYield)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                Yield mergedTargetYield = targetYields.isEmpty()
                        ? Yield.basic()
                        : merge(targetYields.toArray(new Yield[0]));
                mergedLink.setTargetYield(mergedTargetYield);

                mergedLinks.add(mergedLink);
            }
            result.setLinks(mergedLinks);
        }

        return result;
    }
}
