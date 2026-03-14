package cn.agilean.kanban.test.support;

import cn.agilean.kanban.api.card.CardServiceClient;
import cn.agilean.kanban.test.IntegrationTestApplication;
import cn.agilean.kanban.test.client.SchemaApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 集成测试基类
 * <p>
 * 使用 OpenFeign + Nacos 服务发现调用后端服务进行集成测试。
 */
@SpringBootTest(classes = IntegrationTestApplication.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected CardServiceClient cardServiceClient;

    @Autowired
    protected SchemaApiClient schemaClient;

    /** 测试组织ID */
    protected static final String TEST_ORG_ID = TestDataBuilder.TEST_ORG_ID;

    /** 测试操作人ID */
    protected static final String TEST_OPERATOR_ID = "test-operator-001";

    /** 测试卡片类型ID */
    protected static final String TEST_CARD_TYPE_ID = TestDataBuilder.TEST_CARD_TYPE_ID;
}
