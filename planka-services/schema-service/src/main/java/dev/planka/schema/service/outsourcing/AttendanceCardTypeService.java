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
 * 考勤卡片类型服务
 * <p>
 * 负责创建考勤相关的卡片类型：
 * - 考勤记录卡片类型
 * - 请假申请卡片类型
 * - 加班申请卡片类型
 * - 补卡申请卡片类型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceCardTypeService {

    private final SchemaServiceClient schemaServiceClient;

    private static final String SYSTEM_OPERATOR = "system";

    /**
     * 初始化组织的考勤卡片类型
     * <p>
     * 采用容错策略：
     * - 检查每个卡片类型是否已存在
     * - 只创建不存在的卡片类型
     * - 某个卡片类型创建失败不影响其他卡片类型
     *
     * @param orgId            组织ID
     * @param memberCardTypeId 成员卡片类型ID
     */
    public void initAttendanceCardTypes(String orgId, String memberCardTypeId) {
        log.info("开始初始化组织 {} 的考勤卡片类型", orgId);

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        // 1. 创建考勤记录卡片类型
        try {
            if (createAttendanceRecordCardType(orgId, memberCardTypeId)) {
                successCount++;
            } else {
                skipCount++;
            }
        } catch (Exception e) {
            log.error("创建考勤记录卡片类型失败: orgId={}", orgId, e);
            failCount++;
        }

        // 2. 创建请假申请卡片类型
        try {
            if (createLeaveApplicationCardType(orgId, memberCardTypeId)) {
                successCount++;
            } else {
                skipCount++;
            }
        } catch (Exception e) {
            log.error("创建请假申请卡片类型失败: orgId={}", orgId, e);
            failCount++;
        }

        // 3. 创建加班申请卡片类型
        try {
            if (createOvertimeApplicationCardType(orgId, memberCardTypeId)) {
                successCount++;
            } else {
                skipCount++;
            }
        } catch (Exception e) {
            log.error("创建加班申请卡片类型失败: orgId={}", orgId, e);
            failCount++;
        }

        // 4. 创建补卡申请卡片类型
        try {
            if (createMakeupApplicationCardType(orgId, memberCardTypeId)) {
                successCount++;
            } else {
                skipCount++;
            }
        } catch (Exception e) {
            log.error("创建补卡申请卡片类型失败: orgId={}", orgId, e);
            failCount++;
        }

        log.info("组织 {} 的考勤卡片类型初始化完成: 成功={}, 跳过={}, 失败={}",
                orgId, successCount, skipCount, failCount);

        // 如果全部失败，抛出异常
        if (failCount > 0 && successCount == 0 && skipCount == 0) {
            throw new RuntimeException("所有考勤卡片类型创建失败");
        }
    }

    /**
     * 创建考勤记录卡片类型
     *
     * @return true=创建成功, false=已存在跳过
     */
    private boolean createAttendanceRecordCardType(String orgId, String memberCardTypeId) {
        String cardTypeId = SystemSchemaIds.attendanceRecordCardTypeId(orgId);
        log.info("创建考勤记录卡片类型: {}", cardTypeId);

        // 检查是否已存在
        if (isCardTypeExists(cardTypeId)) {
            log.info("考勤记录卡片类型已存在，跳过创建: {}", cardTypeId);
            return false;
        }

        // 创建卡片类型
        EntityCardType cardType = new EntityCardType(
                CardTypeId.of(cardTypeId),
                orgId,
                "考勤记录"
        );
        cardType.setCode("attendance-record");
        cardType.setDescription("记录每日的签到签出信息");
        cardType.setSystemType(true);

        createCardType(orgId, cardType);

        // 创建字段
        createDateField(orgId, cardTypeId, "日期", "date", "考勤日期", true, DateFieldConfig.DateFormat.DATE);
        createTextField(orgId, cardTypeId, "签到时间", "sign-in-time", "签到时间", false);
        createTextField(orgId, cardTypeId, "签出时间", "sign-out-time", "签出时间", false);
        createNumberField(orgId, cardTypeId, "工作时长", "work-duration", "实际工作时长（分钟）", false);
        createEnumField(orgId, cardTypeId, "状态", "status", "考勤状态", false, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("normal", "正常", "normal", true, "#52c41a", 1),
                        new EnumFieldConfig.EnumOptionDefinition("late", "迟到", "late", true, "#faad14", 2),
                        new EnumFieldConfig.EnumOptionDefinition("early", "早退", "early", true, "#fa8c16", 3),
                        new EnumFieldConfig.EnumOptionDefinition("absent", "旷工", "absent", true, "#f5222d", 4),
                        new EnumFieldConfig.EnumOptionDefinition("leave", "请假", "leave", true, "#1890ff", 5),
                        new EnumFieldConfig.EnumOptionDefinition("overtime", "加班", "overtime", true, "#722ed1", 6)
                ));
        createMultiLineTextField(orgId, cardTypeId, "备注", "remark", "备注信息", false);

        // 创建关联：考勤记录 -> 成员
        createLinkToMember(orgId, cardTypeId, memberCardTypeId, "成员", "member", "关联的成员", true);

        log.info("考勤记录卡片类型创建完成: {}", cardTypeId);
        return true;
    }

    /**
     * 创建请假申请卡片类型
     *
     * @return true=创建成功, false=已存在跳过
     */
    private boolean createLeaveApplicationCardType(String orgId, String memberCardTypeId) {
        String cardTypeId = SystemSchemaIds.leaveApplicationCardTypeId(orgId);
        log.info("创建请假申请卡片类型: {}", cardTypeId);

        // 检查是否已存在
        if (isCardTypeExists(cardTypeId)) {
            log.info("请假申请卡片类型已存在，跳过创建: {}", cardTypeId);
            return false;
        }

        // 创建卡片类型
        EntityCardType cardType = new EntityCardType(
                CardTypeId.of(cardTypeId),
                orgId,
                "请假申请"
        );
        cardType.setCode("leave-application");
        cardType.setDescription("记录请假申请");
        cardType.setSystemType(true);

        createCardType(orgId, cardType);

        // 创建字段
        createEnumField(orgId, cardTypeId, "请假类型", "leave-type", "请假类型", true, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("sick", "病假", "sick", true, "#ff7875", 1),
                        new EnumFieldConfig.EnumOptionDefinition("time-off", "调休", "time-off", true, "#ffc069", 2),
                        new EnumFieldConfig.EnumOptionDefinition("annual", "年假", "annual", true, "#95de64", 3),
                        new EnumFieldConfig.EnumOptionDefinition("personal", "事假", "personal", true, "#69c0ff", 4)
                ));
        createDateField(orgId, cardTypeId, "开始日期", "start-date", "请假开始日期", true, DateFieldConfig.DateFormat.DATE);
        createDateField(orgId, cardTypeId, "结束日期", "end-date", "请假结束日期", true, DateFieldConfig.DateFormat.DATE);
        createNumberField(orgId, cardTypeId, "请假时长", "duration", "请假天数", false);
        createEnumField(orgId, cardTypeId, "审批状态", "approval-status", "审批状态", false, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("pending", "待审批", "pending", true, "#faad14", 1),
                        new EnumFieldConfig.EnumOptionDefinition("approved", "已批准", "approved", true, "#52c41a", 2),
                        new EnumFieldConfig.EnumOptionDefinition("rejected", "已拒绝", "rejected", true, "#f5222d", 3)
                ));
        createDateField(orgId, cardTypeId, "审批时间", "approval-time", "审批时间", false, DateFieldConfig.DateFormat.DATETIME);
        createMultiLineTextField(orgId, cardTypeId, "请假原因", "reason", "请假原因", true);
        createMultiLineTextField(orgId, cardTypeId, "审批意见", "approval-comment", "审批意见", false);

        // 创建关联：请假申请 -> 申请人
        createLinkToMember(orgId, cardTypeId, memberCardTypeId, "申请人", "applicant", "请假申请人", true);
        // 创建关联：请假申请 -> 审批人
        createLinkToMember(orgId, cardTypeId, memberCardTypeId, "审批人", "approver", "审批人", false);

        log.info("请假申请卡片类型创建完成: {}", cardTypeId);
        return true;
    }

    /**
     * 创建加班申请卡片类型
     *
     * @return true=创建成功, false=已存在跳过
     */
    private boolean createOvertimeApplicationCardType(String orgId, String memberCardTypeId) {
        String cardTypeId = SystemSchemaIds.overtimeApplicationCardTypeId(orgId);
        log.info("创建加班申请卡片类型: {}", cardTypeId);

        // 检查是否已存在
        if (isCardTypeExists(cardTypeId)) {
            log.info("加班申请卡片类型已存在，跳过创建: {}", cardTypeId);
            return false;
        }

        // 创建卡片类型
        EntityCardType cardType = new EntityCardType(
                CardTypeId.of(cardTypeId),
                orgId,
                "加班申请"
        );
        cardType.setCode("overtime-application");
        cardType.setDescription("记录加班申请");
        cardType.setSystemType(true);

        createCardType(orgId, cardType);

        // 创建字段
        createEnumField(orgId, cardTypeId, "加班类型", "overtime-type", "加班类型", true, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("workday", "工作日", "workday", true, "#1890ff", 1),
                        new EnumFieldConfig.EnumOptionDefinition("weekend", "周末", "weekend", true, "#52c41a", 2),
                        new EnumFieldConfig.EnumOptionDefinition("holiday", "节假日", "holiday", true, "#f5222d", 3)
                ));
        createDateField(orgId, cardTypeId, "加班日期", "overtime-date", "加班日期", true, DateFieldConfig.DateFormat.DATE);
        createTextField(orgId, cardTypeId, "开始时间", "start-time", "加班开始时间", true);
        createTextField(orgId, cardTypeId, "结束时间", "end-time", "加班结束时间", true);
        createNumberField(orgId, cardTypeId, "加班时长", "duration", "加班时长（分钟）", false);
        createEnumField(orgId, cardTypeId, "审批状态", "approval-status", "审批状态", false, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("pending", "待审批", "pending", true, "#faad14", 1),
                        new EnumFieldConfig.EnumOptionDefinition("approved", "已批准", "approved", true, "#52c41a", 2),
                        new EnumFieldConfig.EnumOptionDefinition("rejected", "已拒绝", "rejected", true, "#f5222d", 3)
                ));
        createDateField(orgId, cardTypeId, "审批时间", "approval-time", "审批时间", false, DateFieldConfig.DateFormat.DATETIME);
        createMultiLineTextField(orgId, cardTypeId, "加班原因", "reason", "加班原因", true);
        createMultiLineTextField(orgId, cardTypeId, "审批意见", "approval-comment", "审批意见", false);

        // 创建关联：加班申请 -> 申请人
        createLinkToMember(orgId, cardTypeId, memberCardTypeId, "申请人", "applicant", "加班申请人", true);
        // 创建关联：加班申请 -> 审批人
        createLinkToMember(orgId, cardTypeId, memberCardTypeId, "审批人", "approver", "审批人", false);

        log.info("加班申请卡片类型创建完成: {}", cardTypeId);
        return true;
    }

    /**
     * 创建补卡申请卡片类型
     *
     * @return true=创建成功, false=已存在跳过
     */
    private boolean createMakeupApplicationCardType(String orgId, String memberCardTypeId) {
        String cardTypeId = SystemSchemaIds.makeupApplicationCardTypeId(orgId);
        log.info("创建补卡申请卡片类型: {}", cardTypeId);

        // 检查是否已存在
        if (isCardTypeExists(cardTypeId)) {
            log.info("补卡申请卡片类型已存在，跳过创建: {}", cardTypeId);
            return false;
        }

        // 创建卡片类型
        EntityCardType cardType = new EntityCardType(
                CardTypeId.of(cardTypeId),
                orgId,
                "补卡申请"
        );
        cardType.setCode("makeup-application");
        cardType.setDescription("记录补卡申请");
        cardType.setSystemType(true);

        createCardType(orgId, cardType);

        // 创建字段
        createDateField(orgId, cardTypeId, "补卡日期", "makeup-date", "补卡日期", true, DateFieldConfig.DateFormat.DATE);
        createEnumField(orgId, cardTypeId, "补卡类型", "makeup-type", "补卡类型", true, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("sign-in", "补签到", "sign-in", true, "#1890ff", 1),
                        new EnumFieldConfig.EnumOptionDefinition("sign-out", "补签出", "sign-out", true, "#52c41a", 2)
                ));
        createTextField(orgId, cardTypeId, "补卡时间", "makeup-time", "补卡时间", true);
        createEnumField(orgId, cardTypeId, "审批状态", "approval-status", "审批状态", false, false,
                List.of(
                        new EnumFieldConfig.EnumOptionDefinition("pending", "待审批", "pending", true, "#faad14", 1),
                        new EnumFieldConfig.EnumOptionDefinition("approved", "已批准", "approved", true, "#52c41a", 2),
                        new EnumFieldConfig.EnumOptionDefinition("rejected", "已拒绝", "rejected", true, "#f5222d", 3)
                ));
        createDateField(orgId, cardTypeId, "审批时间", "approval-time", "审批时间", false, DateFieldConfig.DateFormat.DATETIME);
        createMultiLineTextField(orgId, cardTypeId, "补卡原因", "reason", "补卡原因", true);
        createMultiLineTextField(orgId, cardTypeId, "审批意见", "approval-comment", "审批意见", false);

        // 创建关联：补卡申请 -> 申请人
        createLinkToMember(orgId, cardTypeId, memberCardTypeId, "申请人", "applicant", "补卡申请人", true);
        // 创建关联：补卡申请 -> 审批人
        createLinkToMember(orgId, cardTypeId, memberCardTypeId, "审批人", "approver", "审批人", false);

        log.info("补卡申请卡片类型创建完成: {}", cardTypeId);
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
        // 使用卡片类型ID作为前缀，确保字段ID唯一
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
        // 使用卡片类型ID作为前缀，确保字段ID唯一
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
     * 创建数字字段
     */
    private void createNumberField(String orgId, String cardTypeId, String name, String code, String description, boolean required) {
        createNumberField(orgId, cardTypeId, name, code, description, required, 2);
    }

    /**
     * 创建数字字段（带精度）
     */
    private void createNumberField(String orgId, String cardTypeId, String name, String code, String description, boolean required, Integer precision) {
        // 使用卡片类型ID作为前缀，确保字段ID唯一
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
        // 使用卡片类型ID作为前缀，确保字段ID唯一
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
        // 使用卡片类型ID作为前缀，确保字段ID唯一
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
        // 1. 创建关联类型 - 使用源卡片类型ID作为前缀，确保关联类型ID唯一
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
