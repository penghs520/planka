import type { Page, Locator } from '@playwright/test'

/**
 * 选择组织页 Page Object
 */
export class SelectOrgPage {
  readonly title: Locator
  readonly orgList: Locator
  readonly createOrgButton: Locator
  readonly logoutButton: Locator

  constructor(private page: Page) {
    this.title = page.locator('.select-org-title')
    this.orgList = page.locator('.org-list')
    this.createOrgButton = page.locator('.create-org-section button')
    this.logoutButton = page.locator('.select-org-footer button')
  }

  async goto() {
    await this.page.goto('/select-org')
  }

  async selectOrg(orgName: string) {
    await this.page.locator(`.org-item:has-text("${orgName}")`).click()
  }

  async waitForOrgList() {
    await this.orgList.waitFor({ state: 'visible' })
  }
}
