package cn.planka.api.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 侧栏钉选：可多选多个结构定义，顺序与列表一致。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SidebarPreferencesDTO {

    /** 已钉选的结构定义 ID，可多选、有序 */
    @Builder.Default
    private List<String> pinnedCascadeRelationIds = new ArrayList<>();
}
