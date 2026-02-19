package dev.planka.extension.history.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 卡片操作历史实体
 * <p>
 * 注意：此实体对应动态分表 card_history_{cardTypeId}，
 * 不使用 @TableName 注解，表名在运行时动态指定
 */
@Data
public class CardHistoryEntity {

    /**
     * 历史记录ID（雪花算法）
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 组织ID
     */
    private String orgId;

    /**
     * 卡片ID
     */
    private Long cardId;

    /**
     * 卡片类型ID
     */
    private String cardTypeId;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作人IP
     */
    private String operatorIp;

    /**
     * 操作来源（JSON）
     */
    @TableField("operation_source")
    private String operationSource;

    /**
     * 历史消息（JSON）
     */
    @TableField("message")
    private String message;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
