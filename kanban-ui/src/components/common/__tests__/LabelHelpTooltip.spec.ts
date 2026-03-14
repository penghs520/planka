import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LabelHelpTooltip from '../LabelHelpTooltip.vue'
// Remove unused import to fix lint error
// import { IconInfoCircle } from '@arco-design/web-vue/es/icon'

describe('LabelHelpTooltip.vue', () => {
  it('renders simple tooltip', () => {
    const wrapper = mount(LabelHelpTooltip, {
      props: {
        simple: true,
        content: 'Help text'
      },
      global: {
        stubs: {
          'a-tooltip': {
            name: 'a-tooltip',
            template: '<div class="arco-tooltip"><slot /></div>',
            props: ['content']
          },
          'a-popover': {
            name: 'a-popover',
            template: '<div class="arco-popover"><slot /></div>'
          },
          IconInfoCircle: { template: '<span class="icon-info-circle"></span>' }
        }
      }
    })

    // Check if simple prop logic is working - it should render a-tooltip and NOT a-popover
    // Using findComponent is reliable if stubs are correct
    expect(wrapper.findComponent({ name: 'a-tooltip' }).exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'a-popover' }).exists()).toBe(false)
  })

  it('renders popover by default', () => {
    const wrapper = mount(LabelHelpTooltip, {
      props: {
        title: 'Title',
        content: 'Content'
      },
      global: {
        stubs: {
          'a-tooltip': {
            name: 'a-tooltip',
            template: '<div class="arco-tooltip"><slot /></div>'
          },
          'a-popover': {
            name: 'a-popover',
            template: '<div class="arco-popover"><slot /><div class="popover-content-slot"><slot name="content" /></div></div>'
          },
          IconInfoCircle: { template: '<span class="icon-info-circle"></span>' }
        }
      }
    })
    expect(wrapper.findComponent({ name: 'a-popover' }).exists()).toBe(true)
    // The title and content are rendered inside the popover content slot, which we mock
    expect(wrapper.find('.popover-title').text()).toBe('Title')
    expect(wrapper.find('.popover-content').html()).toContain('Content')
  })

  it('renders slot content in popover', () => {
      const wrapper = mount(LabelHelpTooltip, {
        slots: {
            default: '<div class="custom">Custom Content</div>'
        },
        global: {
          stubs: {
              'a-tooltip': {
                name: 'a-tooltip',
                template: '<div class="arco-tooltip"><slot /></div>'
              },
              'a-popover': {
                  name: 'a-popover',
                  template: '<div class="arco-popover"><slot /><div class="popover-content-slot"><slot name="content" /></div></div>'
              },
              IconInfoCircle: { template: '<span class="icon-info-circle"></span>' }
          }
        }
      })
      expect(wrapper.find('.custom').exists()).toBe(true)
      expect(wrapper.find('.custom').text()).toBe('Custom Content')
  })
})
