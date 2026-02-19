/**
 * 组织角色枚举
 */
export enum OrganizationRole {
  /** 所有者 */
  OWNER = 'OWNER',
  /** 管理员 */
  ADMIN = 'ADMIN',
  /** 普通成员 */
  MEMBER = 'MEMBER',
}

/**
 * 角色显示配置
 */
export const OrganizationRoleConfig: Record<OrganizationRole, { label: string; color: string }> = {
  [OrganizationRole.OWNER]: { label: '所有者', color: 'gold' },
  [OrganizationRole.ADMIN]: { label: '管理员', color: 'blue' },
  [OrganizationRole.MEMBER]: { label: '成员', color: 'gray' },
}

/**
 * 组织信息
 */
export interface OrganizationDTO {
  id: string
  name: string
  description: string | null
  logo: string | null
  memberCardTypeId: string | null
  attendanceEnabled: boolean | null
  status: string
  createdBy: string
  createdAt: string
}

/**
 * 创建组织请求
 */
export interface CreateOrganizationRequest {
  name: string
  description?: string
  logo?: string
}

/**
 * 更新组织请求
 */
export interface UpdateOrganizationRequest {
  name?: string
  description?: string
  logo?: string
  attendanceEnabled?: boolean
}

/**
 * 成员信息
 */
export interface MemberDTO {
  id: string
  userId: string
  orgId: string
  memberCardId: string | null
  role: OrganizationRole
  status: string
  invitedBy: string | null
  joinedAt: string
  email: string
  nickname: string
  avatar: string | null
}

/**
 * 成员卡片选项
 * 用于下拉选择器等场景，返回简洁的成员信息
 */
export interface MemberOptionDTO {
  /** 成员卡片ID（用于筛选） */
  memberCardId: string
  /** 成员名称（用于显示） */
  name: string
}

/**
 * 成员卡片类型选项
 * 用于添加成员时选择类型
 */
export interface MemberCardTypeOption {
  id: string
  name: string
  icon?: string
}

/**
 * 添加成员请求
 */
export interface AddMemberRequest {
  email: string
  nickname: string
  role?: OrganizationRole
  cardTypeId?: string  // 成员卡片类型ID，当有多种成员类型时需要指定
}

/**
 * 修改成员角色请求
 */
export interface UpdateMemberRoleRequest {
  role: OrganizationRole
}
