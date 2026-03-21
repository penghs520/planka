import type { Page, Locator } from '@playwright/test'

/**
 * 工作区页面 Page Object
 */
export class WorkspacePage {
  readonly sidebar: Locator
  readonly content: Locator
  readonly header: Locator
  readonly settingsIcon: Locator

  constructor(private page: Page) {
    this.sidebar = page.locator('.nav-views-embed')
    this.content = page.locator('.workspace-page')
    this.header = page.locator('.app-sidebar')
    this.settingsIcon = page.locator('.header-right .icon-settings, .header-right [class*="settings"]')
  }

  async goto() {
    await this.page.goto('/workspace')
  }

  async waitForLoaded() {
    await this.sidebar.waitFor({ state: 'visible' })
  }

  async clickMenuItem(name: string) {
    await this.page.locator(`.nav-views-embed .menu-item:has-text("${name}")`).click()
  }

  async navigateToAdmin() {
    await this.page.goto('/admin')
  }

  async clickFirstCard() {
    await this.page.locator('.table-row').first().click()
  }

  async getCardRows() {
    return this.page.locator('.table-row')
  }
}
