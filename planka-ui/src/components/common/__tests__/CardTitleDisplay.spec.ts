import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import CardTitleDisplay from '../CardTitleDisplay.vue'
// Remove type import if not used or causing issues with type checking in tests for now

// Manually define JointTitleArea if not exported or use string literals
const JointTitleArea = {
  PREFIX: 'PREFIX',
  SUFFIX: 'SUFFIX'
}

describe('CardTitleDisplay.vue', () => {
  it('renders fallback when no title', () => {
    const wrapper = mount(CardTitleDisplay, {
      props: {
        title: null,
        fallback: 'No Title'
      }
    })
    expect(wrapper.text()).toBe('No Title')
  })

  it('renders fallback default value', () => {
      const wrapper = mount(CardTitleDisplay, {
        props: {
          title: null
        }
      })
      expect(wrapper.text()).toBe('-')
    })

  it('renders PURE title', () => {
    const title = {
      type: 'PURE',
      value: 'My Title',
      displayValue: 'My Title'
    } as any
    const wrapper = mount(CardTitleDisplay, {
      props: { title }
    })
    expect(wrapper.text()).toBe('My Title')
  })

  it('renders JOINT title with prefix', () => {
    const title = {
      type: 'JOINT',
      area: JointTitleArea.PREFIX,
      value: 'Task 1',
      multiParts: [
        { parts: [{ name: '[FE]' }] },
        { parts: [{ name: '[Bug]' }] }
      ]
    } as any
    const wrapper = mount(CardTitleDisplay, {
      props: { title }
    })
    expect(wrapper.text()).toBe('[FE] [Bug]Task 1')
  })

  it('renders JOINT title with suffix', () => {
    const title = {
        type: 'JOINT',
        area: JointTitleArea.SUFFIX,
        value: 'Task 1',
        multiParts: [
          { parts: [{ name: '[FE]' }] }
        ]
      } as any
      const wrapper = mount(CardTitleDisplay, {
        props: { title }
      })
      // Based on template: name + parts
      expect(wrapper.text()).toBe('Task 1[FE]')
  })

  it('renders displayValue as fallback for other types', () => {
      const title = {
          type: 'UNKNOWN',
          displayValue: 'Fallback Display'
      } as any
      const wrapper = mount(CardTitleDisplay, {
          props: { title }
      })
      expect(wrapper.text()).toBe('Fallback Display')
  })
})
