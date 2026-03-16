import type { Router } from 'vue-router'
import i18n from '@/i18n'

const TOKEN_KEY = 'token'
const ORG_ID_KEY = 'orgId'

/**
 * 检查是否已登录
 */
function isLoggedIn(): boolean {
  return !!localStorage.getItem(TOKEN_KEY)
}

/**
 * 检查是否已选择组织
 */
function hasOrgId(): boolean {
  return !!localStorage.getItem(ORG_ID_KEY)
}

/**
 * 获取应用标题（国际化）
 */
function getAppTitle(): string {
  return i18n.global.t('common.login.title')
}

/**
 * 设置路由守卫
 */
export function setupRouterGuards(router: Router) {
  router.beforeEach((to, _from, next) => {
    // 设置页面标题（支持国际化）
    const titleKey = to.meta.titleKey as string
    const appTitle = getAppTitle()
    if (titleKey) {
      const pageTitle = i18n.global.t(titleKey)
      document.title = `${pageTitle} - ${appTitle}`
    } else {
      document.title = appTitle
    }

    // 检查是否需要认证
    const requiresAuth = to.matched.some((record) => record.meta.requiresAuth !== false)
    // 检查是否跳过组织检查（如组织选择页）
    const skipOrgCheck = to.matched.some((record) => record.meta.skipOrgCheck === true)

    // 如果需要认证
    if (requiresAuth) {
      // 未登录，跳转到登录页
      if (!isLoggedIn()) {
        next({
          path: '/login',
          query: { redirect: to.fullPath },
        })
        return
      }

      // 已登录但未选择组织，且不是跳过组织检查的页面
      if (!hasOrgId() && !skipOrgCheck) {
        next('/select-org')
        return
      }
    }

    // 已登录且已选择组织，访问登录页或激活页，跳转到首页
    if ((to.path === '/login' || to.path === '/activate') && isLoggedIn() && hasOrgId()) {
      next('/')
      return
    }

    // 已登录访问登录页，但未选择组织，跳转到组织选择页
    if (to.path === '/login' && isLoggedIn() && !hasOrgId()) {
      next('/select-org')
      return
    }

    next()
  })
}
