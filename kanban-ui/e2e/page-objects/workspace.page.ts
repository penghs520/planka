import type { Page, Locator } from '@playwright/test'

/**
 * 工作区页面 Page Object
 */
export class WorkspacePage {
  readonly sidebar: Locator
  readonly content: Locator
  readonly header: Locator
  readonly menuSearch: Locator
  readonly settingsIcon: Locator

  constructor(private page: Page) {
    this.sidebar = page.locator('.workspace-sidebar')
    this.content = page.locator('.workspace-content')
    this.header = page.locator('.workspace-header')
    this.menuSearch = page.locator('.search-box input')
    this.settingsIcon = page.locator('.header-right .icon-settings, .header-right [class*="settings"]')
  }

  async goto() {
    await this.page.goto('/workspace')
  }

  async waitForLoaded() {
    await this.header.waitFor({ state: 'visible' })
  }

  async clickMenuItem(name: string) {
    await this.page.locator(`.menu-item:has-text("${name}")`).click()
  }

  async searchMenu(keyword: string) {
    await this.menuSearch.fill(keyword)
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
