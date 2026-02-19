package dev.planka.api.schema.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考勤申请校验响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceApplicationValidationResponse {

    /**
     * 是否通过校验
     */
    private boolean valid;

    /**
     * 错误信息（校验失败时）
     */
    private String message;

    public static AttendanceApplicationValidationResponse success() {
        return new AttendanceApplicationValidationResponse(true, null);
    }

    public static AttendanceApplicationValidationResponse failure(String message) {
        return new AttendanceApplicationValidationResponse(false, message);
    }
}
