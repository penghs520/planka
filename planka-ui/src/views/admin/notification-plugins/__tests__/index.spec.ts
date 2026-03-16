import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import NotificationPlugins from '../index.vue'
import { pluginApi } from '@/api/plugin'

vi.mock('@/api/plugin')

// Mock i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key })
}))

const globalStubs = {
  stubs: {
    'a-button': { template: '<button @click="$emit(\'click\')"><slot /><slot name="icon" /></button>' },
    'a-table': { template: '<div><slot /><slot name="pluginState" v-bind="{ record: {} }" /><slot name="actions" v-bind="{ record: {} }" /></div>', props: ['columns', 'data', 'loading', 'pagination'] },
    'a-tag': { template: '<span><slot /></span>' },
    'a-space': { template: '<div><slot /></div>' },
    'a-modal': { template: '<div><slot /></div>', props: ['visible', 'title'] },
    'a-upload': { template: '<div><slot name="upload-button" /></div>' },
    'a-alert': { template: '<div><slot /></div>' },
    'icon-upload': { template: '<span />' }
  }
}

describe('NotificationPlugins', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(pluginApi.list).mockResolvedValue({ data: [] } as any)
  })

  it('should render plugin list', async () => {
    const mockPlugins = [
      {
        pluginId: 'email',
        pluginName: 'Email Plugin',
        pluginState: 'STARTED',
        version: '1.0.0',
        provider: 'Planka',
      },
    ]

    vi.mocked(pluginApi.list).mockResolvedValue({ data: mockPlugins } as any)

    const wrapper = mount(NotificationPlugins, { global: globalStubs })
    await wrapper.vm.$nextTick()

    expect(pluginApi.list).toHaveBeenCalled()
  })

  it('should handle plugin upload', async () => {
    vi.mocked(pluginApi.upload).mockResolvedValue({ data: 'success' } as any)

    const wrapper = mount(NotificationPlugins, { global: globalStubs })
    const file = new File(['content'], 'test.jar', { type: 'application/java-archive' })

    await (wrapper.vm as any).customUpload({
      fileItem: { file },
      onSuccess: vi.fn(),
      onError: vi.fn(),
    })

    expect(pluginApi.upload).toHaveBeenCalledWith(file)
  })

  it('should handle plugin start', async () => {
    vi.mocked(pluginApi.start).mockResolvedValue({ data: null } as any)

    const wrapper = mount(NotificationPlugins, { global: globalStubs })
    await (wrapper.vm as any).handleStart({ pluginId: 'email' })

    expect(pluginApi.start).toHaveBeenCalledWith('email')
  })
})
