<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps<{
  loading?: boolean
  disabled?: boolean
  text?: string
  size?: 'mini' | 'small' | 'medium' | 'large'
}>()

defineEmits<{
  (e: 'click', event: Event): void
}>()
</script>

<template>
  <a-button
    class="save-btn"
    :class="size ? `save-btn-${size}` : ''"
    type="primary"
    :loading="loading"
    :disabled="disabled"
    @click="$emit('click', $event)"
  >
    <slot>{{ text || t('common.action.save') }}</slot>
  </a-button>
</template>

<style scoped>
.save-btn {
  border-radius: 6px !important;
  padding: 0 16px !important;
  background: var(--color-primary) !important;
  border: none !important;
  box-shadow: none !important;
  height: 28px !important;
  line-height: 28px !important;
  font-size: 14px;
  font-weight: 500;
  color: #fff !important;
}

.save-btn:hover {
  background: var(--color-primary-hover) !important;
}

.save-btn:active {
  background: var(--color-primary-active) !important;
}

/* 禁用：需覆盖上方 !important，否则与可点击主按钮难以区分 */
.save-btn.arco-btn-disabled,
.save-btn[disabled] {
  background: var(--color-fill-2) !important;
  color: var(--color-text-4) !important;
  cursor: not-allowed !important;
}

.save-btn.arco-btn-disabled:hover,
.save-btn[disabled]:hover,
.save-btn.arco-btn-disabled:active,
.save-btn[disabled]:active,
.save-btn.arco-btn-disabled:focus-visible,
.save-btn[disabled]:focus-visible {
  background: var(--color-fill-2) !important;
  color: var(--color-text-4) !important;
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
