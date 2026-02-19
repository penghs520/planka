package dev.planka.user.service;

import dev.planka.api.card.CardServiceClient;
import dev.planka.api.card.request.CreateCardRequest;
import dev.planka.api.schema.SchemaServiceClient;
import dev.planka.api.schema.dto.inheritance.FieldConfigListWithSource;
import dev.planka.api.schema.service.FieldConfigQueryService;
import dev.planka.common.result.Result;
import dev.planka.domain.card.CardId;
import dev.planka.domain.card.CardTitle;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.card.OrgId;
import dev.planka.domain.field.FieldValue;
import dev.planka.domain.field.TextFieldValue;
import dev.planka.domain.schema.definition.fieldconfig.FieldConfig;
import dev.planka.user.model.UserEntity;
import dev.planka.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 成员卡片服务
 * <p>
 * 负责创建成员卡片类型和成员卡片实例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCardService {

    private final SchemaServiceClient schemaServiceClient;
    private final FieldConfigQueryService fieldConfigQueryService;
    private final CardServiceClient cardServiceClient;
    private final UserRepository userRepository;

    /**
     * 成员卡片类型名称
     */
    private static final String MEMBER_CARD_TYPE_NAME = "成员";
    /**
     * 成员卡片类型编码
     */
    private static final String MEMBER_CARD_TYPE_CODE = "member";

    /**
     * 内置属性定义
     */
    private static final String FIELD_USERNAME = "用户名";
    private static final String FIELD_USERNAME_CODE = "username";
    private static final String FIELD_EMAIL = "邮箱";
    private static final String FIELD_EMAIL_CODE = "email";
    private static final String FIELD_PHONE = "联系电话";
    private static final String FIELD_PHONE_CODE = "phone";




    /**
     * 创建成员卡片
     * <p>
     * 当用户加入组织时调用，创建一个成员卡片实例
     *
     * @param orgId            组织ID
     * @param memberCardTypeId 成员卡片类型ID
     * @param userId           用户ID
     * @return 成员卡片ID
     * @throws RuntimeException 如果创建失败
     */
    public String createMemberCard(String orgId, String memberCardTypeId, String userId) {
        log.info("Creating member card for user: {} in org: {}", userId, orgId);

        // 1. 获取用户信息
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));

        // 2. 查询成员卡片类型的字段配置，获取fieldId
        Map<String, String> fieldCodeToId = getFieldIdsByCardType(memberCardTypeId);

        // 3. 构建属性值
        Map<String, FieldValue<?>> fieldValues = new HashMap<>();

        String usernameFieldId = fieldCodeToId.get(FIELD_USERNAME_CODE);
        if (usernameFieldId != null) {
            fieldValues.put(usernameFieldId, new TextFieldValue(usernameFieldId, user.getNickname()));
        }

        String emailFieldId = fieldCodeToId.get(FIELD_EMAIL_CODE);
        if (emailFieldId != null) {
            fieldValues.put(emailFieldId, new TextFieldValue(emailFieldId, user.getEmail()));
        }

        String phoneFieldId = fieldCodeToId.get(FIELD_PHONE_CODE);
        if (phoneFieldId != null && user.getPhone() != null) {
            fieldValues.put(phoneFieldId, new TextFieldValue(phoneFieldId, user.getPhone()));
        }

        // 4. 创建卡片
        String displayName = user.getNickname() != null ? user.getNickname() : user.getEmail();
        CreateCardRequest request = new CreateCardRequest(
                new OrgId(orgId),
                CardTypeId.of(memberCardTypeId),
                CardTitle.pure(displayName),
                "成员卡片",
                fieldValues
        );

        // 使用 "system" 作为操作人，因为创建成员卡片时成员卡片还不存在
        Result<CardId> result = cardServiceClient.create("system", request);
        if (!result.isSuccess()) {
            log.error("Failed to create member card: {}", result.getMessage());
            throw new RuntimeException("创建成员卡片失败: " + result.getMessage());
        }

        String memberCardId = result.getData().asStr();
        log.info("Member card created: {} for user: {}", memberCardId, userId);
        return memberCardId;
    }

    /**
     * 获取卡片类型关联的字段配置 code -> fieldId 映射
     */
    private Map<String, String> getFieldIdsByCardType(String cardTypeId) {
        Map<String, String> fieldCodeToId = new HashMap<>();

        // 通过字段配置查询接口获取卡片类型的字段配置列表
        Result<FieldConfigListWithSource> result = fieldConfigQueryService.getFieldConfigListWithSource(cardTypeId);

        if (result.isSuccess() && result.getData() != null) {
            for (FieldConfig fieldConfig : result.getData().getFields()) {
                if (fieldConfig.getCode() != null) {
                    fieldCodeToId.put(fieldConfig.getCode(), fieldConfig.getFieldId().value());
                }
            }
        }

        return fieldCodeToId;
    }

    /**
     * 删除成员卡片
     *
     * @param memberCardId 成员卡片ID
     */
    public void deleteMemberCard(String memberCardId, String operatorId) {
        log.info("Deleting member card: {}", memberCardId);

        Result<Void> result = cardServiceClient.discard(memberCardId, "成员离开组织", operatorId);
        if (!result.isSuccess()) {
            log.warn("Failed to delete member card {}: {}", memberCardId, result.getMessage());
        }
    }
}
