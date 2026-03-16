<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps<{
  loading?: boolean
  text?: string
  size?: 'mini' | 'small' | 'medium' | 'large'
}>()

defineEmits<{
  (e: 'click', event: Event): void
}>()
</script>

<template>
  <a-button class="save-btn" :class="size ? `save-btn-${size}` : ''" type="primary" :loading="loading" @click="$emit('click', $event)">
    <slot>{{ text || t('common.action.save') }}</slot>
  </a-button>
</template>

<style scoped>
.save-btn {
  border-radius: 6px !important;
  padding: 0 16px !important;
  background: #3350EB !important;
  border: none !important;
  box-shadow: none !important;
  height: 28px !important;
  line-height: 28px !important;
  font-size: 14px;
  font-weight: 500;
  color: #fff !important;
}

.save-btn:hover {
  background: #4E83FD !important;
}

.save-btn:active {
  background: #245BDB !important;
}

/* 小尺寸 */
.save-btn-small {
  height: 28px !important;
  line-height: 28px !important;
  font-size: 14px;
}

/* 迷你尺寸 */
.save-btn-mini {
  height: 24px !important;
  line-height: 24px !important;
  font-size: 12px;
  padding: 0 12px !important;
}

/* 中尺寸 */
.save-btn-medium {
  height: 32px !important;
  line-height: 32px !important;
  font-size: 14px;
}

/* 大尺寸 */
.save-btn-large {
  height: 40px !important;
  line-height: 40px !important;
  font-size: 16px;
}
</style>
