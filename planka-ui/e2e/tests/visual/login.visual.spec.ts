import { test, expect } from '@playwright/test'

test.describe('登录页视觉回归', () => {
  test.use({ storageState: { cookies: [], origins: [] } })

  test('登录页面整体外观', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await expect(page).toHaveScreenshot('login-page.png')
  })

  test('登录表单校验错误状态', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await page.locator('.login-form-wrapper button[type="submit"]').click()
    await page.waitForTimeout(300)
    await expect(page).toHaveScreenshot('login-validation-error.png')
  })
})
