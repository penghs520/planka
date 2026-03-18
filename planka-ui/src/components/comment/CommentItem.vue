<script setup lang="ts">
/**
 * 单条评论组件
 */
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import type { CommentDTO } from '@/types/comment'
import { formatRelativeTime } from '@/utils/date'
import { marked } from 'marked'

const { t } = useI18n()
const orgStore = useOrgStore()

const props = defineProps<{
  /** 评论数据 */
  comment: CommentDTO
  /** 是否为回复 */
  isReply?: boolean
}>()

const emit = defineEmits<{
  reply: [comment: CommentDTO]
  withdraw: [comment: CommentDTO]
  delete: [comment: CommentDTO]
}>()

// 是否是当前用户的评论
const isOwnComment = computed(() => {
  return props.comment.authorId === orgStore.currentMember?.id
})

// 是否可以撤回（2分钟内）
const canWithdraw = computed(() => {
  if (!isOwnComment.value || props.comment.status !== 'ACTIVE') return false
  const createdAt = new Date(props.comment.createdAt).getTime()
  const now = Date.now()
  const twoMinutes = 2 * 60 * 1000
  return now - createdAt < twoMinutes
})

// 是否已撤回
const isWithdrawn = computed(() => props.comment.status === 'WITHDRAWN')

// 渲染 Markdown 内容
const renderedContent = computed(() => {
  if (isWithdrawn.value) {
    return `<span class="comment-withdrawn">${t('comment.withdrawnMessage')}</span>`
  }
  return marked.parse(props.comment.content) as string
})

// 格式化时间
const formattedTime = computed(() => formatRelativeTime(props.comment.createdAt))

// 编辑信息
const editInfo = computed(() => {
  if (props.comment.editCount > 0) {
    return t('comment.edited')
  }
  return null
})

// 操作来源是否已删除（规则被删除）
const isSourceDeleted = computed(() => {
  const source = props.comment.operationSource
  if (!source || source.type !== 'BIZ_RULE') return false
  // 后端会在 ruleName 后添加 " (已删除)" 标记
  return source.ruleName?.includes('(已删除)') ?? false
})

// 清理后的规则名称（移除删除标记）
const cleanRuleName = computed(() => {
  const source = props.comment.operationSource
  if (!source || source.type !== 'BIZ_RULE') return null
  return source.ruleName?.replace(/\s*\(已删除\)$/, '') ?? null
})

// 操作来源显示文本
const operationSourceText = computed(() => {
  const source = props.comment.operationSource
  if (!source) return null

  if (source.type === 'BIZ_RULE' && cleanRuleName.value) {
    return t('comment.operationSource.bizRule', { ruleName: cleanRuleName.value })
  }
  if (source.type === 'API_CALL' && source.appName) {
    return t('comment.operationSource.apiCall', { appName: source.appName })
  }
  return null
})
</script>

<template>
  <div class="comment-item" :class="{ 'comment-item--reply': isReply }">
    <!-- 评论头部 -->
    <div class="comment-item__header">
      <a-avatar :size="isReply ? 28 : 32" class="comment-item__avatar">
        <img v-if="comment.authorAvatar" :src="comment.authorAvatar" :alt="comment.authorName ?? undefined" />
        <span v-else>{{ (comment.authorName || 'U')[0] }}</span>
      </a-avatar>
      <div class="comment-item__meta">
        <span class="comment-item__author">{{ comment.authorName || t('comment.anonymous') }}</span>
        <span
          v-if="operationSourceText"
          class="comment-item__source"
          :class="{ 'comment-item__source--deleted': isSourceDeleted }"
        >
          {{ operationSourceText }}
          <template v-if="isSourceDeleted">({{ t('comment.deleted') }})</template>
        </span>
        <span v-if="comment.replyToMemberName" class="comment-item__reply-to">
          {{ t('comment.replyTo') }} {{ comment.replyToMemberName }}
        </span>
        <span class="comment-item__time">{{ formattedTime }}</span>
        <span v-if="editInfo" class="comment-item__edited">{{ editInfo }}</span>
      </div>
    </div>

    <!-- 评论内容 -->
    <div class="comment-item__content" v-html="renderedContent" />

    <!-- 评论操作 -->
    <div v-if="comment.status === 'ACTIVE'" class="comment-item__actions">
      <a-button type="text" size="mini" @click="emit('reply', comment)">
        {{ t('comment.reply') }}
      </a-button>
      <a-button
        v-if="canWithdraw"
        type="text"
        size="mini"
        status="warning"
        @click="emit('withdraw', comment)"
      >
        {{ t('comment.withdraw') }}
      </a-button>
    </div>

    <!-- 回复列表 -->
    <div v-if="comment.replies?.length" class="comment-item__replies">
      <CommentItem
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        :is-reply="true"
        @reply="emit('reply', $event)"
        @withdraw="emit('withdraw', $event)"
        @delete="emit('delete', $event)"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.comment-item {
  &__header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  &__author {
    font-weight: 500;
    color: var(--color-text-1);
  }

  &__reply-to {
    color: var(--color-text-3);
    font-size: 12px;
  }

  &__time {
    color: var(--color-text-3);
    font-size: 12px;
  }

  &__edited {
    color: var(--color-text-4);
    font-size: 12px;
    font-style: italic;
  }

  &__source {
    color: var(--color-text-3);
    font-size: 12px;
    padding: 2px 8px;
    background: var(--color-fill-2);
    border-radius: 4px;

    &--deleted {
      text-decoration: line-through;
      color: var(--color-text-4);
      background: var(--color-fill-1);
    }
  }

  &__content {
    margin-left: 40px;
    color: var(--color-text-1);
    line-height: 1.6;

    :deep(p) {
      margin: 0 0 8px;
    }

    :deep(img) {
      max-width: 100%;
      border-radius: 4px;
    }

    :deep(.comment-withdrawn) {
      color: var(--color-text-4);
      font-style: italic;
    }
  }

  &__actions {
    margin-left: 40px;
    margin-top: 4px;
    display: flex;
    gap: 8px;
  }

  &__replies {
    margin-left: 40px;
    margin-top: 12px;
    padding-left: 12px;
    border-left: 2px solid var(--color-border-2);
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &--reply {
    .comment-item__content {
      margin-left: 36px;
    }

    .comment-item__actions {
      margin-left: 36px;
    }
  }
}
</style>
