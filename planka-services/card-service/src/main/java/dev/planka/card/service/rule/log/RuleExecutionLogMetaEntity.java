package dev.planka.card.service.rule.log;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 规则执行日志元数据实体
 * <p>
 * 记录已创建的分表信息
 */
@Getter
@Setter
@TableName("biz_rule_execution_log_meta")
public class RuleExecutionLogMetaEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 卡片类型ID */
    private String cardTypeId;

    /** 表名 */
    private String tableName;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
