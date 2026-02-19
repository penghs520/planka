package dev.planka.api.schema;

import dev.planka.common.result.Result;
import dev.planka.domain.outsourcing.OutsourcingConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 考勤配置服务契约
 */
@FeignClient(name = "schema-service", contextId = "outsourcingConfigClient")
public interface OutsourcingConfigClient {

    /**
     * 获取组织的考勤配置
     *
     * @param orgId 组织ID
     * @return 考勤配置
     */
    @GetMapping("/api/v1/outsourcing-config/{orgId}")
    Result<OutsourcingConfig> getByOrgId(@PathVariable("orgId") String orgId);

    /**
     * 创建或更新考勤配置
     *
     * @param orgId      组织ID
     * @param operatorId 操作人ID（Header）
     * @param config     考勤配置
     * @return 保存后的考勤配置
     */
    @PostMapping("/api/v1/outsourcing-config/{orgId}")
    Result<OutsourcingConfig> saveOrUpdate(
            @PathVariable("orgId") String orgId,
            @RequestHeader("X-Operator-Id") String operatorId,
            @RequestBody OutsourcingConfig config
    );

    /**
     * 创建组织的默认考勤配置
     *
     * @param orgId      组织ID
     * @param operatorId 操作人ID（Header）
     * @return 创建的默认配置
     */
    @PostMapping("/api/v1/outsourcing-config/{orgId}/default")
    Result<OutsourcingConfig> createDefaultConfig(
            @PathVariable("orgId") String orgId,
            @RequestHeader("X-Operator-Id") String operatorId
    );

    /**
     * 初始化组织的考勤卡片类型
     * <p>
     * 当组织首次开启考勤功能时调用，创建考勤记录、请假申请、加班申请、补卡申请等卡片类型
     *
     * @param orgId 组织ID
     * @return 初始化结果
     */
    @PostMapping("/api/v1/outsourcing-config/{orgId}/init")
    Result<Void> initOutsourcingForOrg(@PathVariable("orgId") String orgId);
}
