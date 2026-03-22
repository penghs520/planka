<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import {
  WORKSPACE_CREATE_VIEW_TYPE_OPTIONS,
  type WorkspaceCreateViewKind,
} from '@/constants/workspace-create-view'

defineProps<{
  pickedKind: WorkspaceCreateViewKind | null
}>()

const emit = defineEmits<{
  select: [kind: WorkspaceCreateViewKind]
}>()

const { t } = useI18n()

function onSelect(kind: WorkspaceCreateViewKind, enabled: boolean) {
  if (!enabled) return
  emit('select', kind)
}
</script>

<template>
  <div class="drawer-content drawer-content--pick-type">
    <p class="pick-type-hint">
      {{ t('sidebar.createViewPickTypeHint') }}
    </p>
    <div class="view-type-grid">
      <button
        v-for="opt in WORKSPACE_CREATE_VIEW_TYPE_OPTIONS"
        :key="opt.kind"
        type="button"
        class="view-type-card"
        :class="{
          'view-type-card--selected': pickedKind === opt.kind,
          'view-type-card--disabled': !opt.enabled,
        }"
        :disabled="!opt.enabled"
        @click="onSelect(opt.kind, opt.enabled)"
      >
        <div class="view-type-card-head">
          <span class="view-type-card-title">{{ t(opt.titleKey) }}</span>
        </div>
        <p class="view-type-card-desc">
          {{ t(opt.descriptionKey) }}
        </p>
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.drawer-content {
  padding: 0 16px;
}

.drawer-content--pick-type {
  padding-top: 8px;
}

.pick-type-hint {
  margin: 0 0 16px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--color-text-2);
}

.view-type-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.view-type-card {
  display: block;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  background: var(--color-bg-2);
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.view-type-card:hover:not(:disabled) {
  border-color: rgb(var(--primary-6));
  background: var(--color-fill-1);
}

.view-type-card--selected:not(:disabled) {
  border-color: rgb(var(--primary-6));
  background: var(--color-primary-light-1);
}

.view-type-card--disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.view-type-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.view-type-card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-1);
}

.view-type-card-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
  color: var(--color-text-3);
}
</style>
