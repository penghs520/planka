<script setup lang="ts">
/**
 * 表格右键菜单组件
 * 支持新增/删除行列操作
 */
import { useI18n } from 'vue-i18n'
import type { Editor } from '@tiptap/vue-3'

const { t } = useI18n()

defineProps<{
  editor: Editor
}>()

const emit = defineEmits<{
  close: []
}>()

// 菜单项配置
const menuItems = [
  { key: 'addRowBefore', icon: '↑', action: 'addRowBefore' },
  { key: 'addRowAfter', icon: '↓', action: 'addRowAfter' },
  { key: 'deleteRow', icon: '−', action: 'deleteRow', danger: true },
  { key: 'divider' },
  { key: 'addColumnBefore', icon: '←', action: 'addColumnBefore' },
  { key: 'addColumnAfter', icon: '→', action: 'addColumnAfter' },
  { key: 'deleteColumn', icon: '−', action: 'deleteColumn', danger: true },
  { key: 'divider2' },
  { key: 'deleteTable', icon: '✕', action: 'deleteTable', danger: true },
]

function executeAction(action: string, editor: Editor) {
  switch (action) {
    case 'addRowBefore':
      editor.chain().focus().addRowBefore().run()
      break
    case 'addRowAfter':
      editor.chain().focus().addRowAfter().run()
      break
    case 'deleteRow':
      editor.chain().focus().deleteRow().run()
      break
    case 'addColumnBefore':
      editor.chain().focus().addColumnBefore().run()
      break
    case 'addColumnAfter':
      editor.chain().focus().addColumnAfter().run()
      break
    case 'deleteColumn':
      editor.chain().focus().deleteColumn().run()
      break
    case 'deleteTable':
      editor.chain().focus().deleteTable().run()
      break
  }
  emit('close')
}
</script>

<template>
  <div class="table-context-menu" @mousedown.prevent>
    <template v-for="item in menuItems" :key="item.key">
      <div v-if="item.key.startsWith('divider')" class="menu-divider" />
      <button
        v-else
        type="button"
        class="menu-item"
        :class="{ danger: item.danger }"
        @click="executeAction(item.action!, editor)"
      >
        <span class="menu-icon">{{ item.icon }}</span>
        <span class="menu-label">{{ t(`common.editor.tableOperations.${item.action}`) }}</span>
      </button>
    </template>
  </div>
</template>

<style scoped lang="scss">
.table-context-menu {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 4px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  min-width: 160px;
  z-index: 200;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 12px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--color-text-1);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;

  &:hover {
    background: var(--color-fill-2);
  }

  &.danger {
    color: rgb(var(--danger-6));

    &:hover {
      background: rgba(var(--danger-6), 0.1);
    }
  }
}

.menu-icon {
  width: 16px;
  text-align: center;
  font-size: 14px;
}

.menu-label {
  flex: 1;
}

.menu-divider {
  height: 1px;
  background: var(--color-border);
  margin: 4px 8px;
}
</style>
