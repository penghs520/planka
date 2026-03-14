package cn.agilean.kanban.api.card.request;

import cn.agilean.kanban.domain.link.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Yield 单元测试")
class YieldTest {

    @Test
    @DisplayName("forLinkPath: 单层路径应构建一层嵌套 Yield")
    void forLinkPath_singleNode() {
        Path path = new Path(List.of("link1:SOURCE"));

        Yield yield = Yield.forLinkPath(path);

        // 外层只返回基本字段
        assertThat(yield.getField()).isNotNull();
        assertThat(yield.getField().isAllFields()).isFalse();
        // 外层包含一个 link
        assertThat(yield.getLinks()).hasSize(1);
        YieldLink link = yield.getLinks().get(0);
        assertThat(link.getLinkFieldId()).isEqualTo("link1:SOURCE");
        // 最内层返回全部字段
        assertThat(link.getTargetYield().getField().isAllFields()).isTrue();
        assertThat(link.getTargetYield().getLinks()).isNull();
    }

    @Test
    @DisplayName("forLinkPath: 多层路径应构建嵌套 Yield")
    void forLinkPath_multipleNodes() {
        Path path = new Path(List.of("link1:SOURCE", "link2:TARGET", "link3:SOURCE"));

        Yield yield = Yield.forLinkPath(path);

        // 第一层
        assertThat(yield.getLinks()).hasSize(1);
        YieldLink level1 = yield.getLinks().get(0);
        assertThat(level1.getLinkFieldId()).isEqualTo("link1:SOURCE");

        // 第二层
        Yield level1Target = level1.getTargetYield();
        assertThat(level1Target.getField().isAllFields()).isFalse();
        assertThat(level1Target.getLinks()).hasSize(1);
        YieldLink level2 = level1Target.getLinks().get(0);
        assertThat(level2.getLinkFieldId()).isEqualTo("link2:TARGET");

        // 第三层
        Yield level2Target = level2.getTargetYield();
        assertThat(level2Target.getField().isAllFields()).isFalse();
        assertThat(level2Target.getLinks()).hasSize(1);
        YieldLink level3 = level2Target.getLinks().get(0);
        assertThat(level3.getLinkFieldId()).isEqualTo("link3:SOURCE");

        // 最内层返回全部字段，无更多关联
        Yield innermost = level3.getTargetYield();
        assertThat(innermost.getField().isAllFields()).isTrue();
        assertThat(innermost.getLinks()).isNull();
    }

    @Test
    @DisplayName("merge: 空数组应返回 basic Yield")
    void merge_emptyArray_shouldReturnBasicYield() {
        Yield result = Yield.merge(new Yield[0]);

        assertThat(result).isNotNull();
        assertThat(result.getField()).isNotNull();
        assertThat(result.getField().isAllFields()).isFalse();
        assertThat(result.getLinks()).isNull();
    }

    @Test
    @DisplayName("merge: null 数组应返回 basic Yield")
    void merge_nullArray_shouldReturnBasicYield() {
        Yield result = Yield.merge((Yield[]) null);

        assertThat(result).isNotNull();
        assertThat(result.getField()).isNotNull();
        assertThat(result.getField().isAllFields()).isFalse();
    }

    @Test
    @DisplayName("merge: 应合并 fieldIds")
    void merge_shouldMergeFieldIds() {
        Yield yield1 = createYieldWithFields(Set.of("field1", "field2"), false, false);
        Yield yield2 = createYieldWithFields(Set.of("field2", "field3"), false, false);

        Yield result = Yield.merge(yield1, yield2);

        assertThat(result.getField().getFieldIds()).containsExactlyInAnyOrder("field1", "field2", "field3");
    }

    @Test
    @DisplayName("merge: 任意 Yield 的 allFields 为 true 则结果为 true")
    void merge_anyAllFieldsTrue_shouldResultAllFieldsTrue() {
        Yield yield1 = createYieldWithFields(Set.of("field1"), false, false);
        Yield yield2 = createYieldWithFields(Set.of("field2"), true, false);

        Yield result = Yield.merge(yield1, yield2);

        assertThat(result.getField().isAllFields()).isTrue();
    }

    @Test
    @DisplayName("merge: 任意 Yield 的 includeDescription 为 true 则结果为 true")
    void merge_anyIncludeDescriptionTrue_shouldResultIncludeDescriptionTrue() {
        Yield yield1 = createYieldWithFields(Set.of("field1"), false, false);
        Yield yield2 = createYieldWithFields(Set.of("field2"), false, true);

        Yield result = Yield.merge(yield1, yield2);

        assertThat(result.getField().isIncludeDescription()).isTrue();
    }

    @Test
    @DisplayName("merge: 应合并相同 linkFieldId 的 links")
    void merge_shouldMergeLinksWithSameLinkFieldId() {
        // yield1: link1 -> field1
        YieldLink link1 = createYieldLink("link1:SOURCE", Set.of("field1"));
        Yield yield1 = new Yield();
        yield1.setField(YieldField.basic());
        yield1.setLinks(List.of(link1));

        // yield2: link1 -> field2
        YieldLink link2 = createYieldLink("link1:SOURCE", Set.of("field2"));
        Yield yield2 = new Yield();
        yield2.setField(YieldField.basic());
        yield2.setLinks(List.of(link2));

        Yield result = Yield.merge(yield1, yield2);

        assertThat(result.getLinks()).hasSize(1);
        YieldLink mergedLink = result.getLinks().get(0);
        assertThat(mergedLink.getLinkFieldId()).isEqualTo("link1:SOURCE");
        assertThat(mergedLink.getTargetYield().getField().getFieldIds())
                .containsExactlyInAnyOrder("field1", "field2");
    }

    @Test
    @DisplayName("merge: 不同 linkFieldId 的 links 应保持独立")
    void merge_shouldKeepDifferentLinkFieldIdsSeparate() {
        YieldLink link1 = createYieldLink("link1:SOURCE", Set.of("field1"));
        Yield yield1 = new Yield();
        yield1.setField(YieldField.basic());
        yield1.setLinks(List.of(link1));

        YieldLink link2 = createYieldLink("link2:TARGET", Set.of("field2"));
        Yield yield2 = new Yield();
        yield2.setField(YieldField.basic());
        yield2.setLinks(List.of(link2));

        Yield result = Yield.merge(yield1, yield2);

        assertThat(result.getLinks()).hasSize(2);
        assertThat(result.getLinks().stream().map(YieldLink::getLinkFieldId))
                .containsExactlyInAnyOrder("link1:SOURCE", "link2:TARGET");
    }

    @Test
    @DisplayName("merge: 应递归合并嵌套的 targetYield")
    void merge_shouldRecursivelyMergeNestedTargetYields() {
        // 创建嵌套结构：link1 -> link2 -> field1
        YieldLink nestedLink1 = createYieldLink("link2:TARGET", Set.of("field1"));
        Yield nestedYield1 = new Yield();
        nestedYield1.setField(YieldField.basic());
        nestedYield1.setLinks(List.of(nestedLink1));

        YieldLink link1 = new YieldLink();
        link1.setLinkFieldId("link1:SOURCE");
        link1.setTargetYield(nestedYield1);

        Yield yield1 = new Yield();
        yield1.setField(YieldField.basic());
        yield1.setLinks(List.of(link1));

        // yield2: link1 -> link2 -> field2
        YieldLink nestedLink2 = createYieldLink("link2:TARGET", Set.of("field2"));
        Yield nestedYield2 = new Yield();
        nestedYield2.setField(YieldField.basic());
        nestedYield2.setLinks(List.of(nestedLink2));

        YieldLink link2 = new YieldLink();
        link2.setLinkFieldId("link1:SOURCE");
        link2.setTargetYield(nestedYield2);

        Yield yield2 = new Yield();
        yield2.setField(YieldField.basic());
        yield2.setLinks(List.of(link2));

        Yield result = Yield.merge(yield1, yield2);

        // 验证第一层
        assertThat(result.getLinks()).hasSize(1);
        YieldLink level1 = result.getLinks().get(0);
        assertThat(level1.getLinkFieldId()).isEqualTo("link1:SOURCE");

        // 验证第二层（递归合并）
        Yield level1Target = level1.getTargetYield();
        assertThat(level1Target.getLinks()).hasSize(1);
        YieldLink level2 = level1Target.getLinks().get(0);
        assertThat(level2.getLinkFieldId()).isEqualTo("link2:TARGET");
        assertThat(level2.getTargetYield().getField().getFieldIds())
                .containsExactlyInAnyOrder("field1", "field2");
    }

    @Test
    @DisplayName("merge: null 元素应被忽略")
    void merge_nullElements_shouldBeIgnored() {
        Yield yield1 = createYieldWithFields(Set.of("field1"), false, false);

        Yield result = Yield.merge(yield1, null);

        assertThat(result.getField().getFieldIds()).containsExactly("field1");
    }

    // 辅助方法
    private Yield createYieldWithFields(Set<String> fieldIds, boolean allFields, boolean includeDescription) {
        Yield yield = new Yield();
        YieldField field = new YieldField();
        field.setAllFields(allFields);
        field.setIncludeDescription(includeDescription);
        field.setFieldIds(fieldIds.isEmpty() ? null : new HashSet<>(fieldIds));
        yield.setField(field);
        return yield;
    }

    private YieldLink createYieldLink(String linkFieldId, Set<String> targetFieldIds) {
        YieldLink link = new YieldLink();
        link.setLinkFieldId(linkFieldId);

        Yield targetYield = new Yield();
        YieldField field = new YieldField();
        field.setAllFields(false);
        field.setFieldIds(new HashSet<>(targetFieldIds));
        targetYield.setField(field);

        link.setTargetYield(targetYield);
        return link;
    }
}
