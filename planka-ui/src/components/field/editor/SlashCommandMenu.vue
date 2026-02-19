<script setup lang="ts">
/**
 * 斜杠命令菜单组件
 * 输入 / 后显示命令列表，支持键盘导航
 * 自动检测位置，当底部空间不足时在上方显示
 */
import { ref, computed, onMounted, onUnmounted, nextTick, type Component } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  IconH1,
  IconH2,
  IconH3,
  IconUnorderedList,
  IconOrderedList,
  IconCheckSquare,
  IconQuote,
  IconCode,
  IconApps,
  IconImage,
  IconMinus,
} from '@arco-design/web-vue/es/icon'

const { t } = useI18n()

const props = defineProps<{
  /** 光标在编辑器中的位置 */
  cursorTop?: number
  /** 编辑器容器的 DOM 元素 */
  editorElement?: HTMLElement
}>()

const emit = defineEmits<{
  select: [command: string]
  close: []
  /** 位置调整事件，返回是否需要显示在上方 */
  positionAdjust: [showAbove: boolean, menuHeight: number]
}>()

// 命令列表
const commands = computed<{ id: string; label: string; icon: Component; description: string }[]>(() => [
  {
    id: 'heading1',
    label: t('common.editor.slashMenu.heading1'),
    icon: IconH1,
    description: t('common.editor.slashMenu.heading1Desc'),
  },
  {
    id: 'heading2',
    label: t('common.editor.slashMenu.heading2'),
    icon: IconH2,
    description: t('common.editor.slashMenu.heading2Desc'),
  },
  {
    id: 'heading3',
    label: t('common.editor.slashMenu.heading3'),
    icon: IconH3,
    description: t('common.editor.slashMenu.heading3Desc'),
  },
  {
    id: 'bulletList',
    label: t('common.editor.slashMenu.bulletList'),
    icon: IconUnorderedList,
    description: t('common.editor.slashMenu.bulletListDesc'),
  },
  {
    id: 'orderedList',
    label: t('common.editor.slashMenu.orderedList'),
    icon: IconOrderedList,
    description: t('common.editor.slashMenu.orderedListDesc'),
  },
  {
    id: 'taskList',
    label: t('common.editor.slashMenu.taskList'),
    icon: IconCheckSquare,
    description: t('common.editor.slashMenu.taskListDesc'),
  },
  {
    id: 'blockquote',
    label: t('common.editor.slashMenu.quote'),
    icon: IconQuote,
    description: t('common.editor.slashMenu.quoteDesc'),
  },
  {
    id: 'codeBlock',
    label: t('common.editor.slashMenu.codeBlock'),
    icon: IconCode,
    description: t('common.editor.slashMenu.codeBlockDesc'),
  },
  {
    id: 'table',
    label: t('common.editor.slashMenu.table'),
    icon: IconApps,
    description: t('common.editor.slashMenu.tableDesc'),
  },
  {
    id: 'image',
    label: t('common.editor.slashMenu.image'),
    icon: IconImage,
    description: t('common.editor.slashMenu.imageDesc'),
  },
  {
    id: 'horizontalRule',
    label: t('common.editor.slashMenu.divider'),
    icon: IconMinus,
    description: t('common.editor.slashMenu.dividerDesc'),
  },
])

const selectedIndex = ref(0)
const menuRef = ref<HTMLElement | null>(null)

// 键盘导航
function handleKeyDown(event: KeyboardEvent) {
  if (event.key === 'ArrowUp') {
    selectedIndex.value = (selectedIndex.value - 1 + commands.value.length) % commands.value.length
    scrollToSelected()
  } else if (event.key === 'ArrowDown') {
    selectedIndex.value = (selectedIndex.value + 1) % commands.value.length
    scrollToSelected()
  } else if (event.key === 'Enter') {
    const command = commands.value[selectedIndex.value]
    if (command) {
      selectCommand(command.id)
    }
  }
}

function scrollToSelected() {
  const menu = menuRef.value
  if (!menu) return

  const selectedItem = menu.querySelector('.menu-item.selected') as HTMLElement
  if (selectedItem) {
    selectedItem.scrollIntoView({ block: 'nearest' })
  }
}

function selectCommand(commandId: string) {
  emit('select', commandId)
}

// 点击外部关闭
function handleClickOutside(event: MouseEvent) {
  if (menuRef.value && !menuRef.value.contains(event.target as Node)) {
    emit('close')
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  // 检测位置并通知父组件
  nextTick(() => {
    checkPosition()
  })
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// 检测菜单位置，如果底部空间不足则通知父组件调整
function checkPosition() {
  if (!menuRef.value || !props.editorElement) return

  const menuRect = menuRef.value.getBoundingClientRect()
  const editorRect = props.editorElement.getBoundingClientRect()
  const viewportHeight = window.innerHeight

  // 计算菜单底部是否超出视口
  const menuBottom = editorRect.top + (props.cursorTop || 0) + menuRect.height + 4
  const needShowAbove = menuBottom > viewportHeight - 20 // 留 20px 边距

  if (needShowAbove) {
    emit('positionAdjust', true, menuRect.height)
  }
}

// 暴露方法给父组件
defineExpose({
  handleKeyDown,
})
</script>

<template>
  <div ref="menuRef" class="slash-command-menu">
    <div class="menu-header">{{ t('common.editor.slashMenu.title') }}</div>
    <div class="menu-list">
      <div
        v-for="(command, index) in commands"
        :key="command.id"
        class="menu-item"
        :class="{ selected: index === selectedIndex }"
        @click="selectCommand(command.id)"
        @mouseenter="selectedIndex = index"
      >
        <div class="menu-item-icon">
          <component :is="command.icon" />
        </div>
        <div class="menu-item-content">
          <div class="menu-item-label">{{ command.label }}</div>
          <div class="menu-item-desc">{{ command.description }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.slash-command-menu {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 280px;
  max-height: 320px;
  overflow: hidden;
  z-index: 1000;
}

.menu-header {
  padding: 8px 12px;
  font-size: 12px;
  color: var(--color-text-3);
  border-bottom: 1px solid var(--color-border);
}

.menu-list {
  overflow-y: auto;
  max-height: 280px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover,
  &.selected {
    background: var(--color-fill-2);
  }
}

.menu-item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  background: var(--color-fill-1);
  color: var(--color-text-2);
  font-size: 16px;
}

.menu-item-content {
  flex: 1;
  min-width: 0;
}

.menu-item-label {
  font-size: 14px;
  color: var(--color-text-1);
  font-weight: 500;
}

.menu-item-desc {
  font-size: 12px;
  color: var(--color-text-3);
  margin-top: 2px;
}
</style>
