package cn.agilean.kanban.notification.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 用户信息
 * 用于表达式解析中的操作人数据
 */
@Data
@Builder
public class UserInfo {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 用户属性（扩展字段）
     * 用于支持 ${操作人.部门} 等表达式
     */
    private Map<String, Object> attributes;
}
