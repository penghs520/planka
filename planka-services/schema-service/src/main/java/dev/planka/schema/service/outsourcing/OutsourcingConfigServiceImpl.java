package dev.planka.schema.service.outsourcing;

import dev.planka.api.schema.request.CreateSchemaRequest;
import dev.planka.api.schema.request.UpdateSchemaRequest;
import dev.planka.common.exception.CommonErrorCode;
import dev.planka.common.result.Result;
import dev.planka.domain.outsourcing.*;
import dev.planka.domain.schema.SchemaType;
import dev.planka.domain.schema.definition.SchemaDefinition;
import dev.planka.schema.service.common.SchemaCommonService;
import dev.planka.schema.service.common.SchemaQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 考勤配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutsourcingConfigServiceImpl implements OutsourcingConfigService {

    private final SchemaCommonService schemaCommonService;
    private final SchemaQuery schemaQuery;
    private final AttendanceCardTypeService attendanceCardTypeService;
    private final WorkloadCardTypeService workloadCardTypeService;
    // TODO: 注入 StatsDefinitionClient（用于初始化数据表）
    // private final StatsDefinitionClient statsDefinitionClient;

    @Override
    public Result<OutsourcingConfig> getByOrgId(String orgId) {
        // 查询组织下的考勤配置
        List<SchemaDefinition<?>> schemas = schemaQuery.queryPaged(
                orgId,
                SchemaType.OUTSOURCING_CONFIG,
                0,
                1
        );

        if (schemas.isEmpty()) {
            // 配置不存在是正常情况（新建组织还未开启考勤配置）
            // 返回成功但数据为空，前端可以通过 data === null 判断是否已配置
            log.debug("考勤配置不存在: orgId={}", orgId);
            return Result.success(null);
        }

        SchemaDefinition<?> schema = schemas.get(0);
        if (!(schema instanceof OutsourcingConfig)) {
            log.error("Schema类型不匹配: expected=OutsourcingConfig, actual={}", schema.getClass().getName());
            return Result.failure(CommonErrorCode.INTERNAL_ERROR, "配置类型错误");
        }

        return Result.success((OutsourcingConfig) schema);
    }

    @Override
    @Transactional
    public Result<OutsourcingConfig> saveOrUpdate(OutsourcingConfig config, String operatorId) {
        // 校验配置
        try {
            config.validate();
        } catch (IllegalArgumentException e) {
            log.warn("考勤配置校验失败: {}", e.getMessage());
            return Result.failure(CommonErrorCode.VALIDATION_ERROR, "配置校验失败: " + e.getMessage());
        }

        // 检查是否已存在配置
        Result<OutsourcingConfig> existingResult = getByOrgId(config.getOrgId());
        boolean isFirstEnable = false;

        if (existingResult.isSuccess() && existingResult.getData() != null) {
            // 更新现有配置
            OutsourcingConfig existing = existingResult.getData();

            // 考勤功能开关已移至组织设置，此处不再检查
            isFirstEnable = false;

            // 将前端传来的配置属性复制到现有配置对象中
            copyConfigProperties(existing, config);

            // 使用 SchemaCommonService 更新
            UpdateSchemaRequest updateRequest = new UpdateSchemaRequest();
            updateRequest.setDefinition(existing);
            updateRequest.setExpectedVersion(existing.getContentVersion());

            Result<SchemaDefinition<?>> updateResult = schemaCommonService.update(
                    existing.getId().value(),
                    operatorId,
                    updateRequest
            );

            if (!updateResult.isSuccess()) {
                return Result.failure(updateResult.getCode(), updateResult.getMessage());
            }

            config = (OutsourcingConfig) updateResult.getData();
        } else {
            // 创建新配置
            if (config.getId() == null) {
                OutsourcingConfig originalConfig = config;
                config = new OutsourcingConfig(
                        OutsourcingConfigId.generate(),
                        originalConfig.getOrgId(),
                        originalConfig.getName()
                );
                // 复制所有配置属性
                copyConfigProperties(config, originalConfig);
            }

            // 考勤功能开关已移至组织设置，此处不再检查
            isFirstEnable = false;

            // 使用 SchemaCommonService 创建
            CreateSchemaRequest createRequest = new CreateSchemaRequest();
            createRequest.setDefinition(config);

            Result<SchemaDefinition<?>> createResult = schemaCommonService.create(
                    config.getOrgId(),
                    operatorId,
                    createRequest
            );

            if (!createResult.isSuccess()) {
                return Result.failure(createResult.getCode(), createResult.getMessage());
            }

            config = (OutsourcingConfig) createResult.getData();
        }

        // 首次开启时初始化数据表
        if (isFirstEnable) {
            try {
                initOutsourcingForOrg(config.getOrgId());
            } catch (Exception e) {
                log.error("初始化考勤数据表失败: orgId={}", config.getOrgId(), e);
                // 不影响配置保存，只记录错误
            }
        }

        log.info("考勤配置保存成功: orgId={}, configId={}", config.getOrgId(), config.getId().value());
        return Result.success(config);
    }

    @Override
    public void initOutsourcingForOrg(String orgId) {
        log.info("初始化组织 {} 的考勤数据表", orgId);

        // 获取组织的成员卡片类型ID
        String memberCardTypeId = getMemberCardTypeId(orgId);
        if (memberCardTypeId == null) {
            log.error("无法获取组织的成员卡片类型ID，跳过考勤卡片类型初始化: orgId={}", orgId);
            return;
        }

        // 初始化考勤卡片类型
        try {
            attendanceCardTypeService.initAttendanceCardTypes(orgId, memberCardTypeId);
            log.info("考勤卡片类型初始化成功: orgId={}", orgId);
        } catch (Exception e) {
            log.error("初始化考勤卡片类型失败: orgId={}", orgId, e);
            throw new RuntimeException("初始化考勤卡片类型失败: " + e.getMessage(), e);
        }

        // 初始化工时卡片类型
        try {
            workloadCardTypeService.initWorkloadCardTypes(orgId, memberCardTypeId);
            log.info("工时卡片类型初始化成功: orgId={}", orgId);
        } catch (Exception e) {
            log.error("初始化工时卡片类型失败: orgId={}", orgId, e);
            throw new RuntimeException("初始化工时卡片类型失败: " + e.getMessage(), e);
        }

        // TODO: 调用 statsDefinitionClient.initOutsourcing(orgId)
        // 创建考勤相关的数据表：
        // - 考勤记录表
        // - 个人服务费结算表
        // - 项目服务费结算表

        log.info("组织 {} 的考勤数据表初始化完成", orgId);
    }

    @Override
    @Transactional
    public Result<OutsourcingConfig> createDefaultConfig(String orgId, String operatorId) {
        log.info("为组织 {} 创建默认考勤配置", orgId);

        // 获取组织的成员卡片类型ID
        String memberCardTypeId = getMemberCardTypeId(orgId);
        if (memberCardTypeId == null) {
            String errorMsg = "无法获取组织的成员卡片类型ID";
            log.error("创建默认考勤配置失败: orgId={}, error={}", orgId, errorMsg);
            return Result.failure(CommonErrorCode.VALIDATION_ERROR, errorMsg);
        }

        // 检查是否已存在配置
        Result<OutsourcingConfig> existingResult = getByOrgId(orgId);
        if (existingResult.isSuccess()) {
            log.info("组织 {} 的考勤配置已存在，更新嵌套对象", orgId);
            OutsourcingConfig existing = existingResult.getData();

            // 创建新的默认配置
            OutsourcingConfig defaultConfig = createDefaultOutsourcingConfig(orgId, memberCardTypeId);

            // 更新嵌套对象（如果为 null）
            if (existing.getOvertimeConf() != null && existing.getOvertimeConf().getNonWorkOvertime() == null) {
                existing.getOvertimeConf().setNonWorkOvertime(defaultConfig.getOvertimeConf().getNonWorkOvertime());
            }
            if (existing.getAttendanceChangeConf() != null) {
                if (existing.getAttendanceChangeConf().getSignIn() == null) {
                    existing.getAttendanceChangeConf().setSignIn(defaultConfig.getAttendanceChangeConf().getSignIn());
                }
                if (existing.getAttendanceChangeConf().getSignOut() == null) {
                    existing.getAttendanceChangeConf().setSignOut(defaultConfig.getAttendanceChangeConf().getSignOut());
                }
            }
            if (existing.getSettlementConf() != null) {
                if (existing.getSettlementConf().getPersonalServiceFeeConf() == null) {
                    existing.getSettlementConf().setPersonalServiceFeeConf(defaultConfig.getSettlementConf().getPersonalServiceFeeConf());
                }
                if (existing.getSettlementConf().getProjectServiceFeeConf() == null) {
                    existing.getSettlementConf().setProjectServiceFeeConf(defaultConfig.getSettlementConf().getProjectServiceFeeConf());
                }
            }

            // 使用 SchemaCommonService 更新
            UpdateSchemaRequest updateRequest = new UpdateSchemaRequest();
            updateRequest.setDefinition(existing);
            updateRequest.setExpectedVersion(existing.getContentVersion());

            Result<SchemaDefinition<?>> updateResult = schemaCommonService.update(
                    existing.getId().value(),
                    operatorId,
                    updateRequest
            );

            if (!updateResult.isSuccess()) {
                log.error("更新考勤配置失败: orgId={}, error={}", orgId, updateResult.getMessage());
                return Result.failure(updateResult.getCode(), updateResult.getMessage());
            }

            OutsourcingConfig updated = (OutsourcingConfig) updateResult.getData();
            log.info("考勤配置更新成功: orgId={}, configId={}", orgId, updated.getId().value());
            return Result.success(updated);
        }

        // 创建默认配置
        OutsourcingConfig config = createDefaultOutsourcingConfig(orgId, memberCardTypeId);

        // 使用 SchemaCommonService 创建
        CreateSchemaRequest createRequest = new CreateSchemaRequest();
        createRequest.setDefinition(config);

        Result<SchemaDefinition<?>> createResult = schemaCommonService.create(
                orgId,
                operatorId,
                createRequest
        );

        if (!createResult.isSuccess()) {
            log.error("创建默认考勤配置失败: orgId={}, error={}", orgId, createResult.getMessage());
            return Result.failure(createResult.getCode(), createResult.getMessage());
        }

        OutsourcingConfig created = (OutsourcingConfig) createResult.getData();
        log.info("默认考勤配置创建成功: orgId={}, configId={}", orgId, created.getId().value());

        return Result.success(created);
    }

    /**
     * 获取组织的成员卡片类型ID
     */
    private String getMemberCardTypeId(String orgId) {
        try {
            // 直接返回成员属性集 ID
            // 成员属性集是系统内置的，ID 格式为：{orgId}:member-trait
            String memberAbstractTypeId = orgId + ":member-trait";

            log.info("使用成员属性集: {}", memberAbstractTypeId);
            return memberAbstractTypeId;
        } catch (Exception e) {
            log.error("获取成员卡片类型失败: orgId={}", orgId, e);
            return null;
        }
    }

    /**
     * 创建默认考勤配置对象
     */
    private OutsourcingConfig createDefaultOutsourcingConfig(String orgId, String memberCardTypeId) {
        OutsourcingConfig config = new OutsourcingConfig(
                OutsourcingConfigId.generate(),
                orgId,
                "考勤配置"
        );

        // 设置全局配置默认值
        config.setDurationUnit(DurationUnit.MINUTE);
        config.setDecimalScale(0);
        config.setCardAttendanceRequired(false);

        // 设置签到配置默认值
        AttendanceConf attendanceConf = new AttendanceConf();
        attendanceConf.setWorkStart("08:30");
        attendanceConf.setWorkEnd("17:30");
        attendanceConf.setLunchStart("11:30");
        attendanceConf.setLunchEnd("13:30");
        attendanceConf.setWorkDuration(8.0);
        attendanceConf.setImpactWm(false);
        attendanceConf.setAccumulatedOvertime(true);
        attendanceConf.setAbsenceWhenNoSignInOrOut(false);
        config.setAttendanceConf(attendanceConf);

        // 设置请假配置默认值
        LeaveConf leaveConf = new LeaveConf();
        leaveConf.setLeaveUnit(LeaveUnit.HALF_DAY);
        leaveConf.setLimitRules(java.util.Collections.emptyList());
        config.setLeaveConf(leaveConf);

        // 设置加班配置默认值
        OvertimeConf overtimeConf = new OvertimeConf();
        overtimeConf.setCalWay(OvertimeCalWay.ACTUAL_ATTENDANCE);
        overtimeConf.setStartDuration(0);
        overtimeConf.setLimitRules(java.util.Collections.emptyList());
        // 初始化 nonWorkOvertime 为空对象而不是 null
        OvertimeConf.NonWorkOvertime nonWorkOvertime = new OvertimeConf.NonWorkOvertime();
        overtimeConf.setNonWorkOvertime(nonWorkOvertime);
        config.setOvertimeConf(overtimeConf);

        // 设置补卡配置默认值
        AttendanceChangeConf attendanceChangeConf = new AttendanceChangeConf();
        attendanceChangeConf.setCount(5);
        attendanceChangeConf.setWindow(10);
        attendanceChangeConf.setWindowUnit(WindowUnit.CALENDAR_DAY);
        attendanceChangeConf.setAllowWeekendOrHoliday(false);
        // 初始化 signIn 和 signOut 为空对象而不是 null
        AttendanceChangeConf.TimeLimit signIn = new AttendanceChangeConf.TimeLimit();
        AttendanceChangeConf.TimeLimit signOut = new AttendanceChangeConf.TimeLimit();
        attendanceChangeConf.setSignIn(signIn);
        attendanceChangeConf.setSignOut(signOut);
        config.setAttendanceChangeConf(attendanceChangeConf);

        // 设置结算配置默认值
        SettlementConf settlementConf = new SettlementConf();
        settlementConf.setMethod(SettlementMethod.MANUAL);
        settlementConf.setAbsenteeismDeductionCoefficient(2);
        settlementConf.setDurationUnit(DurationUnit.MINUTE);
        settlementConf.setDecimalScale(0);
        settlementConf.setVutIds(java.util.Collections.singletonList(memberCardTypeId));
        // 初始化 personalServiceFeeConf 和 projectServiceFeeConf 为空对象
        SettlementConf.PersonalServiceFeeConf personalServiceFeeConf = new SettlementConf.PersonalServiceFeeConf();
        SettlementConf.ProjectServiceFeeConf projectServiceFeeConf = new SettlementConf.ProjectServiceFeeConf();
        projectServiceFeeConf.setColumns(java.util.Collections.emptyList());
        settlementConf.setPersonalServiceFeeConf(personalServiceFeeConf);
        settlementConf.setProjectServiceFeeConf(projectServiceFeeConf);
        config.setSettlementConf(settlementConf);

        return config;
    }


    /**
     * 复制配置属性
     */
    private void copyConfigProperties(OutsourcingConfig target, OutsourcingConfig source) {
        target.setDurationUnit(source.getDurationUnit());
        target.setDecimalScale(source.getDecimalScale());
        target.setMemberCardTypeId(source.getMemberCardTypeId());
        target.setMemberFilter(source.getMemberFilter());
        target.setCardAttendanceRequired(source.getCardAttendanceRequired());

        // 只复制非 null 的嵌套对象，避免覆盖现有配置
        if (source.getAttendanceConf() != null) {
            target.setAttendanceConf(source.getAttendanceConf());
        }
        if (source.getLeaveConf() != null) {
            target.setLeaveConf(source.getLeaveConf());
        }
        if (source.getOvertimeConf() != null) {
            target.setOvertimeConf(source.getOvertimeConf());
        }
        if (source.getAttendanceChangeConf() != null) {
            target.setAttendanceChangeConf(source.getAttendanceChangeConf());
        }
        if (source.getSettlementConf() != null) {
            target.setSettlementConf(source.getSettlementConf());
        }
    }
}
