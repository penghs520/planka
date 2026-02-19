package dev.planka.user.service;

import dev.planka.api.schema.SchemaServiceClient;
import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.common.result.Result;
import dev.planka.common.util.SystemSchemaIds;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.AbstractCardType;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.fieldconfig.SingleLineTextFieldConfig;
import dev.planka.domain.schema.definition.fieldconfig.ValueSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 成员属性集服务
 * <p>
 * 负责创建成员属性集，定义成员共有的属性（用户名、邮箱、电话）。
 * 其他成员类型可以继承此属性集来获得这些属性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCardTypeService {

    private final SchemaServiceClient schemaServiceClient;

    /**
     * 成员属性集名称
     */
    private static final String MEMBER_TRAIT_NAME = "成员属性集";
    /**
     * 成员属性集编码
     */
    private static final String MEMBER_TRAIT_CODE = "member-trait";

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

    private static final String SYSTEM_OPERATOR = "system";

    /**
     * 创建成员属性集
     * <p>
     * 当创建组织时调用，创建一个名为"成员属性集"的属性集，
     * 包含系统属性：用户名、邮箱、联系电话
     * <p>
     * 使用固定ID：{orgId}:member-trait
     *
     * @param orgId 组织ID
     * @return 成员属性集ID
     * @throws RuntimeException 如果创建失败
     */
    public String createMemberAbstractCardType(String orgId) {
        log.info("Creating member abstract card type for org: {}", orgId);

        // 使用固定ID
        String abstractCardTypeId = SystemSchemaIds.memberAbstractCardTypeId(orgId);

        // 1. 创建成员属性集
        AbstractCardType cardType = new AbstractCardType(
                CardTypeId.of(abstractCardTypeId),
                orgId,
                MEMBER_TRAIT_NAME
        );
        cardType.setCode(MEMBER_TRAIT_CODE);
        cardType.setDescription("成员属性集，定义成员共有的属性（用户名、邮箱、电话），所有成员类型都继承此类型");
        cardType.setSystemType(true);

        CreateSchemaRequest cardTypeRequest = new CreateSchemaRequest();
        cardTypeRequest.setDefinition(cardType);

        Result<SchemaDefinition<?>> cardTypeResult = schemaServiceClient.create(orgId, SYSTEM_OPERATOR, cardTypeRequest);
        if (!cardTypeResult.isSuccess()) {
            log.error("Failed to create member abstract card type: {}", cardTypeResult.getMessage());
            throw new RuntimeException("创建成员属性集失败: " + cardTypeResult.getMessage());
        }

        log.info("Member abstract card type created: {}", abstractCardTypeId);

        // 2. 创建内置属性定义（关联到属性集）
        createSystemTextField(SystemSchemaIds.memberUsernameFieldId(orgId), orgId, abstractCardTypeId, FIELD_USERNAME, FIELD_USERNAME_CODE, "成员用户名，用于登录");
        createSystemTextField(SystemSchemaIds.memberEmailFieldId(orgId), orgId, abstractCardTypeId, FIELD_EMAIL, FIELD_EMAIL_CODE, "成员邮箱地址");
        createSystemTextField(SystemSchemaIds.memberPhoneFieldId(orgId), orgId, abstractCardTypeId, FIELD_PHONE, FIELD_PHONE_CODE, "成员联系电话");

        log.info("Member abstract card type and fields created successfully for org: {}", orgId);
        return abstractCardTypeId;
    }


    /**
     * 创建成员卡片类型
     * <p>
     * 当创建组织时调用，创建一个名为"成员"的卡片类型，
     * 继承自成员属性集，获得系统属性：用户名、邮箱、联系电话
     * <p>
     * 使用固定ID：{orgId}:member
     *
     * @param orgId                    组织ID
     * @param memberAbstractCardTypeId 继承的抽象成员卡片类型ID
     * @return 成员卡片类型ID
     * @throws RuntimeException 如果创建失败
     */
    public String createMemberCardType(String orgId, String memberAbstractCardTypeId) {
        log.info("Creating member card type for org: {}", orgId);

        // 使用固定ID
        String memberCardTypeId = SystemSchemaIds.memberCardTypeId(orgId);

        // 1. 创建成员卡片类型
        EntityCardType cardType = new EntityCardType(
                CardTypeId.of(memberCardTypeId),
                orgId,
                MEMBER_CARD_TYPE_NAME
        );
        cardType.setCode(MEMBER_CARD_TYPE_CODE);
        cardType.setDescription("组织成员卡片类型，与系统用户绑定，继承成员属性集");
        cardType.setSystemType(true);
        // 设置继承关系：继承成员属性集
        cardType.setParentTypeIds(Set.of(CardTypeId.of(memberAbstractCardTypeId)));

        CreateSchemaRequest cardTypeRequest = new CreateSchemaRequest();
        cardTypeRequest.setDefinition(cardType);

        // 使用 "system" 作为操作人，因为这是系统自动创建的卡片类型
        Result<SchemaDefinition<?>> cardTypeResult = schemaServiceClient.create(orgId, "system", cardTypeRequest);
        if (!cardTypeResult.isSuccess()) {
            log.error("Failed to create member card type: {}", cardTypeResult.getMessage());
            throw new RuntimeException("创建成员卡片类型失败: " + cardTypeResult.getMessage());
        }

        log.info("Member card type created: {}", memberCardTypeId);

        // 注意：字段定义已移到属性集中，这里不再创建

        log.info("Member card type created successfully for org: {}", orgId);
        return memberCardTypeId;
    }

    /**
     * 创建系统文本字段配置
     */
    private void createSystemTextField(String fieldId, String orgId, String cardTypeId, String name, String code, String description) {

        SingleLineTextFieldConfig fieldConfig = new SingleLineTextFieldConfig(
                FieldConfigId.of(fieldId),
                orgId,
                name,
                CardTypeId.of(cardTypeId),
                FieldId.of(fieldId),
                true // 系统内置字段
        );
        fieldConfig.setCode(code);
        fieldConfig.setDescription(description);
        fieldConfig.setValueSource(ValueSource.SYSTEM);
        fieldConfig.setRequired(false);
        fieldConfig.setReadOnly(true);

        CreateSchemaRequest fieldRequest = new CreateSchemaRequest();
        fieldRequest.setDefinition(fieldConfig);

        Result<SchemaDefinition<?>> fieldResult = schemaServiceClient.create(orgId, SYSTEM_OPERATOR, fieldRequest);
        if (!fieldResult.isSuccess()) {
            log.error("Failed to create system field config {}: {}", code, fieldResult.getMessage());
            throw new RuntimeException("创建系统字段配置失败: " + fieldResult.getMessage());
        }

        log.info("System field config created: {} ({}) for abstract card type: {}", name, code, cardTypeId);
    }
}
