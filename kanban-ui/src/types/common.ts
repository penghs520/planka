/**
 * 实体状态枚举
 */
export enum EntityState {
  /** 草稿状态 */
  DRAFT = 'DRAFT',
  /** 活跃状态 */
  ACTIVE = 'ACTIVE',
  /** 停用状态 */
  DISABLED = 'DISABLED',
  /** 已删除状态 */
  DELETED = 'DELETED',
}

/**
 * 状态显示配置
 */
export const EntityStateConfig: Record<EntityState, { label: string; color: string }> = {
  [EntityState.DRAFT]: { label: '草稿', color: 'gray' },
  [EntityState.ACTIVE]: { label: '启用', color: 'green' },
  [EntityState.DISABLED]: { label: '停用', color: 'orange' },
  [EntityState.DELETED]: { label: '已删除', color: 'red' },
}
