<script setup lang="ts">
import { computed } from 'vue'

export interface ToggleOption {
  label: string
  value: string | number | boolean
}

interface Props {
  modelValue: string | number | boolean
  options: ToggleOption[]
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number | boolean]
}>()

const selectedValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
})

function handleSelect(value: string | number | boolean): void {
  if (!props.disabled) {
    selectedValue.value = value
  }
}
</script>

<template>
  <div class="toggle-group" :class="{ disabled }">
    <button
      v-for="option in options"
      :key="String(option.value)"
      type="button"
      class="toggle-btn"
      :class="{ active: selectedValue === option.value }"
      :disabled="disabled"
      @click="handleSelect(option.value)"
    >
      {{ option.label }}
    </button>
  </div>
</template>

<style scoped>
.toggle-group {
  display: inline-flex;
  background: var(--color-fill-1);
  border-radius: 8px;
  padding: 3px;
  gap: 2px;
}

.toggle-group.disabled {
  pointer-events: none;
}

.toggle-group.disabled .toggle-btn {
  color: var(--color-text-3);
}

.toggle-group.disabled .toggle-btn.active {
  color: var(--color-text-2);
  background: var(--color-bg-1);
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
}

.toggle-btn {
  position: relative;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-2);
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
}

.toggle-btn:hover:not(:disabled):not(.active) {
  color: var(--color-text-1);
  background: rgba(255, 255, 255, 0.5);
}

.toggle-btn.active {
  color: var(--color-primary);
  background: #fff;
  box-shadow: 0 1px 3px rgba(31, 35, 41, 0.08), 0 1px 2px rgba(31, 35, 41, 0.06);
}

.toggle-btn:disabled {
  cursor: not-allowed;
}
</style>
