<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{
  cardTypeId: string
  cardTypeName?: string
}>()

const { t } = useI18n()
</script>

<template>
  <div class="flow-management-tab">
    <a-empty :description="t('admin.cardType.flowManagementInDev')" />
  </div>
</template>

<style scoped>
.flow-management-tab {
  box-sizing: border-box;
  min-height: 240px;
  padding: 24px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
