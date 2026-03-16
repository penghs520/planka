package cn.planka.api.card.util;

import cn.planka.api.card.request.Yield;
import cn.planka.api.card.request.YieldLink;
import cn.planka.domain.link.Path;
import cn.planka.domain.schema.definition.condition.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConditionYieldBuilder 单元测试")
class ConditionYieldBuilderTest {

    @Test
    @DisplayName("buildYieldForCurrentCard: 应从 Subject 中收集 path 为空的字段")
    void buildYieldForCurrentCard_shouldCollectSubjectFieldsWithEmptyPath() {
        // Given: 创建一个条件，Subject 的 path 为空
        TextConditionItem.TextSubject subject = new TextConditionItem.TextSubject(null, "field1");
        TextConditionItem.TextOperator.Equal operator = new TextConditionItem.TextOperator.Equal("value");
        TextConditionItem conditionItem = new TextConditionItem(subject, operator);
        Condition condition = Condition.of(conditionItem);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

        // Then
        assertThat(yield.getField()).isNotNull();
        assertThat(yield.getField().getFieldIds()).containsExactly("field1");
        assertThat(yield.getLinks()).isNull();
    }

    @Test
    @DisplayName("buildYieldForCurrentCard: 应从 ReferenceValue 中收集 CurrentCard 类型的字段")
    void buildYieldForCurrentCard_shouldCollectCurrentCardReferenceValues() {
        // Given: 创建一个数字条件，操作符使用 CurrentCard 引用
        NumberConditionItem.NumberSubject subject = new NumberConditionItem.NumberSubject(null, "field1");
        ReferenceSource.CurrentCard currentCard = new ReferenceSource.CurrentCard(null);
        NumberConditionItem.NumberValue.ReferenceValue refValue = new NumberConditionItem.NumberValue.ReferenceValue(currentCard, "refField");
        NumberConditionItem.NumberOperator.Equal operator = new NumberConditionItem.NumberOperator.Equal(refValue);
        NumberConditionItem conditionItem = new NumberConditionItem(subject, operator);
        Condition condition = Condition.of(conditionItem);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

        // Then: 应该包含 Subject 的 field1 和 ReferenceValue 的 refField
        assertThat(yield.getField().getFieldIds()).containsExactlyInAnyOrder("field1", "refField");
    }

    @Test
    @DisplayName("buildYieldForMemberCard: 应从 ReferenceValue 中收集 Member 类型的字段")
    void buildYieldForMemberCard_shouldCollectMemberReferenceValues() {
        // Given: 创建一个数字条件，操作符使用 Member 引用
        NumberConditionItem.NumberSubject subject = new NumberConditionItem.NumberSubject(null, "field1");
        ReferenceSource.Member member = new ReferenceSource.Member(null);
        NumberConditionItem.NumberValue.ReferenceValue refValue = new NumberConditionItem.NumberValue.ReferenceValue(member, "memberField");
        NumberConditionItem.NumberOperator.Equal operator = new NumberConditionItem.NumberOperator.Equal(refValue);
        NumberConditionItem conditionItem = new NumberConditionItem(subject, operator);
        Condition condition = Condition.of(conditionItem);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForMemberCard(condition);

        // Then: Member 类型只收集 ReferenceValue 中的字段，不包含 Subject 的 field1
        assertThat(yield.getField().getFieldIds()).containsExactly("memberField");
    }

    @Test
    @DisplayName("buildYieldForParameterCard: 应从 ReferenceValue 中收集 ParameterCard 类型的字段")
    void buildYieldForParameterCard_shouldCollectParameterCardReferenceValues() {
        // Given: 创建一个日期条件，操作符使用 ParameterCard 引用
        DateConditionItem.DateSubject.FieldDateSubject subject = new DateConditionItem.DateSubject.FieldDateSubject(null, "dateField");
        ReferenceSource.ParameterCard paramCard = new ReferenceSource.ParameterCard(null, "paramTypeId");
        DateConditionItem.DateValue.ReferenceValue refValue = new DateConditionItem.DateValue.ReferenceValue(paramCard, "paramDateField");
        DateConditionItem.DateOperator.Equal operator = new DateConditionItem.DateOperator.Equal(refValue);
        DateConditionItem conditionItem = new DateConditionItem(subject, operator);
        Condition condition = Condition.of(conditionItem);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForParameterCard(condition);

        // Then
        assertThat(yield.getField().getFieldIds()).containsExactly("paramDateField");
    }

    @Test
    @DisplayName("buildYieldForContextualCard: 应从 ReferenceValue 中收集 ContextualCard 类型的字段")
    void buildYieldForContextualCard_shouldCollectContextualCardReferenceValues() {
        // Given: 创建一个数字条件，操作符使用 ContextualCard 引用
        NumberConditionItem.NumberSubject subject = new NumberConditionItem.NumberSubject(null, "field1");
        ReferenceSource.ContextualCard contextualCard = new ReferenceSource.ContextualCard(null, "contextId", 123L);
        NumberConditionItem.NumberValue.ReferenceValue refValue = new NumberConditionItem.NumberValue.ReferenceValue(contextualCard, "contextField");
        NumberConditionItem.NumberOperator.Equal operator = new NumberConditionItem.NumberOperator.Equal(refValue);
        NumberConditionItem conditionItem = new NumberConditionItem(subject, operator);
        Condition condition = Condition.of(conditionItem);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForContextualCard(condition);

        // Then
        assertThat(yield.getField().getFieldIds()).containsExactly("contextField");
    }

    @Test
    @DisplayName("应处理多级关联路径")
    void shouldHandleMultiLevelPath() {
        // Given: 创建一个条件，ReferenceValue 使用带路径的 CurrentCard
        Path path = new Path(List.of("link1:SOURCE", "link2:TARGET"));
        ReferenceSource.CurrentCard currentCard = new ReferenceSource.CurrentCard(path);
        NumberConditionItem.NumberValue.ReferenceValue refValue = new NumberConditionItem.NumberValue.ReferenceValue(currentCard, "refField");
        NumberConditionItem.NumberOperator.Equal operator = new NumberConditionItem.NumberOperator.Equal(refValue);
        NumberConditionItem.NumberSubject subject = new NumberConditionItem.NumberSubject(null, "field1");
        NumberConditionItem conditionItem = new NumberConditionItem(subject, operator);
        Condition condition = Condition.of(conditionItem);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

        // Then
        assertThat(yield.getField().getFieldIds()).containsExactly("field1");
        assertThat(yield.getLinks()).hasSize(1);

        YieldLink level1 = yield.getLinks().get(0);
        assertThat(level1.getLinkFieldId()).isEqualTo("link1:SOURCE");

        Yield level1Target = level1.getTargetYield();
        assertThat(level1Target.getLinks()).hasSize(1);

        YieldLink level2 = level1Target.getLinks().get(0);
        assertThat(level2.getLinkFieldId()).isEqualTo("link2:TARGET");
        assertThat(level2.getTargetYield().getField().getFieldIds()).containsExactly("refField");
    }

    @Test
    @DisplayName("应处理 ConditionGroup 嵌套")
    void shouldHandleNestedConditionGroup() {
        // Given: 创建嵌套的条件组
        TextConditionItem.TextSubject subject1 = new TextConditionItem.TextSubject(null, "field1");
        TextConditionItem conditionItem1 = new TextConditionItem(subject1, new TextConditionItem.TextOperator.Equal("value1"));

        NumberConditionItem.NumberSubject subject2 = new NumberConditionItem.NumberSubject(null, "field2");
        ReferenceSource.CurrentCard currentCard = new ReferenceSource.CurrentCard(null);
        NumberConditionItem.NumberValue.ReferenceValue refValue = new NumberConditionItem.NumberValue.ReferenceValue(currentCard, "refField");
        NumberConditionItem conditionItem2 = new NumberConditionItem(subject2, new NumberConditionItem.NumberOperator.Equal(refValue));

        ConditionGroup group = ConditionGroup.and(conditionItem1, conditionItem2);
        Condition condition = Condition.of(group);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

        // Then
        assertThat(yield.getField().getFieldIds()).containsExactlyInAnyOrder("field1", "field2", "refField");
    }

    @Test
    @DisplayName("空条件应返回 basic Yield")
    void emptyCondition_shouldReturnBasicYield() {
        // Given
        Condition condition = new Condition();

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

        // Then
        assertThat(yield.getField()).isNotNull();
        assertThat(yield.getField().isAllFields()).isFalse();
    }

    @Test
    @DisplayName("应处理 LinkConditionItem 的 ReferenceValue")
    void shouldHandleLinkConditionItemReferenceValue() {
        // Given: 创建一个关联条件，使用 Member 引用
        LinkConditionItem.LinkSubject subject = new LinkConditionItem.LinkSubject(null, new cn.planka.domain.link.LinkFieldId("link1:SOURCE"));
        ReferenceSource.Member member = new ReferenceSource.Member(null);
        LinkConditionItem.LinkValue.ReferenceValue refValue = new LinkConditionItem.LinkValue.ReferenceValue(member);
        LinkConditionItem.LinkOperator.In operator = new LinkConditionItem.LinkOperator.In(refValue);
        LinkConditionItem conditionItem = new LinkConditionItem(subject, operator);
        Condition condition = Condition.of(conditionItem);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForMemberCard(condition);

        // Then: Link 条件的 ReferenceValue 只添加路径，不添加 fieldId
        // 因为 LinkValue.ReferenceValue 没有 fieldId 字段
        assertThat(yield.getField().getFieldIds()).isEmpty();
    }

    @Test
    @DisplayName("不同 ReferenceSource 类型应分别收集")
    void differentReferenceSources_shouldBeCollectedSeparately() {
        // Given: 创建一个条件，包含多种 ReferenceValue
        NumberConditionItem.NumberSubject subject = new NumberConditionItem.NumberSubject(null, "field1");

        // CurrentCard 引用
        ReferenceSource.CurrentCard currentCard = new ReferenceSource.CurrentCard(null);
        NumberConditionItem.NumberValue.ReferenceValue currentCardRef = new NumberConditionItem.NumberValue.ReferenceValue(currentCard, "currentField");

        // Member 引用
        ReferenceSource.Member member = new ReferenceSource.Member(null);
        NumberConditionItem.NumberValue.ReferenceValue memberRef = new NumberConditionItem.NumberValue.ReferenceValue(member, "memberField");

        Condition condition = Condition.and(
                new NumberConditionItem(subject, new NumberConditionItem.NumberOperator.Equal(currentCardRef)),
                new NumberConditionItem(subject, new NumberConditionItem.NumberOperator.Equal(memberRef))
        );

        // When & Then
        Yield currentCardYield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);
        assertThat(currentCardYield.getField().getFieldIds()).contains("currentField");
        assertThat(currentCardYield.getField().getFieldIds()).doesNotContain("memberField");

        Yield memberYield = ConditionYieldBuilder.buildYieldForMemberCard(condition);
        assertThat(memberYield.getField().getFieldIds()).containsExactly("memberField");
    }

    @Test
    @DisplayName("buildYieldForCurrentCard: Subject 带 Path 时字段应收集到关联卡片的 Yield")
    void buildYieldForCurrentCard_subjectWithPath_shouldCollectToLinkedYield() {
        // Given: 创建一个条件，Subject 的 path 指向关联卡片
        Path path = new Path(List.of("parent:SOURCE"));
        TextConditionItem.TextSubject subject = new TextConditionItem.TextSubject(path, "parentStatus");
        TextConditionItem.TextOperator.Equal operator = new TextConditionItem.TextOperator.Equal("进行中");
        TextConditionItem conditionItem = new TextConditionItem(subject, operator);
        Condition condition = Condition.of(conditionItem);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

        // Then: 当前卡片的字段为空，关联卡片包含 parentStatus 字段
        assertThat(yield.getField().getFieldIds()).isEmpty();
        assertThat(yield.getLinks()).hasSize(1);

        YieldLink link = yield.getLinks().get(0);
        assertThat(link.getLinkFieldId()).isEqualTo("parent:SOURCE");
        assertThat(link.getTargetYield().getField().getFieldIds()).containsExactly("parentStatus");
        assertThat(link.getTargetYield().getLinks()).isNull();
    }

    @Test
    @DisplayName("buildYieldForCurrentCard: 多个不同路径的 Subject 应分别收集到对应关联卡片")
    void buildYieldForCurrentCard_multiplePaths_shouldCollectToDifferentLinkedYields() {
        // Given: 创建两个条件，Subject 分别指向不同的关联卡片
        Path parentPath = new Path(List.of("parent:SOURCE"));
        TextConditionItem.TextSubject parentSubject = new TextConditionItem.TextSubject(parentPath, "parentName");
        TextConditionItem parentCondition = new TextConditionItem(parentSubject, new TextConditionItem.TextOperator.Equal("父需求"));

        Path childPath = new Path(List.of("children:TARGET"));
        NumberConditionItem.NumberSubject childSubject = new NumberConditionItem.NumberSubject(childPath, "childCount");
        NumberConditionItem childCondition = new NumberConditionItem(childSubject, new NumberConditionItem.NumberOperator.GreaterThan(
                new NumberConditionItem.NumberValue.StaticValue(5.0)));

        Condition condition = Condition.and(parentCondition, childCondition);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

        // Then: 当前卡片字段为空，有两个关联卡片
        assertThat(yield.getField().getFieldIds()).isEmpty();
        assertThat(yield.getLinks()).hasSize(2);

        // 验证 parent:SOURCE 关联
        YieldLink parentLink = yield.getLinks().stream()
                .filter(l -> l.getLinkFieldId().equals("parent:SOURCE"))
                .findFirst()
                .orElseThrow();
        assertThat(parentLink.getTargetYield().getField().getFieldIds()).containsExactly("parentName");

        // 验证 children:TARGET 关联
        YieldLink childLink = yield.getLinks().stream()
                .filter(l -> l.getLinkFieldId().equals("children:TARGET"))
                .findFirst()
                .orElseThrow();
        assertThat(childLink.getTargetYield().getField().getFieldIds()).containsExactly("childCount");
    }

    @Test
    @DisplayName("buildYieldForCurrentCard: Subject 多级路径应构建嵌套 Yield")
    void buildYieldForCurrentCard_multiLevelSubjectPath_shouldBuildNestedYield() {
        // Given: 创建一个条件，Subject 的 path 指向多级关联
        Path path = new Path(List.of("parent:SOURCE", "grandparent:SOURCE"));
        TextConditionItem.TextSubject subject = new TextConditionItem.TextSubject(path, "grandpaName");
        TextConditionItem conditionItem = new TextConditionItem(subject, new TextConditionItem.TextOperator.Equal("祖父需求"));
        Condition condition = Condition.of(conditionItem);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

        // Then: 验证多级嵌套结构
        assertThat(yield.getField().getFieldIds()).isEmpty();
        assertThat(yield.getLinks()).hasSize(1);

        YieldLink level1 = yield.getLinks().get(0);
        assertThat(level1.getLinkFieldId()).isEqualTo("parent:SOURCE");

        Yield level1Target = level1.getTargetYield();
        assertThat(level1Target.getField().getFieldIds()).isEmpty();
        assertThat(level1Target.getLinks()).hasSize(1);

        YieldLink level2 = level1Target.getLinks().get(0);
        assertThat(level2.getLinkFieldId()).isEqualTo("grandparent:SOURCE");
        assertThat(level2.getTargetYield().getField().getFieldIds()).containsExactly("grandpaName");
    }

    @Test
    @DisplayName("buildYieldForCurrentCard: 相同前缀路径不同深度应正确合并")
    void buildYieldForCurrentCard_samePrefixDifferentDepth_shouldMergeCorrectly() {
        // Given: 两个条件共享相同的第一级路径，但一个是一级，一个是两级
        // 条件1: parent:SOURCE -> parentName
        Path parentPath = new Path(List.of("parent:SOURCE"));
        TextConditionItem.TextSubject parentSubject = new TextConditionItem.TextSubject(parentPath, "parentName");
        TextConditionItem parentCondition = new TextConditionItem(parentSubject, new TextConditionItem.TextOperator.Equal("父需求"));

        // 条件2: parent:SOURCE -> grandparent:SOURCE -> grandpaName
        Path grandpaPath = new Path(List.of("parent:SOURCE", "grandparent:SOURCE"));
        TextConditionItem.TextSubject grandpaSubject = new TextConditionItem.TextSubject(grandpaPath, "grandpaName");
        TextConditionItem grandpaCondition = new TextConditionItem(grandpaSubject, new TextConditionItem.TextOperator.Equal("祖父需求"));

        Condition condition = Condition.and(parentCondition, grandpaCondition);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition);

        // Then: 验证 parent:SOURCE 只有一个 link，但包含两个层级的字段
        assertThat(yield.getField().getFieldIds()).isEmpty();
        assertThat(yield.getLinks()).hasSize(1);

        YieldLink parentLink = yield.getLinks().get(0);
        assertThat(parentLink.getLinkFieldId()).isEqualTo("parent:SOURCE");

        Yield parentYield = parentLink.getTargetYield();
        // parent:SOURCE 层级包含 parentName 字段
        assertThat(parentYield.getField().getFieldIds()).containsExactly("parentName");
        // parent:SOURCE 层级还有一个子关联 grandparent:SOURCE
        assertThat(parentYield.getLinks()).hasSize(1);

        YieldLink grandpaLink = parentYield.getLinks().get(0);
        assertThat(grandpaLink.getLinkFieldId()).isEqualTo("grandparent:SOURCE");
        assertThat(grandpaLink.getTargetYield().getField().getFieldIds()).containsExactly("grandpaName");
    }

    @Test
    @DisplayName("buildYieldForCurrentCard: 多个 Condition 应合并字段")
    void buildYieldForCurrentCard_multipleConditions_shouldMergeFields() {
        // Given: 创建两个独立的 Condition
        TextConditionItem.TextSubject subject1 = new TextConditionItem.TextSubject(null, "field1");
        TextConditionItem conditionItem1 = new TextConditionItem(subject1, new TextConditionItem.TextOperator.Equal("value1"));
        Condition condition1 = Condition.of(conditionItem1);

        NumberConditionItem.NumberSubject subject2 = new NumberConditionItem.NumberSubject(null, "field2");
        NumberConditionItem conditionItem2 = new NumberConditionItem(subject2, new NumberConditionItem.NumberOperator.GreaterThan(
                new NumberConditionItem.NumberValue.StaticValue(5.0)));
        Condition condition2 = Condition.of(conditionItem2);

        // When: 传入多个 Condition
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition1, condition2);

        // Then: 应合并两个条件的字段
        assertThat(yield.getField().getFieldIds()).containsExactlyInAnyOrder("field1", "field2");
    }

    @Test
    @DisplayName("buildYieldForCurrentCard: 空 Condition 数组应返回 basic Yield")
    void buildYieldForCurrentCard_emptyConditionsArray_shouldReturnBasicYield() {
        // When: 传入空数组
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(new Condition[0]);

        // Then
        assertThat(yield).isNotNull();
        assertThat(yield.getField()).isNotNull();
        assertThat(yield.getField().isAllFields()).isFalse();
    }

    @Test
    @DisplayName("buildYieldForCurrentCard: null Condition 数组应返回 basic Yield")
    void buildYieldForCurrentCard_nullConditions_shouldReturnBasicYield() {
        // When: 传入 null
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard((Condition[]) null);

        // Then
        assertThat(yield).isNotNull();
        assertThat(yield.getField()).isNotNull();
        assertThat(yield.getField().isAllFields()).isFalse();
    }

    @Test
    @DisplayName("buildYieldForCurrentCard: 多个 Condition 中 null 元素应被忽略")
    void buildYieldForCurrentCard_conditionsWithNull_shouldIgnoreNull() {
        // Given
        TextConditionItem.TextSubject subject = new TextConditionItem.TextSubject(null, "field1");
        TextConditionItem conditionItem = new TextConditionItem(subject, new TextConditionItem.TextOperator.Equal("value1"));
        Condition condition = Condition.of(conditionItem);

        // When: 传入包含 null 的数组
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition, null);

        // Then
        assertThat(yield.getField().getFieldIds()).containsExactly("field1");
    }

    @Test
    @DisplayName("buildYieldForParameterCard: 多个 Condition 应合并字段")
    void buildYieldForParameterCard_multipleConditions_shouldMergeFields() {
        // Given: 创建两个独立的 Condition，都包含 ParameterCard 引用
        ReferenceSource.ParameterCard paramCard = new ReferenceSource.ParameterCard(null, "paramTypeId");

        NumberConditionItem.NumberSubject subject1 = new NumberConditionItem.NumberSubject(null, "field1");
        NumberConditionItem.NumberValue.ReferenceValue refValue1 = new NumberConditionItem.NumberValue.ReferenceValue(paramCard, "paramField1");
        NumberConditionItem conditionItem1 = new NumberConditionItem(subject1, new NumberConditionItem.NumberOperator.Equal(refValue1));
        Condition condition1 = Condition.of(conditionItem1);

        DateConditionItem.DateSubject.FieldDateSubject subject2 = new DateConditionItem.DateSubject.FieldDateSubject(null, "field2");
        DateConditionItem.DateValue.ReferenceValue refValue2 = new DateConditionItem.DateValue.ReferenceValue(paramCard, "paramField2");
        DateConditionItem conditionItem2 = new DateConditionItem(subject2, new DateConditionItem.DateOperator.Equal(refValue2));
        Condition condition2 = Condition.of(conditionItem2);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForParameterCard(condition1, condition2);

        // Then
        assertThat(yield.getField().getFieldIds()).containsExactlyInAnyOrder("paramField1", "paramField2");
    }

    @Test
    @DisplayName("buildYieldForMemberCard: 多个 Condition 应合并字段")
    void buildYieldForMemberCard_multipleConditions_shouldMergeFields() {
        // Given: 创建两个独立的 Condition，都包含 Member 引用
        ReferenceSource.Member member = new ReferenceSource.Member(null);

        NumberConditionItem.NumberSubject subject1 = new NumberConditionItem.NumberSubject(null, "field1");
        NumberConditionItem.NumberValue.ReferenceValue refValue1 = new NumberConditionItem.NumberValue.ReferenceValue(member, "memberField1");
        NumberConditionItem conditionItem1 = new NumberConditionItem(subject1, new NumberConditionItem.NumberOperator.Equal(refValue1));
        Condition condition1 = Condition.of(conditionItem1);

        NumberConditionItem.NumberSubject subject2 = new NumberConditionItem.NumberSubject(null, "field2");
        NumberConditionItem.NumberValue.ReferenceValue refValue2 = new NumberConditionItem.NumberValue.ReferenceValue(member, "memberField2");
        NumberConditionItem conditionItem2 = new NumberConditionItem(subject2, new NumberConditionItem.NumberOperator.GreaterThan(refValue2));
        Condition condition2 = Condition.of(conditionItem2);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForMemberCard(condition1, condition2);

        // Then
        assertThat(yield.getField().getFieldIds()).containsExactlyInAnyOrder("memberField1", "memberField2");
    }

    @Test
    @DisplayName("buildYieldForContextualCard: 多个 Condition 应合并字段")
    void buildYieldForContextualCard_multipleConditions_shouldMergeFields() {
        // Given: 创建两个独立的 Condition，都包含 ContextualCard 引用
        ReferenceSource.ContextualCard contextualCard = new ReferenceSource.ContextualCard(null, "contextId", 123L);

        NumberConditionItem.NumberSubject subject1 = new NumberConditionItem.NumberSubject(null, "field1");
        NumberConditionItem.NumberValue.ReferenceValue refValue1 = new NumberConditionItem.NumberValue.ReferenceValue(contextualCard, "contextField1");
        NumberConditionItem conditionItem1 = new NumberConditionItem(subject1, new NumberConditionItem.NumberOperator.Equal(refValue1));
        Condition condition1 = Condition.of(conditionItem1);

        NumberConditionItem.NumberSubject subject2 = new NumberConditionItem.NumberSubject(null, "field2");
        NumberConditionItem.NumberValue.ReferenceValue refValue2 = new NumberConditionItem.NumberValue.ReferenceValue(contextualCard, "contextField2");
        NumberConditionItem conditionItem2 = new NumberConditionItem(subject2, new NumberConditionItem.NumberOperator.LessThan(refValue2));
        Condition condition2 = Condition.of(conditionItem2);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForContextualCard(condition1, condition2);

        // Then
        assertThat(yield.getField().getFieldIds()).containsExactlyInAnyOrder("contextField1", "contextField2");
    }

    @Test
    @DisplayName("多个 Condition 的多级路径应正确合并")
    void multipleConditions_multiLevelPaths_shouldMergeCorrectly() {
        // Given: 创建两个 Condition，分别指向不同的关联路径
        Path path1 = new Path(List.of("link1:SOURCE"));
        ReferenceSource.CurrentCard currentCard1 = new ReferenceSource.CurrentCard(path1);
        NumberConditionItem.NumberSubject subject1 = new NumberConditionItem.NumberSubject(null, "field1");
        NumberConditionItem.NumberValue.ReferenceValue refValue1 = new NumberConditionItem.NumberValue.ReferenceValue(currentCard1, "refField1");
        NumberConditionItem conditionItem1 = new NumberConditionItem(subject1, new NumberConditionItem.NumberOperator.Equal(refValue1));
        Condition condition1 = Condition.of(conditionItem1);

        Path path2 = new Path(List.of("link2:TARGET"));
        ReferenceSource.CurrentCard currentCard2 = new ReferenceSource.CurrentCard(path2);
        NumberConditionItem.NumberSubject subject2 = new NumberConditionItem.NumberSubject(null, "field2");
        NumberConditionItem.NumberValue.ReferenceValue refValue2 = new NumberConditionItem.NumberValue.ReferenceValue(currentCard2, "refField2");
        NumberConditionItem conditionItem2 = new NumberConditionItem(subject2, new NumberConditionItem.NumberOperator.Equal(refValue2));
        Condition condition2 = Condition.of(conditionItem2);

        // When
        Yield yield = ConditionYieldBuilder.buildYieldForCurrentCard(condition1, condition2);

        // Then
        assertThat(yield.getField().getFieldIds()).containsExactlyInAnyOrder("field1", "field2");
        assertThat(yield.getLinks()).hasSize(2);

        YieldLink link1 = yield.getLinks().stream()
                .filter(l -> l.getLinkFieldId().equals("link1:SOURCE"))
                .findFirst()
                .orElseThrow();
        assertThat(link1.getTargetYield().getField().getFieldIds()).containsExactly("refField1");

        YieldLink link2 = yield.getLinks().stream()
                .filter(l -> l.getLinkFieldId().equals("link2:TARGET"))
                .findFirst()
                .orElseThrow();
        assertThat(link2.getTargetYield().getField().getFieldIds()).containsExactly("refField2");
    }
}
