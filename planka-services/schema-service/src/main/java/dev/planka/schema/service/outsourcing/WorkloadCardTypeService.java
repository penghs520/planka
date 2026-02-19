package dev.planka.schema.service.outsourcing;

import dev.planka.api.schema.SchemaServiceClient;
import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.common.result.Result;
import dev.planka.common.util.SystemSchemaIds;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.field.FieldConfigId;
import dev.planka.domain.field.FieldId;
import dev.planka.domain.link.LinkPosition;
import dev.planka.domain.link.LinkTypeId;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.fieldconfig.*;
import dev.planka.domain.schema.definition.link.LinkTypeDefinition;
import dev.planka.domain.schema.definition.linkconfig.LinkFieldConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工时卡片类型服务
 * <p>
 * 负责创建工时相关的卡片类型：
 * - 工时卡片类型
 * - 结算记录卡片类型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkloadCardTypeService {

    private final SchemaServiceClient schemaServiceClient;

    private static final String SYSTEM_OPERATOR = "system";

    /**
     * 初始化组织的工时卡片类型
     * <p>
     * 采用容错策略：
     * - 检查每个卡片类型是否已存在
     * - 只创建不存在的卡片类型
     * - 某个卡片类型创建失败不影响其他卡片类型
     *
     * @param orgId            组织ID
     * @param memberCardTypeId 成员卡片类型ID
     */
    public void initWorkloadCardTypes(String orgId, String memberCardTypeId) {
        log.info("开始初始化组织 {} 的工时卡片类型", orgId);

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        // 1. 创建工时卡片类型
        try {
            if (createWorklogCardType(orgId, memberCardTypeId)) {
                successCount++;
            } else {
                skipCount++;
            }
        } catch (Exception e) {
            log.error("创建工时卡片类型失败: orgId={}", orgId, e);
            failCount++;
        }

        // 2. 创建结算记录卡片类型
        try {
            if (createSettlementRecordCardType(orgId, memberCardTypeId)) {
                successCount++;
            } else {
                skipCount++;
            }
        } catch (Exception e) {
            log.error("创建结算记录卡片类型失败: orgId={}", orgId, e);
            failCount++;
        }

        log.info("组织 {} 的工时卡片类型初始化完成: 成功={}, 跳过={}, 失败={}",
                orgId, successCount, skipCount, failCount);

        // 如果全部失败，抛出异常
        if (failCount > 0 && successCount == 0 && skipCount == 0) {
            throw new RuntimeException("所有工时卡片类型创建失败");
        }
    }

    /**
     * 创建工时卡片类型
     *
     * @return true=创建成功, false=已存在跳过
     */
    private boolean createWorklogCardType(String orgId, String memberCardTypeId) {
        String cardTypeId = SystemSchemaIds.worklogCardTypeId(orgId);
        log.info("创建工时卡片类型: {}", cardTypeId);

        // 检查是否已存在
        if (isCardTypeExists(cardTypeId)) {
            log.info("工时卡片类型已存在，跳过创建: {}", cardTypeId);
            return false;
        }

        // 创建卡片类型
        EntityCardType cardType = new EntityCardType(
                CardTypeId.of(cardTypeId),
                orgId,
                "工时记录"
        );
        cardType.setCode("worklog");
        cardType.setDescription("记录工作时长和工作内容");
        cardType.setSystemType(true);

        createCardType(orgId, cardType);

        // 创建字段
        createDateField(orgId, cardTypeId, "工时日期", "date", "工时发生的日期", true, DateFieldConfig.DateFormat.DATE);
        createNumberField(orgId, cardTypeId, "工时数", "hours", "工作时长（小时）", true, 2);
        createMultiLineTextField(orgId, cardTypeId, "工作内容", "description", "工作内容描述", false);
        createEnumField(orgId, cardTypeId, "工时类型", "type", "工时类型", true, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("normal", "正常工时", "normal", true, "#52c41a", 1),
                        new EnumFieldConfig.EnumOptionDefinition("overtime", "加班工时", "overtime", true, "#722ed1", 2),
                        new EnumFieldConfig.EnumOptionDefinition("leave", "请假扣减", "leave", true, "#f5222d", 3)
                ));
        createEnumField(orgId, cardTypeId, "审核状态", "status", "审核状态", true, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("draft", "草稿", "draft", true, "#d9d9d9", 1),
                        new EnumFieldConfig.EnumOptionDefinition("submitted", "已提交", "submitted", true, "#1890ff", 2),
                        new EnumFieldConfig.EnumOptionDefinition("approved", "已审核", "approved", true, "#52c41a", 3),
                        new EnumFieldConfig.EnumOptionDefinition("rejected", "已驳回", "rejected", true, "#f5222d", 4)
                ));
        createEnumField(orgId, cardTypeId, "锁定状态", "locked", "锁定状态", true, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("unlocked", "未锁定", "unlocked", true, "#52c41a", 1),
                        new EnumFieldConfig.EnumOptionDefinition("locked", "已锁定", "locked", true, "#f5222d", 2)
                ));

        // 创建关联：工时记录 -> 成员
        createLinkToMember(orgId, cardTypeId, memberCardTypeId, "工时成员", "member", "关联的成员", true);

        log.info("工时卡片类型创建完成: {}", cardTypeId);
        return true;
    }

    /**
     * 创建结算记录卡片类型
     *
     * @return true=创建成功, false=已存在跳过
     */
    private boolean createSettlementRecordCardType(String orgId, String memberCardTypeId) {
        String cardTypeId = SystemSchemaIds.settlementRecordCardTypeId(orgId);
        log.info("创建结算记录卡片类型: {}", cardTypeId);

        // 检查是否已存在
        if (isCardTypeExists(cardTypeId)) {
            log.info("结算记录卡片类型已存在，跳过创建: {}", cardTypeId);
            return false;
        }

        // 创建卡片类型
        EntityCardType cardType = new EntityCardType(
                CardTypeId.of(cardTypeId),
                orgId,
                "结算记录"
        );
        cardType.setCode("settlement-record");
        cardType.setDescription("记录月度结算信息");
        cardType.setSystemType(true);

        createCardType(orgId, cardType);

        // 创建字段
        createTextField(orgId, cardTypeId, "结算月份", "month", "格式：YYYY-MM", true);
        createNumberField(orgId, cardTypeId, "总工作天数", "total-work-days", "本月总工作天数", true, 0);
        createNumberField(orgId, cardTypeId, "总工作时长", "total-work-hours", "本月总工作时长（小时）", true, 2);
        createNumberField(orgId, cardTypeId, "总加班时长", "total-overtime-hours", "本月总加班时长（小时）", true, 2);
        createNumberField(orgId, cardTypeId, "总请假天数", "total-leave-days", "本月总请假天数", true, 0);
        createNumberField(orgId, cardTypeId, "总旷工天数", "total-absent-days", "本月总旷工天数", true, 0);
        createNumberField(orgId, cardTypeId, "服务费", "service-fee", "应付服务费（元）", true, 2);
        createEnumField(orgId, cardTypeId, "结算状态", "status", "结算状态", true, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("pending", "待结算", "pending", true, "#faad14", 1),
                        new EnumFieldConfig.EnumOptionDefinition("completed", "已完成", "completed", true, "#52c41a", 2),
                        new EnumFieldConfig.EnumOptionDefinition("revoked", "已撤销", "revoked", true, "#f5222d", 3)
                ));
        createDateField(orgId, cardTypeId, "结算时间", "settlement-time", "结算完成时间", false, DateFieldConfig.DateFormat.DATETIME);

        // 创建关联：结算记录 -> 成员
        createLinkToMember(orgId, cardTypeId, memberCardTypeId, "成员", "member", "关联的成员", true);
        // 创建关联：结算记录 -> 操作人
        createLinkToMember(orgId, cardTypeId, memberCardTypeId, "操作人", "operator", "执行结算的操作人", true);

        log.info("结算记录卡片类型创建完成: {}", cardTypeId);
        return true;
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查卡片类型是否已存在
     *
     * @param cardTypeId 卡片类型ID
     * @return true=已存在, false=不存在
     */
    private boolean isCardTypeExists(String cardTypeId) {
        try {
            Result<SchemaDefinition<?>> result = schemaServiceClient.getById(cardTypeId);
            return result.isSuccess() && result.getData() != null;
        } catch (Exception e) {
            log.warn("检查卡片类型是否存在时发生异常: cardTypeId={}", cardTypeId, e);
            return false;
        }
    }

    /**
     * 创建卡片类型
     */
    private void createCardType(String orgId, EntityCardType cardType) {
        CreateSchemaRequest request = new CreateSchemaRequest();
        request.setDefinition(cardType);

        Result<SchemaDefinition<?>> result = schemaServiceClient.create(orgId, SYSTEM_OPERATOR, request);
        if (!result.isSuccess()) {
            log.error("创建卡片类型失败: {}, error: {}", cardType.getName(), result.getMessage());
            throw new RuntimeException("创建卡片类型失败: " + result.getMessage());
        }

        log.info("卡片类型创建成功: {} ({})", cardType.getName(), cardType.getId().value());
    }

    /**
     * 创建文本字段
     */
    private void createTextField(String orgId, String cardTypeId, String name, String code, String description, boolean required) {
        String fieldId = cardTypeId + ":" + code;

        SingleLineTextFieldConfig fieldConfig = new SingleLineTextFieldConfig(
                FieldConfigId.of(fieldId),
                orgId,
                name,
                CardTypeId.of(cardTypeId),
                FieldId.of(fieldId),
                true
        );
        fieldConfig.setCode(code);
        fieldConfig.setDescription(description);
        fieldConfig.setRequired(required);
        fieldConfig.setValueSource(ValueSource.MANUAL);

        createFieldConfig(fieldConfig);
    }

    /**
     * 创建多行文本字段
     */
    private void createMultiLineTextField(String orgId, String cardTypeId, String name, String code, String description, boolean required) {
        String fieldId = cardTypeId + ":" + code;

        MultiLineTextFieldConfig fieldConfig = new MultiLineTextFieldConfig(
                FieldConfigId.of(fieldId),
                orgId,
                name,
                CardTypeId.of(cardTypeId),
                FieldId.of(fieldId),
                true
        );
        fieldConfig.setCode(code);
        fieldConfig.setDescription(description);
        fieldConfig.setRequired(required);
        fieldConfig.setValueSource(ValueSource.MANUAL);

        createFieldConfig(fieldConfig);
    }

    /**
     * 创建数字字段（带精度）
     */
    private void createNumberField(String orgId, String cardTypeId, String name, String code, String description, boolean required, Integer precision) {
        String fieldId = cardTypeId + ":" + code;

        NumberFieldConfig fieldConfig = new NumberFieldConfig(
                FieldConfigId.of(fieldId),
                orgId,
                name,
                CardTypeId.of(cardTypeId),
                FieldId.of(fieldId),
                true
        );
        fieldConfig.setCode(code);
        fieldConfig.setDescription(description);
        fieldConfig.setRequired(required);
        fieldConfig.setValueSource(ValueSource.MANUAL);
        fieldConfig.setPrecision(precision);

        createFieldConfig(fieldConfig);
    }

    /**
     * 创建日期字段
     */
    private void createDateField(String orgId, String cardTypeId, String name, String code, String description, boolean required, DateFieldConfig.DateFormat dateFormat) {
        String fieldId = cardTypeId + ":" + code;

        DateFieldConfig fieldConfig = new DateFieldConfig(
                FieldConfigId.of(fieldId),
                orgId,
                name,
                CardTypeId.of(cardTypeId),
                FieldId.of(fieldId),
                true
        );
        fieldConfig.setCode(code);
        fieldConfig.setDescription(description);
        fieldConfig.setRequired(required);
        fieldConfig.setValueSource(ValueSource.MANUAL);
        fieldConfig.setDateFormat(dateFormat);

        createFieldConfig(fieldConfig);
    }

    /**
     * 创建枚举字段
     */
    private void createEnumField(String orgId, String cardTypeId, String name, String code, String description, boolean required, boolean multiSelect, List<EnumFieldConfig.EnumOptionDefinition> options) {
        String fieldId = cardTypeId + ":" + code;

        EnumFieldConfig fieldConfig = new EnumFieldConfig(
                FieldConfigId.of(fieldId),
                orgId,
                name,
                CardTypeId.of(cardTypeId),
                FieldId.of(fieldId),
                true
        );
        fieldConfig.setCode(code);
        fieldConfig.setDescription(description);
        fieldConfig.setRequired(required);
        fieldConfig.setValueSource(ValueSource.MANUAL);
        fieldConfig.setMultiSelect(multiSelect);
        fieldConfig.setOptions(options);

        createFieldConfig(fieldConfig);
    }

    /**
     * 创建关联到成员的关联字段
     */
    private void createLinkToMember(String orgId, String sourceCardTypeId, String targetCardTypeId, String name, String code, String description, boolean required) {
        // 1. 创建关联类型
        String linkTypeId = sourceCardTypeId + ":link:" + code;

        LinkTypeDefinition linkType = new LinkTypeDefinition(
                LinkTypeId.of(linkTypeId),
                orgId,
                name
        );
        linkType.setCode(code);
        linkType.setDescription(description);
        linkType.setSystemLinkType(true);
        linkType.setSourceCardTypeIds(List.of(CardTypeId.of(sourceCardTypeId)));
        linkType.setTargetCardTypeIds(List.of(CardTypeId.of(targetCardTypeId)));
        linkType.setSourceName(name);
        linkType.setTargetName("关联的" + name);
        linkType.setSourceMultiSelect(false);
        linkType.setTargetMultiSelect(true);

        CreateSchemaRequest linkTypeRequest = new CreateSchemaRequest();
        linkTypeRequest.setDefinition(linkType);

        Result<SchemaDefinition<?>> linkTypeResult = schemaServiceClient.create(orgId, SYSTEM_OPERATOR, linkTypeRequest);
        if (!linkTypeResult.isSuccess()) {
            log.error("创建关联类型失败: {}, error: {}", name, linkTypeResult.getMessage());
            throw new RuntimeException("创建关联类型失败: " + linkTypeResult.getMessage());
        }

        log.info("关联类型创建成功: {} ({})", name, linkTypeId);

        // 2. 创建源端关联字段配置
        String linkFieldId = linkTypeId + ":" + LinkPosition.SOURCE.name();

        LinkFieldConfig linkFieldConfig = new LinkFieldConfig(
                FieldConfigId.of(linkFieldId),
                orgId,
                name,
                CardTypeId.of(sourceCardTypeId),
                FieldId.of(linkFieldId),
                true
        );
        linkFieldConfig.setCode(code);
        linkFieldConfig.setDescription(description);
        linkFieldConfig.setRequired(required);
        linkFieldConfig.setValueSource(ValueSource.MANUAL);
        linkFieldConfig.setDisplayName(name);
        linkFieldConfig.setMultiple(false);

        createFieldConfig(linkFieldConfig);
    }

    /**
     * 创建字段配置
     */
    private void createFieldConfig(FieldConfig fieldConfig) {
        CreateSchemaRequest request = new CreateSchemaRequest();
        request.setDefinition(fieldConfig);

        Result<SchemaDefinition<?>> result = schemaServiceClient.create(fieldConfig.getOrgId(), SYSTEM_OPERATOR, request);
        if (!result.isSuccess()) {
            log.error("创建字段配置失败: {}, error: {}", fieldConfig.getName(), result.getMessage());
            throw new RuntimeException("创建字段配置失败: " + result.getMessage());
        }

        log.info("字段配置创建成功: {} ({})", fieldConfig.getName(), fieldConfig.getId().value());
    }
}
