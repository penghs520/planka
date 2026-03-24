package cn.planka.history.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 卡片历史表元数据实体
 * <p>
 * 记录每个实体类型对应的历史表名，用于动态分表
 */
@Data
@TableName("card_history_meta")
public class CardHistoryMetaEntity {

    /**
     * 自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 实体类型ID
     */
    private String cardTypeId;

    /**
     * 历史表名
     */
    private String tableName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
