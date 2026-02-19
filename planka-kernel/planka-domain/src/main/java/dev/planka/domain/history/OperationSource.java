package dev.planka.domain.history;

import dev.planka.domain.history.source.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import dev.planka.domain.history.source.*;

/**
 * 操作来源 - Jackson 多态序列化
 * <p>
 * 支持以下来源类型：
 * <ul>
 *     <li>USER - 用户操作（默认）</li>
 *     <li>BIZ_RULE - 业务规则触发</li>
 *     <li>THIRD_PARTY - 三方系统同步</li>
 *     <li>FIELD_LINKAGE - 字段联动更新</li>
 *     <li>SCHEDULED_TASK - 定时任务执行</li>
 *     <li>IMPORT - 数据导入</li>
 *     <li>API_CALL - API 调用</li>
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserOperationSource.class, name = UserOperationSource.TYPE),
        @JsonSubTypes.Type(value = BizRuleOperationSource.class, name = BizRuleOperationSource.TYPE),
        @JsonSubTypes.Type(value = ThirdPartyOperationSource.class, name = ThirdPartyOperationSource.TYPE),
        @JsonSubTypes.Type(value = FieldLinkageOperationSource.class, name = FieldLinkageOperationSource.TYPE),
        @JsonSubTypes.Type(value = ScheduledTaskOperationSource.class, name = ScheduledTaskOperationSource.TYPE),
        @JsonSubTypes.Type(value = ImportOperationSource.class, name = ImportOperationSource.TYPE),
        @JsonSubTypes.Type(value = ApiCallOperationSource.class, name = ApiCallOperationSource.TYPE)
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface OperationSource {

    /**
     * 获取来源类型标识
     */
    String getType();

    /**
     * 获取国际化消息码
     */
    String getMessageKey();
}
