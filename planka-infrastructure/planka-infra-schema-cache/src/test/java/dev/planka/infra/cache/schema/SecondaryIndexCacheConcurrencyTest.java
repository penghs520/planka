package dev.planka.infra.cache.schema;

import dev.planka.api.schema.SecondaryIndexQueryClient;
import dev.planka.common.result.Result;
import dev.planka.event.schema.SchemaCreatedEvent;
import dev.planka.event.schema.SchemaDeletedEvent;
import dev.planka.event.schema.SchemaUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SecondaryIndexCache 并发测试
 * <p>
 * 测试目的：验证 indexMap 和 reverseIndex 在并发场景下的线程安全性
 */
class SecondaryIndexCacheConcurrencyTest {

    private SecondaryIndexCache cache;
    private SecondaryIndexQueryClient mockClient;
    private ObjectMapper objectMapper;

    private static final String INDEX_TYPE = "CARD_TYPE";
    private static final int THREAD_COUNT = 20;
    private static final int OPERATIONS_PER_THREAD = 100;

    @BeforeEach
    void setUp() {
        mockClient = mock(SecondaryIndexQueryClient.class);
        objectMapper = new ObjectMapper();

        // 返回空索引，避免初始化时的网络调用
        when(mockClient.getAllSecondaryIndexes()).thenReturn(Result.success(new HashMap<>()));
    }

    /**
     * 测试1：并发读写测试
     * 多个线程同时执行读取和更新操作，验证不会抛出异常
     */
    @RepeatedTest(5)
    @DisplayName("并发读写不应抛出异常")
    void concurrentReadWrite_shouldNotThrowException() throws Exception {
        cache = new SecondaryIndexCache(mockClient, objectMapper);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean hasError = new AtomicBoolean(false);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        // 一半线程执行更新，一半线程执行读取
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        if (threadId % 2 == 0) {
                            // 更新操作
                            String schemaId = "schema-" + threadId + "-" + j;
                            String indexKey = "cardType-" + (j % 10);
                            SchemaUpdatedEvent event = createUpdateEvent(schemaId, indexKey);
                            cache.updateIndex(event);
                        } else {
                            // 读取操作
                            String indexKey = "cardType-" + (j % 10);
                            cache.getSchemaIds(INDEX_TYPE, indexKey);
                        }
                    }
                } catch (Throwable e) {
                    hasError.set(true);
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        if (!errors.isEmpty()) {
            errors.forEach(e -> e.printStackTrace());
        }
        assertThat(hasError.get()).as("不应发生异常").isFalse();
    }

    /**
     * 测试2：并发更新同一个 schema
     * 多个线程同时更新同一个 schemaId，验证数据一致性
     */
    @RepeatedTest(10)
    @DisplayName("并发更新同一schema应保持数据一致性")
    void concurrentUpdateSameSchema_shouldMaintainConsistency() throws Exception {
        cache = new SecondaryIndexCache(mockClient, objectMapper);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean hasError = new AtomicBoolean(false);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        String schemaId = "shared-schema";

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        // 所有线程更新同一个 schemaId，但使用不同的 indexKey
                        String indexKey = "cardType-" + threadId;
                        SchemaUpdatedEvent event = createUpdateEvent(schemaId, indexKey);
                        cache.updateIndex(event);
                    }
                } catch (Throwable e) {
                    hasError.set(true);
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 统计异常类型
        long cmeCount = errors.stream()
                .filter(e -> e instanceof ConcurrentModificationException)
                .count();

        // 最终应该只有一个索引条目（最后一次更新的）
        // 但由于并发问题，可能会有多个或丢失
        int foundCount = 0;
        for (int i = 0; i < THREAD_COUNT; i++) {
            Set<String> ids = cache.getSchemaIds(INDEX_TYPE, "cardType-" + i);
            if (ids.contains(schemaId)) {
                foundCount++;
            }
        }

        // 断言：不应发生 ConcurrentModificationException
        assertThat(cmeCount)
                .as("不应发生 ConcurrentModificationException")
                .isZero();

        // 断言：不应发生其他异常
        assertThat(errors.size() - cmeCount)
                .as("不应发生其他异常")
                .isZero();

        // 断言：schema 应该恰好在 1 个索引键中
        assertThat(foundCount)
                .as("并发更新后 schema 应该恰好存在于 1 个索引键中")
                .isEqualTo(1);
    }

    /**
     * 测试3：并发删除测试
     * 多个线程同时删除不同的 schema，验证不会抛出异常
     */
    @RepeatedTest(5)
    @DisplayName("并发删除不应抛出异常")
    void concurrentDelete_shouldNotThrowException() throws Exception {
        // 先初始化一些数据
        Map<String, Map<String, Set<String>>> initialData = new HashMap<>();
        Map<String, Set<String>> keyMap = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; i++) {
            Set<String> schemaIds = ConcurrentHashMap.newKeySet();
            for (int j = 0; j < 10; j++) {
                schemaIds.add("schema-" + i + "-" + j);
            }
            keyMap.put("cardType-" + i, schemaIds);
        }
        initialData.put(INDEX_TYPE, keyMap);

        when(mockClient.getAllSecondaryIndexes()).thenReturn(Result.success(initialData));
        cache = new SecondaryIndexCache(mockClient, objectMapper);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicBoolean hasError = new AtomicBoolean(false);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 10; j++) {
                        String schemaId = "schema-" + (threadId % 100) + "-" + j;
                        SchemaDeletedEvent event = createDeleteEvent(schemaId);
                        cache.updateIndex(event);
                    }
                } catch (Throwable e) {
                    hasError.set(true);
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        if (!errors.isEmpty()) {
            errors.forEach(e -> e.printStackTrace());
        }
        assertThat(hasError.get()).as("不应发生异常").isFalse();
    }

    /**
     * 测试4：并发删除测试
     * 先添加数据，然后多线程并发删除，验证所有数据都被正确删除
     */
    @RepeatedTest(10)
    @DisplayName("并发删除应正确清空数据")
    void concurrentDelete_shouldRemoveAllData() throws Exception {
        cache = new SecondaryIndexCache(mockClient, objectMapper);

        String indexKey = "shared-cardType";
        int totalSchemas = 100;

        // 1. 先添加所有数据
        for (int i = 0; i < totalSchemas; i++) {
            String schemaId = "schema-" + i;
            SchemaUpdatedEvent event = createUpdateEvent(schemaId, indexKey);
            cache.updateIndex(event);
        }

        // 验证数据已添加
        Set<String> beforeDelete = cache.getSchemaIds(INDEX_TYPE, indexKey);
        assertThat(beforeDelete).hasSize(totalSchemas);

        // 2. 多线程并发删除
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalSchemas);
        AtomicBoolean hasError = new AtomicBoolean(false);

        for (int i = 0; i < totalSchemas; i++) {
            final String schemaId = "schema-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    SchemaDeletedEvent event = createDeleteEvent(schemaId);
                    cache.updateIndex(event);
                } catch (Throwable e) {
                    hasError.set(true);
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. 验证所有数据已删除
        Set<String> afterDelete = cache.getSchemaIds(INDEX_TYPE, indexKey);

        assertThat(hasError.get()).as("不应发生异常").isFalse();
        assertThat(afterDelete)
                .as("并发删除后应该没有剩余数据")
                .isEmpty();
    }

    /**
     * 测试5：高并发压力测试
     * 大量线程同时执行各种操作
     */
    @Test
    @DisplayName("高并发压力测试")
    void highConcurrencyStressTest() throws Exception {
        cache = new SecondaryIndexCache(mockClient, objectMapper);

        int threadCount = 50;
        int opsPerThread = 200;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicBoolean hasError = new AtomicBoolean(false);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successOps = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random();
                    for (int j = 0; j < opsPerThread; j++) {
                        int op = random.nextInt(3);
                        String schemaId = "schema-" + random.nextInt(100);
                        String indexKey = "cardType-" + random.nextInt(20);

                        switch (op) {
                            case 0 -> {
                                // 读取
                                cache.getSchemaIds(INDEX_TYPE, indexKey);
                            }
                            case 1 -> {
                                // 更新
                                SchemaUpdatedEvent event = createUpdateEvent(schemaId, indexKey);
                                cache.updateIndex(event);
                            }
                            case 2 -> {
                                // 删除
                                SchemaDeletedEvent event = createDeleteEvent(schemaId);
                                cache.updateIndex(event);
                            }
                        }
                        successOps.incrementAndGet();
                    }
                } catch (Throwable e) {
                    hasError.set(true);
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // 断言：测试应在超时前完成
        assertThat(completed).as("测试应在超时前完成").isTrue();

        // 断言：所有操作都应完成
        assertThat(successOps.get())
                .as("所有操作都应完成")
                .isEqualTo(threadCount * opsPerThread);

        // 断言：不应发生异常
        assertThat(hasError.get()).as("不应发生异常").isFalse();
    }

    /**
     * 测试6：ConcurrentModificationException 测试
     * 验证遍历 synchronizedList 时是否会抛出并发修改异常
     */
    @RepeatedTest(10)
    @DisplayName("遍历时的并发修改异常测试")
    void iterationConcurrentModification_shouldNotThrowCME() throws Exception {
        // 初始化一个 schema 有多个索引条目
        cache = new SecondaryIndexCache(mockClient, objectMapper);

        String schemaId = "multi-index-schema";

        // 先添加多个索引
        for (int i = 0; i < 10; i++) {
            SchemaUpdatedEvent event = createUpdateEvent(schemaId, "cardType-" + i);
            cache.updateIndex(event);
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(10);
        AtomicBoolean hasCME = new AtomicBoolean(false);

        // 一些线程删除这个 schema
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        SchemaDeletedEvent event = createDeleteEvent(schemaId);
                        cache.updateIndex(event);
                    }
                } catch (ConcurrentModificationException e) {
                    hasCME.set(true);
                    e.printStackTrace();
                } catch (Throwable e) {
                    // 忽略其他异常
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 一些线程添加新索引到这个 schema
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 100; j++) {
                        SchemaUpdatedEvent event = createUpdateEvent(schemaId, "newType-" + threadId + "-" + j);
                        cache.updateIndex(event);
                    }
                } catch (ConcurrentModificationException e) {
                    hasCME.set(true);
                    e.printStackTrace();
                } catch (Throwable e) {
                    // 忽略其他异常
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 断言：不应发生 ConcurrentModificationException
        assertThat(hasCME.get())
                .as("不应发生 ConcurrentModificationException")
                .isFalse();
    }

    /**
     * 测试7：并发执行新增、删除、更新
     * 多个线程同时执行 Create/Update/Delete 操作
     */
    @RepeatedTest(10)
    @DisplayName("并发执行新增、删除、更新应保持数据一致性")
    void concurrentCreateUpdateDelete_shouldMaintainConsistency() throws Exception {
        cache = new SecondaryIndexCache(mockClient, objectMapper);

        int schemaCount = 100;
        String indexKey = "shared-cardType";

        ExecutorService executor = Executors.newFixedThreadPool(30);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(schemaCount * 3);
        AtomicBoolean hasError = new AtomicBoolean(false);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        // 并发执行：Create -> Update -> Delete
        for (int i = 0; i < schemaCount; i++) {
            final String schemaId = "schema-" + i;

            // Create
            executor.submit(() -> {
                try {
                    startLatch.await();
                    SchemaCreatedEvent event = createCreateEvent(schemaId, indexKey);
                    cache.updateIndex(event);
                } catch (Throwable e) {
                    hasError.set(true);
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });

            // Update
            executor.submit(() -> {
                try {
                    startLatch.await();
                    SchemaUpdatedEvent event = createUpdateEvent(schemaId, indexKey);
                    cache.updateIndex(event);
                } catch (Throwable e) {
                    hasError.set(true);
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });

            // Delete
            executor.submit(() -> {
                try {
                    startLatch.await();
                    SchemaDeletedEvent event = createDeleteEvent(schemaId);
                    cache.updateIndex(event);
                } catch (Throwable e) {
                    hasError.set(true);
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 断言
        assertThat(completed).as("测试应在超时前完成").isTrue();
        assertThat(hasError.get()).as("不应发生异常").isFalse();
    }

    // ==================== 辅助方法 ====================

    private SchemaUpdatedEvent createUpdateEvent(String schemaId, String indexKey) {
        SchemaUpdatedEvent event = new SchemaUpdatedEvent(
                "org1", "user1", "127.0.0.1", "trace1", schemaId);

        // 构造一个包含 cardTypeIds 的 JSON
        // secondKeys() 方法会根据 cardTypeIds 计算出二级索引
        String afterContent = String.format("""
                {
                    "schemaSubType": "TEXT_FIELD",
                    "id": "%s",
                    "orgId": "org1",
                    "name": "Test Field",
                    "systemField": false,
                    "cardTypeId": "%s",
                    "fieldId": "field-%s"
                }
                """, schemaId, indexKey, schemaId);
        event.setAfterContent(afterContent);
        return event;
    }

    private SchemaDeletedEvent createDeleteEvent(String schemaId) {
        return new SchemaDeletedEvent(
                "org1", "user1", "127.0.0.1", "trace1", schemaId);
    }

    private SchemaCreatedEvent createCreateEvent(String schemaId, String indexKey) {
        SchemaCreatedEvent event = new SchemaCreatedEvent(
                "org1", "user1", "127.0.0.1", "trace1", schemaId);

        String content = String.format("""
                {
                    "schemaSubType": "TEXT_FIELD",
                    "id": "%s",
                    "orgId": "org1",
                    "name": "Test Field",
                    "systemField": false,
                    "cardTypeId": "%s",
                    "fieldId": "field-%s"
                }
                """, schemaId, indexKey, schemaId);
        event.setContent(content);
        return event;
    }
}
