<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { cardTypeApi } from '@/api'
import CardTypeSelect from '@/components/card-type/CardTypeSelect.vue'
import type { LinkFieldQuickCreateState } from './link-field-quick-types'

const props = defineProps<{
  cardTypeId: string
}>()

const state = defineModel<LinkFieldQuickCreateState>({ required: true })

const { t } = useI18n()

interface CardTypeOption {
  id: string
  name: string
  schemaSubType: string
}

const cardTypes = ref<CardTypeOption[]>([])

const cardTypeNameMap = computed(() => {
  const m: Record<string, string> = {}
  for (const ct of cardTypes.value) {
    m[ct.id] = ct.name
  }
  return m
})

function joinNames(ids: string[]): string {
  return ids.map((id) => cardTypeNameMap.value[id] || id).filter(Boolean).join('、')
}

const sourceCardTypeName = computed(() =>
  joinNames([props.cardTypeId].filter(Boolean))
)

const targetCardTypeName = computed(() => joinNames(state.value.peerCardTypeIds))

/** 对侧实体类型：下拉单选，与 state.peerCardTypeIds（0~1 个元素）同步 */
const peerCardTypeIdModel = computed({
  get: () => state.value.peerCardTypeIds[0] ?? '',
  set: (id: string) => {
    state.value.peerCardTypeIds = id ? [id] : []
  },
})

onMounted(async () => {
  try {
    cardTypes.value = await cardTypeApi.listOptions()
  } catch (e) {
    console.error('Failed to load card types for link field quick create', e)
  }
})

function validate(): boolean {
  if (!state.value.targetName?.trim()) {
    Message.error(t('admin.linkType.requiredTargetName'))
    return false
  }
  if (!state.value.peerCardTypeIds.length) {
    Message.error(t('admin.linkType.requiredTargetCardType'))
    return false
  }
  return true
}

defineExpose({ validate })
</script>

<template>
  <div class="link-quick-section">
    <a-row :gutter="16">
      <a-col :span="24">
        <a-form-item :label="t('admin.linkType.targetCardTypeLabel')" required>
          <CardTypeSelect
            v-model="peerCardTypeIdModel"
            :multiple="false"
            :placeholder="t('admin.cardType.fieldConfig.linkFieldQuickCreate.peerSidePlaceholder')"
            :limit-concrete-single="true"
            :options="cardTypes"
          />
        </a-form-item>
      </a-col>
    </a-row>

    <a-row :gutter="16" class="name-row">
      <a-col :span="24">
        <a-form-item>
          <template #label>
            <span v-if="sourceCardTypeName && targetCardTypeName">
              {{ t('admin.linkType.targetNameDynamic', { source: sourceCardTypeName, target: targetCardTypeName }) }}
            </span>
            <span v-else>
              {{ t('admin.linkType.targetName') }}
              <span class="label-hint">{{ t('admin.linkType.sourceNameHint') }}</span>
            </span>
          </template>
          <a-input
            v-model="state.targetName"
            :placeholder="t('admin.linkType.targetNamePlaceholder', { source: sourceCardTypeName || '…' })"
            :max-length="20"
            :disabled="!sourceCardTypeName || !targetCardTypeName"
          />
        </a-form-item>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :span="24">
        <a-form-item :label="t('admin.linkType.targetCode')">
          <a-input
            v-model="state.targetCode"
            :placeholder="t('admin.linkType.targetCodePlaceholder')"
            :max-length="50"
          />
        </a-form-item>
      </a-col>
    </a-row>

    <a-form-item :label="t('admin.linkType.descriptionLabel')">
      <a-textarea
        v-model="state.description"
        :placeholder="t('admin.linkType.descriptionPlaceholder')"
        :max-length="200"
        :auto-size="{ minRows: 2, maxRows: 4 }"
      />
    </a-form-item>
  </div>
</template>

<style scoped>
/* 与上方 basic-info-grid 一致，避免整行拉满 */
.link-quick-section {
  margin-top: 24px;
}

.link-quick-section :deep(.arco-form-item) {
  max-width: 320px;
}

.label-hint {
  font-size: 12px;
  font-weight: normal;
  color: var(--color-text-3);
}

.name-row {
  margin-top: 4px;
}
</style>
