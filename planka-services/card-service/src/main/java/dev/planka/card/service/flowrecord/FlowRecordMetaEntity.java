package dev.planka.card.service.flowrecord;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流动记录元数据实体
 * <p>
 * 用于管理按价值流动态创建的流动记录表
 */
@Data
@TableName("flow_record_meta")
public class FlowRecordMetaEntity {

    /**
     * 自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 价值流ID
     */
    private String streamId;

    /**
     * 流动记录表名
     */
    private String tableName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
