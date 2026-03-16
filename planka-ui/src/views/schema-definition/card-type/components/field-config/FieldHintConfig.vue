<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import TextExpressionTemplateEditor from '@/components/common/text-expression-template/TextExpressionTemplateEditor.vue'
import type { FieldProvider } from '@/components/common/text-expression-template/types'
import type { FieldOption } from '@/types/field-option'
import { fieldOptionsApi } from '@/api/field-options'
import { useOrgStore } from '@/stores/org'
import { getMemberFieldsCached } from '@/views/schema-definition/card-type/components/permission/memberFieldsCache'

const { t } = useI18n()
const orgStore = useOrgStore()

interface Props {
  hintText?: string
  showHintBelowLabel?: boolean
  showHintAsTooltip?: boolean
  disabled?: boolean
  cardTypeId?: string
}

const props = withDefaults(defineProps<Props>(), {
  hintText: '',
  showHintBelowLabel: false,
  showHintAsTooltip: false,
  disabled: false,
  cardTypeId: undefined,
})

const emit = defineEmits<{
  'update:hintText': [value: string]
  'update:showHintBelowLabel': [value: boolean]
  'update:showHintAsTooltip': [value: boolean]
}>()

const hintTextValue = computed({
  get: () => props.hintText || '',
  set: (value: string) => emit('update:hintText', value),
})

const showHintBelowLabelValue = computed({
  get: () => props.showHintBelowLabel ?? false,
  set: (value: boolean) => emit('update:showHintBelowLabel', value),
})

const showHintAsTooltipValue = computed({
  get: () => props.showHintAsTooltip ?? false,
  set: (value: boolean) => emit('update:showHintAsTooltip', value),
})

// 表达式模板编辑器的字段提供者（带请求缓存）
let cardFieldsCache: Promise<FieldOption[]> | null = null
const linkFieldsCache = new Map<string, Promise<FieldOption[]>>()

const expressionFieldProvider: FieldProvider = {
  getCardFields: () => {
    if (!cardFieldsCache && props.cardTypeId) {
      cardFieldsCache = fieldOptionsApi.getFields(props.cardTypeId)
    }
    return cardFieldsCache ?? Promise.resolve([])
  },
  getMemberFields: () => getMemberFieldsCached(orgStore.currentOrg?.memberCardTypeId),
  getFieldsByLinkFieldId: (id: string) => {
    let cached = linkFieldsCache.get(id)
    if (!cached) {
      cached = fieldOptionsApi.getFieldsByLinkFieldId(id)
      linkFieldsCache.set(id, cached)
    }
    return cached
  },
}
</script>

<template>
  <div class="field-hint-config">
    <div class="section-header">
      <span class="section-title">{{ t('admin.cardType.fieldConfig.hintConfig') }}</span>
    </div>

    <!-- 提示信息文案 -->
    <div class="config-row">
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.hintText') }}</label>
        <TextExpressionTemplateEditor
          :model-value="hintTextValue"
          :field-provider="expressionFieldProvider"
          :placeholder="t('admin.cardType.fieldConfig.hintTextPlaceholder')"
          :disabled="disabled"
          @update:model-value="hintTextValue = $event"
        />
        <span class="field-hint">{{ t('admin.cardType.fieldConfig.hintTextHelp') }}</span>
      </div>
    </div>

    <!-- 显示方式选项 - 只有当输入了提示文案时才显示 -->
    <div v-if="hintTextValue?.trim()" class="config-row">
      <div class="config-field">
        <label class="field-label">{{ t('admin.cardType.fieldConfig.hintDisplayMode') }}</label>
        <div class="checkbox-group">
          <a-checkbox
            v-model="showHintBelowLabelValue"
            :disabled="disabled"
          >
            {{ t('admin.cardType.fieldConfig.showHintBelowLabel') }}
          </a-checkbox>
          <a-checkbox
            v-model="showHintAsTooltipValue"
            :disabled="disabled"
          >
            {{ t('admin.cardType.fieldConfig.showHintAsTooltip') }}
          </a-checkbox>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.field-hint-config {
  margin-top: 24px;
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-1);
  letter-spacing: 0.02em;
}

.config-row {
  margin-bottom: 16px;
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
}

.field-hint {
  font-size: 12px;
  color: var(--color-text-3);
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  background: var(--color-fill-1);
  border-radius: 6px;

  :deep(.arco-checkbox) {
    font-size: 13px;
    color: var(--color-text-1);
  }
}
</style>
