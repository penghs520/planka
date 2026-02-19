package dev.planka.event.notification;

import dev.planka.common.util.AssertUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

//内置通知事件：修改密码
@Getter
public class ChangePasswordNotificationEvent extends NotificationEvent {

    private final String userId;

    @JsonCreator
    public ChangePasswordNotificationEvent(String orgId, String operatorId, String sourceIp, String traceId, String userId) {
        super(orgId, operatorId, sourceIp, traceId);
        this.userId = AssertUtils.requireNotBlank(userId, "userId can't be blank");
    }
}
