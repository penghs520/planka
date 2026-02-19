package dev.planka.card.service.sequence;

import dev.planka.card.mapper.SequenceSegmentMapper;
import dev.planka.domain.card.CardTypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 号段服务
 * 提供组织级别的编号分配功能
 */
@Service
public class SequenceSegmentService {

    private static final Logger logger = LoggerFactory.getLogger(SequenceSegmentService.class);

    /**
     * 号段类型：卡片编号
     */
    public static final String SEGMENT_KEY_CARD_CODE = "CARD_CODE";

    /**
     * 默认号段步长
     */
    private static final int DEFAULT_STEP = 1000;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY = 3;

    /**
     * 组织号段缓冲区映射
     * key: orgId + ":" + segmentKey
     */
    private final ConcurrentHashMap<String, SegmentBuffer> buffers = new ConcurrentHashMap<>();

    private final SequenceSegmentMapper segmentMapper;

    public SequenceSegmentService(SequenceSegmentMapper segmentMapper) {
        this.segmentMapper = segmentMapper;
    }

    /**
     * 获取下一个卡片编号
     *
     * @param orgId 组织ID
     * @return 组织内递增的编号
     */
    public long getNextCodeInOrg(String orgId) {
        String cacheKey = orgId + ":" + SEGMENT_KEY_CARD_CODE;
        SegmentBuffer buffer = buffers.computeIfAbsent(cacheKey, SegmentBuffer::new);
        return buffer.getNext(() -> loadNextSegment(orgId, cacheKey));
    }

    /**
     * 获取指定类型的下一个编号
     */
    public long getNextCode(String orgId, CardTypeId typeId, String segmentKey) {
        String cacheKey = orgId + ":" + typeId.value() + ":" + segmentKey;
        SegmentBuffer buffer = buffers.computeIfAbsent(cacheKey, SegmentBuffer::new);
        return buffer.getNext(() -> loadNextSegment(orgId, cacheKey));
    }

    /**
     * 批量获取卡片编号
     *
     * @param orgId 组织ID
     * @param count 需要的编号数量
     * @return 编号数组
     */
    public long[] getNextCodesInOrg(String orgId, int count) {
        long[] codes = new long[count];
        for (int i = 0; i < count; i++) {
            codes[i] = getNextCodeInOrg(orgId);
        }
        return codes;
    }

    /**
     * 从数据库加载下一个号段
     */
    private Segment loadNextSegment(String orgId, String segmentKey) {
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                // 查询当前号段记录
                SequenceSegmentEntity entity = segmentMapper.findByOrgIdAndKey(orgId, segmentKey);

                if (entity == null) {
                    // 第一次为该组织分配编号，插入新记录
                    int inserted = segmentMapper.insertNewSegment(orgId, segmentKey, DEFAULT_STEP);
                    if (inserted > 0) {
                        logger.info("为组织 {} 创建新号段记录，初始号段 [1, {}]", orgId, DEFAULT_STEP);
                        return new Segment(1, DEFAULT_STEP);
                    }
                    // 插入失败，可能是并发插入，重新查询
                    entity = segmentMapper.findByOrgIdAndKey(orgId, segmentKey);
                    if (entity == null) {
                        throw new RuntimeException("创建号段记录失败");
                    }
                }

                // 使用乐观锁更新获取下一个号段
                long oldMax = entity.getCurrentMax();
                int step = entity.getStep() != null ? entity.getStep() : DEFAULT_STEP;
                int version = entity.getVersion();

                int updated = segmentMapper.updateAndIncrementMax(orgId, segmentKey, version);
                if (updated > 0) {
                    long start = oldMax + 1;
                    long end = oldMax + step;
                    logger.info("为组织 {} 分配新号段 [{}, {}]", orgId, start, end);
                    return new Segment(start, end);
                }

                // 更新失败，版本冲突，重试
                logger.warn("号段更新版本冲突，重试 {}/{}", retry + 1, MAX_RETRY);
            } catch (Exception e) {
                logger.error("加载号段失败，重试 {}/{}", retry + 1, MAX_RETRY, e);
                if (retry == MAX_RETRY - 1) {
                    throw new RuntimeException("加载号段失败", e);
                }
            }
        }
        throw new RuntimeException("加载号段失败：超过最大重试次数");
    }
}
