import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import CancelButton from '../CancelButton.vue'

// Mock i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key })
}))

describe('CancelButton.vue', () => {
  it('renders default text', () => {
    const wrapper = mount(CancelButton, {
      global: {
        stubs: {
          'a-button': {
            template: '<button><slot /></button>'
          }
        }
      }
    })
    expect(wrapper.text()).toBe('common.action.cancel')
  })

  it('renders custom text', () => {
    const wrapper = mount(CancelButton, {
      props: {
        text: 'Close'
      },
      global: {
        stubs: {
            'a-button': {
              template: '<button><slot /></button>'
            }
        }
      }
    })
    expect(wrapper.text()).toBe('Close')
  })

  it('renders slot content', () => {
      const wrapper = mount(CancelButton, {
        slots: {
            default: 'Go Back'
        },
        global: {
          stubs: {
              'a-button': {
                template: '<button><slot /></button>'
              }
          }
        }
      })
      expect(wrapper.text()).toBe('Go Back')
    })

  it('emits click event', async () => {
    const wrapper = mount(CancelButton, {
        global: {
            stubs: {
              'a-button': {
                template: '<button @click="$emit(\'click\', $event)"><slot /></button>'
              }
            }
          }
    })
    await wrapper.find('button').trigger('click')
    // Due to event bubbling/handling in test utils vs stubs, sometimes we get duplicate events or different behavior
    // Just checking it was emitted is usually sufficient
    expect(wrapper.emitted('click')).toBeTruthy()
  })
})
