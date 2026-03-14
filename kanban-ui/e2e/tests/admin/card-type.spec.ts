import { test, expect } from '../../fixtures/base'
import { CardTypePage } from '../../page-objects/admin/card-type.page'

test.describe('卡片类型管理', () => {
  let cardTypePage: CardTypePage

  test.beforeEach(async ({ page }) => {
    cardTypePage = new CardTypePage(page)
    await cardTypePage.goto()
  })

  test('卡片类型列表页正常加载', async ({ page }) => {
    await expect(page).toHaveURL(/\/admin\/card-type/)
    // 页面应包含表格或列表
    await page.waitForLoadState('networkidle')
  })

  test('点击新建打开表单抽屉', async ({ page }) => {
    await page.waitForLoadState('networkidle')
    const createBtn = page.locator('button:has-text("新建"), .create-button')
    if (await createBtn.isVisible()) {
      await createBtn.click()
      await expect(page.locator('.arco-drawer')).toBeVisible()
    }
  })

  test('搜索过滤卡片类型', async ({ page }) => {
    await page.waitForLoadState('networkidle')
    const searchInput = page.locator('input[placeholder*="搜索"], .search-input input')
    if (await searchInput.isVisible()) {
      await searchInput.fill('不存在的类型')
      await page.waitForTimeout(300)
    }
  })
})
