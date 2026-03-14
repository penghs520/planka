import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useOrgStore } from '@/stores/org'

/**
 * 权限相关组合函数
 */
export function usePermission() {
  const userStore = useUserStore()
  const orgStore = useOrgStore()

  // 是否为超级管理员
  const isSuperAdmin = computed(() => userStore.isSuperAdmin)

  // 是否为当前组织所有者
  const isOwner = computed(() => orgStore.isOwner)

  // 是否为当前组织管理员（包含所有者）
  const isAdmin = computed(() => orgStore.isAdmin)

  // 是否可以管理成员
  const canManageMembers = computed(() => orgStore.canManageMembers)

  // 是否可以编辑组织信息
  const canEditOrg = computed(() => isAdmin.value)

  // 是否可以删除组织
  const canDeleteOrg = computed(() => isOwner.value)

  // 是否可以修改成员角色
  const canChangeRole = computed(() => isOwner.value)

  return {
    isSuperAdmin,
    isOwner,
    isAdmin,
    canManageMembers,
    canEditOrg,
    canDeleteOrg,
    canChangeRole,
  }
}
