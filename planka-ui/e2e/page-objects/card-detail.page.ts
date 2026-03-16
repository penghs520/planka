import type { Page, Locator } from '@playwright/test'

/**
 * 卡片详情页 Page Object
 */
export class CardDetailPage {
  readonly drawer: Locator
  readonly drawerTitle: Locator
  readonly closeButton: Locator

  constructor(private page: Page) {
    this.drawer = page.locator('.arco-drawer')
    this.drawerTitle = page.locator('.arco-drawer-title')
    this.closeButton = page.locator('.arco-drawer .arco-drawer-close-btn')
  }

  async waitForOpen() {
    await this.drawer.waitFor({ state: 'visible' })
  }

  async close() {
    await this.closeButton.click()
    await this.drawer.waitFor({ state: 'hidden' })
  }
}
