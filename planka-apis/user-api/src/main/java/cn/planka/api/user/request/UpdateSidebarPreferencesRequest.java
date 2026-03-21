package cn.planka.api.user.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSidebarPreferencesRequest {

    /** 钉选的结构定义 ID 列表（多选、顺序即展示顺序）；可为空列表表示全部不钉选 */
    @NotNull(message = "pinnedStructureIds 不能为 null")
    private List<String> pinnedStructureIds;
}
