package dev.planka.oss.entity;

import dev.planka.oss.plugin.FileCategory;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件元数据实体
 */
@Data
@TableName("sys_file_meta")
public class FileMeta {

    @TableId(type = IdType.INPUT)
    private String id;

    private String orgId;

    /**
     * 操作者 ID（当前用户在当前组织对应的成员卡 ID，即当前成员 ID，不是用户 ID）
     */
    private String operatorId;

    @TableField("category")
    private FileCategory category;

    private String originalName;

    private String objectKey;

    private String url;

    private Long size;

    private String contentType;

    private String storagePlugin;

    private LocalDateTime createdAt;
}
