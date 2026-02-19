package dev.planka.user.service;

import dev.planka.api.schema.SchemaServiceClient;
import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.common.result.Result;
import dev.planka.common.util.SystemSchemaIds;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.link.LinkTypeId;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.AbstractCardType;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任意卡属性集服务
 * <p>
 * 负责创建任意卡属性集及其内置关联类型。
 * 任意卡属性集与成员卡片类型之间创建三个关联关系：创建人、归档人、丢弃人。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RootCardTypeService {

    private final SchemaServiceClient schemaServiceClient;

    /** 任意卡属性集名称 */
    private static final String ROOT_CARD_TYPE_NAME = "任意卡属性集";
    /** 任意卡属性集编码 */
    private static final String ROOT_CARD_TYPE_CODE = "any-trait";

    /** 创建人关联名称 */
    private static final String CREATOR_LINK_NAME = "创建人";
    private static final String CREATOR_LINK_TARGET_NAME = "创建的卡";
    private static final String CREATOR_LINK_CODE = "creator";

    /** 归档人关联名称 */
    private static final String ARCHIVER_LINK_NAME = "归档人";
    private static final String ARCHIVER_LINK_TARGET_NAME = "归档的卡";
    private static final String ARCHIVER_LINK_CODE = "archiver";

    /** 丢弃人关联名称 */
    private static final String DISCARDER_LINK_NAME = "丢弃人";
    private static final String DISCARDER_LINK_TARGET_NAME = "丢弃的卡";
    private static final String DISCARDER_LINK_CODE = "discarder";

    /** 系统操作人 */
    private static final String SYSTEM_OPERATOR = "system";

    /**
     * 创建任意卡属性集及其关联
     * <p>
     * 包括：
     * 1. 创建任意卡属性集
     * 2. 创建三个关联类型（创建人、归档人、丢弃人）
     * 3. 为每个关联类型创建对应的 LinkFieldConfig
     *
     * @param orgId 组织ID
     * @return 任意卡属性集ID
     */
    public String createRootCardType(String orgId) {
        log.info("Creating root card type for org: {}", orgId);

        // 计算固定ID
        String rootCardTypeId = SystemSchemaIds.anyTraitTypeId(orgId);
        String memberAbstractCardTypeId = SystemSchemaIds.memberAbstractCardTypeId(orgId);

        // 1. 创建任意卡属性集
        createTraitCardType(orgId, rootCardTypeId);

        // 2. 创建三个关联类型及其配置（目标端指向成员属性集，这样任何继承成员属性集的实体类型都可以作为关联目标）
        createLinkTypeWithConfig(orgId, rootCardTypeId, memberAbstractCardTypeId,
                SystemSchemaIds.creatorLinkTypeId(orgId), CREATOR_LINK_NAME, CREATOR_LINK_TARGET_NAME, CREATOR_LINK_CODE);
        createLinkTypeWithConfig(orgId, rootCardTypeId, memberAbstractCardTypeId,
                SystemSchemaIds.archiverLinkTypeId(orgId), ARCHIVER_LINK_NAME, ARCHIVER_LINK_TARGET_NAME, ARCHIVER_LINK_CODE);
        createLinkTypeWithConfig(orgId, rootCardTypeId, memberAbstractCardTypeId,
                SystemSchemaIds.discarderLinkTypeId(orgId), DISCARDER_LINK_NAME, DISCARDER_LINK_TARGET_NAME, DISCARDER_LINK_CODE);

        log.info("Root card type and link types created successfully for org: {}", orgId);
        return rootCardTypeId;
    }

    /**
     * 创建任意卡属性集
     */
    private void createTraitCardType(String orgId, String rootCardTypeId) {
        AbstractCardType cardType = new AbstractCardType(
                CardTypeId.of(rootCardTypeId),
                orgId,
                ROOT_CARD_TYPE_NAME
        );
        cardType.setCode(ROOT_CARD_TYPE_CODE);
        cardType.setDescription("系统任意卡属性集，所有卡片类型隐式继承此类型");
        cardType.setSystemType(true);

        CreateSchemaRequest request = new CreateSchemaRequest();
        request.setDefinition(cardType);

        Result<SchemaDefinition<?>> result = schemaServiceClient.create(orgId, SYSTEM_OPERATOR, request);
        if (!result.isSuccess()) {
            log.error("Failed to create root card type: {}", result.getMessage());
            throw new RuntimeException("创建任意卡属性集失败: " + result.getMessage());
        }

        log.info("Root card type created: {}", rootCardTypeId);
    }

    /**
     * 创建关联类型及其配置
     *
     * @param orgId                     组织ID
     * @param rootCardTypeId            任意卡属性集ID（源端）
     * @param memberAbstractCardTypeId  成员属性集ID（目标端）
     * @param linkTypeId                关联类型ID
     * @param linkName                  关联名称（源端名称）
     * @param targetName                目标端名称
     * @param linkCode                  关联编码
     */
    private void createLinkTypeWithConfig(String orgId, String rootCardTypeId,
                                          String memberAbstractCardTypeId, String linkTypeId,
                                          String linkName, String targetName, String linkCode) {
        // 1. 创建关联类型定义
        LinkTypeDefinition linkType = new LinkTypeDefinition(
                LinkTypeId.of(linkTypeId),
                orgId,
                linkName
        );
        linkType.setCode(linkCode);
        linkType.setSourceName(linkName);
        linkType.setTargetName(targetName);
        linkType.setSourceCardTypeIds(List.of(CardTypeId.of(rootCardTypeId)));
        linkType.setTargetCardTypeIds(List.of(CardTypeId.of(memberAbstractCardTypeId)));
        linkType.setSourceMultiSelect(false);  // 单选（一个创建人/归档人/丢弃人）
        linkType.setTargetMultiSelect(true);   // 目标端多选
        linkType.setSourceVisible(true);
        linkType.setTargetVisible(false);      // 目标端不显示
        linkType.setSystemLinkType(true);
        linkType.setSystemInput(true);

        CreateSchemaRequest linkTypeRequest = new CreateSchemaRequest();
        linkTypeRequest.setDefinition(linkType);

        Result<SchemaDefinition<?>> linkTypeResult = schemaServiceClient.create(orgId, SYSTEM_OPERATOR, linkTypeRequest);
        if (!linkTypeResult.isSuccess()) {
            log.error("Failed to create link type {}: {}", linkName, linkTypeResult.getMessage());
            throw new RuntimeException("创建关联类型失败: " + linkName + " - " + linkTypeResult.getMessage());
        }

        log.info("Link type created: {} ({})", linkName, linkTypeId);
   }

}
