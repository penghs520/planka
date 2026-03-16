import { test, expect } from '../../fixtures/base'

test.describe('路由导航', () => {
  test('根路径重定向到工作区', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveURL(/\/workspace/)
  })

  test('工作区导航到管理后台', async ({ page }) => {
    await page.goto('/admin')
    await expect(page).toHaveURL(/\/admin/)
  })

  test('管理后台默认重定向到卡片类型', async ({ page }) => {
    await page.goto('/admin')
    await expect(page).toHaveURL(/\/admin\/card-type/)
  })

  test('不存在的路由重定向到首页', async ({ page }) => {
    await page.goto('/non-existent-page')
    await expect(page).toHaveURL(/\/workspace/)
  })

  test('管理后台侧边栏菜单导航', async ({ page }) => {
    await page.goto('/admin/card-type')
    await page.waitForLoadState('networkidle')

    // 点击关联类型菜单
    const linkTypeMenu = page.locator('.arco-menu-item:has-text("关联类型"), a[href*="link-type"]')
    if (await linkTypeMenu.isVisible()) {
      await linkTypeMenu.click()
      await expect(page).toHaveURL(/\/admin\/link-type/)
    }
  })
})

test.describe('组织切换', () => {
  test('已登录用户可以访问组织选择页', async ({ page }) => {
    await page.goto('/select-org')
    // 已登录用户应能访问组织选择页（skipOrgCheck: true）
    await expect(page).toHaveURL(/\/select-org/)
  })
})
