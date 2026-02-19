package dev.planka.card.service.rule.log;

import dev.planka.card.mapper.RuleExecutionLogMapper;
import dev.planka.card.mapper.RuleExecutionLogMetaMapper;
import dev.planka.domain.card.CardTypeId;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * 规则执行日志表管理器
 * <p>
 * 负责按卡片类型动态创建和管理执行日志表
 */
@Slf4j
@Component
public class RuleExecutionLogTableManager {

    /**
     * 表名前缀
     */
    private static final String TABLE_PREFIX = "biz_rule_execution_log_";

    /**
     * CardTypeId 合法字符模式（只允许数字，因为是雪花算法生成）
     */
    private static final Pattern CARD_TYPE_ID_PATTERN = Pattern.compile("^\\d+$");

    /**
     * 表名缓存：cardTypeId -> tableName
     */
    private final ConcurrentMap<String, String> tableCache = new ConcurrentHashMap<>();

    private final RuleExecutionLogMapper logMapper;
    private final RuleExecutionLogMetaMapper metaMapper;

    public RuleExecutionLogTableManager(RuleExecutionLogMapper logMapper, RuleExecutionLogMetaMapper metaMapper) {
        this.logMapper = logMapper;
        this.metaMapper = metaMapper;
    }

    /**
     * 启动时加载已有的表元数据到缓存
     */
    @PostConstruct
    public void init() {
        metaMapper.selectList(null).forEach(meta -> {
            tableCache.put(meta.getCardTypeId(), meta.getTableName());
            log.debug("加载规则执行日志表缓存: {} -> {}", meta.getCardTypeId(), meta.getTableName());
        });
        log.info("规则执行日志表管理器初始化完成，加载了 {} 个表", tableCache.size());
    }

    /**
     * 获取或创建执行日志表
     *
     * @param cardTypeId 卡片类型ID
     * @return 表名
     */
    public String getOrCreateTable(CardTypeId cardTypeId) {
        String cardTypeIdValue = cardTypeId.value();

        // 从缓存中获取
        String tableName = tableCache.get(cardTypeIdValue);
        if (tableName != null) {
            return tableName;
        }

        // 缓存未命中，尝试创建表
        return createTableIfNotExists(cardTypeIdValue);
    }

    /**
     * 创建表（如果不存在）
     */
    @Transactional
    protected String createTableIfNotExists(String cardTypeId) {
        // 双重检查
        String tableName = tableCache.get(cardTypeId);
        if (tableName != null) {
            return tableName;
        }

        // 从数据库查询元数据
        RuleExecutionLogMetaEntity meta = metaMapper.findByCardTypeId(cardTypeId);
        if (meta != null) {
            tableCache.put(cardTypeId, meta.getTableName());
            return meta.getTableName();
        }

        // 生成表名并创建表
        tableName = generateTableName(cardTypeId);

        // 创建物理表
        logMapper.createTable(tableName);
        log.info("创建规则执行日志表: {}", tableName);

        // 保存元数据
        meta = new RuleExecutionLogMetaEntity();
        meta.setCardTypeId(cardTypeId);
        meta.setTableName(tableName);
        meta.setCreatedAt(LocalDateTime.now());
        metaMapper.insert(meta);

        // 更新缓存
        tableCache.put(cardTypeId, tableName);

        return tableName;
    }

    /**
     * 生成表名
     * <p>
     * 表名格式: biz_rule_execution_log_{cardTypeId}
     * 需要验证 cardTypeId 格式以防止 SQL 注入
     */
    private String generateTableName(String cardTypeId) {
        // 验证 cardTypeId 格式（只允许数字）
        if (!CARD_TYPE_ID_PATTERN.matcher(cardTypeId).matches()) {
            throw new IllegalArgumentException("非法的 CardTypeId 格式: " + cardTypeId);
        }
        return TABLE_PREFIX + cardTypeId;
    }

    /**
     * 检查表是否存在
     */
    public boolean tableExists(CardTypeId cardTypeId) {
        String cardTypeIdValue = cardTypeId.value();

        // 先检查缓存
        if (tableCache.containsKey(cardTypeIdValue)) {
            return true;
        }

        // 检查元数据表
        RuleExecutionLogMetaEntity meta = metaMapper.findByCardTypeId(cardTypeIdValue);
        if (meta != null) {
            tableCache.put(cardTypeIdValue, meta.getTableName());
            return true;
        }

        return false;
    }

    /**
     * 获取表名（不自动创建）
     *
     * @param cardTypeId 卡片类型ID
     * @return 表名，如果不存在返回 null
     */
    public String getTableName(CardTypeId cardTypeId) {
        String cardTypeIdValue = cardTypeId.value();

        // 从缓存中获取
        String tableName = tableCache.get(cardTypeIdValue);
        if (tableName != null) {
            return tableName;
        }

        // 从数据库查询
        RuleExecutionLogMetaEntity meta = metaMapper.findByCardTypeId(cardTypeIdValue);
        if (meta != null) {
            tableCache.put(cardTypeIdValue, meta.getTableName());
            return meta.getTableName();
        }

        return null;
    }
}
