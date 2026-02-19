<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { VueDraggable } from 'vue-draggable-plus'
import { IconPlus, IconDragDotVertical } from '@arco-design/web-vue/es/icon'
import LabelHelpTooltip from '@/components/common/LabelHelpTooltip.vue'
import PresetColorPicker from '@/components/common/PresetColorPicker.vue'
import type { EnumOptionDTO } from '@/types/view-data'

interface Props {
  multiSelect: boolean
  options: EnumOptionDTO[]
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
})

const emit = defineEmits<{
  'update:multiSelect': [value: boolean]
  'update:options': [value: EnumOptionDTO[]]
}>()

const { t } = useI18n()

const multiSelectValue = computed({
  get: () => props.multiSelect,
  set: (value: boolean) => emit('update:multiSelect', value),
})

const dragOptions = {
  animation: 200,
  handle: '.drag-handle',
  ghostClass: 'enum-option-ghost',
}

const localEnumOptions = computed({
  get: () => props.options || [],
  set: (val: EnumOptionDTO[]) => {
    emit('update:options', val.map((opt, index) => ({
      ...opt,
      order: index,
    })))
  },
})

function addEnumOption(): void {
  const options = props.options || []
  const newOption: EnumOptionDTO = {
    id: `opt_${Date.now()}`,
    value: '',
    label: '',
    order: options.length,
    enabled: true,
  }
  emit('update:options', [...options, newOption])
}

function removeEnumOption(index: number): void {
  const options = [...props.options]
  options.splice(index, 1)
  emit('update:options', options)
}

function updateEnumOption(index: number, key: keyof EnumOptionDTO, value: unknown): void {
  const options = [...props.options]
  if (!options[index]) return
  options[index] = { ...options[index], [key]: value } as EnumOptionDTO
  emit('update:options', options)
}

const enumOptionHelpContent = computed(() => `
<p><strong>${t('admin.cardType.fieldConfig.optionValue')}：</strong>${t('admin.cardType.fieldConfig.enumOptionValueDesc')}</p>
<p><strong>${t('admin.cardType.fieldConfig.displayLabel')}：</strong>${t('admin.cardType.fieldConfig.enumOptionLabelDesc')}</p>
<p style="margin-top: 8px; color: #ad6800">
  <strong>${t('admin.cardType.fieldConfig.notice')}：</strong>${t('admin.cardType.fieldConfig.enumOptionNotice')}
</p>
`)
</script>

<template>
  <div class="enum-field-config">
    <div class="section-header">
      <span class="section-title">{{ t('admin.cardType.fieldConfig.enumConfig') }}</span>
    </div>

    <div class="config-row">
      <a-checkbox v-model="multiSelectValue" :disabled="disabled">
        {{ t('admin.cardType.fieldConfig.multiSelect') }}
      </a-checkbox>
    </div>

    <div class="config-row">
      <div class="field-label-with-help">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.enumOptions') }}</label>
        <LabelHelpTooltip
          :title="t('admin.cardType.fieldConfig.enumOptionsHelp')"
          :content="enumOptionHelpContent"
        />
      </div>
      <div class="enum-options">
      <VueDraggable
        v-model="localEnumOptions"
        v-bind="dragOptions"
        class="enum-options-list"
        :disabled="disabled"
      >
        <div
          v-for="(option, index) in localEnumOptions"
          :key="option.id"
          class="enum-option-row"
        >
          <span class="drag-handle" :class="{ disabled }">
            <IconDragDotVertical />
          </span>
          <a-input
            :model-value="option.value"
            :placeholder="t('admin.cardType.fieldConfig.optionValue')"
            style="width: 100px"
            :disabled="disabled"
            @update:model-value="updateEnumOption(index, 'value', $event)"
          />
          <a-input
            :model-value="option.label"
            :placeholder="t('admin.cardType.fieldConfig.displayLabel')"
            style="flex: 1"
            :disabled="disabled"
            @update:model-value="updateEnumOption(index, 'label', $event)"
          />
          <PresetColorPicker
            :model-value="option.color"
            :disabled="disabled"
            @update:model-value="updateEnumOption(index, 'color', $event)"
          />
          <a-switch
            :model-value="option.enabled"
            size="small"
            :disabled="disabled"
            @update:model-value="updateEnumOption(index, 'enabled', $event)"
          />
          <a-button
            type="text"
            status="danger"
            size="mini"
            :disabled="disabled"
            @click="removeEnumOption(index)"
          >
            <template #icon>
              <icon-delete />
            </template>
          </a-button>
        </div>
      </VueDraggable>
      <a-button type="dashed" long :disabled="disabled" @click="addEnumOption">
        <template #icon>
          <IconPlus />
        </template>
        {{ t('admin.cardType.fieldConfig.addOption') }}
      </a-button>
    </div>
    </div>
  </div>
</template>

<style scoped>
.enum-field-config {
  margin-top: 24px;
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-1);
  letter-spacing: 0.02em;
}

.config-row {
  margin-bottom: 16px;
}

.field-label-with-help {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
}

.enum-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.enum-options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.enum-option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-bg-2);
  padding: 4px 8px;
  border-radius: 4px;
  transition: box-shadow 0.2s;
}

.enum-option-row:hover {
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.drag-handle {
  cursor: grab;
  color: var(--color-text-3);
  display: flex;
  align-items: center;
}

.drag-handle:hover {
  color: var(--color-text-2);
}

.drag-handle:active {
  cursor: grabbing;
}

.drag-handle.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.enum-option-ghost {
  opacity: 0.5;
  background: var(--color-primary-light-4);
}
</style>
