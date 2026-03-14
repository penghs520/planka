package cn.agilean.kanban.test.support;

import cn.agilean.kanban.api.card.request.*;
import cn.agilean.kanban.api.schema.request.CreateSchemaRequest;
import cn.agilean.kanban.api.schema.request.UpdateSchemaRequest;
import cn.agilean.kanban.domain.card.CardCycle;
import cn.agilean.kanban.domain.card.CardTitle;
import cn.agilean.kanban.domain.card.CardTypeId;
import cn.agilean.kanban.domain.card.OrgId;
import cn.agilean.kanban.domain.field.FieldConfigId;
import cn.agilean.kanban.domain.field.FieldId;
import cn.agilean.kanban.domain.schema.definition.AbstractSchemaDefinition;
import cn.agilean.kanban.domain.schema.definition.fieldconfig.MultiLineTextFieldConfig;
import cn.agilean.kanban.domain.schema.definition.fieldconfig.SingleLineTextFieldConfig;

import java.util.HashMap;
import java.util.List;

/**
 * 测试数据构建器
 * <p>
 * 提供测试所需的各种数据构建方法
 */
public final class TestDataBuilder {

    /** 测试组织ID */
    public static final String TEST_ORG_ID = "test-org-001";

    /** 测试卡片类型ID */
    public static final String TEST_CARD_TYPE_ID = "test-card-type-001";

    private TestDataBuilder() {
    }

    // ==================== Card 相关 ====================

    /**
     * 创建卡片请求
     */
    public static CreateCardRequest createCardRequest(String titleValue) {
        return new CreateCardRequest(
                OrgId.of(TEST_ORG_ID),
                CardTypeId.of(TEST_CARD_TYPE_ID),
                CardTitle.pure(titleValue),
                "测试描述: " + titleValue,
                new HashMap<>()); // fieldValues
    }

    /**
     * 创建查询范围
     */
    public static QueryScope createQueryScope() {
        QueryScope scope = new QueryScope();
        scope.setCardTypeIds(List.of(TEST_CARD_TYPE_ID));
        scope.setCardCycles(List.of(CardCycle.ACTIVE));
        return scope;
    }

    /**
     * 创建分页信息
     */
    public static Page createPage(int pageNum, int pageSize) {
        Page page = new Page();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    /**
     * 创建排序和分页
     */
    public static SortAndPage createSortAndPage(int pageNum, int pageSize) {
        SortAndPage sortAndPage = new SortAndPage();
        sortAndPage.setPage(createPage(pageNum, pageSize));
        return sortAndPage;
    }

    /**
     * 创建分页查询请求
     */
    public static CardPageQueryRequest createPageQueryRequest(int pageNum, int pageSize) {
        CardPageQueryRequest request = new CardPageQueryRequest();
        request.setQueryContext(new QueryContext());
        request.setQueryScope(createQueryScope());
        request.setSortAndPage(createSortAndPage(pageNum, pageSize));
        request.setYield(new Yield());
        return request;
    }

    /**
     * 创建列表查询请求
     */
    public static CardQueryRequest createQueryRequest() {
        CardQueryRequest request = new CardQueryRequest();
        request.setQueryContext(new QueryContext());
        request.setQueryScope(createQueryScope());
        request.setYield(new Yield());
        return request;
    }

    /**
     * 创建计数请求
     */
    public static CardCountRequest createCountRequest() {
        CardCountRequest request = new CardCountRequest();
        request.setQueryContext(new QueryContext());
        request.setQueryScope(createQueryScope());
        return request;
    }

    // ==================== Schema 相关 ====================

    /**
     * 创建单行文本字段配置
     */
    public static SingleLineTextFieldConfig createSingleLineTextFieldConfig(String name) {
        SingleLineTextFieldConfig config = new SingleLineTextFieldConfig(
                FieldConfigId.generate(), null, name, null, FieldId.generate(), false);
        config.setCode("field_" + System.currentTimeMillis());
        config.setMaxLength(500);
        config.setPlaceholder("请输入" + name);
        return config;
    }

    /**
     * 创建多行文本字段配置
     */
    public static MultiLineTextFieldConfig createMultiLineTextFieldConfig(String name) {
        MultiLineTextFieldConfig config = new MultiLineTextFieldConfig(
                FieldConfigId.generate(), null, name, null, FieldId.generate(), false);
        config.setCode("textarea_" + System.currentTimeMillis());
        config.setMaxLength(2000);
        return config;
    }

    /**
     * 创建Schema请求（使用单行文本字段配置）
     */
    public static CreateSchemaRequest createTextFieldRequest(String name) {
        CreateSchemaRequest request = new CreateSchemaRequest();
        request.setDefinition(createSingleLineTextFieldConfig(name));
        return request;
    }

    /**
     * 创建通用Schema请求
     */
    public static CreateSchemaRequest createSchemaRequest(AbstractSchemaDefinition<?> definition) {
        CreateSchemaRequest request = new CreateSchemaRequest();
        request.setDefinition(definition);
        return request;
    }

    /**
     * 创建更新请求
     */
    public static UpdateSchemaRequest createUpdateRequest(AbstractSchemaDefinition<?> definition,
            Integer expectedVersion) {
        UpdateSchemaRequest request = new UpdateSchemaRequest();
        request.setDefinition(definition);
        request.setExpectedVersion(expectedVersion);
        return request;
    }

    /**
     * 生成唯一的测试名称
     */
    public static String uniqueName(String prefix) {
        return prefix + "_" + System.currentTimeMillis();
    }
}
