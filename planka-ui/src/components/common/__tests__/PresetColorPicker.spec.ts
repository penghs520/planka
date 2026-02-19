import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PresetColorPicker from '../PresetColorPicker.vue'

describe('PresetColorPicker.vue', () => {
  it('renders without color', () => {
    const wrapper = mount(PresetColorPicker, {
      global: {
        stubs: {
          'a-popover': {
            template: '<div><slot /><slot name="content" /></div>'
          },
          'a-button': true,
          'a-color-picker': true
        }
      }
    })

    expect(wrapper.find('.no-color').exists()).toBe(true)
    expect(wrapper.find('.no-color').text()).toBe('无')
    expect(wrapper.find('.color-preview').exists()).toBe(false)
  })

  it('renders with color', () => {
    const color = '#F5222D'
    const wrapper = mount(PresetColorPicker, {
      props: {
        modelValue: color
      },
      global: {
        stubs: {
          'a-popover': {
            template: '<div><slot /><slot name="content" /></div>'
          },
          'a-button': true,
          'a-color-picker': true
        }
      }
    })

    expect(wrapper.find('.no-color').exists()).toBe(false)
    expect(wrapper.find('.color-preview').exists()).toBe(true)
    expect(wrapper.find('.color-preview').attributes('style')).toContain('background-color: rgb(245, 34, 45)') // jsdom normalizes colors
  })

  it('selects preset color', async () => {
    const wrapper = mount(PresetColorPicker, {
      global: {
        stubs: {
          'a-popover': {
            template: '<div><slot /><slot name="content" /></div>'
          },
          'a-button': true,
          'a-color-picker': true
        }
      }
    })

    const presetColors = wrapper.findAll('.color-item')
    expect(presetColors.length).toBeGreaterThan(0)

    await presetColors[0]?.trigger('click')
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')![0]).toEqual(['#F5222D'])
  })

  it('clears color', async () => {
    const wrapper = mount(PresetColorPicker, {
      props: {
        modelValue: '#F5222D'
      },
      global: {
        stubs: {
          'a-popover': {
            template: '<div><slot /><slot name="content" /></div>'
          },
          'a-button': {
            template: '<button @click="$emit(\'click\')"><slot /></button>'
          },
          'a-color-picker': true
        }
      }
    })

    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')![0]).toEqual([''])
  })
})
