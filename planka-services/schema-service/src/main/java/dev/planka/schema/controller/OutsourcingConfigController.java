package dev.planka.schema.controller;

import dev.planka.common.result.Result;
import dev.planka.domain.outsourcing.OutsourcingConfig;
import dev.planka.schema.service.outsourcing.OutsourcingConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 考勤配置控制器
 * <p>
 * 提供考勤配置的查询和保存接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/outsourcing-config")
@RequiredArgsConstructor
public class OutsourcingConfigController {

    private final OutsourcingConfigService configService;

    /**
     * 获取组织的考勤配置
     *
     * @param orgId 组织ID
     * @return 考勤配置
     */
    @GetMapping("/{orgId}")
    public Result<OutsourcingConfig> getByOrgId(@PathVariable String orgId) {
        log.info("获取考勤配置: orgId={}", orgId);
        return configService.getByOrgId(orgId);
    }

    /**
     * 创建或更新考勤配置
     *
     * @param orgId      组织ID
     * @param config     考勤配置
     * @param operatorId 操作人ID（从请求头获取）
     * @return 保存后的考勤配置
     */
    @PutMapping("/{orgId}")
    public Result<OutsourcingConfig> saveOrUpdate(
            @PathVariable String orgId,
            @Validated @RequestBody OutsourcingConfig config,
            @RequestHeader(value = "X-User-Id", required = false) String operatorId) {

        log.info("保存考勤配置: orgId={}, operatorId={}", orgId, operatorId);

        // 确保 orgId 一致
        config.setOrgId(orgId);

        // 如果没有提供操作人ID，使用默认值
        if (operatorId == null || operatorId.isBlank()) {
            operatorId = "system";
        }

        return configService.saveOrUpdate(config, operatorId);
    }

    /**
     * 创建组织的默认考勤配置
     *
     * @param orgId      组织ID
     * @param operatorId 操作人ID（从请求头获取）
     * @return 创建的默认配置
     */
    @PostMapping("/{orgId}/default")
    public Result<OutsourcingConfig> createDefaultConfig(
            @PathVariable String orgId,
            @RequestHeader(value = "X-Operator-Id", required = false) String operatorId) {

        log.info("创建默认考勤配置: orgId={}, operatorId={}", orgId, operatorId);

        // 如果没有提供操作人ID，使用默认值
        if (operatorId == null || operatorId.isBlank()) {
            operatorId = "system";
        }

        return configService.createDefaultConfig(orgId, operatorId);
    }

    /**
     * 初始化组织的考勤卡片类型
     * <p>
     * 当组织首次开启考勤功能时调用，创建考勤记录、请假申请、加班申请、补卡申请等卡片类型
     *
     * @param orgId 组织ID
     * @return 初始化结果
     */
    @PostMapping("/{orgId}/init")
    public Result<Void> initOutsourcingForOrg(@PathVariable String orgId) {
        log.info("初始化考勤卡片类型: orgId={}", orgId);

        try {
            configService.initOutsourcingForOrg(orgId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("初始化考勤卡片类型失败: orgId={}", orgId, e);
            return Result.failure("INIT_ERROR", "初始化考勤卡片类型失败: " + e.getMessage());
        }
    }
}
