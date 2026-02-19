package dev.planka.extension.history.service;

import dev.planka.common.util.SystemSchemaIds;
import dev.planka.domain.card.CardTypeId;
import dev.planka.extension.history.mapper.CardHistoryMapper;
import dev.planka.extension.history.mapper.CardHistoryMetaMapper;
import dev.planka.extension.history.model.CardHistoryMetaEntity;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * 卡片历史表管理器
 * <p>
 * 负责按卡片类型动态创建和管理历史记录表
 */
@Slf4j
@Component
public class CardHistoryTableManager {

    /**
     * 表名前缀
     */
    private static final String TABLE_PREFIX = "card_history_";

    /**
     * CardTypeId 合法字符模式（只允许数字，因为是雪花算法生成）
     */
    private static final Pattern CARD_TYPE_ID_PATTERN = Pattern.compile("^\\d+$");

    /**
     * 表名缓存：cardTypeId -> tableName
     */
    private final ConcurrentMap<String, String> tableCache = new ConcurrentHashMap<>();

    private final CardHistoryMapper cardHistoryMapper;
    private final CardHistoryMetaMapper cardHistoryMetaMapper;

    public CardHistoryTableManager(CardHistoryMapper cardHistoryMapper, CardHistoryMetaMapper cardHistoryMetaMapper) {
        this.cardHistoryMapper = cardHistoryMapper;
        this.cardHistoryMetaMapper = cardHistoryMetaMapper;
    }

    /**
     * 启动时加载已有的表元数据到缓存
     */
    @PostConstruct
    public void init() {
        cardHistoryMetaMapper.selectList(null).forEach(meta -> {
            tableCache.put(meta.getCardTypeId(), meta.getTableName());
            log.debug("加载卡片历史表缓存: {} -> {}", meta.getCardTypeId(), meta.getTableName());
        });
        log.info("卡片历史表管理器初始化完成，加载了 {} 个表", tableCache.size());
    }

    /**
     * 获取或创建历史记录表
     *
     * @param cardTypeId 卡片类型ID
     * @return 表名
     */
    public String getOrCreateTable(CardTypeId cardTypeId) {
        return getOrCreateTable(cardTypeId.value());
    }

    /**
     * 获取或创建历史记录表
     *
     * @param cardTypeId 卡片类型ID字符串
     * @return 表名
     */
    public String getOrCreateTable(String cardTypeId) {
        // 从缓存中获取
        String tableName = tableCache.get(cardTypeId);
        if (tableName != null) {
            return tableName;
        }

        // 缓存未命中，尝试创建表
        return createTableIfNotExists(cardTypeId);
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
        CardHistoryMetaEntity meta = cardHistoryMetaMapper.findByCardTypeId(cardTypeId);
        if (meta != null) {
            tableCache.put(cardTypeId, meta.getTableName());
            return meta.getTableName();
        }

        // 生成表名并创建表
        tableName = generateTableName(cardTypeId);

        // 创建物理表
        cardHistoryMapper.createTable(tableName);
        log.info("创建卡片历史表: {}", tableName);

        // 保存元数据
        meta = new CardHistoryMetaEntity();
        meta.setCardTypeId(cardTypeId);
        meta.setTableName(tableName);
        meta.setCreatedAt(LocalDateTime.now());
        cardHistoryMetaMapper.insert(meta);

        // 更新缓存
        tableCache.put(cardTypeId, tableName);

        return tableName;
    }

    /**
     * 生成表名
     * <p>
     * 表名格式: card_history_{cardTypeId}
     * 需要验证 cardTypeId 格式以防止 SQL 注入
     */
    private String generateTableName(String cardTypeId) {
        // 系统内置 Schema ID 包含冒号（如 org123:member），需要转义
        if (SystemSchemaIds.isSystemSchemaId(cardTypeId)) {
            // 将冒号替换为下划线：org123:member -> org123_member
            String escapedId = cardTypeId.replace(':', '_');
            return TABLE_PREFIX + escapedId;
        }

        // 普通ID验证（纯数字雪花ID）
        if (!CARD_TYPE_ID_PATTERN.matcher(cardTypeId).matches()) {
            throw new IllegalArgumentException("非法的 CardTypeId 格式: " + cardTypeId);
        }
        return TABLE_PREFIX + cardTypeId;
    }

    /**
     * 检查表是否存在
     */
    public boolean tableExists(CardTypeId cardTypeId) {
        return tableExists(cardTypeId.value());
    }

    /**
     * 检查表是否存在
     */
    public boolean tableExists(String cardTypeId) {
        // 先检查缓存
        if (tableCache.containsKey(cardTypeId)) {
            return true;
        }

        // 检查元数据表
        CardHistoryMetaEntity meta = cardHistoryMetaMapper.findByCardTypeId(cardTypeId);
        if (meta != null) {
            tableCache.put(cardTypeId, meta.getTableName());
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
        return getTableName(cardTypeId.value());
    }

    /**
     * 获取表名（不自动创建）
     *
     * @param cardTypeId 卡片类型ID字符串
     * @return 表名，如果不存在返回 null
     */
    public String getTableName(String cardTypeId) {
        // 从缓存中获取
        String tableName = tableCache.get(cardTypeId);
        if (tableName != null) {
            return tableName;
        }

        // 从数据库查询
        CardHistoryMetaEntity meta = cardHistoryMetaMapper.findByCardTypeId(cardTypeId);
        if (meta != null) {
            tableCache.put(cardTypeId, meta.getTableName());
            return meta.getTableName();
        }

        return null;
    }
}
