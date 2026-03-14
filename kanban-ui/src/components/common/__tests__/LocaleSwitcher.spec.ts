import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import LocaleSwitcher from '../LocaleSwitcher.vue'
import * as i18n from '@/i18n'

// Mock vue-i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ locale: { value: 'zh-CN' } })
}))

// Mock i18n module
vi.mock('@/i18n', () => ({
  setLocale: vi.fn(),
  getLocale: vi.fn().mockReturnValue('zh-CN'),
  supportedLocales: [
    { label: '中文', value: 'zh-CN' },
    { label: 'English', value: 'en-US' }
  ]
}))

describe('LocaleSwitcher.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders current locale name', () => {
    const wrapper = mount(LocaleSwitcher, {
      global: {
        stubs: {
          'a-dropdown': {
            template: '<div><slot /><slot name="content" /></div>'
          },
          'a-button': {
            template: '<button><slot name="icon" /><slot /></button>'
          },
          'a-doption': {
            template: '<div class="doption"><slot /></div>'
          },
          IconLanguage: true
        }
      }
    })
    expect(wrapper.find('.locale-text').text()).toBe('中文')
  })

  it('renders dropdown options', () => {
    const wrapper = mount(LocaleSwitcher, {
      global: {
        stubs: {
          'a-dropdown': {
            template: '<div><slot /><slot name="content" /></div>'
          },
          'a-button': {
            template: '<button><slot name="icon" /><slot /></button>'
          },
          'a-doption': {
            template: '<div class="doption"><slot /></div>'
          },
          IconLanguage: true
        }
      }
    })
    const options = wrapper.findAll('.doption')
    expect(options).toHaveLength(2)
    expect(options[0]?.text()).toBe('中文')
    expect(options[1]?.text()).toBe('English')
  })

  it('calls setLocale when option clicked', async () => {
    const wrapper = mount(LocaleSwitcher, {
      global: {
        stubs: {
          'a-dropdown': {
            template: '<div><slot /><slot name="content" /></div>'
          },
          'a-button': {
            template: '<button><slot name="icon" /><slot /></button>'
          },
          'a-doption': {
            template: '<div class="doption" @click="$emit(\'click\')"><slot /></div>'
          },
          IconLanguage: true
        }
      }
    })

    // Simulate clicking the English option (index 1)
    await wrapper.findAll('.doption')[1]?.trigger('click')

    expect(i18n.setLocale).toHaveBeenCalledWith('en-US')
  })
})
