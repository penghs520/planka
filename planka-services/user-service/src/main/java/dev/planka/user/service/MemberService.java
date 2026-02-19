package dev.planka.user.service;

import dev.planka.api.card.CardServiceClient;
import dev.planka.api.card.dto.CardDTO;
import dev.planka.api.user.dto.MemberDTO;
import dev.planka.api.user.dto.MemberOptionDTO;
import dev.planka.api.user.enums.OrganizationRole;
import dev.planka.api.user.enums.UserStatus;
import dev.planka.api.user.request.AddMemberRequest;
import dev.planka.api.user.request.UpdateMemberRoleRequest;
import dev.planka.common.result.PageResult;
import dev.planka.common.result.Result;
import dev.planka.common.util.SnowflakeIdGenerator;
import dev.planka.common.util.SystemSchemaIds;
import dev.planka.domain.card.CardStyle;
import dev.planka.domain.card.CardTypeId;
import dev.planka.domain.schema.definition.cardtype.CardTypeDefinition;
import dev.planka.domain.schema.definition.cardtype.EntityCardType;
import dev.planka.domain.schema.definition.condition.Condition;
import dev.planka.domain.schema.definition.condition.TitleConditionItem;
import dev.planka.infra.cache.schema.query.CardTypeCacheQuery;
import dev.planka.user.model.OrganizationEntity;
import dev.planka.user.model.UserEntity;
import dev.planka.user.model.UserOrganizationEntity;
import dev.planka.user.repository.OrganizationRepository;
import dev.planka.user.repository.UserOrganizationRepository;
import dev.planka.user.repository.UserRepository;
import dev.planka.user.security.PasswordEncoder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.planka.api.card.request.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成员服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationService organizationService;
    private final MemberCardService memberCardService;
    private final PasswordEncoder passwordEncoder;
    private final CardServiceClient cardServiceClient;
    private final CardTypeCacheQuery cardTypeCacheQuery;

    @Value("${user.invitation.default-password:changeme}")
    private String defaultPassword;

    /**
     * 获取组织成员列表
     */
    public Result<PageResult<MemberDTO>> listMembers(String orgId, int page, int size) {
        Page<UserOrganizationEntity> pageResult = userOrganizationRepository.findByOrgId(orgId, page, size);

        List<String> userIds = pageResult.getRecords().stream()
                .map(UserOrganizationEntity::getUserId)
                .collect(Collectors.toList());

        Map<String, UserEntity> userMap = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<MemberDTO> members = pageResult.getRecords().stream()
                .map(uo -> toMemberDTO(uo, userMap.get(uo.getUserId())))
                .collect(Collectors.toList());

        return Result.success(new PageResult<>(
                members,
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize(),
                pageResult.getTotal()
        ));
    }

    /**
     * 获取成员卡片选项列表
     * <p>
     * 通过调用 CardServiceClient 查询成员卡片，支持按名称关键字搜索
     *
     * @param orgId   组织ID
     * @param page    页码（从1开始）
     * @param size    每页数量
     * @param keyword 搜索关键字（可选，匹配成员卡片名称）
     */
    public Result<PageResult<MemberOptionDTO>> getMemberOptions(String orgId, int page, int size, String keyword) {
        // 1. 获取成员卡片类型ID
        String memberCardTypeId = SystemSchemaIds.memberCardTypeId(orgId);

        // 2. 构建查询请求
        CardPageQueryRequest queryRequest = buildMemberOptionsQueryRequest(orgId, memberCardTypeId, keyword, page, size);

        // 3. 调用 CardServiceClient 查询
        Result<PageResult<CardDTO>> result = cardServiceClient.pageQuery("system", queryRequest);
        if (!result.isSuccess()) {
            log.error("Failed to query member cards: {}", result.getMessage());
            return Result.failure(result.getCode(), result.getMessage());
        }

        // 4. 转换为 MemberOptionDTO
        PageResult<CardDTO> cardPageResult = result.getData();
        List<MemberOptionDTO> options = cardPageResult.getContent().stream()
                .map(card -> new MemberOptionDTO(card.getId().value(), card.getTitle().getDisplayValue()))
                .collect(Collectors.toList());

        return Result.success(new PageResult<>(
                options,
                cardPageResult.getPage(),
                cardPageResult.getSize(),
                cardPageResult.getTotal()
        ));
    }

    /**
     * 构建成员卡片选项查询请求
     */
    private CardPageQueryRequest buildMemberOptionsQueryRequest(
            String orgId, String memberCardTypeId, String keyword, int page, int size) {

        CardPageQueryRequest queryRequest = new CardPageQueryRequest();

        // 1. 查询上下文
        QueryContext queryContext = new QueryContext();
        queryContext.setOrgId(orgId);
        queryContext.setOperatorId("system");
        queryRequest.setQueryContext(queryContext);

        // 2. 查询范围：指定成员卡片类型和活跃状态
        QueryScope queryScope = new QueryScope();
        queryScope.setCardTypeIds(List.of(memberCardTypeId));
        queryScope.setCardStyles(List.of(CardStyle.ACTIVE));
        queryRequest.setQueryScope(queryScope);

        // 3. 查询条件：使用 TitleConditionItem 进行关键字搜索
        if (keyword != null && !keyword.isBlank()) {
            TitleConditionItem titleCondition = new TitleConditionItem(
                    new TitleConditionItem.TitleSubject(null),
                    new TitleConditionItem.TitleOperator.Contains(keyword.trim())
            );
            queryRequest.setCondition(Condition.of(titleCondition));
        }

        // 4. 返回定义：只需要基本字段
        Yield yield = new Yield();
        YieldField yieldField = new YieldField();
        yieldField.setAllFields(false);
        yield.setField(yieldField);
        queryRequest.setYield(yield);

        // 5. 排序和分页：按名称升序
        SortAndPage sortAndPage = new SortAndPage();
        dev.planka.api.card.request.Page pageParam = new dev.planka.api.card.request.Page();
        pageParam.setPageNum(page);
        pageParam.setPageSize(size);
        sortAndPage.setPage(pageParam);

        Sort titleSort = new Sort();
        SortField sortField = new SortField();
        sortField.setFieldId("title");
        titleSort.setSortField(sortField);
        sortAndPage.setSorts(List.of(titleSort));

        queryRequest.setSortAndPage(sortAndPage);

        return queryRequest;
    }

    /**
     * 添加成员
     */
    @Transactional
    public Result<MemberDTO> addMember(String orgId, String operatorId, AddMemberRequest request) {
        // 1. 检查操作者权限
        Result<Void> permCheck = organizationService.checkAdminPermission(orgId, operatorId);
        if (!permCheck.isSuccess()) {
            return Result.failure(permCheck.getCode(), permCheck.getMessage());
        }

        // 2. 检查组织是否存在
        OrganizationEntity org = organizationRepository.findById(orgId).orElse(null);
        if (org == null) {
            return Result.failure("ORG_001", "组织不存在");
        }

        // 3. 查找或创建用户
        UserEntity user = userRepository.findByEmail(request.email()).orElse(null);
        if (user == null) {
            // 创建新用户（使用默认密码）
            user = new UserEntity();
            user.setId(SnowflakeIdGenerator.generateStr());
            user.setEmail(request.email());
            user.setNickname(request.nickname());
            user.setStatus(UserStatus.ACTIVE.name());
            user.setPasswordHash(passwordEncoder.encode(defaultPassword));
            user.setUsingDefaultPassword(true);
            userRepository.save(user);
            log.info("Created new user for invitation: {}, using default password", request.email());
        }

        // 4. 检查是否已是成员
        UserOrganizationEntity existing = userOrganizationRepository
                .findByUserIdAndOrgId(user.getId(), orgId).orElse(null);
        if (existing != null) {
            return Result.failure("MEMBER_001", "该用户已是组织成员");
        }

        // 5. 创建成员关系
        String role = request.role();
        if (role == null || role.isBlank()) {
            role = OrganizationRole.MEMBER.name();
        }

        UserOrganizationEntity userOrg = new UserOrganizationEntity();
        userOrg.setId(SnowflakeIdGenerator.generateStr());
        userOrg.setUserId(user.getId());
        userOrg.setOrgId(orgId);
        userOrg.setRole(role);
        userOrg.setStatus("ACTIVE");
        userOrg.setInvitedBy(operatorId);

        // 6. 确定成员卡片类型
        String memberCardTypeId = request.cardTypeId();
        if (memberCardTypeId == null || memberCardTypeId.isBlank()) {
            // 使用默认成员卡片类型
            memberCardTypeId = org.getMemberCardTypeId();
        } else {
            // 验证指定类型是否继承成员属性集
            Result<Void> validationResult = validateMemberCardType(orgId, memberCardTypeId);
            if (!validationResult.isSuccess()) {
                return Result.failure(validationResult.getCode(), validationResult.getMessage());
            }
        }

        // 7. 创建成员卡片
        String memberCardId = memberCardService.createMemberCard(orgId, memberCardTypeId, user.getId());
        userOrg.setMemberCardId(memberCardId);

        userOrganizationRepository.save(userOrg);
        log.info("Member added: {} to org {} by {}", user.getEmail(), orgId, operatorId);

        return Result.success(toMemberDTO(userOrg, user));
    }

    /**
     * 验证成员卡片类型是否有效（继承成员属性集）
     *
     * @param orgId      组织ID
     * @param cardTypeId 卡片类型ID
     * @return 验证结果
     */
    private Result<Void> validateMemberCardType(String orgId, String cardTypeId) {
        // 查询继承成员属性集的所有实体类型
        String memberAbstractTypeId = SystemSchemaIds.memberAbstractCardTypeId(orgId);
        Optional<CardTypeDefinition> cardTypeOpt = cardTypeCacheQuery.getById(CardTypeId.of(cardTypeId));
        if (cardTypeOpt.isEmpty()) {
            return Result.failure("MEMBER_005", "指定的卡片类型不存在");
        }

        CardTypeDefinition cardTypeDefinition = cardTypeOpt.get();

        if (!(cardTypeDefinition instanceof EntityCardType entityCardType)) {
            return Result.failure("MEMBER_006", "指定的卡片类型不能是属性集");
        }

        Set<CardTypeId> parentTypeIds = entityCardType.getParentTypeIds();
        if (CollectionUtils.isEmpty(parentTypeIds) || !parentTypeIds.stream().map(CardTypeId::value).toList().contains(memberAbstractTypeId)){
            return Result.failure("MEMBER_007", "指定的卡片类型不是有效的成员类型");
        }

        return Result.success(null);
    }

    /**
     * 获取成员详情
     */
    public Result<MemberDTO> getMember(String memberId) {
        UserOrganizationEntity userOrg = userOrganizationRepository.findById(memberId).orElse(null);
        if (userOrg == null) {
            return Result.failure("MEMBER_002", "成员不存在");
        }

        UserEntity user = userRepository.findById(userOrg.getUserId()).orElse(null);
        return Result.success(toMemberDTO(userOrg, user));
    }

    /**
     * 移除成员
     */
    @Transactional
    public Result<Void> removeMember(String memberId, String operatorId) {
        UserOrganizationEntity userOrg = userOrganizationRepository.findById(memberId).orElse(null);
        if (userOrg == null) {
            return Result.failure("MEMBER_002", "成员不存在");
        }

        // 检查权限
        Result<Void> permCheck = organizationService.checkAdminPermission(userOrg.getOrgId(), operatorId);
        if (!permCheck.isSuccess()) {
            return permCheck;
        }

        // 不能移除 OWNER
        if (OrganizationRole.OWNER.name().equals(userOrg.getRole())) {
            return Result.failure("MEMBER_003", "不能移除组织所有者");
        }

        // 删除成员卡片
        if (userOrg.getMemberCardId() != null) {
            memberCardService.deleteMemberCard(userOrg.getMemberCardId(), operatorId);
        }

        // 删除成员关系
        userOrganizationRepository.delete(memberId);
        log.info("Member removed: {} from org {} by {}", memberId, userOrg.getOrgId(), operatorId);

        return Result.success(null);
    }

    /**
     * 修改成员角色
     */
    @Transactional
    public Result<MemberDTO> updateMemberRole(String memberId, String operatorId, UpdateMemberRoleRequest request) {
        UserOrganizationEntity userOrg = userOrganizationRepository.findById(memberId).orElse(null);
        if (userOrg == null) {
            return Result.failure("MEMBER_002", "成员不存在");
        }

        // 只有 OWNER 可以修改角色
        Result<Void> permCheck = organizationService.checkOwnerPermission(userOrg.getOrgId(), operatorId);
        if (!permCheck.isSuccess()) {
            return Result.failure(permCheck.getCode(), permCheck.getMessage());
        }

        // 不能修改 OWNER 的角色
        if (OrganizationRole.OWNER.name().equals(userOrg.getRole())) {
            return Result.failure("MEMBER_004", "不能修改组织所有者的角色");
        }

        userOrg.setRole(request.role());
        userOrganizationRepository.save(userOrg);

        UserEntity user = userRepository.findById(userOrg.getUserId()).orElse(null);
        return Result.success(toMemberDTO(userOrg, user));
    }

    private MemberDTO toMemberDTO(UserOrganizationEntity userOrg, UserEntity user) {
        return new MemberDTO(
                userOrg.getId(),
                userOrg.getUserId(),
                userOrg.getOrgId(),
                userOrg.getMemberCardId(),
                userOrg.getRole(),
                userOrg.getStatus(),
                userOrg.getInvitedBy(),
                userOrg.getJoinedAt(),
                user != null ? user.getEmail() : null,
                user != null ? user.getNickname() : null,
                user != null ? user.getAvatar() : null
        );
    }
}
