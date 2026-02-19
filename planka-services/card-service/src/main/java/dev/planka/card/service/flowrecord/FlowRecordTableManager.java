package dev.planka.card.service.flowrecord;

import dev.planka.card.mapper.FlowRecordMapper;
import dev.planka.card.mapper.FlowRecordMetaMapper;
import dev.planka.domain.stream.StreamId;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * 流动记录表管理器
 * <p>
 * 负责按价值流动态创建和管理流动记录表
 */
@Component
public class FlowRecordTableManager {

    private static final Logger logger = LoggerFactory.getLogger(FlowRecordTableManager.class);

    /**
     * 表名前缀
     */
    private static final String TABLE_PREFIX = "flow_record_";

    /**
     * StreamId 合法字符模式（只允许数字，因为是雪花算法生成）
     */
    private static final Pattern STREAM_ID_PATTERN = Pattern.compile("^\\d+$");

    /**
     * 表名缓存：streamId -> tableName
     */
    private final ConcurrentMap<String, String> tableCache = new ConcurrentHashMap<>();

    private final FlowRecordMapper flowRecordMapper;
    private final FlowRecordMetaMapper flowRecordMetaMapper;

    public FlowRecordTableManager(FlowRecordMapper flowRecordMapper, FlowRecordMetaMapper flowRecordMetaMapper) {
        this.flowRecordMapper = flowRecordMapper;
        this.flowRecordMetaMapper = flowRecordMetaMapper;
    }

    /**
     * 启动时加载已有的表元数据到缓存
     */
    @PostConstruct
    public void init() {
        flowRecordMetaMapper.selectList(null).forEach(meta -> {
            tableCache.put(meta.getStreamId(), meta.getTableName());
            logger.debug("加载流动记录表缓存: {} -> {}", meta.getStreamId(), meta.getTableName());
        });
        logger.info("流动记录表管理器初始化完成，加载了 {} 个表", tableCache.size());
    }

    /**
     * 获取或创建流动记录表
     *
     * @param streamId 价值流ID
     * @return 表名
     */
    public String getOrCreateTable(StreamId streamId) {
        String streamIdValue = streamId.value();

        // 从缓存中获取
        String tableName = tableCache.get(streamIdValue);
        if (tableName != null) {
            return tableName;
        }

        // 缓存未命中，尝试创建表
        return createTableIfNotExists(streamIdValue);
    }

    /**
     * 创建表（如果不存在）
     */
    @Transactional
    protected String createTableIfNotExists(String streamId) {
        // 双重检查
        String tableName = tableCache.get(streamId);
        if (tableName != null) {
            return tableName;
        }

        // 从数据库查询元数据
        FlowRecordMetaEntity meta = flowRecordMetaMapper.findByStreamId(streamId);
        if (meta != null) {
            tableCache.put(streamId, meta.getTableName());
            return meta.getTableName();
        }

        // 生成表名并创建表
        tableName = generateTableName(streamId);

        // 创建物理表
        flowRecordMapper.createTable(tableName);
        logger.info("创建流动记录表: {}", tableName);

        // 保存元数据
        meta = new FlowRecordMetaEntity();
        meta.setStreamId(streamId);
        meta.setTableName(tableName);
        meta.setCreatedAt(LocalDateTime.now());
        flowRecordMetaMapper.insert(meta);

        // 更新缓存
        tableCache.put(streamId, tableName);

        return tableName;
    }

    /**
     * 生成表名
     * <p>
     * 表名格式: flow_record_{streamId}
     * 需要验证 streamId 格式以防止 SQL 注入
     */
    private String generateTableName(String streamId) {
        // 验证 streamId 格式（只允许数字）
        if (!STREAM_ID_PATTERN.matcher(streamId).matches()) {
            throw new IllegalArgumentException("非法的 StreamId 格式: " + streamId);
        }
        return TABLE_PREFIX + streamId;
    }

    /**
     * 检查表是否存在
     */
    public boolean tableExists(StreamId streamId) {
        String streamIdValue = streamId.value();

        // 先检查缓存
        if (tableCache.containsKey(streamIdValue)) {
            return true;
        }

        // 检查元数据表
        FlowRecordMetaEntity meta = flowRecordMetaMapper.findByStreamId(streamIdValue);
        if (meta != null) {
            tableCache.put(streamIdValue, meta.getTableName());
            return true;
        }

        return false;
    }

    /**
     * 获取表名（不自动创建）
     *
     * @param streamId 价值流ID
     * @return 表名，如果不存在返回 null
     */
    public String getTableName(StreamId streamId) {
        String streamIdValue = streamId.value();

        // 从缓存中获取
        String tableName = tableCache.get(streamIdValue);
        if (tableName != null) {
            return tableName;
        }

        // 从数据库查询
        FlowRecordMetaEntity meta = flowRecordMetaMapper.findByStreamId(streamIdValue);
        if (meta != null) {
            tableCache.put(streamIdValue, meta.getTableName());
            return meta.getTableName();
        }

        return null;
    }
}
