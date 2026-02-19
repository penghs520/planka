package dev.planka.card.service.sequence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 号段分配表实体
 */
@Data
@TableName("sequence_segment")
public class SequenceSegmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 组织ID
     */
    private String orgId;

    /**
     * 号段类型标识（如 CARD_CODE）
     */
    private String segmentKey;

    /**
     * 当前已分配的最大值
     */
    private Long currentMax;

    /**
     * 每次获取的号段步长
     */
    private Integer step;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
