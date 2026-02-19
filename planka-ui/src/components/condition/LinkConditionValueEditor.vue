<template>
  <div class="link-condition-value-editor">
    <!-- 操作符选择 -->
    <a-select
      :model-value="operatorType"
      size="small"
      style="width: 120px"
      @update:model-value="handleOperatorChange"
    >
      <a-option value="HAS_ANY">{{ t('common.linkValue.hasAny') }}</a-option>
      <a-option value="IS_EMPTY">{{ t('common.linkValue.isEmpty') }}</a-option>
      <a-option value="IN">{{ t('common.linkValue.inList') }}</a-option>
      <a-option value="NOT_IN">{{ t('common.linkValue.notInList') }}</a-option>
    </a-select>

    <!-- IN/NOT_IN 时显示值选择器（静态值和引用值集成在同一个下拉框） -->
    <LinkValueSelector
      v-if="needsCardSelection"
      :model-value="linkValue"
      :link-field-id="linkFieldId"
      :selected-cards="selectedCardInfos"
      :placeholder="t('common.linkValue.selectCard')"
      style="flex: 1; min-width: 180px"
      @update:model-value="handleLinkValueChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { LinkOperator, LinkValue } from '@/types/condition'
import LinkValueSelector, { type CardInfo } from './LinkValueSelector.vue'

const { t } = useI18n()

/**
 * Props定义
 */
const props = withDefaults(
  defineProps<{
    /** 当前操作符 */
    modelValue: LinkOperator

    /** 关联字段ID，格式 "{linkTypeId}:{SOURCE|TARGET}" */
    linkFieldId?: string

    /** 已选中卡片的详细信息（用于回显） */
    selectedCardInfos?: CardInfo[]
  }>(),
  {
    linkFieldId: '',
    selectedCardInfos: () => [],
  }
)

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: LinkOperator]
}>()

/**
 * 当前操作符类型
 */
const operatorType = computed(() => props.modelValue.type)

/**
 * 是否需要卡片选择
 */
const needsCardSelection = computed(() => {
  return operatorType.value === 'IN' || operatorType.value === 'NOT_IN'
})

/**
 * 当前 LinkValue（用于 LinkValueSelector）
 */
const linkValue = computed(() => {
  if (needsCardSelection.value) {
    const op = props.modelValue as { type: 'IN' | 'NOT_IN'; value?: LinkValue }
    return op.value
  }
  return undefined
})

/**
 * 处理操作符变化
 */
function handleOperatorChange(type: LinkOperator['type']) {
  let newOperator: LinkOperator

  switch (type) {
    case 'HAS_ANY':
      newOperator = { type: 'HAS_ANY' }
      break
    case 'IS_EMPTY':
      newOperator = { type: 'IS_EMPTY' }
      break
    case 'IN':
      newOperator = {
        type: 'IN',
        value: { type: 'STATIC', cardIds: [] },
      }
      break
    case 'NOT_IN':
      newOperator = {
        type: 'NOT_IN',
        value: { type: 'STATIC', cardIds: [] },
      }
      break
    default:
      newOperator = { type: 'HAS_ANY' }
  }

  emit('update:modelValue', newOperator)
}

/**
 * 处理 LinkValue 变化
 */
function handleLinkValueChange(value: LinkValue) {
  if (operatorType.value === 'IN' || operatorType.value === 'NOT_IN') {
    const newOperator: LinkOperator = {
      type: operatorType.value,
      value,
    }
    emit('update:modelValue', newOperator)
  }
}
</script>

<style scoped lang="scss">
.link-condition-value-editor {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
</style>
