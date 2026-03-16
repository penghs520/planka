import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BasicInfoForm from './BasicInfoForm.vue'
import { SchemaSubType } from '@/types/schema'
import type { EntityCardType } from '@/types/card-type'
import { EntityState } from '@/types/common'

// Mock dependencies
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

vi.mock('@/api/card-type', () => ({
  cardTypeApi: {
    getFields: vi.fn().mockResolvedValue([]),
    getFieldOptions: vi.fn().mockResolvedValue([]),
  },
}))

describe('BasicInfoForm.vue', () => {
  const createConcreteFormData = (): EntityCardType => ({
    id: 'test-id',
    orgId: 'test-org',
    name: 'Test Card Type',
    description: 'Test Description',
    enabled: true,
    contentVersion: 1,
    state: EntityState.ACTIVE,
    schemaSubType: SchemaSubType.ENTITY_CARD_TYPE,
    parentTypeIds: [],
  })

  it('renders correctly', () => {
    const wrapper = mount(BasicInfoForm, {
      props: {
        formData: createConcreteFormData(),
        mode: 'edit',
      },
      global: {
        stubs: {
          CardTypeSelect: true,
          CardTypeIcon: true,
          LabelHelpTooltip: true,
          PathFieldSelector: true,
          IconColorPicker: true,
          'a-form': true,
          'a-form-item': true,
          'a-input': true,
          'a-textarea': true,
          'a-switch': true,
          'a-space': true,
          'a-row': true,
          'a-col': true,
          'a-select': true,
          'a-input-number': true,
          'a-radio-group': true,
          'a-radio': true,
          'VueDraggable': true,
          'icon-menu': true,
          'icon-delete': true,
          'icon-plus': true,
          'a-button': true
        },
      },
    })

    expect(wrapper.exists()).toBe(true)
  })

  it('initializes title composition rule when enabled', async () => {
    const formData = createConcreteFormData()
    const wrapper = mount(BasicInfoForm, {
      props: {
        formData,
        mode: 'edit',
      },
      global: {
        stubs: {
          CardTypeSelect: true,
          CardTypeIcon: true,
          LabelHelpTooltip: true,
          PathFieldSelector: true,
          IconColorPicker: true,
          'a-form': {
            template: '<form><slot /></form>'
          },
          'a-form-item': {
            template: '<div><slot name="label" /><slot /></div>',
            props: ['label']
          },
          'a-input': true,
          'a-textarea': true,
          'a-switch': {
            template: '<div class="a-switch" @click="$emit(\'update:modelValue\', !modelValue)"></div>',
            props: ['modelValue'],
            emits: ['update:modelValue']
          },
          'a-space': {
            template: '<div><slot /></div>'
          },
          'a-row': true,
          'a-col': true,
          'a-select': true,
          'a-input-number': true,
          'a-radio-group': true,
          'a-radio': true,
          'draggable': true,
          'icon-menu': true,
          'icon-delete': true,
          'icon-plus': true,
          'a-button': true
        },
      },
    })

    // Find the switches
    // In test environment, it might take a tick for watchers to settle
    await wrapper.vm.$nextTick()

    const switches = wrapper.findAll('.a-switch')
    // With stubs, findComponent might behave differently depending on how deep the tree renders
    // But since we stubbed a-switch, we should find them.

    expect(switches.length).toBeGreaterThan(0)

    // The Title Composition switch is the last one
    const titleSwitch = switches[switches.length - 1]

    // Toggle Title Composition switch
    if (titleSwitch) {
      await titleSwitch.trigger('click')
    }

    expect(formData.titleCompositionRule).toBeDefined()
    expect(formData.titleCompositionRule?.enabled).toBe(true)
    expect(formData.titleCompositionRule?.area).toBe('PREFIX')
    expect(formData.titleCompositionRule?.parts).toEqual([])
  })

  it('adds and removes title parts', async () => {
    const formData = createConcreteFormData()
    const wrapper = mount(BasicInfoForm, {
      props: {
        formData,
        mode: 'edit',
      },
      global: {
        stubs: {
          CardTypeSelect: true,
          CardTypeIcon: true,
          LabelHelpTooltip: true,
          PathFieldSelector: true,
          IconColorPicker: true,
          'a-form': { template: '<form><slot /></form>' },
          'a-form-item': { template: '<div><slot name="label" /><slot /></div>', props: ['label'] },
          'a-input': true,
          'a-textarea': true,
          'a-switch': {
            template: '<div class="a-switch" @click="$emit(\'update:modelValue\', !modelValue)"></div>',
            props: ['modelValue'],
            emits: ['update:modelValue']
          },
          'a-space': { template: '<div><slot /></div>' },
          'a-row': true,
          'a-col': true,
          'a-select': true,
          'a-input-number': true,
          'a-radio-group': true,
          'a-radio': true,
          'draggable': true,
          'icon-menu': true,
          'icon-delete': true,
          'icon-plus': true,
          'a-button': true,
          'VueDraggable': { template: '<div><slot /></div>' }
        },
      },
    })

    await wrapper.vm.$nextTick()
    const switches = wrapper.findAll('.a-switch')
    const titleSwitch = switches[switches.length - 1]
    if (titleSwitch) {
      await titleSwitch.trigger('click')
    }

    // Find PathFieldSelector and emit select
    const selector = wrapper.findComponent({ name: 'PathFieldSelector' })
    expect(selector.exists()).toBe(true)

    // Add a field
    const fieldSelection = {
      fieldId: 'field_1',
      path: { linkNodes: [] }
    }
    await selector.vm.$emit('select', fieldSelection)

    expect(formData.titleCompositionRule?.parts).toHaveLength(1)
    expect(formData.titleCompositionRule?.parts[0]).toEqual({ fieldId: 'field_1', path: undefined })
  })
})
