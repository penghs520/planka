import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import HighlightText from '../HighlightText.vue'

describe('HighlightText.vue', () => {
  it('renders text without highlight when no keyword provided', () => {
    const wrapper = mount(HighlightText, {
      props: {
        text: 'Hello World'
      }
    })
    expect(wrapper.text()).toBe('Hello World')
    expect(wrapper.find('.highlight-text').exists()).toBe(false)
  })

  it('renders empty when no text provided', () => {
    const wrapper = mount(HighlightText)
    expect(wrapper.text()).toBe('')
  })

  it('highlights keyword correctly', () => {
    const wrapper = mount(HighlightText, {
      props: {
        text: 'Hello World',
        keyword: 'Hello'
      }
    })
    expect(wrapper.text()).toBe('Hello World')
    const highlight = wrapper.find('.highlight-text')
    expect(highlight.exists()).toBe(true)
    expect(highlight.text()).toBe('Hello')
  })

  it('highlights keyword case insensitively', () => {
    const wrapper = mount(HighlightText, {
      props: {
        text: 'Hello World',
        keyword: 'hello'
      }
    })
    const highlight = wrapper.find('.highlight-text')
    expect(highlight.exists()).toBe(true)
    expect(highlight.text()).toBe('Hello')
  })

  it('highlights multiple occurrences', () => {
    const wrapper = mount(HighlightText, {
      props: {
        text: 'Hello Hello',
        keyword: 'Hello'
      }
    })
    const highlights = wrapper.findAll('.highlight-text')
    expect(highlights.length).toBe(2)
  })

  it('escapes special regex characters in keyword', () => {
    const wrapper = mount(HighlightText, {
      props: {
        text: 'Hello (World)',
        keyword: '('
      }
    })
    const highlight = wrapper.find('.highlight-text')
    expect(highlight.exists()).toBe(true)
    expect(highlight.text()).toBe('(')
  })
})
