<script setup lang="ts">
/**
 * 链接字段显示组件
 */
import { computed } from 'vue'

const props = defineProps<{
  value?: string | null
  placeholder?: string
}>()

const url = computed(() => {
  return props.value || ''
})

const displayValue = computed(() => {
  return props.value || props.placeholder || '-'
})

const hasValue = computed(() => !!props.value)

function handleClick(e: MouseEvent) {
  if (hasValue.value) {
    e.stopPropagation()
  }
}
</script>

<template>
  <a
v-if="hasValue" 
     :href="url" 
     target="_blank" 
     rel="noopener noreferrer" 
     class="field-web-url-display"
     @click="handleClick"
  >
    {{ displayValue }}
  </a>
  <span v-else class="field-web-url-empty">{{ displayValue }}</span>
</template>

<style scoped>
.field-web-url-display {
  color: rgb(var(--primary-6));
  text-decoration: none;
  cursor: pointer;
}

.field-web-url-display:hover {
  text-decoration: underline;
}

.field-web-url-empty {
  color: inherit;
}
</style>
