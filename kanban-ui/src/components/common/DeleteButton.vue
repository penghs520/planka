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
  <a-button class="delete-btn" :class="size ? `delete-btn-${size}` : ''" @click="$emit('click', $event)">
    <slot>{{ text || t('common.action.delete') }}</slot>
  </a-button>
</template>

<style scoped>
.delete-btn {
  border-radius: 6px !important;
  padding: 0 16px !important;
  background: #FEF0F0 !important;
  border: none !important;
  color: #F54A45 !important;
  height: 28px !important;
  line-height: 28px !important;
  font-size: 14px;
  font-weight: 500;
}

.delete-btn:hover {
  background: #FFE8E6 !important;
  color: #F54A45 !important;
}

.delete-btn:active {
  background: #FFD7D6 !important;
  color: #F54A45 !important;
}

/* 小尺寸 */
.delete-btn-small {
  height: 28px !important;
  line-height: 28px !important;
  font-size: 14px;
}

/* 迷你尺寸 */
.delete-btn-mini {
  height: 24px !important;
  line-height: 24px !important;
  font-size: 12px;
  padding: 0 12px !important;
}

/* 中尺寸 */
.delete-btn-medium {
  height: 32px !important;
  line-height: 32px !important;
  font-size: 14px;
}

/* 大尺寸 */
.delete-btn-large {
  height: 40px !important;
  line-height: 40px !important;
  font-size: 16px;
}
</style>
