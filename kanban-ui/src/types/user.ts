/**
 * 用户状态枚举
 */
export enum UserStatus {
  /** 待激活 */
  PENDING_ACTIVATION = 'PENDING_ACTIVATION',
  /** 正常 */
  ACTIVE = 'ACTIVE',
  /** 已禁用 */
  DISABLED = 'DISABLED',
  /** 已锁定 */
  LOCKED = 'LOCKED',
}

/**
 * 用户状态显示配置
 * labelKey 用于国际化，运行时通过 t(labelKey) 获取实际标签
 */
export const UserStatusConfig: Record<UserStatus, { labelKey: string; color: string }> = {
  [UserStatus.PENDING_ACTIVATION]: { labelKey: 'common.userStatus.pendingActivation', color: 'orange' },
  [UserStatus.ACTIVE]: { labelKey: 'common.userStatus.active', color: 'green' },
  [UserStatus.DISABLED]: { labelKey: 'common.userStatus.disabled', color: 'gray' },
  [UserStatus.LOCKED]: { labelKey: 'common.userStatus.locked', color: 'red' },
}

/**
 * 用户信息
 */
export interface UserDTO {
  id: string
  email: string
  nickname: string
  avatar: string | null
  phone: string | null
  superAdmin: boolean
  status: UserStatus
  usingDefaultPassword: boolean
  lastLoginAt: string | null
  createdAt: string
}

/**
 * 更新用户信息请求
 */
export interface UpdateUserRequest {
  nickname?: string
  avatar?: string
  phone?: string
}

/**
 * 修改密码请求
 */
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}
