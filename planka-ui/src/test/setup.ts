import { config } from '@vue/test-utils'

// Mock Arco Design components
config.global.stubs = {
  'a-button': {
    template: '<button class="arco-btn" @click="$emit(\'click\', $event)"><slot name="icon" /><slot /></button>'
  },
  'a-tooltip': {
    template: '<div class="arco-tooltip"><slot /></div>',
    props: ['content', 'position']
  },
  'a-popover': {
    template: '<div class="arco-popover"><slot /><div class="arco-popover-content"><slot name="content" /></div></div>',
    props: ['content', 'position', 'contentStyle']
  },
  'a-dropdown': {
    template: '<div class="arco-dropdown"><slot /><div class="arco-dropdown-content"><slot name="content" /></div></div>',
    props: ['trigger', 'position']
  },
  'a-doption': {
    template: '<div class="arco-dropdown-option" @click="$emit(\'click\')"><slot /></div>'
  },
  'a-space': {
    template: '<div class="arco-space"><slot /></div>'
  },
  'a-spin': {
    template: '<div class="arco-spin"><slot /></div>'
  },
  IconPlusCircle: true,
  IconInfoCircle: true,
  IconLanguage: true
}

// Mock i18n
config.global.mocks = {
  $t: (key: string) => key
}
