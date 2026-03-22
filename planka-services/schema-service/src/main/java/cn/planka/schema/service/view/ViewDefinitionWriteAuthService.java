package cn.planka.schema.service.view;

import cn.planka.api.user.UserServiceContract;
import cn.planka.api.user.dto.MemberCardDirectoryEnrichmentDTO;
import cn.planka.api.user.request.MemberCardIdsRequest;
import cn.planka.common.result.Result;
import cn.planka.domain.schema.definition.AbstractSchemaDefinition;
import cn.planka.domain.schema.definition.view.AbstractViewDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 视图定义更新/删除：创建人或组织 OWNER/ADMIN。
 */
@Service
@RequiredArgsConstructor
public class ViewDefinitionWriteAuthService {

    private final UserServiceContract userServiceContract;

    public boolean canModify(AbstractViewDefinition view, String operatorMemberCardId, String orgId) {
        return canModify((AbstractSchemaDefinition<?>) view, operatorMemberCardId, orgId);
    }

    /**
     * 菜单分组等与视图相同的「创建人或组织 OWNER/ADMIN」规则。
     */
    public boolean canModify(AbstractSchemaDefinition<?> schema, String operatorMemberCardId, String orgId) {
        if (operatorMemberCardId == null || operatorMemberCardId.isBlank()) {
            return false;
        }
        if (operatorMemberCardId.equals(schema.getCreatedBy())) {
            return true;
        }
        Result<List<MemberCardDirectoryEnrichmentDTO>> r = userServiceContract.enrichMembersByMemberCards(
                orgId,
                new MemberCardIdsRequest(List.of(operatorMemberCardId)));
        if (!r.isSuccess() || r.getData() == null || r.getData().isEmpty()) {
            return false;
        }
        String role = r.getData().get(0).role();
        return "OWNER".equals(role) || "ADMIN".equals(role);
    }
}
