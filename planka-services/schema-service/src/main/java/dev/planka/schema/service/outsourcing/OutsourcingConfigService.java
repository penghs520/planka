package dev.planka.schema.service.outsourcing;

import dev.planka.common.result.Result;
import dev.planka.domain.outsourcing.OutsourcingConfig;

/**
 * 考勤配置服务接口
 */
public interface OutsourcingConfigService {

    /**
     * 获取组织的考勤配置
     *
     * @param orgId 组织ID
     * @return 考勤配置
     */
    Result<OutsourcingConfig> getByOrgId(String orgId);

    /**
     * 创建或更新考勤配置
     *
     * @param config     考勤配置
     * @param operatorId 操作人ID
     * @return 保存后的考勤配置
     */
    Result<OutsourcingConfig> saveOrUpdate(OutsourcingConfig config, String operatorId);

    /**
     * 首次开启考勤功能（初始化数据表）
     *
     * @param orgId 组织ID
     */
    void initOutsourcingForOrg(String orgId);

    /**
     * 创建组织的默认考勤配置
     *
     * @param orgId      组织ID
     * @param operatorId 操作人ID
     * @return 创建的默认配置
     */
    Result<OutsourcingConfig> createDefaultConfig(String orgId, String operatorId);
}
