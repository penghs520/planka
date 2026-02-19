<template>
  <div class="validation-rule-config">
    <div class="section-header">
      <h4>{{ t('admin.cardType.fieldConfig.validationRules') }}</h4>
      <a-button type="primary" size="small" @click="addRule">
        <template #icon><icon-plus /></template>
        {{ t('admin.cardType.fieldConfig.addValidationRule') }}
      </a-button>
    </div>

    <a-empty
      v-if="!localRules || localRules.length === 0"
      :description="t('admin.cardType.fieldConfig.noValidationRules')"
    />

    <div v-else class="rules-list">
      <a-collapse :default-active-key="[0]" :bordered="false">
        <a-collapse-item
          v-for="(rule, index) in localRules"
          :key="index"
          :header="`规则 ${index + 1}${rule.description ? ': ' + rule.description : ''}`"
        >
          <template #extra>
            <div class="rule-actions" @click.stop>
              <a-switch
                v-model="rule.enabled"
                size="small"
                :checked-text="t('admin.cardType.fieldConfig.ruleEnabled')"
                :unchecked-text="t('admin.cardType.fieldConfig.ruleEnabled')"
                @change="emitUpdate"
              />
              <a-button
                type="text"
                status="danger"
                size="small"
                @click="removeRule(index)"
              >
                <template #icon><icon-delete /></template>
              </a-button>
            </div>
          </template>

          <div class="rule-form">
            <!-- 规则描述 -->
            <a-form-item :label="t('admin.cardType.fieldConfig.ruleDescription')">
              <a-input
                v-model="rule.description"
                :placeholder="t('admin.cardType.fieldConfig.ruleDescriptionPlaceholder')"
                @change="emitUpdate"
              />
            </a-form-item>

            <!-- 校验条件 -->
            <a-form-item :label="t('admin.cardType.fieldConfig.validationCondition')">
              <template #extra>
                <div class="hint-text">
                  {{ t('admin.cardType.fieldConfig.validationConditionHint') }}
                </div>
              </template>
              <ConditionEditor
                v-model="rule.condition"
                :card-type-id="cardTypeId"
                :available-fields="availableFields"
                :link-types="linkTypes"
                @update:model-value="emitUpdate"
              />
            </a-form-item>

            <!-- 错误消息 -->
            <a-form-item
              :label="t('admin.cardType.fieldConfig.errorMessage')"
              required
            >
              <template #extra>
                <div class="hint-text">
                  {{ t('admin.cardType.fieldConfig.errorMessageHint') }}
                </div>
              </template>
              <TextExpressionTemplateEditor
                :model-value="rule.errorMessage ?? ''"
                :field-provider="expressionFieldProvider"
                :placeholder="t('admin.cardType.fieldConfig.errorMessagePlaceholder')"
                @update:model-value="(val: string) => handleErrorMessageUpdate(index, val)"
              />
            </a-form-item>
          </div>
        </a-collapse-item>
      </a-collapse>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconPlus, IconDelete } from '@arco-design/web-vue/es/icon'
import type { ValidationRule } from '@/types/field-config'
import type { FieldOption } from '@/types/field-option'
import type { LinkTypeVO } from '@/types/link-type'
import type { FieldProvider } from '@/components/common/text-expression-template/types'
import ConditionEditor from '@/components/condition/ConditionEditor.vue'
import TextExpressionTemplateEditor from '@/components/common/text-expression-template/TextExpressionTemplateEditor.vue'
import { fieldOptionsApi } from '@/api/field-options'
import { useOrgStore } from '@/stores/org'
import { getMemberFieldsCached } from '@/views/schema-definition/card-type/components/permission/memberFieldsCache'

const { t } = useI18n()
const orgStore = useOrgStore()

/**
 * Props 定义
 */
const props = withDefaults(
  defineProps<{
    /** 校验规则列表（v-model绑定） */
    validationRules?: ValidationRule[]
    /** 属性ID */
    fieldId?: string
    /** 卡片类型ID */
    cardTypeId?: string
    /** 可用字段列表 */
    availableFields?: FieldOption[]
    /** 关联类型列表 */
    linkTypes?: LinkTypeVO[]
    /** 是否禁用 */
    disabled?: boolean
  }>(),
  {
    validationRules: () => [],
    fieldId: undefined,
    cardTypeId: undefined,
    availableFields: () => [],
    linkTypes: () => [],
    disabled: false,
  }
)

/**
 * Emits 定义
 */
const emit = defineEmits<{
  'update:validationRules': [rules: ValidationRule[]]
}>()

/**
 * 本地规则列表
 */
const localRules = ref<ValidationRule[]>([])

/**
 * 监听 props 变化
 */
watch(
  () => props.validationRules,
  (newRules) => {
    localRules.value = newRules ? JSON.parse(JSON.stringify(newRules)) : []
  },
  { immediate: true, deep: true }
)

/**
 * 添加规则
 */
function addRule() {
  const newRule: ValidationRule = {
    condition: undefined,
    errorMessage: '',
    enabled: true,
    description: '',
  }
  localRules.value.push(newRule)
  emitUpdate()
}

/**
 * 删除规则
 */
function removeRule(index: number) {
  localRules.value.splice(index, 1)
  emitUpdate()
}

/**
 * 发送更新事件
 */
function emitUpdate() {
  emit('update:validationRules', localRules.value)
}

/**
 * 处理错误消息更新
 */
function handleErrorMessageUpdate(index: number, value: string) {
  localRules.value[index]!.errorMessage = value
  emitUpdate()
}

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

<style scoped lang="scss">
.validation-rule-config {
  margin-top: 32px;

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    h4 {
      margin: 0;
      font-size: 14px;
      font-weight: 500;
      color: var(--color-text-1);
    }
  }

  .rules-list {
    :deep(.arco-collapse) {
      background: transparent;
    }

    :deep(.arco-collapse-item) {
      margin-bottom: 12px;
      border: 1px solid var(--color-border-1);
      border-radius: var(--radius-md);
      background: var(--color-bg-1);
    }

    :deep(.arco-collapse-item-header) {
      padding: 12px 16px;
      font-weight: 500;
    }

    :deep(.arco-collapse-item-content) {
      padding: 16px;
      border-top: 1px solid var(--color-border-1);
    }
  }

  .rule-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .rule-form {
    :deep(.arco-form-item) {
      margin-bottom: 16px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .hint-text {
      font-size: 12px;
      color: var(--color-text-3);
      margin-top: 4px;
    }
  }
}
</style>
