<script setup lang="ts">
import { ref, watch, onBeforeUnmount, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { StarterKit } from '@tiptap/starter-kit'
import { Placeholder } from '@tiptap/extension-placeholder'
import { ExpressionVariableNode } from './TextExpressionVariableNode'
import {
  createSuggestionExtension,
  deactivateSuggestion,
  suggestionPluginKey,
  type SuggestionState,
} from './TextExpressionSuggestionPlugin'
import { parseTemplate, serializeTemplate, createNameResolverCache } from './useTextExpressionTemplate'
import TextExpressionSuggestionPanel from './TextExpressionSuggestionPanel.vue'
import type { FieldProvider } from './types'
import type { EditorView } from '@tiptap/pm/view'

const { t } = useI18n()

const props = withDefaults(defineProps<{
  modelValue: string
  fieldProvider: FieldProvider
  placeholder?: string
}>(), {
  placeholder: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

// 建议面板状态
const suggestionVisible = ref(false)
const suggestionAnchorTop = ref(0)
const suggestionAnchorBottom = ref(0)
const suggestionAnchorLeft = ref(0)

// 保存触发位置的独立副本，不受编辑器 blur 影响
let savedTriggerPos = -1

// 防循环标志
let isUpdatingFromModel = false
let isUpdatingFromEditor = false

// 名称解析缓存
const nameCache = createNameResolverCache()

// AbortController 用于取消异步操作
let initAbortController: AbortController | null = null

/**
 * 更新编辑器内容（提取的公共逻辑）
 */
async function updateEditorContent(template: string, provider: FieldProvider) {
  if (template) {
    await nameCache.resolveTemplate(template, provider, t)
  }
  if (!editor.value) return

  isUpdatingFromModel = true
  try {
    const doc = parseTemplate(template, nameCache.resolve)
    editor.value.commands.setContent(doc)
  } finally {
    isUpdatingFromModel = false
  }
}

// 初始化：预加载基础字段名称，然后解析模板中的多级路径
async function initNameCache(signal: AbortSignal) {
  try {
    await nameCache.preload(props.fieldProvider, t)
    if (signal.aborted) return

    if (props.modelValue) {
      await nameCache.resolveTemplate(props.modelValue, props.fieldProvider, t)
      if (signal.aborted) return
    }

    // 预加载完成后重新渲染编辑器内容
    if (editor.value && props.modelValue && !signal.aborted) {
      isUpdatingFromModel = true
      try {
        const doc = parseTemplate(props.modelValue, nameCache.resolve)
        editor.value.commands.setContent(doc)
      } finally {
        isUpdatingFromModel = false
      }
    }
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') return
    console.error('[TextExpression] Failed to initialize name cache:', error)
  }
}

// 建议面板回调
function onSuggestionActivate(state: SuggestionState, view: EditorView) {
  const coords = view.coordsAtPos(state.triggerPos)
  suggestionAnchorTop.value = coords.top
  suggestionAnchorBottom.value = coords.bottom
  suggestionAnchorLeft.value = coords.left
  savedTriggerPos = state.triggerPos
  suggestionVisible.value = true
}

function onSuggestionDeactivate() {
  // 只在面板不可见时才清理（面板点击导致的 blur 不应清理）
  if (!suggestionVisible.value) {
    savedTriggerPos = -1
  }
  suggestionVisible.value = false
}

function onSuggestionUpdate(state: SuggestionState, view: EditorView) {
  const coords = view.coordsAtPos(state.triggerPos)
  suggestionAnchorTop.value = coords.top
  suggestionAnchorBottom.value = coords.bottom
  suggestionAnchorLeft.value = coords.left
}

// 创建编辑器
const editor = useEditor({
  extensions: [
    StarterKit.configure({
      // 只保留基础文本编辑，禁用不需要的功能
      heading: false,
      bulletList: false,
      orderedList: false,
      blockquote: false,
      codeBlock: false,
      horizontalRule: false,
      listItem: false,
    }),
    Placeholder.configure({
      placeholder: () => props.placeholder || t('common.textExpressionTemplate.placeholder'),
    }),
    ExpressionVariableNode,
    createSuggestionExtension({
      onActivate: onSuggestionActivate,
      onDeactivate: onSuggestionDeactivate,
      onUpdate: onSuggestionUpdate,
    }),
  ],
  content: '',
  onUpdate: ({ editor: ed }) => {
    if (isUpdatingFromModel) return
    isUpdatingFromEditor = true
    const template = serializeTemplate(ed.getJSON())
    emit('update:modelValue', template)
    isUpdatingFromEditor = false
  },
})

// 初始化编辑器内容
watch(
  () => props.modelValue,
  async (newVal) => {
    if (isUpdatingFromEditor) return
    if (!editor.value) return
    await updateEditorContent(newVal, props.fieldProvider)
  },
  { immediate: true }
)

// 当 fieldProvider 变化时重新加载名称缓存
watch(
  () => props.fieldProvider,
  async (provider) => {
    await nameCache.preload(provider, t)
    await updateEditorContent(props.modelValue, provider)
  }
)

// 处理变量选择
function handleVariableSelect(expression: string, label: string) {
  if (!editor.value) return
  if (savedTriggerPos < 0) return

  const view = editor.value.view

  // 使用保存的触发位置（不依赖 plugin state，因为 blur 可能已重置它）
  const from = savedTriggerPos - 1 // $ 字符位置
  // 光标可能已因 blur 移动，用 triggerPos + query 长度来计算 to
  // 但 blur 后选区不可靠，所以直接用 triggerPos 作为 to（$ 后面没有 query 因为是点击选择）
  const pluginState = suggestionPluginKey.getState(view.state)
  const queryLen = pluginState?.active ? pluginState.query.length : 0
  const to = savedTriggerPos + queryLen

  editor.value
    .chain()
    .focus()
    .deleteRange({ from, to })
    .insertExpressionVariable({ expression, label })
    .run()

  savedTriggerPos = -1
  deactivateSuggestion(view)
  suggestionVisible.value = false
}

function handleSuggestionClose() {
  if (editor.value) {
    deactivateSuggestion(editor.value.view)
  }
  savedTriggerPos = -1
  suggestionVisible.value = false
}

onMounted(() => {
  // 取消之前的初始化
  initAbortController?.abort()
  initAbortController = new AbortController()
  initNameCache(initAbortController.signal)
})

onBeforeUnmount(() => {
  // 取消异步操作
  initAbortController?.abort()

  // 先关闭建议面板
  if (editor.value) {
    deactivateSuggestion(editor.value.view)
  }
  suggestionVisible.value = false

  // 销毁编辑器
  editor.value?.destroy()
})
</script>

<template>
  <div class="text-expression-template-editor">
    <EditorContent v-if="editor" :editor="editor" class="editor-content" />
    <TextExpressionSuggestionPanel
      :visible="suggestionVisible"
      :anchor-top="suggestionAnchorTop"
      :anchor-bottom="suggestionAnchorBottom"
      :anchor-left="suggestionAnchorLeft"
      :field-provider="fieldProvider"
      @select="handleVariableSelect"
      @close="handleSuggestionClose"
    />
  </div>
</template>

<style scoped lang="scss">
.text-expression-template-editor {
  position: relative;
  width: 100%;
}

.editor-content {
  width: 100%;

  :deep(.tiptap) {
    width: 100%;
    border: 1px solid var(--color-border-2, #e5e6eb);
    border-radius: 6px;
    padding: 8px 12px;
    min-height: 60px;
    max-height: 160px;
    overflow-y: auto;
    font-size: 14px;
    line-height: 1.6;
    color: var(--color-text-1);
    background: #fff;
    outline: none;
    transition: border-color 0.2s;
    box-sizing: border-box;

    &:focus {
      border-color: rgb(var(--primary-6));
    }

    p {
      margin: 0;
    }

    // placeholder 样式
    .is-empty::before {
      content: attr(data-placeholder);
      color: var(--color-text-4, #c9cdd4);
      pointer-events: none;
      float: left;
      height: 0;
    }
  }
}
</style>

<style lang="scss">
// 变量标签样式（全局，因为 Tiptap 渲染在 shadow 外）
.text-expression-variable-chip {
  display: inline-flex;
  align-items: center;
  padding: 0 6px;
  height: 20px;
  background: rgb(var(--primary-1));
  color: rgb(var(--primary-6));
  border-radius: 3px;
  font-size: 12px;
  cursor: default;
  user-select: none;
  vertical-align: middle;
  line-height: 20px;
}
</style>
