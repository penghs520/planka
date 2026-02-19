package dev.planka.common.util;

/**
 * 系统 Schema ID 构建工具类
 * <p>
 * 所有内置 Schema 的 ID 使用组织ID拼接构建，保证可预测性和幂等性。
 * ID 格式：{orgId}:{type} 或 {orgId}:{type}:{name}
 */
public final class SystemSchemaIds {

    private SystemSchemaIds() {
        // 工具类禁止实例化
    }

    // ======================= 卡片类型 ID =======================

    /**
     * 成员卡片类型 ID
     * <p>
     * 格式：{orgId}:member
     *
     * @param orgId 组织ID
     * @return 成员卡片类型ID
     */
    public static String memberCardTypeId(String orgId) {
        return orgId + ":member";
    }

    /**
     * 成员属性集 ID
     * <p>
     * 格式：{orgId}:member-trait
     *
     * @param orgId 组织ID
     * @return 成员属性集ID
     */
    public static String memberAbstractCardTypeId(String orgId) {
        return orgId + ":member-trait";
    }

    public static String memberUsernameFieldId(String orgId) {
        return orgId + ":username";
    }

    public static String memberPhoneFieldId(String orgId) {
        return orgId + ":phone";
    }

    public static String memberEmailFieldId(String orgId) {
        return orgId + ":email";
    }

    /**
     * 任意卡属性集 ID
     * <p>
     * 格式：{orgId}:any-trait
     *
     * @param orgId 组织ID
     * @return 任意卡属性集ID
     */
    public static String anyTraitTypeId(String orgId) {
        return orgId + ":any-trait";
    }

    // ======================= 考勤卡片类型 ID =======================

    /**
     * 考勤记录卡片类型 ID
     * <p>
     * 格式：{orgId}:attendance-record
     *
     * @param orgId 组织ID
     * @return 考勤记录卡片类型ID
     */
    public static String attendanceRecordCardTypeId(String orgId) {
        return orgId + ":attendance-record";
    }

    /**
     * 请假申请卡片类型 ID
     * <p>
     * 格式：{orgId}:leave-application
     *
     * @param orgId 组织ID
     * @return 请假申请卡片类型ID
     */
    public static String leaveApplicationCardTypeId(String orgId) {
        return orgId + ":leave-application";
    }

    /**
     * 加班申请卡片类型 ID
     * <p>
     * 格式：{orgId}:overtime-application
     *
     * @param orgId 组织ID
     * @return 加班申请卡片类型ID
     */
    public static String overtimeApplicationCardTypeId(String orgId) {
        return orgId + ":overtime-application";
    }

    /**
     * 补卡申请卡片类型 ID
     * <p>
     * 格式：{orgId}:makeup-application
     *
     * @param orgId 组织ID
     * @return 补卡申请卡片类型ID
     */
    public static String makeupApplicationCardTypeId(String orgId) {
        return orgId + ":makeup-application";
    }

    // ======================= 工时卡片类型 ID =======================

    /**
     * 工时卡片类型 ID
     * <p>
     * 格式：{orgId}:worklog
     *
     * @param orgId 组织ID
     * @return 工时卡片类型ID
     */
    public static String worklogCardTypeId(String orgId) {
        return orgId + ":worklog";
    }

    /**
     * 结算记录卡片类型 ID
     * <p>
     * 格式：{orgId}:settlement-record
     *
     * @param orgId 组织ID
     * @return 结算记录卡片类型ID
     */
    public static String settlementRecordCardTypeId(String orgId) {
        return orgId + ":settlement-record";
    }

    // ======================= 关联类型 ID =======================

    /**
     * 创建人关联类型 ID
     * <p>
     * 格式：{orgId}:link:creator
     *
     * @param orgId 组织ID
     * @return 创建人关联类型ID
     */
    public static String creatorLinkTypeId(String orgId) {
        return orgId + ":link:creator";
    }

    /**
     * 归档人关联类型 ID
     * <p>
     * 格式：{orgId}:link:archiver
     *
     * @param orgId 组织ID
     * @return 归档人关联类型ID
     */
    public static String archiverLinkTypeId(String orgId) {
        return orgId + ":link:archiver";
    }

    /**
     * 丢弃人关联类型 ID
     * <p>
     * 格式：{orgId}:link:discarder
     *
     * @param orgId 组织ID
     * @return 丢弃人关联类型ID
     */
    public static String discarderLinkTypeId(String orgId) {
        return orgId + ":link:discarder";
    }

    // ======================= 辅助方法 =======================

    /**
     * 判断是否为系统 Schema ID
     * <p>
     * 系统 Schema ID 包含冒号分隔符
     *
     * @param schemaId Schema ID
     * @return 是否为系统 Schema ID
     */
    public static boolean isSystemSchemaId(String schemaId) {
        return schemaId != null && schemaId.contains(":");
    }

    /**
     * 从系统 Schema ID 中提取组织ID
     *
     * @param schemaId 系统 Schema ID
     * @return 组织ID，如果不是系统 Schema ID 则返回 null
     */
    public static String extractOrgId(String schemaId) {
        if (!isSystemSchemaId(schemaId)) {
            return null;
        }
        int colonIndex = schemaId.indexOf(':');
        return schemaId.substring(0, colonIndex);
    }
}
