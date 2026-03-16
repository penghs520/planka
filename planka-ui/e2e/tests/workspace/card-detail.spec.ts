import { test, expect } from '../../fixtures/base'
import { WorkspacePage } from '../../page-objects/workspace.page'
import { CardDetailPage } from '../../page-objects/card-detail.page'

test.describe('卡片详情', () => {
  let workspace: WorkspacePage

  test.beforeEach(async ({ page }) => {
    workspace = new WorkspacePage(page)
    await workspace.goto()
    await workspace.waitForLoaded()
  })

  test('点击卡片打开详情抽屉', async ({ page }) => {
    const rows = await workspace.getCardRows()
    const count = await rows.count()

    if (count > 0) {
      await rows.first().click()
      const cardDetail = new CardDetailPage(page)
      await cardDetail.waitForOpen()
      await expect(cardDetail.drawer).toBeVisible()

      // 关闭抽屉
      await cardDetail.close()
    }
  })
})
