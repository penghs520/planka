<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { IconEmpty } from '@arco-design/web-vue/es/icon'

const route = useRoute()
const { t } = useI18n()

const titleKey = computed(() => (route.meta.titleKey as string) || '')
</script>

<template>
  <div class="admin-feature-placeholder">
    <div class="inner">
      <div class="icon-wrap">
        <IconEmpty class="empty-icon" />
      </div>
      <h2 class="title">{{ titleKey ? t(titleKey) : '' }}</h2>
      <p class="hint">{{ t('admin.placeholder.comingSoon') }}</p>
    </div>
  </div>
</template>

<style scoped>
.admin-feature-placeholder {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
}

.inner {
  text-align: center;
  max-width: 400px;
}

.icon-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.empty-icon {
  font-size: 48px;
  color: var(--color-text-3);
}

.title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-1);
}

.hint {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-3);
}
</style>
