<script setup lang="ts">
/**
 * 评论面板组件 - 评论功能的主容器
 * 默认滚动到底部显示最新评论，向上滚动翻页加载更多
 */
import { ref, onMounted, computed, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { commentApi } from '@/api/comment'
import type { CommentDTO, CommentListResponse } from '@/types/comment'
import CommentList from './CommentList.vue'
import CommentEditor from './CommentEditor.vue'

const { t } = useI18n()

const props = defineProps<{
  /** 卡片 ID */
  cardId: string
  /** 卡片类型 ID */
  cardTypeId: string
}>()

// 评论列表
const comments = ref<CommentDTO[]>([])
const loading = ref(false)
const hasMore = ref(false)
const page = ref(0)
const pageSize = 10

// 列表容器引用
const listRef = ref<HTMLDivElement | null>(null)
// 是否正在加载更多（用于滚动触发）
const isLoadingMore = ref(false)
// 滚动阈值（距离顶部多少像素触发加载）
const SCROLL_THRESHOLD = 50

// 正在回复的评论
const replyingTo = ref<CommentDTO | null>(null)

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  })
}

// 保存当前滚动位置（相对于底部）
const saveScrollPosition = (): number => {
  if (!listRef.value) return 0
  return listRef.value.scrollHeight - listRef.value.scrollTop - listRef.value.clientHeight
}

// 恢复滚动位置（相对于底部）
const restoreScrollPosition = (bottomOffset: number) => {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight - listRef.value.clientHeight - bottomOffset
    }
  })
}

// 加载评论列表
const loadComments = async (reset = false) => {
  if (reset) {
    page.value = 0
    comments.value = []
  }

  // 保存当前滚动位置（相对于底部）
  const bottomOffset = reset ? 0 : saveScrollPosition()

  loading.value = true
  try {
    const response: CommentListResponse = await commentApi.listComments(
      props.cardId,
      page.value,
      pageSize
    )
    if (reset) {
      comments.value = response.comments
      hasMore.value = response.hasMore
      // 首次加载完成后滚动到底部
      scrollToBottom()
    } else {
      // 加载更多时，将旧数据插入到列表开头
      comments.value = [...response.comments, ...comments.value]
      hasMore.value = response.hasMore
      // 恢复滚动位置
      restoreScrollPosition(bottomOffset)
    }
  } catch (error) {
    console.error('Failed to load comments:', error)
    Message.error(t('comment.loadError'))
  } finally {
    loading.value = false
    isLoadingMore.value = false
  }
}

// 处理滚动事件
const handleScroll = async () => {
  if (!listRef.value || loading.value || isLoadingMore.value || !hasMore.value) return

  const { scrollTop } = listRef.value

  // 当滚动到顶部附近时加载更多
  if (scrollTop < SCROLL_THRESHOLD) {
    isLoadingMore.value = true
    page.value++
    await loadComments(false)
  }
}

// 加载更多（手动触发）
const loadMore = async () => {
  if (loading.value || !hasMore.value) return
  page.value++
  await loadComments(false)
}

// 提交评论
const handleSubmit = async (content: string, parentId?: string, replyToMemberId?: string) => {
  try {
    const newComment = await commentApi.createComment({
      cardId: props.cardId,
      cardTypeId: props.cardTypeId,
      content,
      parentId,
      replyToMemberId,
    })

    // 如果是回复，将其添加到对应的评论下
    if (parentId) {
      const updateReplies = (list: CommentDTO[]): CommentDTO[] => {
        return list.map((comment) => {
          if (comment.id === parentId || comment.id === newComment.rootId) {
            return {
              ...comment,
              replies: [...(comment.replies || []), newComment],
            }
          }
          return comment
        })
      }
      comments.value = updateReplies(comments.value)
    } else {
      // 顶级评论添加到列表末尾（最新的在最后）
      comments.value = [...comments.value, newComment]
    }

    // 提交新评论后滚动到底部
    scrollToBottom()

    replyingTo.value = null
    Message.success(t('comment.createSuccess'))
  } catch (error) {
    console.error('Failed to create comment:', error)
    Message.error(t('comment.createError'))
  }
}

// 撤回评论
const handleWithdraw = async (comment: CommentDTO) => {
  try {
    const updatedComment = await commentApi.withdrawComment(comment.id)
    updateCommentInList(updatedComment)
    Message.success(t('comment.withdrawSuccess'))
  } catch (error) {
    console.error('Failed to withdraw comment:', error)
    Message.error(t('comment.withdrawError'))
  }
}

// 删除评论
const handleDelete = async (comment: CommentDTO) => {
  try {
    await commentApi.deleteComment(comment.id)
    removeCommentFromList(comment.id)
    Message.success(t('comment.deleteSuccess'))
  } catch (error) {
    console.error('Failed to delete comment:', error)
    Message.error(t('comment.deleteError'))
  }
}

// 开始回复
const handleReply = (comment: CommentDTO) => {
  replyingTo.value = comment
}

// 取消回复
const handleCancelReply = () => {
  replyingTo.value = null
}

// 更新列表中的评论
const updateCommentInList = (updatedComment: CommentDTO) => {
  const updateInList = (list: CommentDTO[]): CommentDTO[] => {
    return list.map((comment) => {
      if (comment.id === updatedComment.id) {
        return updatedComment
      }
      if (comment.replies?.length) {
        return {
          ...comment,
          replies: updateInList(comment.replies),
        }
      }
      return comment
    })
  }
  comments.value = updateInList(comments.value)
}

// 从列表中移除评论
const removeCommentFromList = (commentId: string) => {
  const removeFromList = (list: CommentDTO[]): CommentDTO[] => {
    return list
      .filter((comment) => comment.id !== commentId)
      .map((comment) => ({
        ...comment,
        replies: comment.replies ? removeFromList(comment.replies) : [],
      }))
  }
  comments.value = removeFromList(comments.value)
}

// 是否显示空状态
const isEmpty = computed(() => !loading.value && comments.value.length === 0)

onMounted(() => {
  loadComments(true)
})
</script>

<template>
  <div class="comment-panel">
    <!-- 评论列表 -->
    <div
      ref="listRef"
      class="comment-panel__list"
      @scroll="handleScroll"
    >
      <a-spin :loading="loading && page === 0">
        <!-- 加载更多提示（在顶部） -->
        <div v-if="hasMore || isLoadingMore" class="comment-panel__load-more">
          <a-button
            v-if="!isLoadingMore"
            type="text"
            :loading="loading"
            @click="loadMore"
          >
            {{ t('comment.loadMore') }}
          </a-button>
          <a-spin v-else size="small" />
        </div>

        <CommentList
          v-if="!isEmpty"
          :comments="comments"
          @reply="handleReply"
          @withdraw="handleWithdraw"
          @delete="handleDelete"
        />

        <!-- 空状态 -->
        <a-empty v-else :description="t('comment.empty')" />
      </a-spin>
    </div>

    <!-- 评论编辑器 - 固定在底部 -->
    <div class="comment-panel__editor">
      <CommentEditor
        :replying-to="replyingTo"
        @submit="handleSubmit"
        @cancel-reply="handleCancelReply"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.comment-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px;

  &__list {
    flex: 1;
    overflow-y: auto;
    padding-bottom: 16px;
    scroll-behavior: smooth;
  }

  &__editor {
    flex-shrink: 0;
    padding-top: 16px;
    border-top: 1px solid var(--color-border-2);
  }

  &__load-more {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 16px 0;
    min-height: 48px;
  }
}
</style>
