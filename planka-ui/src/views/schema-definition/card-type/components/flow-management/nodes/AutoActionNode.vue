<script setup lang="ts">
/**
 * 自动执行节点（横向流：左入右出）
 */
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  data: {
    label: string
    actionsCount?: number
  }
}>()

const { t } = useI18n()

const subtitle = computed(() =>
  t('admin.workflow.actionsCount', { count: props.data.actionsCount ?? 0 }),
)
</script>

<template>
  <div class="wf-node-card wf-node-card--auto-action">
    <Handle id="wf-in" type="target" :position="Position.Left" class="wf-handle wf-handle--target" />
    <div class="wf-node-card__header">
      <div class="wf-node-card__icon wf-node-card__icon--auto">
        <icon-thunderbolt />
      </div>
      <div class="wf-node-card__text">
        <div class="wf-node-card__title">{{ data.label }}</div>
        <div class="wf-node-card__subtitle">
          {{ subtitle }}
        </div>
      </div>
    </div>
    <Handle id="wf-out" type="source" :position="Position.Right" class="wf-handle wf-handle--source" />
  </div>
</template>

<style scoped lang="scss">
.wf-node-card {
  min-width: 240px;
  max-width: 280px;
  padding: 0;
  background: #fff;
  border: 1px solid var(--color-border-1);
  border-radius: 10px;
  box-shadow:
    0 2px 8px rgba(31, 35, 41, 0.06),
    0 0 1px rgba(31, 35, 41, 0.04);
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
  user-select: none;

  &:hover {
    border-color: #c9cdd4;
    box-shadow:
      0 6px 20px rgba(31, 35, 41, 0.08),
      0 0 1px rgba(31, 35, 41, 0.06);
  }
}

.wf-node-card__header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
}

.wf-node-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #fff;
  font-size: 18px;
  flex-shrink: 0;
}

.wf-node-card__icon--auto {
  background: linear-gradient(135deg, #ff9500 0%, #e68600 100%);
}

.wf-node-card__text {
  min-width: 0;
  flex: 1;
}

.wf-node-card__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
  line-height: 1.35;
  word-break: break-word;
}

.wf-node-card__subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-text-3);
  line-height: 1.4;
}
</style>
