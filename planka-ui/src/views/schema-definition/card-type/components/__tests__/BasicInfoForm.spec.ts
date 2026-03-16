import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import BasicInfoForm from '../BasicInfoForm.vue'
import { SchemaSubType } from '@/types/schema'
import type { EntityCardType, AbstractCardType } from '@/types/card-type'
import { EntityState } from '@/types/common'
import { nextTick, reactive } from 'vue'

// Mock components
vi.mock('@/components/card-type/CardTypeSelect.vue', () => ({
  default: { template: '<div></div>' }
}))
vi.mock('@/components/card-type/CardTypeIcon.vue', () => ({
  default: { template: '<div></div>' }
}))
vi.mock('@/components/common/LabelHelpTooltip.vue', () => ({
  default: { template: '<div><slot/></div>' }
}))
vi.mock('../IconColorPicker.vue', () => ({
  default: { template: '<div></div>' }
}))

// Mock useI18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))

// Mock dayjs
vi.mock('dayjs', () => {
  return {
    default: () => ({
      format: (fmt: string) => {
        if (fmt === 'YYYYMMDD') return '20240126'
        if (fmt === 'YYMMDD') return '240126'
        return '2024-01-26'
      }
    })
  }
})

describe('BasicInfoForm.vue', () => {
  const createEntityCardType = (): EntityCardType => ({
    id: '1',
    orgId: 'org1',
    name: 'Test Card Type',
    description: 'Description',
    enabled: true,
    state: EntityState.ACTIVE,
    schemaSubType: SchemaSubType.ENTITY_CARD_TYPE,
    contentVersion: 1,
    code: 'TEST',
    parentTypeIds: []
  })

  const createAbstractCardType = (): AbstractCardType => ({
    id: '2',
    orgId: 'org1',
    name: 'Abstract Card Type',
    description: 'Description',
    enabled: true,
    state: EntityState.ACTIVE,
    schemaSubType: SchemaSubType.TRAIT_CARD_TYPE,
    contentVersion: 1,
    code: 'ABSTRACT'
  })

  it('renders correctly', () => {
    const formData = reactive(createEntityCardType())
    const wrapper = mount(BasicInfoForm, {
      props: {
        formData,
        mode: 'create',
        selectedSubType: SchemaSubType.ENTITY_CARD_TYPE
      },
      global: {
        stubs: {
          'a-form': { template: '<form><slot/></form>' },
          'a-form-item': { template: '<div><slot name="label"/><slot/></div>', props: ['label'] },
          'a-input': true,
          'a-textarea': true,
          'a-radio-group': true,
          'a-radio': true,
          'a-row': { template: '<div><slot/></div>' },
          'a-col': { template: '<div><slot/></div>' },
          'a-space': { template: '<div><slot/></div>' },
          'a-switch': { template: '<button @click="$emit(\'update:modelValue\', !$attrs.modelValue)"><slot name="checked" v-if="$attrs.modelValue"/><slot name="unchecked" v-else/></button>' },
          'a-select': true,
          'a-input-number': true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
    // Basic fields check
    expect(wrapper.findComponent({ name: 'a-input' }).exists()).toBe(true)
    expect(wrapper.text()).toContain('admin.cardType.form.subType')
  })

  it('Code Generation Rule section is visible ONLY when schemaSubType is ENTITY_CARD_TYPE', () => {
    const formData = reactive(createEntityCardType())
    const wrapper = mount(BasicInfoForm, {
      props: {
        formData,
        mode: 'edit'
      },
      global: {
        stubs: {
          'a-form': { template: '<form><slot/></form>' },
          'a-form-item': { template: '<div class="form-item" :label="label"><slot name="label"/><span>{{label}}</span><slot/></div>', props: ['label'] },
          'a-input': true,
          'a-textarea': true,
          'a-radio-group': true,
          'a-radio': true,
          'a-row': { template: '<div><slot/></div>' },
          'a-col': { template: '<div><slot/></div>' },
          'a-space': { template: '<div><slot/></div>' },
          'a-switch': true,
          'a-select': true,
          'a-input-number': true
        }
      }
    })

    // Find the form item with label "admin.cardType.form.codeGenerationRule"
    const formItems = wrapper.findAll('.form-item')
    const codeGenItem = formItems.find(item => item.text().includes('admin.cardType.form.codeGenerationRule'))

    expect(codeGenItem?.exists()).toBe(true)
  })

  it('Code Generation Rule section is HIDDEN when schemaSubType is TRAIT_CARD_TYPE', () => {
    const formData = reactive(createAbstractCardType())
    const wrapper = mount(BasicInfoForm, {
      props: {
        formData,
        mode: 'edit'
      },
      global: {
        stubs: {
          'a-form': { template: '<form><slot/></form>' },
          'a-form-item': { template: '<div class="form-item"><slot name="label"/><span>{{label}}</span><slot/></div>', props: ['label'] },
          'a-input': true,
          'a-textarea': true,
          'a-radio-group': true,
          'a-radio': true,
          'a-row': { template: '<div><slot/></div>' },
          'a-col': { template: '<div><slot/></div>' },
          'a-space': { template: '<div><slot/></div>' },
          'a-switch': true,
          'a-select': true,
          'a-input-number': true
        }
      }
    })

    const formItems = wrapper.findAll('.form-item')
    const codeGenItem = formItems.find(item => item.text().includes('admin.cardType.form.codeGenerationRule'))

    expect(codeGenItem).toBeUndefined()
  })

  it('Enabling rule switch shows configuration fields', async () => {
    const formData = reactive(createEntityCardType())
    const wrapper = mount(BasicInfoForm, {
      props: {
        formData,
        mode: 'edit'
      },
      global: {
        stubs: {
          'a-form': { template: '<form><slot/></form>' },
          'a-form-item': { template: '<div class="form-item"><slot name="label"/><span>{{label}}</span><slot/></div>', props: ['label'] },
          'a-input': true,
          'a-textarea': true,
          'a-radio-group': true,
          'a-radio': true,
          'a-row': { template: '<div><slot/></div>' },
          'a-col': { template: '<div><slot/></div>' },
          'a-space': { template: '<div><slot/></div>' },
          'a-switch': {
            template: '<button class="switch" @click="toggle"><slot/></button>',
            props: ['modelValue'],
            emits: ['update:modelValue'],
            methods: {
              toggle() {
                this.$emit('update:modelValue', !this.modelValue)
              }
            }
          },
          'a-select': true,
          'a-input-number': true
        }
      }
    })

    // Initially rule config panel should not be visible
    expect(wrapper.find('.rule-config-panel').exists()).toBe(false)

    // Toggle switch
    await wrapper.find('.switch').trigger('click')

    // Panel should be visible now
    expect(wrapper.find('.rule-config-panel').exists()).toBe(true)

    // Check if configuration fields exist
    const panel = wrapper.find('.rule-config-panel')
    expect(panel.findAllComponents({ name: 'a-input' }).length).toBeGreaterThan(0) // Prefix, connector
    expect(panel.findComponent({ name: 'a-select' }).exists()).toBe(true) // Date format
    expect(panel.findComponent({ name: 'a-input-number' }).exists()).toBe(true) // Sequence length
  })

  it('Preview code generation logic matches inputs', async () => {
    const formData = reactive(createEntityCardType())
    // Pre-populate with rule
    formData.codeGenerationRule = {
      prefix: 'TASK-',
      dateFormat: 'yyyyMMdd',
      dateSequenceConnector: '-',
      sequenceLength: 4
    }

    const wrapper = mount(BasicInfoForm, {
      props: {
        formData,
        mode: 'edit'
      },
      global: {
        stubs: {
          'a-form': { template: '<form><slot/></form>' },
          'a-form-item': { template: '<div class="form-item"><slot name="label"/><span>{{label}}</span><slot/></div>', props: ['label'] },
          'a-input': true,
          'a-textarea': true,
          'a-radio-group': true,
          'a-radio': true,
          'a-row': { template: '<div><slot/></div>' },
          'a-col': { template: '<div><slot/></div>' },
          'a-space': { template: '<div><slot/></div>' },
          'a-switch': {
            template: '<button class="switch" @click="$emit(\'update:modelValue\', true)"></button>',
            props: ['modelValue']
          },
          'a-select': true,
          'a-input-number': true
        }
      }
    })

    await nextTick()

    const previewBox = wrapper.find('.preview-box .code')
    expect(previewBox.exists()).toBe(true)
    const expectedCode = 'TASK-20240126-0001'
    expect(previewBox.text()).toBe(expectedCode)

    // Modify inputs and check update
    // Use reactive object mutation
    if (formData.codeGenerationRule) {
      formData.codeGenerationRule.prefix = 'BUG-'
      formData.codeGenerationRule.dateFormat = 'yyMMdd'
      formData.codeGenerationRule.sequenceLength = 3
    }

    // We need to wait for watchers to run
    await nextTick()

    const newExpectedCode = 'BUG-240126-001'
    expect(previewBox.text()).toBe(newExpectedCode)
  })
})
