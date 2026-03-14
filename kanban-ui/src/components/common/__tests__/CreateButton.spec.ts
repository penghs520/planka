import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import CreateButton from '../CreateButton.vue'

// Mock i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key })
}))

describe('CreateButton.vue', () => {
  it('renders default text', () => {
    const wrapper = mount(CreateButton, {
      global: {
        stubs: {
          'a-button': {
            template: '<button @click="$emit(\'click\')"><slot name=\'icon\' /><slot /></button>'
          },
          IconPlusCircle: true
        }
      }
    })
    expect(wrapper.text()).toContain('common.action.create')
  })

  it('renders slot content', () => {
    const wrapper = mount(CreateButton, {
      slots: {
        default: 'Add Item'
      },
      global: {
        stubs: {
          'a-button': {
            template: '<button><slot name=\'icon\' /><slot /></button>'
          },
          IconPlusCircle: true
        }
      }
    })
    expect(wrapper.text()).toContain('Add Item')
  })

  it('emits click event', async () => {
    const wrapper = mount(CreateButton, {
      global: {
        stubs: {
          'a-button': {
            template: '<button @click="$emit(\'click\')"><slot /></button>'
          },
          IconPlusCircle: true
        }
      }
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })
})
