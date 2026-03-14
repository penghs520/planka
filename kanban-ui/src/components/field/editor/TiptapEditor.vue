<script setup lang="ts">
/**
 * Tiptap 富文本编辑器组件
 * 类似飞书文档的编辑体验，支持斜杠命令、悬浮工具栏、快捷键
 * 支持粘贴/拖拽图片自动上传
 */
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconLink } from '@arco-design/web-vue/es/icon'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { StarterKit } from '@tiptap/starter-kit'
import { Placeholder } from '@tiptap/extension-placeholder'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableHeader } from '@tiptap/extension-table-header'
import { TableCell } from '@tiptap/extension-table-cell'
import { TaskList } from '@tiptap/extension-task-list'
import { TaskItem } from '@tiptap/extension-task-item'
import { Link } from '@tiptap/extension-link'
import { Underline } from '@tiptap/extension-underline'
import { Color } from '@tiptap/extension-color'
import { TextStyle } from '@tiptap/extension-text-style'
import { Highlight } from '@tiptap/extension-highlight'
import { CodeBlockLowlight } from '@tiptap/extension-code-block-lowlight'
import { ImageResize } from 'tiptap-extension-resize-image'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Extension } from '@tiptap/core'
import { common, createLowlight } from 'lowlight'
import TurndownService from 'turndown'
import { marked } from 'marked'
import { ossApi } from '@/api/oss'
import { FileCategory } from '@/types/oss'
import type { FileDTO } from '@/types/oss'
import { useOrgStore } from '@/stores/org'
import SlashCommandMenu from './SlashCommandMenu.vue'
import TableContextMenu from './TableContextMenu.vue'
import './tiptap-styles.scss'

// 自定义表格单元格，支持段落、有序列表、无序列表（不支持任务列表、代码块等复杂元素）
const CustomTableCell = TableCell.extend({
  content: '(paragraph | bulletList | orderedList)+',
})

const CustomTableHeader = TableHeader.extend({
  content: '(paragraph | bulletList | orderedList)+',
})

const { t } = useI18n()
const orgStore = useOrgStore()

// 跟踪本次编辑会话中新上传的图片（fileId -> url）
const uploadedImages = ref<Map<string, string>>(new Map())
// 跟踪待删除的已保存图片（保存成功后才真正删除）
const pendingDeleteImages = ref<string[]>([])

const props = defineProps<{
  /** 占位符 */
  placeholder?: string
  /** 是否禁用 */
  disabled?: boolean
  /** 编辑器高度 (px) */
  height?: number
}>()

const modelValue = defineModel<string>({ required: false, default: '' })

const emit = defineEmits<{
  save: []
  cancel: []
}>()

// 创建 lowlight 实例
const lowlight = createLowlight(common)

// 创建 Turndown 实例用于 HTML 转 Markdown
const turndownService = new TurndownService({
  headingStyle: 'atx',
  codeBlockStyle: 'fenced',
})

// 保留带样式的标签和列表标签
turndownService.keep(['span', 'mark', 'u', 'br', 'ul', 'ol', 'li'])

// 忽略 colgroup（Tiptap 表格调整列宽时生成）
turndownService.addRule('colgroup', {
  filter: ['colgroup', 'col'],
  replacement: () => '',
})

// 图片转换规则 - 保留宽高和对齐样式
turndownService.addRule('imageWithSize', {
  filter: 'img',
  replacement: (_content, node) => {
    const element = node as HTMLImageElement
    const src = element.getAttribute('src') || ''
    const alt = element.getAttribute('alt') || ''
    const width = element.getAttribute('width') || element.style.width?.replace('px', '')
    const height = element.getAttribute('height') || element.style.height?.replace('px', '')
    const containerStyle = element.getAttribute('containerstyle') || ''

    // 使用 HTML 格式保存（包含宽高和对齐样式）
    const widthAttr = width ? ` width="${width}"` : ''
    const heightAttr = height ? ` height="${height}"` : ''
    const containerAttr = containerStyle ? ` containerstyle="${containerStyle}"` : ''

    // 添加前后换行，确保图片作为独立块级元素
    if (width || height || containerStyle) {
      return `\n\n<img src="${src}" alt="${alt}"${widthAttr}${heightAttr}${containerAttr} />\n\n`
    }

    // 无额外属性使用标准 Markdown 格式
    return `\n\n![${alt}](${src})\n\n`
  },
})

// 表格单元格转换规则 - 保留列表的 HTML 格式以便回显
turndownService.addRule('tableCell', {
  filter: ['th', 'td'],
  replacement: (_content, node) => {
    const element = node as HTMLElement
    const parts: string[] = []

    element.childNodes.forEach((child) => {
      if (child.nodeType === Node.TEXT_NODE) {
        const text = child.textContent?.trim()
        if (text) parts.push(text)
      } else if (child.nodeType === Node.ELEMENT_NODE) {
        const el = child as HTMLElement
        if (el.tagName === 'P') {
          const text = el.textContent?.trim()
          if (text) parts.push(text)
        } else if (el.tagName === 'UL' || el.tagName === 'OL') {
          // 保留列表的 HTML 格式，确保回显时能正确解析
          parts.push(el.outerHTML)
        } else {
          const text = el.textContent?.trim()
          if (text) parts.push(text)
        }
      }
    })

    const cellContent = parts.join('<br>')
    return ` ${cellContent} |`
  },
})

turndownService.addRule('tableRow', {
  filter: 'tr',
  replacement: (content) => `|${content}\n`,
})

turndownService.addRule('table', {
  filter: 'table',
  replacement: (content) => {
    const lines = content.trim().split('\n').filter(Boolean)
    if (lines.length === 0) return ''
    const headerLine = lines[0]
    const cellCount = Math.max(0, (headerLine?.match(/\|/g) || []).length - 1)
    const separator = '|' + ' --- |'.repeat(cellCount)
    return lines[0] + '\n' + separator + '\n' + lines.slice(1).join('\n') + '\n\n'
  },
})

// 任务列表转换规则
turndownService.addRule('taskListItem', {
  filter: (node) => {
    return node.nodeName === 'LI' && node.getAttribute('data-type') === 'taskItem'
  },
  replacement: (content, node) => {
    const checked = (node as HTMLElement).getAttribute('data-checked') === 'true'
    return `- [${checked ? 'x' : ' '}] ${content.trim()}\n`
  },
})

// 保留字体颜色样式
turndownService.addRule('textColor', {
  filter: (node) => {
    return (
      node.nodeName === 'SPAN' &&
      !!node.getAttribute('style') &&
      !!node.getAttribute('style')?.includes('color:')
    )
  },
  replacement: (content, node) => {
    const style = (node as HTMLElement).getAttribute('style') || ''
    const colorMatch = style.match(/color:\s*([^;]+)/)
    if (colorMatch) {
      return `<span style="color: ${colorMatch[1]}">${content}</span>`
    }
    return content
  },
})

// 保留背景色样式
turndownService.addRule('highlightColor', {
  filter: (node) => {
    return (
      node.nodeName === 'MARK' ||
      (node.nodeName === 'SPAN' &&
        !!node.getAttribute('style') &&
        !!node.getAttribute('style')?.includes('background-color:'))
    )
  },
  replacement: (content, node) => {
    if (node.nodeName === 'MARK') {
      const style = (node as HTMLElement).getAttribute('style') || ''
      const colorMatch = style.match(/background-color:\s*([^;]+)/)
      if (colorMatch) {
        return `<mark style="background-color: ${colorMatch[1]}">${content}</mark>`
      }
      return `<mark>${content}</mark>`
    }
    // 处理 span 背景色
    const style = (node as HTMLElement).getAttribute('style') || ''
    const colorMatch = style.match(/background-color:\s*([^;]+)/)
    if (colorMatch) {
      return `<span style="background-color: ${colorMatch[1]}">${content}</span>`
    }
    return content
  },
})

// 斜杠菜单状态
const showSlashMenu = ref(false)
const slashMenuPosition = ref({ top: 0, left: 0 })
const slashMenuRef = ref<InstanceType<typeof SlashCommandMenu> | null>(null)
const slashMenuCursorTop = ref(0)
const editorWrapperRef = ref<HTMLElement | undefined>(undefined)

// 悬浮工具栏状态
const showBubbleMenu = ref(false)
const bubbleMenuPosition = ref({ top: 0, left: 0 })

// 表格右键菜单状态
const showTableMenu = ref(false)
const tableMenuPosition = ref({ top: 0, left: 0 })

// 图片上传状态
const isUploadingImage = ref(false)

// 图片选择 input 引用
const imageInputRef = ref<HTMLInputElement | null>(null)

// 上传图片到 OSS，返回完整的 FileDTO
async function uploadImage(file: File): Promise<FileDTO | null> {
  const orgId = orgStore.currentOrgId
  const memberId = orgStore.currentMemberCardId

  if (!orgId || !memberId) {
    console.warn('Image upload failed: missing orgId or memberId', { orgId, memberId })
    Message.warning(t('oss.upload.failed'))
    return null
  }

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    Message.warning(t('oss.upload.fileTypeNotAllowed'))
    return null
  }

  // 验证文件大小 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    Message.warning(t('oss.upload.fileTooLarge', { size: 10 }))
    return null
  }

  try {
    isUploadingImage.value = true
    const result = await ossApi.upload(file, orgId, memberId, FileCategory.DESCRIPTION_IMAGE)
    // 记录新上传的图片，用于区分新上传和已保存的图片
    uploadedImages.value.set(result.id, result.url)
    return result
  } catch {
    Message.error(t('oss.upload.failed'))
    return null
  } finally {
    isUploadingImage.value = false
  }
}

// 删除 OSS 图片
async function deleteImage(fileId: string) {
  try {
    await ossApi.delete(fileId)
    uploadedImages.value.delete(fileId)
  } catch (error) {
    console.error('Failed to delete image:', error)
  }
}

// 从 URL 解析 fileId
// URL 格式: /api/v1/files/{fileId}/content
function parseFileIdFromUrl(url: string): string | undefined {
  const match = url.match(/\/api\/v1\/files\/([^/]+)\/content/)
  return match ? match[1] : undefined
}

// 根据 URL 查找 fileId（在新上传的图片中查找）
function findUploadedFileId(url: string): string | undefined {
  for (const [fileId, fileUrl] of uploadedImages.value.entries()) {
    if (fileUrl === url) {
      return fileId
    }
  }
  return undefined
}

// 处理图片删除：新上传的立即删除，已保存的加入待删除列表
function handleImageRemoval(src: string) {
  // 先检查是否是新上传的图片
  const uploadedFileId = findUploadedFileId(src)
  if (uploadedFileId) {
    // 新上传的图片，立即删除
    deleteImage(uploadedFileId)
    return
  }

  // 已保存的图片，从 URL 解析 fileId 并加入待删除列表
  const fileId = parseFileIdFromUrl(src)
  if (fileId && !pendingDeleteImages.value.includes(fileId)) {
    pendingDeleteImages.value.push(fileId)
  }
}

// 处理粘贴/拖拽的图片
async function handleImageUpload(file: File) {
  const fileDTO = await uploadImage(file)
  if (fileDTO && editor.value) {
    editor.value.chain().focus().setImage({ src: fileDTO.url }).run()
  }
}

// 处理文件选择
function handleImageInputChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    handleImageUpload(file)
  }
  // 清空 input，允许重复选择同一文件
  input.value = ''
}

// 创建图片上传插件
function createImageUploadPlugin() {
  return new Plugin({
    key: new PluginKey('imageUpload'),
    props: {
      handlePaste: (_view, event) => {
        const items = event.clipboardData?.items
        if (!items) return false

        for (const item of items) {
          if (item.type.startsWith('image/')) {
            event.preventDefault()
            const file = item.getAsFile()
            if (file) {
              handleImageUpload(file)
              return true
            }
          }
        }
        return false
      },
      handleDrop: (_view, event) => {
        const files = event.dataTransfer?.files
        if (!files || files.length === 0) return false

        for (const file of files) {
          if (file.type.startsWith('image/')) {
            event.preventDefault()
            handleImageUpload(file)
            return true
          }
        }
        return false
      },
    },
  })
}

// 更新悬浮工具栏位置
function updateBubbleMenu() {
  if (!editor.value) return

  const { from, to, empty } = editor.value.state.selection

  // 只在有选中文本时显示
  if (empty || from === to) {
    showBubbleMenu.value = false
    return
  }

  // 不在代码块中显示
  if (editor.value.isActive('codeBlock')) {
    showBubbleMenu.value = false
    return
  }

  const coords = editor.value.view.coordsAtPos(from)
  const endCoords = editor.value.view.coordsAtPos(to)
  const editorRect = editor.value.view.dom.getBoundingClientRect()

  // 计算工具栏位置：选中区域上方居中
  const centerX = (coords.left + endCoords.right) / 2 - editorRect.left
  const topY = coords.top - editorRect.top - 40 // 工具栏高度约 36px + 间距

  bubbleMenuPosition.value = {
    top: Math.max(0, topY),
    left: Math.max(0, centerX - 80), // 工具栏宽度约 160px，居中
  }
  showBubbleMenu.value = true
}

// 编辑器实例
const editor = useEditor({
  extensions: [
    StarterKit.configure({
      codeBlock: false,
    }),
    Placeholder.configure({
      placeholder: () => props.placeholder || t('common.editor.placeholder'),
    }),
    Table.configure({
      resizable: true,
      handleWidth: 5,
      cellMinWidth: 50,
      lastColumnResizable: true,
    }),
    TableRow,
    CustomTableHeader,
    CustomTableCell,
    TaskList,
    TaskItem.configure({
      nested: true,
    }),
    Link.configure({
      openOnClick: false,
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
    Extension.create({
      name: 'imageUpload',
      addProseMirrorPlugins() {
        return [createImageUploadPlugin()]
      },
    }),
  ],
  content: '',
  editable: !props.disabled,
  onUpdate: ({ editor }) => {
    const html = editor.getHTML()
    if (html === '<p></p>') {
      modelValue.value = ''
    } else {
      modelValue.value = turndownService.turndown(html)
    }
  },
  onSelectionUpdate: () => {
    updateBubbleMenu()
    // 选择新文本时关闭颜色选择器
    showColorPicker.value = false
  },
  onBlur: () => {
    // 延迟隐藏，允许点击工具栏按钮
    setTimeout(() => {
      if (!editor.value?.isFocused) {
        showBubbleMenu.value = false
        showColorPicker.value = false
      }
    }, 150)
  },
  onTransaction: ({ transaction }) => {
    // 检测图片节点的删除
    if (!transaction.docChanged) return

    // 收集变更前文档中的所有图片 URL
    const imagesBefore = new Set<string>()
    transaction.before.descendants((node) => {
      if (node.type.name === 'imageResize' || node.type.name === 'image') {
        if (node.attrs.src) {
          imagesBefore.add(node.attrs.src)
        }
      }
    })

    // 收集变更后文档中的所有图片 URL
    const imagesAfter = new Set<string>()
    transaction.doc.descendants((node) => {
      if (node.type.name === 'imageResize' || node.type.name === 'image') {
        if (node.attrs.src) {
          imagesAfter.add(node.attrs.src)
        }
      }
    })

    // 找出被删除的图片（在变更前存在但变更后不存在）
    imagesBefore.forEach((src) => {
      if (!imagesAfter.has(src)) {
        handleImageRemoval(src)
      }
    })
  },
})

// 监听斜杠输入
function handleEditorKeyDown(event: KeyboardEvent) {
  if (event.key === '/' && !showSlashMenu.value) {
    // 在表格单元格中不弹出斜杠菜单（表格只支持行内元素）
    if (editor.value?.isActive('table')) {
      return
    }
    setTimeout(() => {
      if (editor.value) {
        const { from } = editor.value.state.selection
        const coords = editor.value.view.coordsAtPos(from)
        const editorRect = editor.value.view.dom.getBoundingClientRect()
        const cursorTop = coords.bottom - editorRect.top + 4
        slashMenuPosition.value = {
          top: cursorTop,
          left: coords.left - editorRect.left,
        }
        slashMenuCursorTop.value = cursorTop
        showSlashMenu.value = true
      }
    }, 0)
  }
}

// 处理斜杠菜单位置调整（当底部空间不足时显示在上方）
function handleSlashMenuPositionAdjust(showAbove: boolean, menuHeight: number) {
  if (showAbove && editor.value) {
    const { from } = editor.value.state.selection
    const coords = editor.value.view.coordsAtPos(from)
    const editorRect = editor.value.view.dom.getBoundingClientRect()
    // 在光标上方显示，减去菜单高度和行高
    slashMenuPosition.value = {
      top: coords.top - editorRect.top - menuHeight - 4,
      left: coords.left - editorRect.left,
    }
  }
}

// 斜杠菜单键盘导航
function handleSlashMenuKeyDown(event: KeyboardEvent) {
  if (!showSlashMenu.value) return

  if (event.key === 'Escape') {
    event.preventDefault()
    showSlashMenu.value = false
    return
  }

  if (event.key === 'ArrowUp' || event.key === 'ArrowDown' || event.key === 'Enter') {
    event.preventDefault()
    slashMenuRef.value?.handleKeyDown(event)
  }
}

// 执行斜杠命令
function executeCommand(command: string) {
  if (!editor.value) return

  const { from } = editor.value.state.selection
  editor.value.chain().focus().deleteRange({ from: from - 1, to: from }).run()

  switch (command) {
    case 'heading1':
      editor.value.chain().focus().toggleHeading({ level: 1 }).run()
      break
    case 'heading2':
      editor.value.chain().focus().toggleHeading({ level: 2 }).run()
      break
    case 'heading3':
      editor.value.chain().focus().toggleHeading({ level: 3 }).run()
      break
    case 'bulletList':
      editor.value.chain().focus().toggleBulletList().run()
      break
    case 'orderedList':
      editor.value.chain().focus().toggleOrderedList().run()
      break
    case 'taskList':
      editor.value.chain().focus().toggleTaskList().run()
      break
    case 'blockquote':
      editor.value.chain().focus().toggleBlockquote().run()
      break
    case 'codeBlock':
      editor.value.chain().focus().toggleCodeBlock().run()
      break
    case 'table':
      editor.value.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()
      break
    case 'image':
      imageInputRef.value?.click()
      break
    case 'horizontalRule':
      editor.value.chain().focus().setHorizontalRule().run()
      break
  }

  showSlashMenu.value = false
}

// 悬浮工具栏按钮状态
const isActive = computed(() => ({
  bold: editor.value?.isActive('bold') ?? false,
  italic: editor.value?.isActive('italic') ?? false,
  strike: editor.value?.isActive('strike') ?? false,
  underline: editor.value?.isActive('underline') ?? false,
  code: editor.value?.isActive('code') ?? false,
  link: editor.value?.isActive('link') ?? false,
  highlight: editor.value?.isActive('highlight') ?? false,
}))

// 当前选中颜色
const currentColor = computed(() => {
  if (!editor.value) return undefined
  const color = editor.value.getAttributes('textStyle').color
  return color as string | undefined
})

const currentHighlightColor = computed(() => {
  if (!editor.value) return undefined
  const color = editor.value.getAttributes('highlight').color
  return color as string | undefined
})

// 颜色选择器状态
const showColorPicker = ref(false)

// 预设颜色
const textColors = [
  { name: 'default', value: undefined, label: '默认' },
  { name: 'gray', value: '#6B7280', label: '灰色' },
  { name: 'red', value: '#EF4444', label: '红色' },
  { name: 'orange', value: '#F97316', label: '橙色' },
  { name: 'yellow', value: '#EAB308', label: '黄色' },
  { name: 'green', value: '#22C55E', label: '绿色' },
  { name: 'blue', value: '#3B82F6', label: '蓝色' },
  { name: 'purple', value: '#8B5CF6', label: '紫色' },
]

const highlightColors = [
  { name: 'none', value: undefined, label: '无' },
  { name: 'gray', value: '#F3F4F6', label: '浅灰' },
  { name: 'red', value: '#FEE2E2', label: '浅红' },
  { name: 'orange', value: '#FFEDD5', label: '浅橙' },
  { name: 'yellow', value: '#FEF9C3', label: '浅黄' },
  { name: 'green', value: '#DCFCE7', label: '浅绿' },
  { name: 'blue', value: '#DBEAFE', label: '浅蓝' },
  { name: 'purple', value: '#F3E8FF', label: '浅紫' },
  { name: 'darkGray', value: '#E5E7EB', label: '灰' },
  { name: 'darkRed', value: '#FECACA', label: '红' },
  { name: 'darkOrange', value: '#FED7AA', label: '橙' },
  { name: 'darkYellow', value: '#FEF08A', label: '黄' },
  { name: 'darkGreen', value: '#BBF7D0', label: '绿' },
  { name: 'darkBlue', value: '#BFDBFE', label: '蓝' },
  { name: 'darkPurple', value: '#E9D5FF', label: '紫' },
]

// 切换颜色选择器
function toggleColorPicker() {
  showColorPicker.value = !showColorPicker.value
}

// 设置颜色
function setColor(color: string | undefined) {
  if (!editor.value) return
  if (color) {
    editor.value.chain().focus().setColor(color).run()
  } else {
    editor.value.chain().focus().unsetColor().run()
  }
  showColorPicker.value = false
}

// 设置高亮/背景色
function setHighlightColor(color: string | undefined) {
  if (!editor.value) return
  if (color) {
    editor.value.chain().focus().toggleHighlight({ color }).run()
  } else {
    editor.value.chain().focus().unsetHighlight().run()
  }
}

// 重置颜色
function resetColors() {
  if (!editor.value) return
  editor.value.chain().focus().unsetColor().unsetHighlight().run()
  showColorPicker.value = false
}

// 悬浮工具栏操作
function toggleBold() {
  editor.value?.chain().focus().toggleBold().run()
}

function toggleItalic() {
  editor.value?.chain().focus().toggleItalic().run()
}

function toggleStrike() {
  editor.value?.chain().focus().toggleStrike().run()
}

function toggleUnderline() {
  editor.value?.chain().focus().toggleUnderline().run()
}

function toggleCode() {
  editor.value?.chain().focus().toggleCode().run()
}

function setLink() {
  if (editor.value?.isActive('link')) {
    editor.value?.chain().focus().unsetLink().run()
  } else {
    const url = window.prompt(t('common.editor.enterUrl'))
    if (url) {
      editor.value?.chain().focus().setLink({ href: url }).run()
    }
  }
}

// 快捷键处理
function handleKeydown(event: KeyboardEvent) {
  if ((event.ctrlKey || event.metaKey) && event.key === 's') {
    event.preventDefault()
    emit('save')
  }
  if (event.key === 'Escape' && !showSlashMenu.value) {
    event.preventDefault()
    emit('cancel')
  }
}

// 监听 disabled 变化
watch(
  () => props.disabled,
  (disabled) => {
    editor.value?.setEditable(!disabled)
  }
)

// 监听外部 modelValue 变化
watch(
  () => modelValue.value,
  async (newValue) => {
    if (!editor.value) return
    const currentMarkdown = turndownService.turndown(editor.value.getHTML())
    if (currentMarkdown !== newValue) {
      const html = await marked(newValue || '')
      editor.value.commands.setContent(html)
    }
  }
)

// 初始化内容
onMounted(async () => {
  if (modelValue.value && editor.value) {
    const html = await marked(modelValue.value)
    editor.value.commands.setContent(html)
  }
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  editor.value?.destroy()
})

// 保存成功后调用：清空跟踪列表，批量删除待删除的已保存图片
async function onSaveSuccess() {
  // 清空新上传图片的跟踪列表（这些图片现在已持久化）
  uploadedImages.value.clear()

  // 批量删除待删除的已保存图片
  if (pendingDeleteImages.value.length > 0) {
    const deletePromises = pendingDeleteImages.value.map((fileId) => deleteImage(fileId))
    await Promise.all(deletePromises)
    pendingDeleteImages.value = []
  }
}

// 取消编辑时调用：删除新上传的图片，清空待删除列表
async function onCancelEdit() {
  // 删除本次会话中新上传的图片
  const deletePromises = Array.from(uploadedImages.value.keys()).map((fileId) =>
    deleteImage(fileId)
  )
  await Promise.all(deletePromises)
  uploadedImages.value.clear()

  // 清空待删除列表（已保存的图片不删除）
  pendingDeleteImages.value = []
}

// 暴露方法供父组件调用
defineExpose({
  onSaveSuccess,
  onCancelEdit,
})

// 点击编辑器外部关闭斜杠菜单和颜色选择器
function handleEditorClick(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (showSlashMenu.value && !target.closest('.slash-command-menu')) {
    showSlashMenu.value = false
  }
  if (showColorPicker.value && !target.closest('.color-picker-wrapper')) {
    showColorPicker.value = false
  }
  if (showTableMenu.value && !target.closest('.table-context-menu')) {
    showTableMenu.value = false
  }
}

// 表格右键菜单处理
function handleContextMenu(event: MouseEvent) {
  if (!editor.value) return

  // 检查是否在表格内右键
  if (editor.value.isActive('table')) {
    event.preventDefault()
    const editorRect = editor.value.view.dom.getBoundingClientRect()
    tableMenuPosition.value = {
      top: event.clientY - editorRect.top,
      left: event.clientX - editorRect.left,
    }
    showTableMenu.value = true
  }
}
</script>

<template>
  <div class="tiptap-editor" @click="handleEditorClick" @contextmenu="handleContextMenu">
    <!-- 隐藏的图片选择 input -->
    <input
      ref="imageInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleImageInputChange"
    >

    <!-- 编辑器内容区 -->
    <div
      ref="editorWrapperRef"
      class="tiptap-content-wrapper"
      :style="{ minHeight: (props.height || 120) + 'px' }"
      @keydown="handleEditorKeyDown"
      @keydown.capture="handleSlashMenuKeyDown"
    >
      <EditorContent :editor="editor" class="tiptap-content" />

      <!-- 悬浮工具栏 (选中文本时显示) -->
      <div
        v-if="showBubbleMenu"
        class="bubble-menu"
        :style="{
          top: bubbleMenuPosition.top + 'px',
          left: bubbleMenuPosition.left + 'px',
        }"
        @mousedown.prevent
      >
        <button
          type="button"
          class="bubble-btn"
          :class="{ active: isActive.bold }"
          :title="t('common.editor.bold')"
          @click="toggleBold"
        >
          <strong>B</strong>
        </button>
        <button
          type="button"
          class="bubble-btn"
          :class="{ active: isActive.italic }"
          :title="t('common.editor.italic')"
          @click="toggleItalic"
        >
          <em>I</em>
        </button>
        <button
          type="button"
          class="bubble-btn"
          :class="{ active: isActive.underline }"
          :title="t('common.editor.underline')"
          @click="toggleUnderline"
        >
          <u>U</u>
        </button>
        <button
          type="button"
          class="bubble-btn"
          :class="{ active: isActive.strike }"
          :title="t('common.editor.strikethrough')"
          @click="toggleStrike"
        >
          <s>S</s>
        </button>
        <button
          type="button"
          class="bubble-btn"
          :class="{ active: isActive.code }"
          :title="t('common.editor.inlineCode')"
          @click="toggleCode"
        >
          <code>&lt;/&gt;</code>
        </button>
        <div class="bubble-divider" />
        <!-- 字体颜色 -->
        <div class="color-picker-wrapper">
          <button
            type="button"
            class="bubble-btn color-btn"
            :class="{ active: currentColor || currentHighlightColor }"
            :title="t('common.editor.textColor')"
            @click="toggleColorPicker"
          >
            <span
              class="color-icon"
              :style="{ color: currentColor || 'currentColor', backgroundColor: currentHighlightColor || 'transparent' }"
            >A</span>
            <span
              class="color-indicator"
              :style="{ backgroundColor: currentColor || currentHighlightColor || 'transparent' }"
            />
          </button>
          <!-- 颜色选择面板 -->
          <div v-if="showColorPicker" class="color-picker-panel" @mousedown.prevent>
            <div class="color-section-title">{{ t('common.editor.textColor') }}</div>
            <div class="color-grid">
              <button
                v-for="color in textColors"
                :key="color.name"
                type="button"
                class="color-item"
                :class="{ active: currentColor === color.value }"
                :title="color.label"
                @click="setColor(color.value)"
              >
                <span class="color-preview" :style="{ color: color.value || 'currentColor' }">A</span>
              </button>
            </div>
            <div class="color-section-title">{{ t('common.editor.backgroundColor') }}</div>
            <div class="color-grid">
              <button
                v-for="color in highlightColors"
                :key="color.name"
                type="button"
                class="color-item"
                :class="{ active: currentHighlightColor === color.value }"
                :title="color.label"
                @click="setHighlightColor(color.value)"
              >
                <span
                  class="color-preview-bg"
                  :style="{ backgroundColor: color.value || 'transparent', border: color.value ? 'none' : '1px dashed var(--color-border)' }"
                />
              </button>
            </div>
            <button type="button" class="color-reset-btn" @click="resetColors">
              {{ t('common.action.reset') }}
            </button>
          </div>
        </div>
        <div class="bubble-divider" />
        <button
          type="button"
          class="bubble-btn"
          :class="{ active: isActive.link }"
          :title="t('common.editor.link')"
          @click="setLink"
        >
          <IconLink />
        </button>
      </div>

      <!-- 斜杠命令菜单 -->
      <SlashCommandMenu
        v-if="showSlashMenu"
        ref="slashMenuRef"
        :cursor-top="slashMenuCursorTop"
        :editor-element="editorWrapperRef"
        :style="{
          position: 'absolute',
          top: slashMenuPosition.top + 'px',
          left: slashMenuPosition.left + 'px',
        }"
        @select="executeCommand"
        @close="showSlashMenu = false"
        @position-adjust="handleSlashMenuPositionAdjust"
      />

      <!-- 表格右键菜单 -->
      <TableContextMenu
        v-if="showTableMenu && editor"
        :editor="editor"
        :style="{
          position: 'absolute',
          top: tableMenuPosition.top + 'px',
          left: tableMenuPosition.left + 'px',
        }"
        @close="showTableMenu = false"
      />
    </div>


  </div>
</template>

<style scoped lang="scss">
.tiptap-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: #fff;
  position: relative;

  &:focus-within {
    border-color: rgb(var(--primary-6));
  }
}

// 悬浮工具栏样式
.bubble-menu {
  position: absolute;
  z-index: 100;
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.bubble-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--color-text-2);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s;
  white-space: nowrap;

  &:hover {
    background: var(--color-fill-2);
    color: var(--color-text-1);
  }

  &.active {
    background: rgb(var(--primary-1));
    color: rgb(var(--primary-6));
  }

  code {
    font-family: monospace;
    font-size: 10px;
    white-space: nowrap;
  }
}

.bubble-divider {
  width: 1px;
  height: 16px;
  background: var(--color-border);
  margin: 0 4px;
}

.tiptap-content-wrapper {
  position: relative;
  padding: 12px 16px;
}

.tiptap-content {
  :deep(.ProseMirror) {
    outline: none;
    min-height: 100%;

    > * + * {
      margin-top: 0.75em;
    }

    p.is-editor-empty:first-child::before {
      content: attr(data-placeholder);
      float: left;
      color: var(--color-text-3);
      pointer-events: none;
      height: 0;
    }
  }
}

// 颜色选择器样式
.color-picker-wrapper {
  position: relative;
  display: inline-flex;
}

.color-btn {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;

  .color-icon {
    font-size: 14px;
    font-weight: 600;
    line-height: 1;
    padding: 1px 3px;
    border-radius: 2px;
  }

  .color-indicator {
    width: 14px;
    height: 2px;
    border-radius: 1px;
  }
}

.color-picker-panel {
  position: absolute;
  top: calc(100% + 4px);
  left: 50%;
  transform: translateX(-50%);
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  z-index: 200;
  min-width: 220px;
}

.color-section-title {
  font-size: 12px;
  color: var(--color-text-3);
  margin-bottom: 8px;
  font-weight: 500;
}

.color-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
  margin-bottom: 12px;
}

.color-item {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  padding: 0;

  &:hover {
    background: var(--color-fill-2);
  }

  &.active {
    background: var(--color-fill-3);
  }
}

.color-preview {
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
}

.color-preview-bg {
  width: 18px;
  height: 18px;
  border-radius: 3px;
}

.color-reset-btn {
  width: 100%;
  padding: 6px 0;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  background: #fff;
  color: var(--color-text-2);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: var(--color-fill-2);
    color: var(--color-text-1);
  }
}
</style>
