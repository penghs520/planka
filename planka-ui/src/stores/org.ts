import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  OrganizationDTO,
  OrganizationRole,
  CreateOrganizationRequest,
  UpdateOrganizationRequest,
  MemberDTO,
} from '@/types/member'
import type { SwitchOrganizationResponse } from '@/types/auth'
import { userApi } from '@/api/user'
import { orgApi } from '@/api/org'
import { memberApi } from '@/api/member'
import { authApi } from '@/api/auth'
import { useUserStore } from './user'

const ORG_ID_KEY = 'orgId'

const MEMBER_CARD_ID_KEY = 'memberCardId'

export const useOrgStore = defineStore('org', () => {
  // 状态
  const currentOrgId = ref<string | null>(localStorage.getItem(ORG_ID_KEY))
  const currentMemberCardId = ref<string | null>(localStorage.getItem(MEMBER_CARD_ID_KEY))
  const myOrgs = ref<OrganizationDTO[]>([])
  const myRole = ref<OrganizationRole | null>(null)
  const currentMember = ref<MemberDTO | null>(null)

  // 计算属性
  const currentOrg = computed(() => {
    return myOrgs.value.find((org) => org.id === currentOrgId.value) || null
  })

  const hasOrg = computed(() => !!currentOrgId.value)

  const isOwner = computed(() => myRole.value === 'OWNER')
  const isAdmin = computed(() => myRole.value === 'OWNER' || myRole.value === 'ADMIN')
  const canManageMembers = computed(() => isAdmin.value)

  /**
   * 切换组织
   * 调用后端验证成员卡状态，成功后更新 Token（包括 refreshToken）
   */
  async function switchOrganization(orgId: string): Promise<SwitchOrganizationResponse> {
    const userStore = useUserStore()
    const response = await authApi.switchOrganization({ orgId })

    // 更新 Token（包括 refreshToken，确保刷新时保留组织上下文）
    userStore.saveTokens(response.accessToken, response.refreshToken, response.expiresIn)

    // 更新组织状态
    currentOrgId.value = response.orgId
    currentMemberCardId.value = response.memberCardId
    myRole.value = response.role as OrganizationRole
    currentMember.value = null

    // 持久化
    localStorage.setItem(ORG_ID_KEY, response.orgId)
    localStorage.setItem(MEMBER_CARD_ID_KEY, response.memberCardId)

    return response
  }

  /**
   * 设置当前组织（仅本地，不调用后端，用于初始化）
   */
  function setCurrentOrgLocal(orgId: string) {
    currentOrgId.value = orgId
    localStorage.setItem(ORG_ID_KEY, orgId)
    // 切换组织后清空角色信息，需要重新获取
    myRole.value = null
    currentMember.value = null
  }

  /**
   * 设置组织列表
   */
  function setOrgs(orgList: OrganizationDTO[]) {
    myOrgs.value = orgList
  }

  /**
   * 清除组织信息
   */
  function clearOrg() {
    currentOrgId.value = null
    currentMemberCardId.value = null
    myRole.value = null
    currentMember.value = null
    localStorage.removeItem(ORG_ID_KEY)
    localStorage.removeItem(MEMBER_CARD_ID_KEY)
  }

  /**
   * 获取我的组织列表
   */
  async function fetchMyOrganizations(): Promise<OrganizationDTO[]> {
    const orgs = await userApi.getMyOrganizations()
    myOrgs.value = orgs
    return orgs
  }

  /**
   * 创建组织
   */
  async function createOrganization(data: CreateOrganizationRequest): Promise<OrganizationDTO> {
    const org = await orgApi.create(data)
    // 创建成功后添加到列表
    myOrgs.value.push(org)
    // 切换到新创建的组织（获取带组织信息的 token）
    await switchOrganization(org.id)
    return org
  }

  /**
   * 更新组织信息
   */
  async function updateOrganization(
    orgId: string,
    data: UpdateOrganizationRequest,
  ): Promise<OrganizationDTO> {
    const org = await orgApi.update(orgId, data)
    // 更新列表中的组织信息 - 使用 splice 确保响应式
    const index = myOrgs.value.findIndex((o) => o.id === orgId)
    if (index !== -1) {
      myOrgs.value.splice(index, 1, org)
    }
    return org
  }

  /**
   * 获取当前用户在当前组织的角色
   */
  async function fetchCurrentOrgRole(): Promise<OrganizationRole | null> {
    if (!currentOrgId.value) return null
    try {
      // 通过成员列表获取当前用户的成员信息
      const result = await memberApi.list(1, 100)
      // 从用户 store 获取当前用户 ID（需要在使用时传入）
      const userId = localStorage.getItem('userId')
      if (userId) {
        const member = result.content.find((m) => m.userId === userId)
        if (member) {
          myRole.value = member.role
          currentMember.value = member
          return member.role
        }
      }
    } catch {
      // 忽略错误
    }
    return null
  }

  /**
   * 直接设置角色（登录时使用）
   */
  function setMyRole(role: OrganizationRole) {
    myRole.value = role
  }

  return {
    // 状态
    currentOrgId,
    currentMemberCardId,
    myOrgs,
    myRole,
    currentMember,
    // 计算属性
    currentOrg,
    hasOrg,
    isOwner,
    isAdmin,
    canManageMembers,
    // 方法
    switchOrganization,
    setCurrentOrgLocal,
    setOrgs,
    clearOrg,
    fetchMyOrganizations,
    createOrganization,
    updateOrganization,
    fetchCurrentOrgRole,
    setMyRole,
  }
})
