<script setup lang="ts">
/**
 * 评论列表组件
 */
import type { CommentDTO } from '@/types/comment'
import CommentItem from './CommentItem.vue'

defineProps<{
  /** 评论列表 */
  comments: CommentDTO[]
}>()

const emit = defineEmits<{
  reply: [comment: CommentDTO]
  withdraw: [comment: CommentDTO]
  delete: [comment: CommentDTO]
}>()
</script>

<template>
  <div class="comment-list">
    <CommentItem
      v-for="comment in comments"
      :key="comment.id"
      :comment="comment"
      @reply="emit('reply', $event)"
      @withdraw="emit('withdraw', $event)"
      @delete="emit('delete', $event)"
    />
  </div>
</template>

<style scoped lang="scss">
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
