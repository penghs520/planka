package cn.planka.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_org_sidebar_workspace_preference")
public class OrgSidebarWorkspacePreferenceEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String orgId;
    /** 完整偏好 JSON */
    private String prefs;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
