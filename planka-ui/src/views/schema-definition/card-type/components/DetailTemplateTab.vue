<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { cardDetailTemplateApi } from '@/api/card-detail-template'
import TemplateEditorCore from '@/views/schema-definition/card-detail-template/TemplateEditorCore.vue'
import type { CardDetailTemplateDefinition } from '@/types/card-detail-template'

const { t } = useI18n()

const props = defineProps<{
  cardTypeId: string
  cardTypeName?: string
  /** 与 PageLayoutTab 顶栏右侧对齐，Teleport 挂载点 */
  toolbarActionsTarget?: HTMLElement | null
}>()

const loading = ref(false)
const definition = ref<CardDetailTemplateDefinition | null>(null)
const persisted = ref(false)

async function fetchEffective() {
  if (!props.cardTypeId) return

  loading.value = true
  try {
    const res = await cardDetailTemplateApi.getEffectiveByCardType(props.cardTypeId)
    definition.value = res.definition
    persisted.value = res.persisted
  } catch (error: any) {
    console.error('Failed to fetch effective detail template:', error)
    Message.error(error.message || t('admin.cardType.detailTemplate.loadFailed'))
    definition.value = null
  } finally {
    loading.value = false
  }
}

watch(
  () => props.cardTypeId,
  () => {
    void fetchEffective()
  },
  { immediate: true },
)

function onPersistedUpdate(v: boolean) {
  persisted.value = v
}
</script>

<template>
  <div class="detail-template-tab">
    <a-spin :loading="loading" class="detail-spin">
      <TemplateEditorCore
        v-if="definition && cardTypeId"
        mode="embedded"
        :embedded-card-type-id="cardTypeId"
        :embedded-card-type-name="cardTypeName"
        :embedded-definition="definition"
        :embedded-persisted="persisted"
        :embedded-toolbar-mount="toolbarActionsTarget"
        @update:persisted="onPersistedUpdate"
      />
    </a-spin>
  </div>
</template>

<style scoped lang="scss">
.detail-template-tab {
  display: flex;
  flex-direction: column;
  flex: 1 1 0;
  width: 100%;
  min-width: 0;
  min-height: 0;
}

.detail-spin {
  display: flex;
  flex-direction: column;
  flex: 1 1 0;
  width: 100%;
  min-width: 0;
  min-height: 0;

  :deep(.arco-spin) {
    display: flex;
    flex-direction: column;
    flex: 1 1 0;
    width: 100%;
    min-width: 0;
    min-height: 0;
  }

  :deep(.arco-spin-children) {
    flex: 1 1 0;
    min-width: 0;
    min-height: 0;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 100%;
  }
}
</style>
