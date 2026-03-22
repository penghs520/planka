import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import SaveButton from '../SaveButton.vue'

// Mock i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key })
}))

describe('SaveButton.vue', () => {
  it('renders default text', () => {
    const wrapper = mount(SaveButton, {
      global: {
        stubs: {
          'a-button': {
            template: '<button><slot /></button>'
          }
        }
      }
    })
    expect(wrapper.text()).toBe('common.action.save')
  })

  it('renders custom text', () => {
    const wrapper = mount(SaveButton, {
      props: {
        text: 'Submit'
      },
      global: {
        stubs: {
            'a-button': {
              template: '<button><slot /></button>'
            }
        }
      }
    })
    expect(wrapper.text()).toBe('Submit')
  })

  it('renders slot content', () => {
      const wrapper = mount(SaveButton, {
        slots: {
            default: 'Update'
        },
        global: {
          stubs: {
              'a-button': {
                template: '<button><slot /></button>'
              }
          }
        }
      })
      expect(wrapper.text()).toBe('Update')
    })

  it('passes loading prop', () => {
      const wrapper = mount(SaveButton, {
        props: {
            loading: true
        },
        global: {
          stubs: {
              'a-button': {
                template: '<button data-testid="btn" :data-loading="loading"><slot /></button>',
                props: ['loading']
              }
          }
        }
      })
      expect(wrapper.get('[data-testid="btn"]').attributes('data-loading')).toBe('true')
  })

  it('emits click event', async () => {
    const wrapper = mount(SaveButton, {
        global: {
            stubs: {
              'a-button': {
                template: '<button @click="$emit(\'click\', $event)"><slot /></button>'
              }
            }
          }
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })
})
