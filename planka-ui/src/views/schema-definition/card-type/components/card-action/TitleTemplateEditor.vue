<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FieldOption } from '@/types/field-option'

const { t } = useI18n()

const props = defineProps<{
  /** 可选字段列表（用于插入字段引用） */
  fieldOptions: FieldOption[]
}>()

const modelValue = defineModel<string>({ default: '' })

const inputRef = ref<HTMLTextAreaElement>()
const dropdownVisible = ref(false)

// 系统字段选项
const systemFields = computed(() => [
  { id: '$title', name: t('admin.cardAction.titleTemplate.systemTitle') },
  { id: '$id', name: t('admin.cardAction.titleTemplate.systemId') },
])

// 合并字段选项
const allFieldOptions = computed(() => [
  ...systemFields.value,
  ...props.fieldOptions,
])

// 插入字段引用
function insertFieldReference(fieldId: string) {
  const expression = `\${${fieldId}}`

  if (inputRef.value) {
    const textarea = inputRef.value
    const start = textarea.selectionStart || 0
    const end = textarea.selectionEnd || 0
    const currentValue = modelValue.value || ''

    // 在光标位置插入表达式
    const newValue = currentValue.slice(0, start) + expression + currentValue.slice(end)
    modelValue.value = newValue

    // 重新设置光标位置
    setTimeout(() => {
      const newPos = start + expression.length
      textarea.setSelectionRange(newPos, newPos)
      textarea.focus()
    }, 0)
  } else {
    // 如果没有 ref，追加到末尾
    modelValue.value = (modelValue.value || '') + expression
  }

  dropdownVisible.value = false
}
</script>

<template>
  <div class="title-template-editor">
    <div class="editor-container">
      <a-textarea
        ref="inputRef"
        v-model="modelValue"
        :placeholder="t('admin.cardAction.titleTemplate.placeholder')"
        :auto-size="{ minRows: 2, maxRows: 4 }"
      />
    </div>

    <div class="editor-toolbar">
      <a-dropdown
        v-model:popup-visible="dropdownVisible"
        trigger="click"
        position="bl"
      >
        <a-button type="text" size="small">
          <template #icon>
            <icon-plus />
          </template>
          {{ t('admin.cardAction.titleTemplate.insertField') }}
        </a-button>
        <template #content>
          <a-doption
            v-for="field in allFieldOptions"
            :key="field.id"
            @click="insertFieldReference(field.id)"
          >
            {{ field.name }}
            <span class="field-id-hint">${{ '{' + field.id + '}' }}</span>
          </a-doption>
        </template>
      </a-dropdown>

      <span class="hint-text">
        {{ t('admin.cardAction.titleTemplate.hint') }}
      </span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.title-template-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.editor-container {
  width: 100%;
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint-text {
  font-size: 12px;
  color: var(--color-text-3);
}

.field-id-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--color-text-4);
  font-family: monospace;
}
</style>
