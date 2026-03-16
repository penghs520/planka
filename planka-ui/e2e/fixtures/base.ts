import { test as base } from '@playwright/test'
import { ApiClient } from '../helpers/api-client'

type E2EFixtures = {
  apiClient: ApiClient
}

export const test = base.extend<E2EFixtures>({
  apiClient: async ({ request }, use) => {
    await use(new ApiClient(request))
  },
})

export { expect } from '@playwright/test'
