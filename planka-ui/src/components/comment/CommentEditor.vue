<script setup lang="ts">
/**
 * 评论编辑器组件
 * 基于 Tiptap 的简化版编辑器，支持基本格式化和图片上传
 */
import { ref, watch, onUnmounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconSend, IconClose } from '@arco-design/web-vue/es/icon'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import { StarterKit } from '@tiptap/starter-kit'
import { Placeholder } from '@tiptap/extension-placeholder'
import { Link } from '@tiptap/extension-link'
import Image from '@tiptap/extension-image'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Extension } from '@tiptap/core'
import TurndownService from 'turndown'
import { ossApi } from '@/api/oss'
import { FileCategory } from '@/types/oss'
import type { FileDTO } from '@/types/oss'
import { useOrgStore } from '@/stores/org'
import type { CommentDTO } from '@/types/comment'

const { t } = useI18n()
const orgStore = useOrgStore()

// 跟踪当前编辑器中上传的图片（fileId -> url 映射）
const uploadedImages = ref<Map<string, string>>(new Map())

const props = defineProps<{
  /** 正在回复的评论 */
  replyingTo?: CommentDTO | null
}>()

const emit = defineEmits<{
  submit: [content: string, parentId?: string, replyToMemberId?: string]
  cancelReply: []
}>()

// 是否正在提交
const submitting = ref(false)

// 创建 Turndown 实例用于 HTML 转 Markdown
const turndownService = new TurndownService({
  headingStyle: 'atx',
  codeBlockStyle: 'fenced',
})

// 图片转换规则
turndownService.addRule('imageWithSize', {
  filter: 'img',
  replacement: (_content, node) => {
    const element = node as HTMLImageElement
    const src = element.getAttribute('src') || ''
    const alt = element.getAttribute('alt') || ''
    const width = element.getAttribute('width') || element.style.width?.replace('px', '')
    const widthAttr = width ? ` width="${width}"` : ''
    return `<img src="${src}" alt="${alt}"${widthAttr} />`
  },
})

// 处理图片粘贴/拖拽上传，返回完整的 FileDTO
const handleImageUpload = async (file: File): Promise<FileDTO | null> => {
  try {
    const orgId = orgStore.currentOrgId
    const memberId = orgStore.currentMemberCardId

    if (!orgId || !memberId) {
      Message.error(t('comment.orgOrMemberNotFound'))
      return null
    }

    const result = await ossApi.upload(
      file,
      orgId,
      memberId,
      FileCategory.COMMENT_IMAGE
    )
    // 记录上传的图片
    uploadedImages.value.set(result.id, result.url)
    return result
  } catch (error) {
    console.error('Failed to upload image:', error)
    Message.error(t('comment.imageUploadError'))
    return null
  }
}

// 删除 OSS 图片
const deleteImage = async (fileId: string) => {
  try {
    await ossApi.delete(fileId)
    uploadedImages.value.delete(fileId)
  } catch (error) {
    console.error('Failed to delete image:', error)
  }
}

// 根据 URL 查找 fileId
const findFileIdByUrl = (url: string): string | undefined => {
  for (const [fileId, fileUrl] of uploadedImages.value.entries()) {
    if (fileUrl === url) {
      return fileId
    }
  }
  return undefined
}

// 图片上传插件
const ImageUploadPlugin = Extension.create({
  name: 'imageUpload',
  addProseMirrorPlugins() {
    return [
      new Plugin({
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
                  handleImageUpload(file).then((fileDTO) => {
                    if (fileDTO) {
                      editor.value?.chain().focus().setImage({ src: fileDTO.url }).run()
                    }
                  })
                }
                return true
              }
            }
            return false
          },
          handleDrop: (_view, event) => {
            const files = event.dataTransfer?.files
            if (!files || files.length === 0) return false

            const file = files[0]
            if (file && file.type.startsWith('image/')) {
              event.preventDefault()
              handleImageUpload(file).then((fileDTO) => {
                if (fileDTO) {
                  editor.value?.chain().focus().setImage({ src: fileDTO.url }).run()
                }
              })
              return true
            }
            return false
          },
        },
      }),
    ]
  },
})

// 初始化编辑器
const editor = useEditor({
  extensions: [
    StarterKit.configure({
      heading: { levels: [1, 2, 3] },
    }),
    Placeholder.configure({
      placeholder: () => t('comment.placeholder'),
    }),
    Link.configure({
      openOnClick: false,
    }),
    Image.configure({
      allowBase64: false,
      inline: true,
    }),
    ImageUploadPlugin,
  ],
  editorProps: {
    attributes: {
      class: 'comment-editor__input',
    },
  },
  onTransaction: ({ transaction }) => {
    // 检测图片节点的删除
    if (!transaction.docChanged) return

    const removedImages: string[] = []
    transaction.steps.forEach((step) => {
      // @ts-expect-error step.slice 是 ReplaceStep 的属性
      const slice = step.slice
      if (!slice) return

      // 检查被替换的内容中是否有图片
      // @ts-expect-error step.from 和 step.to 是 ReplaceStep 的属性
      const { from, to } = step
      if (from === undefined || to === undefined) return

      // 遍历被删除的范围，查找图片节点
      transaction.before.nodesBetween(from, to, (node) => {
        if (node.type.name === 'image') {
          const src = node.attrs.src
          if (src) {
            const fileId = findFileIdByUrl(src)
            if (fileId) {
              removedImages.push(fileId)
            }
          }
        }
      })
    })

    // 删除被移除的图片
    removedImages.forEach((fileId) => {
      deleteImage(fileId)
    })
  },
})

// 获取编辑器内容（Markdown 格式）
const getContent = (): string => {
  if (!editor.value) return ''
  const html = editor.value.getHTML()
  if (html === '<p></p>') return ''
  return turndownService.turndown(html)
}

// 清空编辑器（提交成功后调用，不删除图片因为图片已关联到评论）
const clearEditorAfterSubmit = () => {
  uploadedImages.value.clear()
  editor.value?.commands.clearContent()
}

// 提交评论
const handleSubmit = async () => {
  const content = getContent()
  if (!content.trim()) {
    Message.warning(t('comment.emptyContent'))
    return
  }

  submitting.value = true
  try {
    const parentId = props.replyingTo?.rootId || props.replyingTo?.id
    const replyToMemberId = props.replyingTo?.authorId
    emit('submit', content, parentId, replyToMemberId)
    clearEditorAfterSubmit()
  } finally {
    submitting.value = false
  }
}

// 取消回复
const handleCancelReply = () => {
  emit('cancelReply')
}

// 是否禁用发送按钮
const isSubmitDisabled = computed(() => {
  if (!editor.value) return true
  const html = editor.value.getHTML()
  return html === '<p></p>' || submitting.value
})

// 回复提示信息
const replyHint = computed(() => {
  if (!props.replyingTo) return null
  return t('comment.replyingTo', { name: props.replyingTo.authorName || t('comment.anonymous') })
})

// 监听回复变化，自动聚焦
watch(
  () => props.replyingTo,
  (newVal) => {
    if (newVal) {
      editor.value?.commands.focus()
    }
  }
)

onUnmounted(() => {
  // 清理未提交的图片
  uploadedImages.value.forEach((_, fileId) => {
    deleteImage(fileId)
  })
  editor.value?.destroy()
})
</script>

<template>
  <div class="comment-editor">
    <!-- 回复提示 -->
    <div v-if="replyHint" class="comment-editor__reply-hint">
      <span>{{ replyHint }}</span>
      <a-button type="text" size="mini" @click="handleCancelReply">
        <template #icon><IconClose /></template>
      </a-button>
    </div>

    <!-- 编辑器内容区 -->
    <div class="comment-editor__wrapper">
      <EditorContent :editor="editor" class="comment-editor__content" />
    </div>

    <!-- 工具栏 -->
    <div class="comment-editor__toolbar">
      <div class="comment-editor__toolbar-left">
        <span class="comment-editor__hint">{{ t('comment.supportMarkdown') }}</span>
      </div>
      <div class="comment-editor__toolbar-right">
        <a-button
          type="primary"
          size="small"
          :loading="submitting"
          :disabled="isSubmitDisabled"
          @click="handleSubmit"
        >
          <template #icon><IconSend /></template>
          {{ t('comment.send') }}
        </a-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.comment-editor {
  border: 1px solid var(--color-border-2);
  border-radius: 6px;
  background: var(--color-bg-2);

  &__reply-hint {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    background: var(--color-fill-2);
    border-bottom: 1px solid var(--color-border-2);
    font-size: 12px;
    color: var(--color-text-2);
  }

  &__wrapper {
    min-height: 80px;
    max-height: 200px;
    overflow-y: auto;
  }

  &__content {
    :deep(.comment-editor__input) {
      padding: 12px;
      min-height: 80px;
      outline: none;

      p {
        margin: 0 0 8px;
      }

      p:last-child {
        margin-bottom: 0;
      }

      img {
        max-width: 100%;
        border-radius: 4px;
      }

      &.ProseMirror-focused {
        outline: none;
      }

      .is-editor-empty:first-child::before {
        content: attr(data-placeholder);
        float: left;
        color: var(--color-text-4);
        pointer-events: none;
        height: 0;
      }
    }
  }

  &__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
    border-top: 1px solid var(--color-border-2);
    background: var(--color-fill-1);
  }

  &__hint {
    font-size: 12px;
    color: var(--color-text-4);
  }
}
</style>
