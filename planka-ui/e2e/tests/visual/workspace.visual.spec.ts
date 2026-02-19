import { test, expect } from '@playwright/test'

test.describe('工作区视觉回归', () => {
  test('工作区主页面布局', async ({ page }) => {
    await page.goto('/workspace')
    await page.waitForLoadState('networkidle')
    await expect(page).toHaveScreenshot('workspace-layout.png', {
      mask: [page.locator('[data-testid="timestamp"]')],
    })
  })

  test('卡片详情抽屉', async ({ page }) => {
    await page.goto('/workspace')
    await page.waitForLoadState('networkidle')
    const cardRow = page.locator('.table-row').first()
    if (await cardRow.isVisible()) {
      await cardRow.click()
      await page.waitForSelector('.arco-drawer')
      await expect(page.locator('.arco-drawer')).toHaveScreenshot('card-detail-drawer.png')
    }
  })
})
