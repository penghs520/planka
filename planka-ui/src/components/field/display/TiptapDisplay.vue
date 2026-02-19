<script setup lang="ts">
/**
 * Tiptap 只读显示组件
 * 将 Markdown 转换为 HTML 后渲染，保持与编辑器一致的视觉样式
 */
import { watch, onMounted, computed } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { StarterKit } from '@tiptap/starter-kit'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableHeader } from '@tiptap/extension-table-header'
import { TableCell } from '@tiptap/extension-table-cell'
import { TaskList } from '@tiptap/extension-task-list'
import { TaskItem } from '@tiptap/extension-task-item'
import { Link } from '@tiptap/extension-link'
import { Underline } from '@tiptap/extension-underline'
import { TextStyle } from '@tiptap/extension-text-style'
import { Color } from '@tiptap/extension-color'
import { Highlight } from '@tiptap/extension-highlight'
import { CodeBlockLowlight } from '@tiptap/extension-code-block-lowlight'
import { ImageResize } from 'tiptap-extension-resize-image'
import { common, createLowlight } from 'lowlight'
import { marked } from 'marked'
import '../editor/tiptap-styles.scss'

const props = defineProps<{
  /** Markdown 内容 */
  value?: string | null
  /** 空值占位符 */
  placeholder?: string
  /** 最小高度 (px) */
  height?: number
}>()

// 创建 lowlight 实例
const lowlight = createLowlight(common)

// 判断是否有内容
const hasContent = computed(() => {
  return props.value && props.value.trim().length > 0
})

// 编辑器实例（只读）
const editor = useEditor({
  extensions: [
    StarterKit.configure({
      codeBlock: false,
    }),
    Table.configure({
      resizable: false,
    }),
    TableRow,
    TableHeader,
    TableCell,
    TaskList,
    TaskItem.configure({
      nested: true,
    }),
    Link.configure({
      openOnClick: true,
    }),
    Underline,
    TextStyle,
    Color,
    Highlight.configure({
      multicolor: true,
    }),
    CodeBlockLowlight.configure({
      lowlight,
    }),
    ImageResize.configure({
      inline: false,
    }),
  ],
  content: '',
  editable: false,
})

// 更新内容
async function updateContent() {
  if (!editor.value) return

  if (hasContent.value && props.value) {
    const html = await marked(props.value)
    editor.value.commands.setContent(html)
  } else {
    editor.value.commands.setContent('')
  }
}

// 监听 value 变化
watch(
  () => props.value,
  () => {
    updateContent()
  }
)

// 初始化内容
onMounted(() => {
  updateContent()
})
</script>

<template>
  <div class="tiptap-display-wrapper">
    <!-- 无内容时显示简单占位符 -->
    <div v-if="!hasContent" class="empty-placeholder">
      {{ placeholder || '点击添加描述...' }}
    </div>
    <!-- 有内容时使用 Tiptap 渲染 -->
    <div
      v-show="hasContent"
      class="tiptap-display"
      :style="{ minHeight: height ? height + 'px' : undefined }"
    >
      <EditorContent :editor="editor" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.tiptap-display-wrapper {
  width: 100%;
}

.empty-placeholder {
  color: var(--color-text-3);
  font-size: 14px;
  padding: 8px 0;
  cursor: pointer;
}

.tiptap-display {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: #fff;
  padding: 12px 16px;
  box-sizing: border-box;

  :deep(.ProseMirror) {
    cursor: pointer;
  }
}
</style>
