package cn.planka.user.service;

import cn.planka.api.schema.SchemaServiceClient;
import cn.planka.api.schema.request.CreateSchemaRequest;
import cn.planka.common.result.Result;
import cn.planka.common.util.SystemSchemaIds;
import cn.planka.domain.card.CardTypeId;
import cn.planka.domain.field.FieldConfigId;
import cn.planka.domain.field.FieldId;
import cn.planka.domain.link.LinkTypeId;
import cn.planka.domain.schema.definition.AbstractSchemaDefinition;
import cn.planka.domain.schema.definition.SchemaDefinition;
import cn.planka.domain.schema.definition.cardtype.AbstractCardType;
import cn.planka.domain.schema.definition.cardtype.EntityCardType;
import cn.planka.domain.schema.definition.fieldconfig.EnumFieldConfig;
import cn.planka.domain.schema.definition.fieldconfig.SingleLineTextFieldConfig;
import cn.planka.domain.schema.definition.fieldconfig.ValueSource;
import cn.planka.domain.schema.definition.link.LinkTypeDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 组织创建时写入 schema-service 的内置定义：成员属性集与成员类型、任意卡属性集与系统关联、Team/Project/Issue。
 * <p>
 * Team/Project/Issue 相关创建前通过 {@link #schemaExists(String)} 幂等探测；成员与任意卡段在重复调用时跳过已存在的定义。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuiltinCardTypeService {

    private static final String SYSTEM_OPERATOR = "system";

    // ----- 成员 -----
    private static final String MEMBER_TRAIT_NAME = "成员属性集";
    private static final String MEMBER_TRAIT_CODE = "member-trait";
    private static final String MEMBER_ENTITY_NAME = "成员";
    private static final String MEMBER_ENTITY_CODE = "member";
    private static final String FIELD_USERNAME = "用户名";
    private static final String FIELD_USERNAME_CODE = "username";
    private static final String FIELD_EMAIL = "邮箱";
    private static final String FIELD_EMAIL_CODE = "email";
    private static final String FIELD_PHONE = "联系电话";
    private static final String FIELD_PHONE_CODE = "phone";

    // ----- 任意卡属性集 -----
    private static final String ANY_TRAIT_NAME = "任意卡属性集";
    private static final String ANY_TRAIT_CODE = "any-trait";
    private static final String CREATOR_LINK_CODE = "creator";
    private static final String ARCHIVER_LINK_CODE = "archiver";
    private static final String DISCARDER_LINK_CODE = "discarder";

    private final SchemaServiceClient schemaServiceClient;

    // ==================== 对外：组织创建流程 ====================

    /**
     * 成员属性集 + 用户名/邮箱/电话字段
     *
     * @return 成员属性集 ID（{orgId}:member-trait）
     */
    public String createMemberAbstractCardType(String orgId) {
        String abstractCardTypeId = SystemSchemaIds.memberAbstractCardTypeId(orgId);
        if (!schemaExists(abstractCardTypeId)) {
            log.info("Creating member abstract card type for org: {}", orgId);
            AbstractCardType cardType = new AbstractCardType(
                    CardTypeId.of(abstractCardTypeId),
                    orgId,
                    MEMBER_TRAIT_NAME
            );
            cardType.setCode(MEMBER_TRAIT_CODE);
            cardType.setDescription("成员属性集，定义成员共有的属性（用户名、邮箱、电话），所有成员类型都继承此类型");
            cardType.setSystemType(true);
            createSchema(orgId, cardType);
            log.info("Member abstract card type created: {}", abstractCardTypeId);
        }
        ensureMemberSystemTextField(orgId, abstractCardTypeId, SystemSchemaIds.memberUsernameFieldId(orgId),
                FIELD_USERNAME, FIELD_USERNAME_CODE, "成员用户名，用于登录");
        ensureMemberSystemTextField(orgId, abstractCardTypeId, SystemSchemaIds.memberEmailFieldId(orgId),
                FIELD_EMAIL, FIELD_EMAIL_CODE, "成员邮箱地址");
        ensureMemberSystemTextField(orgId, abstractCardTypeId, SystemSchemaIds.memberPhoneFieldId(orgId),
                FIELD_PHONE, FIELD_PHONE_CODE, "成员联系电话");
        return abstractCardTypeId;
    }

    /**
     * 成员实体类型（继承成员属性集）
     *
     * @return 成员卡片类型 ID（{orgId}:member）
     */
    public String createMemberCardType(String orgId, String memberAbstractCardTypeId) {
        String memberCardTypeId = SystemSchemaIds.memberCardTypeId(orgId);
        if (schemaExists(memberCardTypeId)) {
            return memberCardTypeId;
        }
        log.info("Creating member card type for org: {}", orgId);
        EntityCardType cardType = new EntityCardType(
                CardTypeId.of(memberCardTypeId),
                orgId,
                MEMBER_ENTITY_NAME
        );
        cardType.setCode(MEMBER_ENTITY_CODE);
        cardType.setDescription("组织成员卡片类型，与系统用户绑定，继承成员属性集");
        cardType.setSystemType(true);
        cardType.setParentTypeIds(Set.of(CardTypeId.of(memberAbstractCardTypeId)));
        createSchema(orgId, cardType);
        log.info("Member card type created: {}", memberCardTypeId);
        return memberCardTypeId;
    }

    /**
     * 任意卡属性集 + 创建人/归档人/丢弃人（目标端为成员属性集）
     *
     * @return 任意卡属性集 ID（{orgId}:any-trait）
     */
    public String createAnyTraitAndSystemMemberLinks(String orgId) {
        String rootCardTypeId = SystemSchemaIds.anyTraitTypeId(orgId);
        String memberAbstractCardTypeId = SystemSchemaIds.memberAbstractCardTypeId(orgId);
        if (!schemaExists(rootCardTypeId)) {
            AbstractCardType cardType = new AbstractCardType(
                    CardTypeId.of(rootCardTypeId),
                    orgId,
                    ANY_TRAIT_NAME
            );
            cardType.setCode(ANY_TRAIT_CODE);
            cardType.setDescription("系统任意卡属性集，所有卡片类型隐式继承此类型");
            cardType.setSystemType(true);
            createSchema(orgId, cardType);
            log.info("Any-trait card type created: {}", rootCardTypeId);
        }
        ensureSystemMemberLink(orgId, rootCardTypeId, memberAbstractCardTypeId,
                SystemSchemaIds.creatorLinkTypeId(orgId), "创建人", "创建的卡", CREATOR_LINK_CODE);
        ensureSystemMemberLink(orgId, rootCardTypeId, memberAbstractCardTypeId,
                SystemSchemaIds.archiverLinkTypeId(orgId), "归档人", "归档的卡", ARCHIVER_LINK_CODE);
        ensureSystemMemberLink(orgId, rootCardTypeId, memberAbstractCardTypeId,
                SystemSchemaIds.discarderLinkTypeId(orgId), "丢弃人", "丢弃的卡", DISCARDER_LINK_CODE);
        return rootCardTypeId;
    }

    /**
     * Team / Project / Issue 实体与业务关联（幂等）
     */
    public void initBuiltinTypes(String orgId) {
        createTeamType(orgId);
        createProjectType(orgId);
        createIssueType(orgId);
        createTeamMemberLink(orgId);
        createTeamLeadLink(orgId);
        createProjectLeadLink(orgId);
        createTeamProjectLink(orgId);
        createProjectIssueLink(orgId);
    }

    public boolean hasBuiltinTeamType(String orgId) {
        return schemaExists(SystemSchemaIds.teamCardTypeId(orgId));
    }

    /** 是否已创建团队负责人关联（用于存量组织补建 Lead 等新内置关联） */
    public boolean hasTeamLeadLinkType(String orgId) {
        return schemaExists(SystemSchemaIds.teamLeadLinkTypeId(orgId));
    }

    private boolean schemaExists(String schemaId) {
        Result<SchemaDefinition<?>> r = schemaServiceClient.getById(schemaId);
        return r.isSuccess() && r.getData() != null;
    }

    private void ensureMemberSystemTextField(String orgId, String cardTypeId, String fieldId,
                                               String name, String code, String description) {
        if (schemaExists(fieldId)) {
            return;
        }
        SingleLineTextFieldConfig field = new SingleLineTextFieldConfig(
                FieldConfigId.of(fieldId),
                orgId,
                name,
                CardTypeId.of(cardTypeId),
                FieldId.of(fieldId),
                true
        );
        field.setCode(code);
        field.setDescription(description);
        field.setValueSource(ValueSource.SYSTEM);
        field.setRequired(false);
        field.setReadOnly(true);
        createSchema(orgId, field);
    }

    private void ensureSystemMemberLink(String orgId, String rootCardTypeId, String memberAbstractCardTypeId,
                                        String linkTypeId, String linkName, String targetName, String linkCode) {
        if (schemaExists(linkTypeId)) {
            return;
        }
        LinkTypeDefinition linkType = new LinkTypeDefinition(LinkTypeId.of(linkTypeId), orgId, linkName);
        linkType.setCode(linkCode);
        linkType.setSourceName(linkName);
        linkType.setTargetName(targetName);
        linkType.setSourceCardTypeIds(List.of(CardTypeId.of(rootCardTypeId)));
        linkType.setTargetCardTypeIds(List.of(CardTypeId.of(memberAbstractCardTypeId)));
        linkType.setSourceMultiSelect(false);
        linkType.setTargetMultiSelect(true);
        linkType.setSourceVisible(true);
        linkType.setTargetVisible(false);
        linkType.setSystemLinkType(true);
        linkType.setSystemInput(true);
        createSchema(orgId, linkType);
        log.info("System link type created: {} ({})", linkName, linkTypeId);
    }

    private void createTeamType(String orgId) {
        String typeId = SystemSchemaIds.teamCardTypeId(orgId);
        if (!schemaExists(typeId)) {
            EntityCardType cardType = new EntityCardType(CardTypeId.of(typeId), orgId, "团队");
            cardType.setCode("team");
            cardType.setDescription("内置团队卡片类型");
            cardType.setSystemType(true);
            createSchema(orgId, cardType);
            log.info("Builtin team card type created: {}", typeId);
        }
        ensureTextField(orgId, typeId, SystemSchemaIds.teamIdentifierFieldId(orgId), "标识", "identifier", "团队标识，如 ENG", false);
        ensureTextField(orgId, typeId, SystemSchemaIds.teamColorFieldId(orgId), "颜色", "color", "主题色", false);
    }

    private void createProjectType(String orgId) {
        String typeId = SystemSchemaIds.projectCardTypeId(orgId);
        if (!schemaExists(typeId)) {
            EntityCardType cardType = new EntityCardType(CardTypeId.of(typeId), orgId, "项目");
            cardType.setCode("project");
            cardType.setDescription("内置项目卡片类型");
            cardType.setSystemType(true);
            createSchema(orgId, cardType);
            log.info("Builtin project card type created: {}", typeId);
        }
        ensureTextField(orgId, typeId, SystemSchemaIds.projectIdentifierFieldId(orgId), "标识", "identifier", "项目标识", false);
        ensureEnumField(orgId, typeId, SystemSchemaIds.projectStatusFieldId(orgId), "状态", "status", "项目状态", projectStatusOptions(orgId));
    }

    private void createIssueType(String orgId) {
        String typeId = SystemSchemaIds.issueCardTypeId(orgId);
        if (!schemaExists(typeId)) {
            EntityCardType cardType = new EntityCardType(CardTypeId.of(typeId), orgId, "工作项");
            cardType.setCode("issue");
            cardType.setDescription("内置工作项（Issue）卡片类型");
            cardType.setSystemType(true);
            createSchema(orgId, cardType);
            log.info("Builtin issue card type created: {}", typeId);
        }
        ensureEnumField(orgId, typeId, SystemSchemaIds.issuePriorityFieldId(orgId), "优先级", "priority", "优先级", issuePriorityOptions(orgId));
        ensureEnumField(orgId, typeId, SystemSchemaIds.issueStatusFieldId(orgId), "状态", "status", "状态", issueStatusOptions(orgId));
    }

    private void createTeamMemberLink(String orgId) {
        String linkId = SystemSchemaIds.teamMemberLinkTypeId(orgId);
        if (schemaExists(linkId)) {
            return;
        }
        LinkTypeDefinition link = new LinkTypeDefinition(LinkTypeId.of(linkId), orgId, "团队成员");
        link.setCode("team-member");
        link.setSourceName("团队成员");
        link.setTargetName("所在团队");
        link.setSourceCardTypeIds(List.of(CardTypeId.of(SystemSchemaIds.teamCardTypeId(orgId))));
        link.setTargetCardTypeIds(List.of(CardTypeId.of(SystemSchemaIds.memberAbstractCardTypeId(orgId))));
        link.setSourceMultiSelect(true);
        link.setTargetMultiSelect(true);
        link.setSourceVisible(true);
        link.setTargetVisible(true);
        link.setSystemLinkType(true);
        link.setSystemInput(false);
        createSchema(orgId, link);
        log.info("Builtin link type created: team-member {}", linkId);
    }

    private void createTeamLeadLink(String orgId) {
        String linkId = SystemSchemaIds.teamLeadLinkTypeId(orgId);
        if (schemaExists(linkId)) {
            return;
        }
        LinkTypeDefinition link = new LinkTypeDefinition(LinkTypeId.of(linkId), orgId, "团队负责人");
        link.setCode("team-lead");
        link.setSourceName("负责人");
        link.setTargetName("负责的团队");
        link.setSourceCardTypeIds(List.of(CardTypeId.of(SystemSchemaIds.teamCardTypeId(orgId))));
        link.setTargetCardTypeIds(List.of(CardTypeId.of(SystemSchemaIds.memberAbstractCardTypeId(orgId))));
        link.setSourceMultiSelect(false);
        link.setTargetMultiSelect(false);
        link.setSourceVisible(true);
        link.setTargetVisible(true);
        link.setSystemLinkType(true);
        link.setSystemInput(false);
        createSchema(orgId, link);
        log.info("Builtin link type created: team-lead {}", linkId);
    }

    private void createProjectLeadLink(String orgId) {
        String linkId = SystemSchemaIds.projectLeadLinkTypeId(orgId);
        if (schemaExists(linkId)) {
            return;
        }
        LinkTypeDefinition link = new LinkTypeDefinition(LinkTypeId.of(linkId), orgId, "项目负责人");
        link.setCode("project-lead");
        link.setSourceName("负责人");
        link.setTargetName("负责的项目");
        link.setSourceCardTypeIds(List.of(CardTypeId.of(SystemSchemaIds.projectCardTypeId(orgId))));
        link.setTargetCardTypeIds(List.of(CardTypeId.of(SystemSchemaIds.memberAbstractCardTypeId(orgId))));
        link.setSourceMultiSelect(false);
        link.setTargetMultiSelect(false);
        link.setSourceVisible(true);
        link.setTargetVisible(true);
        link.setSystemLinkType(true);
        link.setSystemInput(false);
        createSchema(orgId, link);
        log.info("Builtin link type created: project-lead {}", linkId);
    }

    private void createTeamProjectLink(String orgId) {
        String linkId = SystemSchemaIds.teamProjectLinkTypeId(orgId);
        if (schemaExists(linkId)) {
            return;
        }
        LinkTypeDefinition link = new LinkTypeDefinition(LinkTypeId.of(linkId), orgId, "团队项目");
        link.setCode("team-project");
        link.setSourceName("团队项目");
        link.setTargetName("所属团队");
        link.setSourceCardTypeIds(List.of(CardTypeId.of(SystemSchemaIds.teamCardTypeId(orgId))));
        link.setTargetCardTypeIds(List.of(CardTypeId.of(SystemSchemaIds.projectCardTypeId(orgId))));
        link.setSourceMultiSelect(true);
        link.setTargetMultiSelect(false);
        link.setSourceVisible(true);
        link.setTargetVisible(true);
        link.setSystemLinkType(true);
        link.setSystemInput(false);
        createSchema(orgId, link);
        log.info("Builtin link type created: team-project {}", linkId);
    }

    private void createProjectIssueLink(String orgId) {
        String linkId = SystemSchemaIds.projectIssueLinkTypeId(orgId);
        if (schemaExists(linkId)) {
            return;
        }
        LinkTypeDefinition link = new LinkTypeDefinition(LinkTypeId.of(linkId), orgId, "项目工作项");
        link.setCode("project-issue");
        link.setSourceName("工作项");
        link.setTargetName("所属项目");
        link.setSourceCardTypeIds(List.of(CardTypeId.of(SystemSchemaIds.projectCardTypeId(orgId))));
        link.setTargetCardTypeIds(List.of(CardTypeId.of(SystemSchemaIds.issueCardTypeId(orgId))));
        link.setSourceMultiSelect(true);
        link.setTargetMultiSelect(false);
        link.setSourceVisible(true);
        link.setTargetVisible(true);
        link.setSystemLinkType(true);
        link.setSystemInput(false);
        createSchema(orgId, link);
        log.info("Builtin link type created: project-issue {}", linkId);
    }

    private void ensureTextField(String orgId, String cardTypeId, String fieldId, String name, String code,
                                 String description, boolean required) {
        if (schemaExists(fieldId)) {
            return;
        }
        SingleLineTextFieldConfig field = new SingleLineTextFieldConfig(
                FieldConfigId.of(fieldId),
                orgId,
                name,
                CardTypeId.of(cardTypeId),
                FieldId.of(fieldId),
                true
        );
        field.setCode(code);
        field.setDescription(description);
        field.setValueSource(ValueSource.MANUAL);
        field.setRequired(required);
        field.setReadOnly(false);
        createSchema(orgId, field);
    }

    private void ensureEnumField(String orgId, String cardTypeId, String fieldId, String name, String code,
                                 String description, List<EnumFieldConfig.EnumOptionDefinition> options) {
        if (schemaExists(fieldId)) {
            return;
        }
        EnumFieldConfig field = new EnumFieldConfig(
                FieldConfigId.of(fieldId),
                orgId,
                name,
                CardTypeId.of(cardTypeId),
                FieldId.of(fieldId),
                true
        );
        field.setCode(code);
        field.setDescription(description);
        field.setValueSource(ValueSource.MANUAL);
        field.setRequired(false);
        field.setReadOnly(false);
        field.setOptions(options);
        field.setMultiSelect(false);
        createSchema(orgId, field);
    }

    private static List<EnumFieldConfig.EnumOptionDefinition> projectStatusOptions(String orgId) {
        String p = orgId + ":project:status:";
        return List.of(
                new EnumFieldConfig.EnumOptionDefinition(p + "planned", "Planned", "PLANNED", true, null, 1),
                new EnumFieldConfig.EnumOptionDefinition(p + "in_progress", "In Progress", "IN_PROGRESS", true, null, 2),
                new EnumFieldConfig.EnumOptionDefinition(p + "completed", "Completed", "COMPLETED", true, null, 3),
                new EnumFieldConfig.EnumOptionDefinition(p + "cancelled", "Cancelled", "CANCELLED", true, null, 4)
        );
    }

    private static List<EnumFieldConfig.EnumOptionDefinition> issuePriorityOptions(String orgId) {
        String p = orgId + ":issue:priority:";
        return List.of(
                new EnumFieldConfig.EnumOptionDefinition(p + "urgent", "Urgent", "URGENT", true, null, 1),
                new EnumFieldConfig.EnumOptionDefinition(p + "high", "High", "HIGH", true, null, 2),
                new EnumFieldConfig.EnumOptionDefinition(p + "medium", "Medium", "MEDIUM", true, null, 3),
                new EnumFieldConfig.EnumOptionDefinition(p + "low", "Low", "LOW", true, null, 4),
                new EnumFieldConfig.EnumOptionDefinition(p + "none", "No Priority", "NO_PRIORITY", true, null, 5)
        );
    }

    private static List<EnumFieldConfig.EnumOptionDefinition> issueStatusOptions(String orgId) {
        String p = orgId + ":issue:status:";
        return List.of(
                new EnumFieldConfig.EnumOptionDefinition(p + "backlog", "Backlog", "BACKLOG", true, null, 1),
                new EnumFieldConfig.EnumOptionDefinition(p + "todo", "Todo", "TODO", true, null, 2),
                new EnumFieldConfig.EnumOptionDefinition(p + "in_progress", "In Progress", "IN_PROGRESS", true, null, 3),
                new EnumFieldConfig.EnumOptionDefinition(p + "done", "Done", "DONE", true, null, 4),
                new EnumFieldConfig.EnumOptionDefinition(p + "cancelled", "Cancelled", "CANCELLED", true, null, 5)
        );
    }

    private void createSchema(String orgId, AbstractSchemaDefinition<?> definition) {
        CreateSchemaRequest request = new CreateSchemaRequest();
        request.setDefinition(definition);
        Result<SchemaDefinition<?>> result = schemaServiceClient.create(orgId, SYSTEM_OPERATOR, request);
        if (!result.isSuccess()) {
            throw new RuntimeException("创建内置 Schema 失败: " + definition.getId() + " — " + result.getMessage());
        }
    }
}
