<script setup lang="ts">

import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps<{
  text?: string
  size?: 'mini' | 'small' | 'medium' | 'large'
}>()

defineEmits<{
  (e: 'click', event: Event): void
}>()
</script>

<template>
  <a-button class="cancel-btn" :class="size ? `cancel-btn-${size}` : ''" @click="$emit('click', $event)">
    <slot>{{ text || t('common.action.cancel') }}</slot>
  </a-button>
</template>

<style scoped>
.cancel-btn {
  border-radius: 6px !important;
  padding: 0 16px !important;
  background: #F2F3F5 !important;
  border: none !important;
  color: var(--color-text-1, #1F2329) !important;
  height: 28px !important;
  line-height: 28px !important;
  font-size: 14px;
  font-weight: 500;
}

.cancel-btn:hover {
  background: #E5E6EB !important;
  color: var(--color-text-1, #1F2329) !important;
}

.cancel-btn:active {
  background: #C9CDD4 !important;
  color: var(--color-text-1, #1F2329) !important;
}

/* 小尺寸 */
.cancel-btn-small {
  height: 28px !important;
  line-height: 28px !important;
  font-size: 14px;
}

/* 迷你尺寸 */
.cancel-btn-mini {
  height: 24px !important;
  line-height: 24px !important;
  font-size: 12px;
  padding: 0 12px !important;
}

/* 中尺寸 */
.cancel-btn-medium {
  height: 32px !important;
  line-height: 32px !important;
  font-size: 14px;
}

/* 大尺寸 */
.cancel-btn-large {
  height: 40px !important;
  line-height: 40px !important;
  font-size: 16px;
}
</style>
