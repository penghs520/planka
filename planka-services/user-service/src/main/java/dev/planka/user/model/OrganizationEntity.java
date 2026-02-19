package dev.planka.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织实体
 */
@Data
@TableName("sys_organization")
public class OrganizationEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String name;
    private String description;
    private String logo;

    private String memberCardTypeId;

    /**
     * 考勤功能是否启用
     */
    private Boolean attendanceEnabled;

    private String status;

    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
