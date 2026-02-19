package dev.planka.api.card;

import dev.planka.api.card.request.UpdateLinkRequest;
import dev.planka.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 关联卡片服务 Feign 客户端接口
 * <p>
 * 使用 OpenFeign 调用 card-service 的关联卡片 REST API
 */
@FeignClient(name = "card-service", contextId = "linkCardServiceClient", path = "/api/v1/links")
public interface LinkCardServiceClient {

    /**
     * 更新关联关系
     *
     * @param operatorId 操作人ID（成员卡片ID）
     * @param orgId      组织ID
     * @param request    更新请求
     * @return 操作结果
     */
    @PutMapping
    Result<Void> updateLink(@RequestHeader("X-Member-Card-Id") String operatorId,
                            @RequestHeader("X-Org-Id") String orgId,
                            @RequestBody UpdateLinkRequest request);
}
