import { test, expect } from '@playwright/test'
import { LoginPage } from '../../page-objects/login.page'
import { TEST_USERS } from '../../helpers/test-data'

test.describe('登录流程', () => {
  // 登录测试需要清除认证状态
  test.use({ storageState: { cookies: [], origins: [] } })

  let loginPage: LoginPage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    await loginPage.goto()
  })

  test('正确凭据登录成功', async ({ page }) => {
    await loginPage.login(TEST_USERS.admin.email, TEST_USERS.admin.password)

    // 登录成功后：
    // - 可能弹出修改密码弹窗（默认密码），此时仍在 /login
    // - 可能跳转到 /select-org 或 /workspace
    await page.waitForTimeout(2000)
    const url = page.url()
    const hasModal = await page.locator('.arco-modal').isVisible().catch(() => false)
    expect(url.includes('/select-org') || url.includes('/workspace') || hasModal).toBeTruthy()
  })

  test('错误密码登录失败', async ({ page }) => {
    await loginPage.login(TEST_USERS.admin.email, 'wrong-password')

    // 应停留在登录页
    await expect(page).toHaveURL(/\/login/)
    // 应显示错误提示
    await expect(page.locator('.arco-message-error')).toBeVisible()
  })

  test('空表单提交显示校验错误', async ({ page }) => {
    await loginPage.loginButton.click()

    // 应显示表单校验错误
    await expect(page.locator('.arco-form-item-message')).toHaveCount(2)
    await expect(page).toHaveURL(/\/login/)
  })

  test('未登录访问工作区重定向到登录页', async ({ page }) => {
    await page.goto('/workspace')

    await expect(page).toHaveURL(/\/login/)
    // 应携带 redirect 参数
    await expect(page).toHaveURL(/redirect/)
  })

  test('邮箱格式校验', async ({ page }) => {
    await loginPage.emailInput.fill('invalid-email')
    await loginPage.passwordInput.fill('somepassword')
    await loginPage.loginButton.click()

    // 应显示邮箱格式错误，且文本内容正确
    const errorMsg = page.locator('.arco-form-item-message')
    await expect(errorMsg).toBeVisible()
    await expect(errorMsg).toHaveText('请输入正确的邮箱格式')
  })
})
