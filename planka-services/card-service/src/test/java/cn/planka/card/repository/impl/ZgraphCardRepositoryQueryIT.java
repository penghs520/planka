package cn.planka.card.repository.impl;

import cn.planka.api.card.dto.CardDTO;
import cn.planka.card.model.CardEntity;
import cn.planka.card.service.evaluator.ConditionResolver;
import cn.planka.common.result.PageResult;
import cn.planka.domain.card.*;
import cn.planka.domain.card.CardCycle;
import cn.planka.domain.field.*;
import cn.planka.domain.schema.definition.condition.*;
import cn.planka.api.card.request.*;
import org.junit.jupiter.api.*;
import zgraph.driver.ZgraphCardQueryClient;
import zgraph.driver.ZgraphClient;
import zgraph.driver.ZgraphWriteClient;
import zgraph.driver.config.ZgraphClientConfig;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZgraphCardRepository 查询集成测试
 * <p>
 * 无需启动 SpringBoot、Nacos 和卡片服务，直接实例化 Repository 连接 zgraph（端口 3897）进行测试。
 * 每个测试用例使用独立的 orgId 和 cardTypeId 实现数据隔离。
 */
@DisplayName("ZgraphCardRepository 查询集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ZgraphCardRepositoryQueryIT {

    private static ZgraphCardRepository repository;
    private static ZgraphClient zgraphClient;
    private static ZgraphCardQueryClient queryClient;
    private static ZgraphWriteClient writeClient;
    private static ConditionResolver conditionResolver;

    private static final String TEST_OPERATOR_ID = "test-operator-001";

    // 使用线程安全的集合存储每个测试用例创建的卡片ID，用于清理
    private static final Map<String, List<CardId>> testCaseCardIds = new ConcurrentHashMap<>();

    @BeforeAll
    static void setUp() {
        // 构造 zgraph 配置
        ZgraphClientConfig config = ZgraphClientConfig.builder()
                .serverAddresses(new ZgraphClientConfig.ServerAddress("127.0.0.1", 3897))
                .username("zgraph")
                .password("zgraph")
                .build();

        // 构造 zgraph 客户端
        zgraphClient = new ZgraphClient(config);
        queryClient = new ZgraphCardQueryClient(zgraphClient);
        writeClient = new ZgraphWriteClient(zgraphClient);
        conditionResolver = new ConditionResolver();

        // 构造 Repository
        repository = new ZgraphCardRepository(queryClient, writeClient, conditionResolver);
    }

    @AfterAll
    static void tearDown() {
        // 清理所有测试数据
        testCaseCardIds.values().stream()
                .flatMap(List::stream)
                .forEach(cardId -> {
                    try {
                        repository.discard(cardId, "测试清理", TEST_OPERATOR_ID);
                    } catch (Exception e) {
                        // 忽略清理失败的错误
                    }
                });

        // 关闭客户端连接
        if (zgraphClient != null) {
            zgraphClient.close();
        }
    }

    /**
     * 生成唯一的测试用例标识符
     */
    private String generateTestCaseId(String testName) {
        return testName + "-" + System.currentTimeMillis();
    }

    /**
     * 创建测试卡片的辅助方法
     */
    private CardId createTestCard(String testCaseId, String orgId, String cardTypeId, String title, CardCycle cardCycle) {
        CardEntity cardEntity = new CardEntity(
                CardId.generate(),
                0L,
                null,
                OrgId.of(orgId),
                CardTypeId.of(cardTypeId),
                CardTitle.pure(title),
                cardCycle,
                null,
                null,
                LocalDateTime.now()
        );
        CardId cardId = repository.create(cardEntity);
        testCaseCardIds.computeIfAbsent(testCaseId, k -> new ArrayList<>()).add(cardId);
        return cardId;
    }

    /**
     * 创建标题包含条件的辅助方法
     */
    private TitleConditionItem titleContains(String keyword) {
        return new TitleConditionItem(
                new TitleConditionItem.TitleSubject(null),
                new TitleConditionItem.TitleOperator.Contains(keyword)
        );
    }

    /**
     * 创建查询请求的辅助方法
     */
    private CardQueryRequest createQueryRequest(String orgId, String cardTypeId, ConditionNode condition) {
        return createQueryRequest(orgId, List.of(cardTypeId), condition);
    }

    /**
     * 创建查询请求的辅助方法
     */
    private CardQueryRequest createQueryRequest(String orgId, List<String> cardTypeIds, ConditionNode condition) {
        CardQueryRequest request = new CardQueryRequest();

        QueryContext queryContext = new QueryContext();
        queryContext.setOperatorId(TEST_OPERATOR_ID);
        queryContext.setOrgId(orgId);
        request.setQueryContext(queryContext);

        QueryScope queryScope = new QueryScope();
        queryScope.setCardTypeIds(cardTypeIds);
        request.setQueryScope(queryScope);

        if (condition != null) {
            request.setCondition(new Condition(condition));
        }

        request.setYield(new Yield());
        return request;
    }

    /**
     * 创建文本字段值
     */
    private TextFieldValue textValue(String fieldId, String value) {
        return new TextFieldValue(fieldId, value);
    }

    /**
     * 创建数字字段值
     */
    private NumberFieldValue numberValue(String fieldId, Double value) {
        return new NumberFieldValue(fieldId, value);
    }

    /**
     * 创建日期字段值
     */
    private DateFieldValue dateValue(String fieldId, String dateStr) {
        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
        Long timestamp = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        return new DateFieldValue(fieldId, timestamp);
    }

    /**
     * 创建枚举字段值
     */
    private EnumFieldValue enumValue(String fieldId, String... optionIds) {
        return new EnumFieldValue(fieldId, new ArrayList<>(Arrays.asList(optionIds)));
    }

    /**
     * 创建链接字段值
     */
    private WebLinkFieldValue urlValue(String fieldId, String url, String displayText) {
        return new WebLinkFieldValue(fieldId, url, displayText);
    }

    /**
     * 创建带字段值的测试卡片
     */
    private CardId createTestCardWithField(String testCaseId, String orgId, String cardTypeId,
                                           String title, FieldValue<?> fieldValue) {
        Map<String, FieldValue<?>> fieldValues = new HashMap<>();
        if (fieldValue != null) {
            fieldValues.put(fieldValue.getFieldId(), fieldValue);
        }
        return createTestCardWithFields(testCaseId, orgId, cardTypeId, title, fieldValues);
    }

    /**
     * 创建带两个字段值的测试卡片
     */
    private CardId createTestCardWithTwoFields(String testCaseId, String orgId, String cardTypeId,
                                               String title, FieldValue<?> value1,
                                               FieldValue<?> value2) {
        Map<String, FieldValue<?>> fieldValues = new HashMap<>();
        fieldValues.put(value1.getFieldId(), value1);
        fieldValues.put(value2.getFieldId(), value2);
        return createTestCardWithFields(testCaseId, orgId, cardTypeId, title, fieldValues);
    }

    /**
     * 创建带三个字段值的测试卡片
     */
    private CardId createTestCardWithThreeFields(String testCaseId, String orgId, String cardTypeId,
                                                 String title, String fieldId1, FieldValue<?> value1,
                                                 String fieldId2, FieldValue<?> value2,
                                                 String fieldId3, FieldValue<?> value3) {
        Map<String, FieldValue<?>> fieldValues = new HashMap<>();
        fieldValues.put(fieldId1, value1);
        fieldValues.put(fieldId2, value2);
        fieldValues.put(fieldId3, value3);
        return createTestCardWithFields(testCaseId, orgId, cardTypeId, title, fieldValues);
    }

    /**
     * 创建带字段值的测试卡片（基础方法）
     */
    private CardId createTestCardWithFields(String testCaseId, String orgId, String cardTypeId,
                                            String title, Map<String, FieldValue<?>> fieldValues) {
        CardEntity cardEntity = new CardEntity(
                CardId.generate(),
                0L,
                null,
                OrgId.of(orgId),
                CardTypeId.of(cardTypeId),
                CardTitle.pure(title),
                CardCycle.ACTIVE,
                null,
                null,
                LocalDateTime.now()
        );
        cardEntity.setFieldValues(fieldValues);

        CardId cardId = repository.create(cardEntity);
        testCaseCardIds.computeIfAbsent(testCaseId, k -> new ArrayList<>()).add(cardId);
        return cardId;
    }

    // ==================== 测试用例 ====================

    @Test
    @Order(1)
    @DisplayName("测试标题包含查询 - 基础流程验证")
    void testQueryByTitleContains() {
        // 生成唯一的测试用例ID和数据
        String testCaseId = generateTestCaseId("title-contains");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String keyword = "集成测试" + System.currentTimeMillis();

        // Given - 创建测试卡片
        CardId cardId1 = createTestCard(testCaseId, orgId, cardTypeId, "任务A-" + keyword, CardCycle.ACTIVE);
        CardId cardId2 = createTestCard(testCaseId, orgId, cardTypeId, "任务B-" + keyword, CardCycle.ACTIVE);
        createTestCard(testCaseId, orgId, cardTypeId, "无关任务C", CardCycle.ACTIVE);

        // When - 执行标题包含查询
        TitleConditionItem condition = titleContains(keyword);
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 验证结果
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId1, cardId2);
    }

    @Test
    @Order(2)
    @DisplayName("测试无条件查询 - 查询所有卡片")
    void testQueryWithoutCondition() {
        // 生成唯一的测试用例ID和数据
        String testCaseId = generateTestCaseId("no-condition");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;

        // Given - 创建测试卡片
        createTestCard(testCaseId, orgId, cardTypeId, "任务A-无条件查询", CardCycle.ACTIVE);
        createTestCard(testCaseId, orgId, cardTypeId, "任务B-无条件查询", CardCycle.ACTIVE);
        createTestCard(testCaseId, orgId, cardTypeId, "任务C-无条件查询", CardCycle.ACTIVE);

        // When - 执行无条件查询
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, null);
        List<CardDTO> result = repository.query(request);

        // Then - 验证结果
        assertThat(result).hasSize(3);
    }

    @Test
    @Order(3)
    @DisplayName("测试根据卡片ID列表查询")
    void testQueryByCardIds() {
        // 生成唯一的测试用例ID和数据
        String testCaseId = generateTestCaseId("by-ids");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;

        // Given - 创建新卡片
        CardId cardId = createTestCard(testCaseId, orgId, cardTypeId, "ID查询测试卡片", CardCycle.ACTIVE);

        // When - 根据ID查询
        List<CardDTO> result = repository.findByIds(List.of(cardId), new Yield(), TEST_OPERATOR_ID);

        // Then - 验证结果
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId);
    }

    @Test
    @Order(4)
    @DisplayName("测试分页查询")
    void testPageQuery() {
        // 生成唯一的测试用例ID和数据
        String testCaseId = generateTestCaseId("pagination");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;

        // Given - 创建多个测试卡片
        for (int i = 0; i < 5; i++) {
            createTestCard(testCaseId, orgId, cardTypeId, "分页测试卡片" + i, CardCycle.ACTIVE);
        }

        // When - 执行分页查询
        CardPageQueryRequest request = new CardPageQueryRequest();
        QueryContext queryContext = new QueryContext();
        queryContext.setOperatorId(TEST_OPERATOR_ID);
        queryContext.setOrgId(orgId);
        request.setQueryContext(queryContext);

        QueryScope queryScope = new QueryScope();
        queryScope.setCardTypeIds(List.of(cardTypeId));
        request.setQueryScope(queryScope);

        SortAndPage sortAndPage = new SortAndPage();
        Page page = new Page();
        page.setPageNum(1);
        page.setPageSize(2);
        sortAndPage.setPage(page);
        request.setSortAndPage(sortAndPage);

        request.setYield(new Yield());

        PageResult<CardDTO> result = repository.pageQuery(request);

        // Then - 验证分页结果
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(5);
    }

    // ==================== 文本条件测试 ====================

    @Test
    @Order(10)
    @DisplayName("测试文本等于查询")
    void testQueryByTextEqual() {
        String testCaseId = generateTestCaseId("text-eq");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "text-field-001";
        String targetValue = "精确匹配文本";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", textValue(fieldId, targetValue));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", textValue(fieldId, "其他文本"));

        // When
        TextConditionItem condition = new TextConditionItem(
                new TextConditionItem.TextSubject(null, fieldId),
                new TextConditionItem.TextOperator.Equal(targetValue)
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(11)
    @DisplayName("测试文本不等于查询")
    void testQueryByTextNotEqual() {
        String testCaseId = generateTestCaseId("text-ne");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "text-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", textValue(fieldId, "排除文本"));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", textValue(fieldId, "保留文本"));
        CardId cardId3 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片C", textValue(fieldId, "其他文本"));

        // When
        TextConditionItem condition = new TextConditionItem(
                new TextConditionItem.TextSubject(null, fieldId),
                new TextConditionItem.TextOperator.NotEqual("排除文本")
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId2, cardId3);
    }

    @Test
    @Order(12)
    @DisplayName("测试文本不包含查询")
    void testQueryByTextNotContains() {
        String testCaseId = generateTestCaseId("text-not-contains");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "text-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", textValue(fieldId, "包含关键字的数据"));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", textValue(fieldId, "不包含数据"));

        // When
        TextConditionItem condition = new TextConditionItem(
                new TextConditionItem.TextSubject(null, fieldId),
                new TextConditionItem.TextOperator.NotContains("关键字")
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId2);
    }

    @Test
    @Order(13)
    @DisplayName("测试文本以...开始查询")
    void testQueryByTextStartsWith() {
        String testCaseId = generateTestCaseId("text-starts");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "text-field-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", textValue(fieldId, "PREFIX-测试数据"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", textValue(fieldId, "其他数据"));

        // When
        TextConditionItem condition = new TextConditionItem(
                new TextConditionItem.TextSubject(null, fieldId),
                new TextConditionItem.TextOperator.StartsWith("PREFIX")
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(14)
    @DisplayName("测试文本以...结束查询")
    void testQueryByTextEndsWith() {
        String testCaseId = generateTestCaseId("text-ends");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "text-field-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", textValue(fieldId, "测试数据-SUFFIX"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", textValue(fieldId, "测试数据"));

        // When
        TextConditionItem condition = new TextConditionItem(
                new TextConditionItem.TextSubject(null, fieldId),
                new TextConditionItem.TextOperator.EndsWith("SUFFIX")
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(15)
    @DisplayName("测试文本为空查询")
    void testQueryByTextIsEmpty() {
        String testCaseId = generateTestCaseId("text-is-empty");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "text-field-001";

        // Given - 创建一个有空文本值的卡片和一个有文本值的卡片
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", textValue(fieldId, ""));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", textValue(fieldId, "有值文本"));

        // When
        TextConditionItem condition = new TextConditionItem(
                new TextConditionItem.TextSubject(null, fieldId),
                new TextConditionItem.TextOperator.IsEmpty()
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(16)
    @DisplayName("测试文本不为空查询")
    void testQueryByTextIsNotEmpty() {
        String testCaseId = generateTestCaseId("text-is-not-empty");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "text-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", textValue(fieldId, ""));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", textValue(fieldId, "有值文本"));
        CardId cardId3 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片C", textValue(fieldId, "其他文本"));

        // When
        TextConditionItem condition = new TextConditionItem(
                new TextConditionItem.TextSubject(null, fieldId),
                new TextConditionItem.TextOperator.IsNotEmpty()
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId2, cardId3);
    }

    // ==================== 数字条件测试 ====================

    @Test
    @Order(20)
    @DisplayName("测试数字大于查询")
    void testQueryByNumberGreaterThan() {
        String testCaseId = generateTestCaseId("num-gt");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "number-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", numberValue(fieldId, 10.0));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", numberValue(fieldId, 20.0));
        CardId cardId3 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片C", numberValue(fieldId, 30.0));

        // When
        NumberConditionItem condition = new NumberConditionItem(
                new NumberConditionItem.NumberSubject(null, fieldId),
                new NumberConditionItem.NumberOperator.GreaterThan(
                        new NumberConditionItem.NumberValue.StaticValue(15.0)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId2, cardId3);
    }

    @Test
    @Order(21)
    @DisplayName("测试数字大于等于查询")
    void testQueryByNumberGreaterThanOrEqual() {
        String testCaseId = generateTestCaseId("num-ge");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "number-field-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", numberValue(fieldId, 10.0));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", numberValue(fieldId, 20.0));

        // When
        NumberConditionItem condition = new NumberConditionItem(
                new NumberConditionItem.NumberSubject(null, fieldId),
                new NumberConditionItem.NumberOperator.GreaterThanOrEqual(
                        new NumberConditionItem.NumberValue.StaticValue(10.0)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId1, cardId2);
    }

    @Test
    @Order(22)
    @DisplayName("测试数字小于查询")
    void testQueryByNumberLessThan() {
        String testCaseId = generateTestCaseId("num-lt");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "number-field-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", numberValue(fieldId, 10.0));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", numberValue(fieldId, 20.0));

        // When
        NumberConditionItem condition = new NumberConditionItem(
                new NumberConditionItem.NumberSubject(null, fieldId),
                new NumberConditionItem.NumberOperator.LessThan(
                        new NumberConditionItem.NumberValue.StaticValue(15.0)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(23)
    @DisplayName("测试数字小于等于查询")
    void testQueryByNumberLessThanOrEqual() {
        String testCaseId = generateTestCaseId("num-le");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "number-field-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", numberValue(fieldId, 10.0));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", numberValue(fieldId, 20.0));

        // When
        NumberConditionItem condition = new NumberConditionItem(
                new NumberConditionItem.NumberSubject(null, fieldId),
                new NumberConditionItem.NumberOperator.LessThanOrEqual(
                        new NumberConditionItem.NumberValue.StaticValue(20.0)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId1, cardId2);
    }

    @Test
    @Order(24)
    @DisplayName("测试数字范围内查询")
    void testQueryByNumberBetween() {
        String testCaseId = generateTestCaseId("num-between");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "number-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", numberValue(fieldId, 10.0));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", numberValue(fieldId, 15.0));
        CardId cardId3 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片C", numberValue(fieldId, 20.0));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片D", numberValue(fieldId, 30.0));

        // When
        NumberConditionItem condition = new NumberConditionItem(
                new NumberConditionItem.NumberSubject(null, fieldId),
                new NumberConditionItem.NumberOperator.Between(
                        new NumberConditionItem.NumberValue.StaticValue(12.0),
                        new NumberConditionItem.NumberValue.StaticValue(25.0)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId2, cardId3);
    }

    // ==================== 日期条件测试 ====================

    @Test
    @Order(30)
    @DisplayName("测试日期等于特定日期查询")
    void testQueryByDateEqualSpecific() {
        String testCaseId = generateTestCaseId("date-eq");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";
        String targetDate = "2024-03-15";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", dateValue(fieldId, targetDate));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", dateValue(fieldId, "2024-03-16"));

        // When
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.Specific(targetDate)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 验证包含目标卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .contains(cardId1);
    }

    @Test
    @Order(31)
    @DisplayName("测试日期早于查询")
    void testQueryByDateBefore() {
        String testCaseId = generateTestCaseId("date-before");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", dateValue(fieldId, "2024-03-10"));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", dateValue(fieldId, "2024-03-14"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片C", dateValue(fieldId, "2024-03-20"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片D", dateValue(fieldId, "2024-03-15"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片E", null);

        // When
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Before(
                        new DateConditionItem.DateValue.Specific("2024-03-15")
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId1, cardId2);
    }

    @Test
    @Order(32)
    @DisplayName("测试日期晚于查询")
    void testQueryByDateAfter() {
        String testCaseId = generateTestCaseId("date-after");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", dateValue(fieldId, "2024-03-10"));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", dateValue(fieldId, "2024-03-20"));

        // When
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.After(
                        new DateConditionItem.DateValue.Specific("2024-03-15")
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId2);
    }

    @Test
    @Order(33)
    @DisplayName("测试日期范围内查询")
    void testQueryByDateBetween() {
        String testCaseId = generateTestCaseId("date-between");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", dateValue(fieldId, "2024-03-01"));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", dateValue(fieldId, "2024-03-10"));
        CardId cardId3 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片C", dateValue(fieldId, "2024-03-20"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片D", dateValue(fieldId, "2024-03-30"));

        // When
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Between(
                        new DateConditionItem.DateValue.Specific("2024-03-05"),
                        new DateConditionItem.DateValue.Specific("2024-03-25")
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId2, cardId3);
    }

    @Test
    @Order(34)
    @DisplayName("测试日期为空查询")
    void testQueryByDateIsEmpty() {
        String testCaseId = generateTestCaseId("date-is-empty");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // Given - 创建一个有日期值的卡片和一个无日期值的卡片（通过不设置该字段）
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", dateValue(fieldId, "2024-03-15"));
        // 卡片B不设置日期字段值
        CardId cardId2 = createTestCard(testCaseId, orgId, cardTypeId, "卡片B", CardCycle.ACTIVE);

        // When
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.IsEmpty()
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId2);
    }

    @Test
    @Order(35)
    @DisplayName("测试日期不为空查询")
    void testQueryByDateIsNotEmpty() {
        String testCaseId = generateTestCaseId("date-is-not-empty");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", dateValue(fieldId, "2024-03-15"));
        createTestCard(testCaseId, orgId, cardTypeId, "卡片B", CardCycle.ACTIVE); // 无日期值

        // When
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.IsNotEmpty()
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    // ==================== 日期关键字值测试 ====================

    @Test
    @Order(36)
    @DisplayName("测试日期关键字查询 - TODAY")
    void testQueryByDateKeywordToday() {
        String testCaseId = generateTestCaseId("date-keyword-today");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        String today = java.time.LocalDate.now().toString();
        String yesterday = java.time.LocalDate.now().minusDays(1).toString();

        // Given - 今天的卡片和昨天的卡片
        CardId cardIdToday = createTestCardWithField(testCaseId, orgId, cardTypeId, "今天卡片", dateValue(fieldId, today));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "昨天卡片", dateValue(fieldId, yesterday));

        // When - 使用 TODAY 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回今天的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdToday);
    }

    @Test
    @Order(37)
    @DisplayName("测试日期关键字查询 - YESTERDAY")
    void testQueryByDateKeywordYesterday() {
        String testCaseId = generateTestCaseId("date-keyword-yesterday");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        String yesterday = java.time.LocalDate.now().minusDays(1).toString();
        String twoDaysAgo = java.time.LocalDate.now().minusDays(2).toString();

        // Given - 只创建昨天和前天的卡片（避免边界问题）
        CardId cardIdYesterday = createTestCardWithField(testCaseId, orgId, cardTypeId, "昨天卡片", dateValue(fieldId, yesterday));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "前天卡片", dateValue(fieldId, twoDaysAgo));

        // When - 使用 YESTERDAY 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.YESTERDAY)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回昨天的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdYesterday);
    }

    @Test
    @Order(38)
    @DisplayName("测试日期关键字查询 - TOMORROW")
    void testQueryByDateKeywordTomorrow() {
        String testCaseId = generateTestCaseId("date-keyword-tomorrow");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        String tomorrow = java.time.LocalDate.now().plusDays(1).toString();

        // Given - 只创建明天的卡片
        CardId cardIdTomorrow = createTestCardWithField(testCaseId, orgId, cardTypeId, "明天卡片", dateValue(fieldId, tomorrow));

        // When - 使用 TOMORROW 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TOMORROW)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回明天的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdTomorrow);
    }

    @Test
    @Order(43)
    @DisplayName("测试日期关键字查询 - THIS_WEEK")
    void testQueryByDateKeywordThisWeek() {
        String testCaseId = generateTestCaseId("date-keyword-this-week");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // 获取本周三、上周一、下周一的日期（确保边界清晰）
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate thisWednesday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).plusDays(2);
        java.time.LocalDate lastWeekMonday = today.minusWeeks(1).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        java.time.LocalDate nextWeekMonday = today.plusWeeks(1).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        // Given - 本周内、上周、下周的卡片
        CardId cardIdThisWeek = createTestCardWithField(testCaseId, orgId, cardTypeId, "本周卡片", dateValue(fieldId, thisWednesday.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "上周卡片", dateValue(fieldId, lastWeekMonday.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "下周卡片", dateValue(fieldId, nextWeekMonday.toString()));

        // When - 使用 THIS_WEEK 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_WEEK)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回本周的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdThisWeek);
    }

    @Test
    @Order(44)
    @DisplayName("测试日期关键字查询 - LAST_WEEK")
    void testQueryByDateKeywordLastWeek() {
        String testCaseId = generateTestCaseId("date-keyword-last-week");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // 获取上周三、本周一的日期（确保边界清晰）
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate lastWeekWednesday = today.minusWeeks(1).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).plusDays(2);
        java.time.LocalDate thisWeekMonday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        // Given - 上周和本周的卡片
        CardId cardIdLastWeek = createTestCardWithField(testCaseId, orgId, cardTypeId, "上周卡片", dateValue(fieldId, lastWeekWednesday.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "本周卡片", dateValue(fieldId, thisWeekMonday.toString()));

        // When - 使用 LAST_WEEK 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.LAST_WEEK)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回上周的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdLastWeek);
    }

    @Test
    @Order(45)
    @DisplayName("测试日期关键字查询 - NEXT_WEEK")
    void testQueryByDateKeywordNextWeek() {
        String testCaseId = generateTestCaseId("date-keyword-next-week");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // 获取下周的日期
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate nextWeekMonday = today.plusWeeks(1).with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        java.time.LocalDate thisWeekDay = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        // Given - 下周和本周的卡片
        CardId cardIdNextWeek = createTestCardWithField(testCaseId, orgId, cardTypeId, "下周卡片", dateValue(fieldId, nextWeekMonday.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "本周卡片", dateValue(fieldId, thisWeekDay.toString()));

        // When - 使用 NEXT_WEEK 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.NEXT_WEEK)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回下周的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdNextWeek);
    }

    @Test
    @Order(46)
    @DisplayName("测试日期关键字查询 - THIS_MONTH")
    void testQueryByDateKeywordThisMonth() {
        String testCaseId = generateTestCaseId("date-keyword-this-month");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // 获取本月15号、上月1号、下月1号的日期（确保边界清晰）
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate thisMonthDay = today.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth()).plusDays(14);
        java.time.LocalDate lastMonthDay = today.minusMonths(1).with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());
        java.time.LocalDate nextMonthDay = today.plusMonths(1).with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());

        // Given - 本月内、上月、下月的卡片
        CardId cardIdThisMonth = createTestCardWithField(testCaseId, orgId, cardTypeId, "本月卡片", dateValue(fieldId, thisMonthDay.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "上月卡片", dateValue(fieldId, lastMonthDay.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "下月卡片", dateValue(fieldId, nextMonthDay.toString()));

        // When - 使用 THIS_MONTH 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_MONTH)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回本月的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdThisMonth);
    }

    @Test
    @Order(47)
    @DisplayName("测试日期关键字查询 - LAST_MONTH")
    void testQueryByDateKeywordLastMonth() {
        String testCaseId = generateTestCaseId("date-keyword-last-month");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // 获取上月和本月的日期
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate firstDayOfMonth = today.with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());
        java.time.LocalDate lastMonthDay = firstDayOfMonth.minusDays(1);

        // Given - 上月和本月的卡片
        CardId cardIdLastMonth = createTestCardWithField(testCaseId, orgId, cardTypeId, "上月卡片", dateValue(fieldId, lastMonthDay.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "本月卡片", dateValue(fieldId, today.toString()));

        // When - 使用 LAST_MONTH 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.LAST_MONTH)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回上月的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdLastMonth);
    }

    @Test
    @Order(48)
    @DisplayName("测试日期关键字查询 - NEXT_MONTH")
    void testQueryByDateKeywordNextMonth() {
        String testCaseId = generateTestCaseId("date-keyword-next-month");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // 获取下月和本月的日期
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate lastDayOfMonth = today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        java.time.LocalDate nextMonthDay = lastDayOfMonth.plusDays(1);

        // Given - 下月和本月的卡片
        CardId cardIdNextMonth = createTestCardWithField(testCaseId, orgId, cardTypeId, "下月卡片", dateValue(fieldId, nextMonthDay.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "本月卡片", dateValue(fieldId, today.toString()));

        // When - 使用 NEXT_MONTH 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.NEXT_MONTH)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回下月的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdNextMonth);
    }

    @Test
    @Order(49)
    @DisplayName("测试日期关键字查询 - THIS_QUARTER")
    void testQueryByDateKeywordThisQuarter() {
        String testCaseId = generateTestCaseId("date-keyword-this-quarter");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // 获取本季度和上季度的日期
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate firstDayOfQuarter = today.with(today.getMonth().firstMonthOfQuarter())
                .with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());
        java.time.LocalDate lastDayOfQuarter = today.with(today.getMonth().firstMonthOfQuarter())
                .plusMonths(2)
                .with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        java.time.LocalDate lastQuarterDay = firstDayOfQuarter.minusDays(1);
        java.time.LocalDate nextQuarterDay = lastDayOfQuarter.plusDays(1);

        // Given - 本季度内、上季度、下季度的卡片
        CardId cardIdThisQuarter = createTestCardWithField(testCaseId, orgId, cardTypeId, "本季度卡片", dateValue(fieldId, today.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "上季度卡片", dateValue(fieldId, lastQuarterDay.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "下季度卡片", dateValue(fieldId, nextQuarterDay.toString()));

        // When - 使用 THIS_QUARTER 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_QUARTER)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回本季度的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdThisQuarter);
    }

    @Test
    @Order(50)
    @DisplayName("测试日期关键字查询 - THIS_YEAR")
    void testQueryByDateKeywordThisYear() {
        String testCaseId = generateTestCaseId("date-keyword-this-year");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        // 获取今年、去年、明年的日期
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate firstDayOfYear = today.with(java.time.temporal.TemporalAdjusters.firstDayOfYear());
        java.time.LocalDate lastDayOfYear = today.with(java.time.temporal.TemporalAdjusters.lastDayOfYear());
        java.time.LocalDate lastYearDay = firstDayOfYear.minusDays(1);
        java.time.LocalDate nextYearDay = lastDayOfYear.plusDays(1);

        // Given - 今年内、去年、明年的卡片
        CardId cardIdThisYear = createTestCardWithField(testCaseId, orgId, cardTypeId, "今年卡片", dateValue(fieldId, today.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "去年卡片", dateValue(fieldId, lastYearDay.toString()));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "明年卡片", dateValue(fieldId, nextYearDay.toString()));

        // When - 使用 THIS_YEAR 关键字查询
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Equal(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.THIS_YEAR)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回今年的卡片
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdThisYear);
    }

    // ==================== 最近日期范围测试 ====================

    @Test
    @Order(51)
    @DisplayName("测试日期最近范围查询 - 最近7天")
    void testQueryByDateRecent7Days() {
        String testCaseId = generateTestCaseId("date-recent-7d");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        String today = java.time.LocalDate.now().toString();
        String threeDaysAgo = java.time.LocalDate.now().minusDays(3).toString();
        String eightDaysAgo = java.time.LocalDate.now().minusDays(8).toString();

        // Given - 3天前（在范围内）和8天前（在范围外）的卡片
        CardId cardIdRecent = createTestCardWithField(testCaseId, orgId, cardTypeId, "3天前卡片", dateValue(fieldId, threeDaysAgo));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "8天前卡片", dateValue(fieldId, eightDaysAgo));
        CardId cardIdToday = createTestCardWithField(testCaseId, orgId, cardTypeId, "今天卡片", dateValue(fieldId, today));

        // When - 使用最近7天查询（Between）
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Between(
                        new DateConditionItem.DateValue.RecentValue(7, DateConditionItem.TimeUnit.DAY),
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该返回今天和3天前的卡片
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardIdToday, cardIdRecent);
    }

    @Test
    @Order(52)
    @DisplayName("测试日期最近范围查询 - 最近30天")
    void testQueryByDateRecent30Days() {
        String testCaseId = generateTestCaseId("date-recent-30d");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        String today = java.time.LocalDate.now().toString();
        String fifteenDaysAgo = java.time.LocalDate.now().minusDays(15).toString();
        String thirtyFiveDaysAgo = java.time.LocalDate.now().minusDays(35).toString();

        // Given - 今天、15天前（在范围内）和35天前（在范围外）的卡片
        CardId cardIdToday = createTestCardWithField(testCaseId, orgId, cardTypeId, "今天卡片", dateValue(fieldId, today));
        CardId cardIdRecent = createTestCardWithField(testCaseId, orgId, cardTypeId, "15天前卡片", dateValue(fieldId, fifteenDaysAgo));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "35天前卡片", dateValue(fieldId, thirtyFiveDaysAgo));

        // When - 使用最近30天查询（Between 30天前 到 今天）
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Between(
                        new DateConditionItem.DateValue.RecentValue(30, DateConditionItem.TimeUnit.DAY),
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该返回今天和15天前的卡片
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardIdToday, cardIdRecent);
    }

    // ==================== 未来日期范围测试 ====================

    @Test
    @Order(53)
    @DisplayName("测试日期未来范围查询 - 未来7天")
    void testQueryByDateFuture7Days() {
        String testCaseId = generateTestCaseId("date-future-7d");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        String today = java.time.LocalDate.now().toString();
        String threeDaysLater = java.time.LocalDate.now().plusDays(3).toString();
        String tenDaysLater = java.time.LocalDate.now().plusDays(10).toString();

        // Given - 今天、3天后（在范围内）和10天后（在范围外）的卡片
        CardId cardIdToday = createTestCardWithField(testCaseId, orgId, cardTypeId, "今天卡片", dateValue(fieldId, today));
        CardId cardIdFuture1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "3天后卡片", dateValue(fieldId, threeDaysLater));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "10天后卡片", dateValue(fieldId, tenDaysLater));

        // When - 使用未来7天查询（Between）
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Between(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY),
                        new DateConditionItem.DateValue.FutureValue(7, DateConditionItem.TimeUnit.DAY)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该返回今天和3天后的卡片
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardIdToday, cardIdFuture1);
    }

    @Test
    @Order(54)
    @DisplayName("测试日期未来范围查询 - 未来30天")
    void testQueryByDateFuture30Days() {
        String testCaseId = generateTestCaseId("date-future-30d");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "date-field-001";

        String today = java.time.LocalDate.now().toString();
        String fifteenDaysLater = java.time.LocalDate.now().plusDays(15).toString();
        String thirtyFiveDaysLater = java.time.LocalDate.now().plusDays(35).toString();

        // Given - 今天、15天后（在范围内）和35天后（在范围外）的卡片
        CardId cardIdToday = createTestCardWithField(testCaseId, orgId, cardTypeId, "今天卡片", dateValue(fieldId, today));
        CardId cardIdFuture = createTestCardWithField(testCaseId, orgId, cardTypeId, "15天后卡片", dateValue(fieldId, fifteenDaysLater));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "35天后卡片", dateValue(fieldId, thirtyFiveDaysLater));

        // When - 使用未来30天查询（Between 今天 到 30天后）
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.FieldDateSubject(null, fieldId),
                new DateConditionItem.DateOperator.Between(
                        new DateConditionItem.DateValue.KeyDateValue(DateConditionItem.KeyDate.TODAY),
                        new DateConditionItem.DateValue.FutureValue(30, DateConditionItem.TimeUnit.DAY)
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该返回今天和15天后的卡片
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardIdToday, cardIdFuture);
    }

    // ==================== 条件组合测试 ====================

    @Test
    @Order(60)
    @DisplayName("测试AND条件组合查询")
    void testQueryWithAndCondition() {
        String testCaseId = generateTestCaseId("and");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String textFieldId = "text-field-001";
        String numFieldId = "num-field-001";

        // Given - 创建符合不同条件的卡片
        // 卡片A: 文本匹配，数字不匹配
        createTestCardWithTwoFields(testCaseId, orgId, cardTypeId, "卡片A",
                textValue(textFieldId, "关键字"), numberValue(numFieldId, 5.0));
        // 卡片B: 文本匹配，数字匹配
        CardId cardIdB = createTestCardWithTwoFields(testCaseId, orgId, cardTypeId, "卡片B",
                textValue(textFieldId, "关键字"), numberValue(numFieldId, 15.0));
        // 卡片C: 文本不匹配，数字匹配
        createTestCardWithTwoFields(testCaseId, orgId, cardTypeId, "卡片C",
                textValue(textFieldId, "其他"), numberValue(numFieldId, 15.0));

        // When - 执行AND组合查询
        TextConditionItem textCondition = new TextConditionItem(
                new TextConditionItem.TextSubject(null, textFieldId),
                new TextConditionItem.TextOperator.Contains("关键字")
        );
        NumberConditionItem numCondition = new NumberConditionItem(
                new NumberConditionItem.NumberSubject(null, numFieldId),
                new NumberConditionItem.NumberOperator.GreaterThan(
                        new NumberConditionItem.NumberValue.StaticValue(10.0)
                )
        );
        ConditionGroup andGroup = ConditionGroup.and(textCondition, numCondition);

        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, andGroup);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardIdB);
    }

    @Test
    @Order(61)
    @DisplayName("测试OR条件组合查询")
    void testQueryWithOrCondition() {
        String testCaseId = generateTestCaseId("or");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String textFieldId = "text-field-001";
        String numFieldId = "num-field-001";

        // Given
        // 卡片A: 文本匹配
        CardId cardIdA = createTestCardWithTwoFields(testCaseId, orgId, cardTypeId, "卡片A",
                textValue(textFieldId, "关键字"), numberValue(numFieldId, 5.0));
        // 卡片B: 数字匹配
        CardId cardIdB = createTestCardWithTwoFields(testCaseId, orgId, cardTypeId, "卡片B",
                textValue(textFieldId, "其他"), numberValue(numFieldId, 15.0));
        // 卡片C: 都不匹配
        createTestCardWithTwoFields(testCaseId, orgId, cardTypeId, "卡片C",
                textValue(textFieldId, "其他"), numberValue(numFieldId, 5.0));

        // When - 执行OR组合查询
        TextConditionItem textCondition = new TextConditionItem(
                new TextConditionItem.TextSubject(null, textFieldId),
                new TextConditionItem.TextOperator.Contains("关键字")
        );
        NumberConditionItem numCondition = new NumberConditionItem(
                new NumberConditionItem.NumberSubject(null, numFieldId),
                new NumberConditionItem.NumberOperator.GreaterThan(
                        new NumberConditionItem.NumberValue.StaticValue(10.0)
                )
        );
        ConditionGroup orGroup = ConditionGroup.or(textCondition, numCondition);

        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, orGroup);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardIdA, cardIdB);
    }

    @Test
    @Order(62)
    @DisplayName("测试嵌套条件组合查询 - (A AND B) OR C")
    void testQueryWithNestedCondition() {
        String testCaseId = generateTestCaseId("nested");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldA = "field-a";
        String fieldB = "field-b";
        String fieldC = "field-c";

        // Given
        // 卡片1: A=1, B=1, C=0 -> (1 AND 1) OR 0 = 1 匹配
        CardId cardId1 = createTestCardWithThreeFields(testCaseId, orgId, cardTypeId, "卡片1",
                fieldA, textValue(fieldA, "匹配A"), fieldB, textValue(fieldB, "匹配B"), fieldC, textValue(fieldC, "其他"));
        // 卡片2: A=1, B=0, C=1 -> (1 AND 0) OR 1 = 1 匹配
        CardId cardId2 = createTestCardWithThreeFields(testCaseId, orgId, cardTypeId, "卡片2",
                fieldA, textValue(fieldA, "匹配A"), fieldB, textValue(fieldB, "其他"), fieldC, textValue(fieldC, "匹配C"));
        // 卡片3: A=1, B=0, C=0 -> (1 AND 0) OR 0 = 0 不匹配
        createTestCardWithThreeFields(testCaseId, orgId, cardTypeId, "卡片3",
                fieldA, textValue(fieldA, "匹配A"), fieldB, textValue(fieldB, "其他"), fieldC, textValue(fieldC, "其他"));

        // When - 构建 (A AND B) OR C
        ConditionGroup andGroup = ConditionGroup.and(
                new TextConditionItem(
                        new TextConditionItem.TextSubject(null, fieldA),
                        new TextConditionItem.TextOperator.Contains("匹配A")
                ),
                new TextConditionItem(
                        new TextConditionItem.TextSubject(null, fieldB),
                        new TextConditionItem.TextOperator.Contains("匹配B")
                )
        );
        ConditionGroup orGroup = ConditionGroup.or(
                andGroup,
                new TextConditionItem(
                        new TextConditionItem.TextSubject(null, fieldC),
                        new TextConditionItem.TextOperator.Contains("匹配C")
                )
        );

        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, orGroup);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId1, cardId2);
    }

    // ==================== 内置日期属性测试 ====================

    @Test
    @Order(70)
    @DisplayName("测试按创建时间(CREATED_AT)查询 - After")
    void testQueryByCreatedAtAfter() {
        String testCaseId = generateTestCaseId("created-at");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;

        // 时间比较的精度默认是天
        long yesterdayMillis = LocalDateTime.now().minusDays(1)
                .toInstant(java.time.ZoneOffset.ofHours(8)).toEpochMilli();

        CardId cardId2 = createTestCard(testCaseId, orgId, cardTypeId, "卡片B", CardCycle.ACTIVE);

        // When - 查询创建时间晚于middleTime的卡片
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.SystemDateSubject(null, DateConditionItem.SystemDateField.CREATED_AT),
                new DateConditionItem.DateOperator.After(
                        new DateConditionItem.DateValue.Specific(String.valueOf(yesterdayMillis))
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回卡片B
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId2);
    }

    @Test
    @Order(71)
    @DisplayName("测试按创建时间(CREATED_AT)查询 - Before")
    void testQueryByCreatedAtBefore() {
        String testCaseId = generateTestCaseId("created-at-before");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;

        // 时间比较的精度默认是天
        long tomorrowMillis = LocalDateTime.now().plusDays(1)
                .toInstant(java.time.ZoneOffset.ofHours(8)).toEpochMilli();

        // Given
        CardId cardId1 = createTestCard(testCaseId, orgId, cardTypeId, "卡片A", CardCycle.ACTIVE);

        // When
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.SystemDateSubject(null, DateConditionItem.SystemDateField.CREATED_AT),
                new DateConditionItem.DateOperator.Before(
                        new DateConditionItem.DateValue.Specific(String.valueOf(tomorrowMillis))
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该只返回卡片A
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(72)
    @DisplayName("测试按更新时间(UPDATED_AT)查询")
    void testQueryByUpdatedAt() {
        String testCaseId = generateTestCaseId("updated-at");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "text-field-001";

        // Given - 创建卡片并更新其中一张
        Map<String, FieldValue<?>> fieldValues1 = new HashMap<>();
        fieldValues1.put(fieldId, textValue(fieldId, "初始值"));
        CardId cardId1 = createTestCardWithFields(testCaseId, orgId, cardTypeId, "卡片A", fieldValues1);

        // 创建第二张卡片但不更新
        Map<String, FieldValue<?>> fieldValues2 = new HashMap<>();
        fieldValues2.put(fieldId, textValue(fieldId, "不变值"));
        createTestCardWithFields(testCaseId, orgId, cardTypeId, "卡片B", fieldValues2);

        long afterOneHour = LocalDateTime.now().plusHours(1)
                .toInstant(ZoneOffset.ofHours(8)).toEpochMilli();

        //第二天更新
        long tomorrowMillis = LocalDateTime.now().plusDays(1)
                .toInstant(java.time.ZoneOffset.ofHours(8)).toEpochMilli();

        // 更新卡片A的字段值，触发更新时间变化
        Map<String, FieldValue<?>> updatedFields = new HashMap<>();
        updatedFields.put(fieldId, textValue(fieldId, "更新后的值"));
        CardEntity updatedCard = new CardEntity(
                cardId1, 0L, null, OrgId.of(orgId), CardTypeId.of(cardTypeId),
                CardTitle.pure("卡片A"), CardCycle.ACTIVE, null, null, LocalDateTime.now()
        );
        //第而天更新
        updatedCard.setUpdatedAt(tomorrowMillis);
        updatedCard.setFieldValues(updatedFields);
        repository.update(updatedCard);

        // When - 查询更新时间晚于updateTime的卡片
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.SystemDateSubject(null, DateConditionItem.SystemDateField.UPDATED_AT),
                new DateConditionItem.DateOperator.After(
                        new DateConditionItem.DateValue.Specific(String.valueOf(afterOneHour))
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该返回被更新的卡片A
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(73)
    @DisplayName("测试按丢弃时间(DISCARDED_AT)查询")
    void testQueryByDiscardedAt() {
        String testCaseId = generateTestCaseId("discarded-at");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;

        // Given - 创建卡片并丢弃其中一张
        CardId cardId1 = createTestCard(testCaseId, orgId, cardTypeId, "卡片A-将被丢弃", CardCycle.ACTIVE);
        createTestCard(testCaseId, orgId, cardTypeId, "卡片B-保留", CardCycle.ACTIVE);

        // 丢弃卡片A
        repository.discard(cardId1, "测试丢弃", TEST_OPERATOR_ID);

        // When - 查询有丢弃时间的卡片（IS_NOT_EMPTY）
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.SystemDateSubject(null, DateConditionItem.SystemDateField.DISCARDED_AT),
                new DateConditionItem.DateOperator.IsNotEmpty()
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该返回被丢弃的卡片A
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(74)
    @DisplayName("测试按归档时间(ARCHIVED_AT)查询")
    void testQueryByArchivedAt() {
        String testCaseId = generateTestCaseId("archived-at");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;

        // Given - 创建卡片并归档其中一张
        CardId cardId1 = createTestCard(testCaseId, orgId, cardTypeId, "卡片A-将被归档", CardCycle.ACTIVE);
        createTestCard(testCaseId, orgId, cardTypeId, "卡片B-保留", CardCycle.ACTIVE);

        // 归档卡片A
        repository.archive(cardId1, TEST_OPERATOR_ID);

        // When - 查询有归档时间的卡片（IS_NOT_EMPTY）
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.SystemDateSubject(null, DateConditionItem.SystemDateField.ARCHIVED_AT),
                new DateConditionItem.DateOperator.IsNotEmpty()
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then - 应该返回被归档的卡片A
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(75)
    @DisplayName("测试按归档时间(ARCHIVED_AT)范围查询")
    void testQueryByArchivedAtBetween() {
        String testCaseId = generateTestCaseId("archived-at-between");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;

        // Given
        CardId cardId1 = createTestCard(testCaseId, orgId, cardTypeId, "卡片A", CardCycle.ACTIVE);

        long beforeArchiveMillis = System.currentTimeMillis();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 归档卡片
        repository.archive(cardId1, TEST_OPERATOR_ID);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long afterArchiveMillis = System.currentTimeMillis();

        // When - 查询归档时间在范围内的卡片
        DateConditionItem condition = new DateConditionItem(
                new DateConditionItem.DateSubject.SystemDateSubject(null, DateConditionItem.SystemDateField.ARCHIVED_AT),
                new DateConditionItem.DateOperator.Between(
                        new DateConditionItem.DateValue.Specific(String.valueOf(beforeArchiveMillis)),
                        new DateConditionItem.DateValue.Specific(String.valueOf(afterArchiveMillis))
                )
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    // ==================== 计数查询测试 ====================

    @Test
    @Order(80)
    @DisplayName("测试计数查询")
    void testCountWithCondition() {
        String testCaseId = generateTestCaseId("count");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "text-field-001";

        // Given - 创建3张卡片，其中2张符合条件
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", textValue(fieldId, "计数测试"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", textValue(fieldId, "计数测试"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片C", textValue(fieldId, "其他内容"));

        // When
        TextConditionItem condition = new TextConditionItem(
                new TextConditionItem.TextSubject(null, fieldId),
                new TextConditionItem.TextOperator.Contains("计数测试")
        );

        CardCountRequest countRequest = new CardCountRequest();
        QueryContext queryContext = new QueryContext();
        queryContext.setOperatorId(TEST_OPERATOR_ID);
        queryContext.setOrgId(orgId);
        countRequest.setQueryContext(queryContext);

        QueryScope queryScope = new QueryScope();
        queryScope.setCardTypeIds(List.of(cardTypeId));
        countRequest.setQueryScope(queryScope);

        Condition cond = new Condition(condition);
        countRequest.setCondition(cond);

        Integer count = repository.count(countRequest);

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ==================== 枚举条件测试 ====================

    @Test
    @Order(90)
    @DisplayName("测试枚举等于查询")
    void testQueryByEnumEqual() {
        String testCaseId = generateTestCaseId("enum-eq");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "enum-field-001";
        String optionId = "option-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", enumValue(fieldId, optionId));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", enumValue(fieldId, "option-002"));

        // When
        EnumConditionItem condition = new EnumConditionItem(
                new EnumConditionItem.EnumSubject(null, fieldId),
                new EnumConditionItem.EnumOperator.Equal(optionId)
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(91)
    @DisplayName("测试枚举不等于查询")
    void testQueryByEnumNotEqual() {
        String testCaseId = generateTestCaseId("enum-ne");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "enum-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", enumValue(fieldId, "option-001"));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", enumValue(fieldId, "option-002"));

        // When
        EnumConditionItem condition = new EnumConditionItem(
                new EnumConditionItem.EnumSubject(null, fieldId),
                new EnumConditionItem.EnumOperator.NotEqual("option-001")
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId2);
    }

    @Test
    @Order(92)
    @DisplayName("测试枚举包含任一查询")
    void testQueryByEnumIn() {
        String testCaseId = generateTestCaseId("enum-in");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "enum-field-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", enumValue(fieldId, "option-001"));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", enumValue(fieldId, "option-002"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片C", enumValue(fieldId, "option-003"));

        // When
        EnumConditionItem condition = new EnumConditionItem(
                new EnumConditionItem.EnumSubject(null, fieldId),
                new EnumConditionItem.EnumOperator.In(List.of("option-001", "option-002"))
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId1, cardId2);
    }

    @Test
    @Order(93)
    @DisplayName("测试枚举不包含任一查询")
    void testQueryByEnumNotIn() {
        String testCaseId = generateTestCaseId("enum-not-in");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "enum-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", enumValue(fieldId, "option-001"));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B", enumValue(fieldId, "option-002"));

        // When
        EnumConditionItem condition = new EnumConditionItem(
                new EnumConditionItem.EnumSubject(null, fieldId),
                new EnumConditionItem.EnumOperator.NotIn(List.of("option-001"))
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId2);
    }

    @Test
    @Order(94)
    @DisplayName("测试枚举为空查询")
    void testQueryByEnumIsEmpty() {
        String testCaseId = generateTestCaseId("enum-is-empty");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "enum-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", enumValue(fieldId, "option-001"));
        CardId cardId2 = createTestCard(testCaseId, orgId, cardTypeId, "卡片B", CardCycle.ACTIVE);

        // When
        EnumConditionItem condition = new EnumConditionItem(
                new EnumConditionItem.EnumSubject(null, fieldId),
                new EnumConditionItem.EnumOperator.IsEmpty()
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId2);
    }

    @Test
    @Order(95)
    @DisplayName("测试枚举不为空查询")
    void testQueryByEnumIsNotEmpty() {
        String testCaseId = generateTestCaseId("enum-is-not-empty");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "enum-field-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A", enumValue(fieldId, "option-001"));
        createTestCard(testCaseId, orgId, cardTypeId, "卡片B", CardCycle.ACTIVE);

        // When
        EnumConditionItem condition = new EnumConditionItem(
                new EnumConditionItem.EnumSubject(null, fieldId),
                new EnumConditionItem.EnumOperator.IsNotEmpty()
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    // ==================== WebURL条件测试 ====================

    @Test
    @Order(110)
    @DisplayName("测试链接等于查询")
    void testQueryByUrlEqual() {
        String testCaseId = generateTestCaseId("url-eq");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "url-field-001";
        String targetUrl = "https://example.com/page1";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A",
                urlValue(fieldId, targetUrl, "页面1"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B",
                urlValue(fieldId, "https://example.com/page2", "页面2"));

        // When
        WebUrlConditionItem condition = new WebUrlConditionItem(
                new WebUrlConditionItem.WebUrlSubject(null, fieldId),
                new WebUrlConditionItem.WebUrlOperator.Equal(targetUrl)
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId1);
    }

    @Test
    @Order(111)
    @DisplayName("测试链接包含查询")
    void testQueryByUrlContains() {
        String testCaseId = generateTestCaseId("url-contains");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "url-field-001";

        // Given
        CardId cardId1 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A",
                urlValue(fieldId, "https://planka.com/page1", "页面1"));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B",
                urlValue(fieldId, "https://planka.com/page2", "页面2"));
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片C",
                urlValue(fieldId, "https://other.com/page", "其他页面"));

        // When
        WebUrlConditionItem condition = new WebUrlConditionItem(
                new WebUrlConditionItem.WebUrlSubject(null, fieldId),
                new WebUrlConditionItem.WebUrlOperator.Contains("planka.com")
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId1, cardId2);
    }

    @Test
    @Order(112)
    @DisplayName("测试链接不包含查询")
    void testQueryByUrlNotContains() {
        String testCaseId = generateTestCaseId("url-not-contains");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "url-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A",
                urlValue(fieldId, "https://planka.com/page1", "页面1"));
        CardId cardId2 = createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片B",
                urlValue(fieldId, "https://other.com/page", "其他页面"));

        // When
        WebUrlConditionItem condition = new WebUrlConditionItem(
                new WebUrlConditionItem.WebUrlSubject(null, fieldId),
                new WebUrlConditionItem.WebUrlOperator.NotContains("planka.com")
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId2);
    }

    @Test
    @Order(113)
    @DisplayName("测试链接为空查询")
    void testQueryByUrlIsEmpty() {
        String testCaseId = generateTestCaseId("url-is-empty");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String fieldId = "url-field-001";

        // Given
        createTestCardWithField(testCaseId, orgId, cardTypeId, "卡片A",
                urlValue(fieldId, "https://example.com", "示例"));
        CardId cardId2 = createTestCard(testCaseId, orgId, cardTypeId, "卡片B", CardCycle.ACTIVE);

        // When
        WebUrlConditionItem condition = new WebUrlConditionItem(
                new WebUrlConditionItem.WebUrlSubject(null, fieldId),
                new WebUrlConditionItem.WebUrlOperator.IsEmpty()
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(1)
                .extracting(CardDTO::getId)
                .containsExactly(cardId2);
    }

    // ==================== 关键字条件测试 ====================

    @Test
    @Order(120)
    @DisplayName("测试关键字包含查询")
    void testQueryByKeywordContains() {
        String testCaseId = generateTestCaseId("keyword-contains");
        String orgId = "org-" + testCaseId;
        String cardTypeId = "type-" + testCaseId;
        String keyword = "关键搜索词" + System.currentTimeMillis();

        // Given - 创建标题包含关键字的卡片
        CardId cardId1 = createTestCard(testCaseId, orgId, cardTypeId, "任务A-" + keyword, CardCycle.ACTIVE);
        CardId cardId2 = createTestCard(testCaseId, orgId, cardTypeId, "任务B-" + keyword, CardCycle.ACTIVE);
        createTestCard(testCaseId, orgId, cardTypeId, "无关任务C", CardCycle.ACTIVE);

        // When
        KeywordConditionItem condition = new KeywordConditionItem(
                new KeywordConditionItem.KeywordSubject(),
                new KeywordConditionItem.KeywordOperator.Contains(keyword)
        );
        CardQueryRequest request = createQueryRequest(orgId, cardTypeId, condition);
        List<CardDTO> result = repository.query(request);

        // Then
        assertThat(result)
                .hasSize(2)
                .extracting(CardDTO::getId)
                .containsExactlyInAnyOrder(cardId1, cardId2);
    }

}
