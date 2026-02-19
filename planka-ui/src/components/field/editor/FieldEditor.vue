<script setup lang="ts">
/**
 * 字段值编辑分发组件
 */
import { computed } from 'vue'
import type { FieldRenderConfig, NumberRenderConfig, EnumRenderConfig, LinkRenderConfig } from '@/types/view-data'
import FieldTextEditor from './FieldTextEditor.vue'
import FieldNumberEditor from './FieldNumberEditor.vue'
import FieldDateEditor from './FieldDateEditor.vue'
import FieldWebUrlEditor from './FieldWebUrlEditor.vue'
import FieldEnumEditor from './FieldEnumEditor.vue'
import FieldLinkEditor from './FieldLinkEditor.vue'
import FieldStructureEditor from './FieldStructureEditor.vue'
import FieldMarkdownEditor from './FieldMarkdownEditor.vue'
import { isValidLinkFieldId } from '@/utils/link-field-utils'

const props = defineProps<{
  fieldType?: string
  fieldId?: string
  renderConfig?: FieldRenderConfig | null
  placeholder?: string
  disabled?: boolean
  autoFocus?: boolean
  /** 固定高度（px），用于描述等特殊字段 */
  height?: number
}>()

const modelValue = defineModel<any>('modelValue', { required: true })

const emit = defineEmits<{
  save: []
  cancel: []
  keydown: [event: KeyboardEvent]
  blur: [event: FocusEvent]
  'popup-visible-change': [visible: boolean]
}>()

const isTextType = computed(() => {
  const type = props.fieldType
  return type === 'TEXT' || type === 'TEXTAREA' || !type
})

const linkFieldId = computed<string | null>(() => {
  if (!props.fieldId) return null
  return isValidLinkFieldId(props.fieldId) ? props.fieldId : null
})

function handleSave() {
  emit('save')
}

function handleCancel() {
  emit('cancel')
}

function handlePopupVisibleChange(visible: boolean) {
  emit('popup-visible-change', visible)
}
</script>

<template>
  <FieldTextEditor
    v-if="isTextType"
    v-model="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :auto-focus="autoFocus"
    @keydown="emit('keydown', $event)"
    @blur="handleSave" 
  />
  <FieldNumberEditor
    v-else-if="fieldType === 'NUMBER'"
    v-model="modelValue"
    :render-config="renderConfig as NumberRenderConfig"
    :placeholder="placeholder"
    :disabled="disabled"
    :auto-focus="autoFocus"
    @keydown="emit('keydown', $event)"
    @blur="handleSave"
  />
  <FieldDateEditor
    v-else-if="fieldType === 'DATE'"
    v-model="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :auto-focus="autoFocus"
    @save="handleSave"
    @popup-visible-change="handlePopupVisibleChange"
  />
  <FieldWebUrlEditor
    v-else-if="fieldType === 'WEB_URL'"
    v-model="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :auto-focus="autoFocus"
    @keydown="emit('keydown', $event)"
    @blur="handleSave"
  />
  <FieldEnumEditor
    v-else-if="fieldType === 'ENUM'"
    v-model="modelValue"
    :render-config="renderConfig as EnumRenderConfig"
    :placeholder="placeholder"
    :disabled="disabled"
    @save="handleSave"
    @popup-visible-change="handlePopupVisibleChange"
  />
  <FieldLinkEditor
    v-else-if="fieldType === 'LINK' && linkFieldId"
    v-model:selected-cards="modelValue"
    :link-field-id="linkFieldId"
    :render-config="renderConfig as LinkRenderConfig"
    :saving="disabled"

    @save="handleSave"
    @cancel="handleCancel"
  />
  <FieldStructureEditor
    v-else-if="fieldType === 'STRUCTURE' && fieldId"
    v-model="modelValue"
    :field-id="fieldId"
    :placeholder="placeholder"
    :disabled="disabled"
    :auto-open="autoFocus"
    @change="handleSave"
    @cancel="handleCancel"
  />
  <FieldMarkdownEditor
    v-else-if="fieldType === 'MARKDOWN'"
    v-model="modelValue"
    :placeholder="placeholder"
    :disabled="disabled"
    :height="height"
    @save="handleSave"
    @cancel="handleCancel"
  />
  <span v-else class="field-editor-fallback">
    <!-- Fallback or warning -->
    不支持的字段类型: {{ fieldType }}
  </span>
</template>

<style scoped>
.field-editor-fallback {
  color: var(--color-text-3);
  font-size: 12px;
}
</style>
