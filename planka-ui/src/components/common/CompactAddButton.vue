<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineEmits<{
  click: []
}>()
</script>

<template>
  <a-button class="add-rule-btn" @click="$emit('click')">
    <slot>{{ t('admin.cardType.permission.addRule') }}</slot>
  </a-button>
</template>

<style scoped>
.add-rule-btn {
  height: 20px;
  padding: 0 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  background-color: #444444 !important;
  border: none !important;
  color: #fff !important;
}

.add-rule-btn:hover {
  background-color: #333333 !important;
  color: #fff !important;
}
</style>
