package cn.planka.notification.recipient;

import cn.planka.domain.schema.definition.rule.action.RecipientSelector;
import cn.planka.notification.model.NotificationContext;
import cn.planka.notification.plugin.NotificationRequest.RecipientInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 接收者解析器
 * 根据接收者选择器解析目标用户
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipientResolver {

    // TODO: 注入 UserRepository 和 CardRepository (Feign Client)

    /**
     * 根据接收者选择器解析目标用户
     */
    public List<RecipientInfo> resolve(RecipientSelector selector, NotificationContext context) {
        try {
            List<RecipientInfo> recipients = new ArrayList<>();

            // 处理多选择器模式
            if (selector.getSelectors() != null && !selector.getSelectors().isEmpty()) {
                for (RecipientSelector.SelectorItem item : selector.getSelectors()) {
                    recipients.addAll(resolveSelector(item, context));
                }
            } else {
                // 向后兼容：单选择器模式
                recipients.addAll(resolveLegacySelector(selector, context));
            }

            // 去重
            return recipients.stream()
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to resolve recipients for selector: {}", selector, e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析单个选择器
     */
    private List<RecipientInfo> resolveSelector(RecipientSelector.SelectorItem item, NotificationContext context) {
        switch (item.getSelectorType()) {
            case CURRENT_OPERATOR:
                return resolveCurrentOperator(context);

            case FIXED_MEMBERS:
                return resolveFixedMembers(item.getMemberIds());

            case FROM_FIELD:
                if ("OPERATOR".equals(item.getSource())) {
                    return resolveFromOperatorField(item.getFieldId(), context);
                } else {
                    return resolveFromCardField(item.getFieldId(), context);
                }

            default:
                log.warn("Unknown selector type: {}", item.getSelectorType());
                return Collections.emptyList();
        }
    }

    /**
     * 解析当前操作人
     */
    private List<RecipientInfo> resolveCurrentOperator(NotificationContext context) {
        if (context.getOperator() == null) {
            return Collections.emptyList();
        }

        RecipientInfo recipient = RecipientInfo.builder()
                .userId(context.getOperator().getUserId())
                .email(context.getOperator().getEmail())
                .mobile(context.getOperator().getMobile())
                .build();

        return List.of(recipient);
    }

    /**
     * 解析固定成员
     */
    private List<RecipientInfo> resolveFixedMembers(List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Collections.emptyList();
        }

        // TODO: 批量查询用户信息
        // List<UserDTO> users = userClient.getUsersByIds(memberIds);

        // 临时返回空列表
        return Collections.emptyList();
    }

    /**
     * 从卡片字段解析接收者
     */
    private List<RecipientInfo> resolveFromCardField(String fieldId, NotificationContext context) {
        if (context.getCardSnapshot() == null || context.getCardSnapshot().getFieldValues() == null) {
            return Collections.emptyList();
        }

        Object fieldValue = context.getCardSnapshot().getFieldValues().get(fieldId);
        if (fieldValue == null) {
            return Collections.emptyList();
        }

        // TODO: 根据字段类型解析用户 ID
        // 如果是 LINK 类型字段，fieldValue 可能是 List<String>
        // 需要查询用户信息

        return Collections.emptyList();
    }

    /**
     * 从操作人字段解析接收者
     */
    private List<RecipientInfo> resolveFromOperatorField(String fieldId, NotificationContext context) {
        if (context.getOperator() == null || context.getOperator().getAttributes() == null) {
            return Collections.emptyList();
        }

        Object fieldValue = context.getOperator().getAttributes().get(fieldId);
        if (fieldValue == null) {
            return Collections.emptyList();
        }

        // TODO: 根据字段值解析用户信息

        return Collections.emptyList();
    }

    /**
     * 解析旧版选择器（向后兼容）
     */
    private List<RecipientInfo> resolveLegacySelector(RecipientSelector selector, NotificationContext context) {
        // TODO: 实现旧版选择器逻辑
        return Collections.emptyList();
    }
}
