<script setup lang="ts">
import { computed } from 'vue'
import { IconMore } from '@arco-design/web-vue/es/icon'
import type { ActionItem } from '@/types/table'

interface Props {
  /** 操作项列表 */
  actions: ActionItem[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  /** 操作触发事件 */
  (e: 'action', key: string): void
}>()

// 过滤出可见的操作项
const visibleActions = computed(() => {
  return props.actions.filter((action) => action.visible !== false)
})

function handleClick(key: string) {
  emit('action', key)
}
</script>

<template>
  <a-dropdown trigger="click" @click.stop>
    <a-button type="text" size="mini" @click.stop>
      <template #icon><IconMore /></template>
    </a-button>
    <template #content>
      <template v-for="action in visibleActions" :key="action.key">
        <a-divider v-if="action.divider" :margin="4" />
        <a-doption
          :class="{ 'danger-option': action.danger }"
          :disabled="action.disabled"
          @click="handleClick(action.key)"
        >
          <template v-if="action.icon" #icon>
            <component :is="action.icon" />
          </template>
          {{ action.label }}
        </a-doption>
      </template>
    </template>
  </a-dropdown>
</template>
