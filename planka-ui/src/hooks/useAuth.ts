import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useOrgStore } from '@/stores/org'

/**
 * 认证相关组合函数
 */
export function useAuth() {
  const userStore = useUserStore()
  const orgStore = useOrgStore()

  const isLoggedIn = computed(() => userStore.isLoggedIn)
  const user = computed(() => userStore.user)
  const isSuperAdmin = computed(() => userStore.isSuperAdmin)

  /**
   * 登出并跳转到登录页
   */
  async function logout() {
    // 先清除所有本地状态
    orgStore.clearOrg()
    await userStore.logout()
    // 使用 window.location 强制刷新，确保状态完全清除
    window.location.href = '/login'
  }

  /**
   * 检查是否需要刷新 Token
   */
  function checkTokenExpiry() {
    if (userStore.isLoggedIn && userStore.isTokenExpiringSoon()) {
      userStore.doRefreshToken().catch(() => {
        // 刷新失败会在拦截器中处理
      })
    }
  }

  return {
    isLoggedIn,
    user,
    isSuperAdmin,
    logout,
    checkTokenExpiry,
  }
}
