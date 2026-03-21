package cn.planka.user.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_sidebar_preference")
public class UserSidebarPreferenceEntity {

    @TableId(type = IdType.INPUT)
    private String id;

    private String userId;
    private String orgId;
    private String prefs;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
