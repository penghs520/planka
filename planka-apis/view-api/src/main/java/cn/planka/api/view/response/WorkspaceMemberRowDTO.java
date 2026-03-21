package cn.planka.api.view.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作区成员目录行（成员卡片 + 用户域补齐字段）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberRowDTO {

    private String memberCardId;
    private String name;
    private String email;
    private List<String> teamNames;
    private String role;
    private LocalDateTime joinedAt;
    private LocalDateTime lastLoginAt;
}
