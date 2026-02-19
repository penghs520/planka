package dev.planka.api.schema.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 考勤申请校验请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceApplicationValidationRequest {

    /**
     * 组织ID
     */
    private String orgId;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 卡片类型ID
     */
    private String cardTypeId;

    /**
     * 字段值
     */
    private Map<String, Object> fieldValues;
}
