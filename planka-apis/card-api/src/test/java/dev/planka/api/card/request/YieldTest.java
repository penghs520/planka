package dev.planka.api.card.request;

import dev.planka.domain.link.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}
