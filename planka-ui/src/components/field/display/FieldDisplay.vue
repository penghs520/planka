<script setup lang="ts">
/**
 * 字段值显示分发组件
 */
import { computed } from 'vue'
import type { CardDTO, FieldValue } from '@/types/card'
import type { FieldRenderConfig, NumberRenderConfig, DateRenderConfig, EnumOptionDTO, StatusOption } from '@/types/view-data'
import { isBuiltinEnumField, getBuiltinEnumOptions, getEnumSelectedOptions } from '@/utils/field-render'
import { isValidLinkFieldId } from '@/utils/link-field-utils'
import FieldTextDisplay from './FieldTextDisplay.vue'
import FieldNumberDisplay from './FieldNumberDisplay.vue'
import FieldDateDisplay from './FieldDateDisplay.vue'
import FieldEnumDisplay from './FieldEnumDisplay.vue'
import FieldLinkDisplay from './FieldLinkDisplay.vue'
import FieldStructureDisplay from './FieldStructureDisplay.vue'
import FieldWebUrlDisplay from './FieldWebUrlDisplay.vue'
import FieldMarkdownDisplay from './FieldMarkdownDisplay.vue'

const props = defineProps<{
  /** 字段 ID（用于判断内置枚举字段、关联字段等） */
  fieldId: string
  /** 字段值 */
  fieldValue?: FieldValue | null
  /** 渲染配置 */
  renderConfig?: FieldRenderConfig | null
  /** 空值占位符 */
  placeholder?: string
  /** 卡片数据（用于内置枚举字段、关联字段） */
  card?: CardDTO | null
  /** 状态选项列表（用于 $statusId 字段） */
  statusOptions?: StatusOption[]
  /** 固定高度（px），用于描述等特殊字段 */
  height?: number
}>()

// 判断字段类型
const fieldType = computed(() => {
  if (actualFieldValue.value?.type) {
    return actualFieldValue.value.type
  }
  // 如果没有 fieldValue，尝试从 renderConfig 或 fieldId 推断
  if (props.renderConfig?.type) {
    return props.renderConfig.type
  }
  // 通过 fieldId 格式检测链接字段
  if (isValidLinkFieldId(props.fieldId)) {
    return 'LINK'
  }
  return 'TEXT'
})

// 是否为内置枚举字段
const isBuiltinEnum = computed(() => {
  return props.fieldId && isBuiltinEnumField(props.fieldId)
})

// 是否为枚举类型
const isEnum = computed(() => {
  return fieldType.value === 'ENUM' || isBuiltinEnum.value
})

// 是否为关联字段
const isLink = computed(() => {
  // 通过 fieldType 或者 fieldId 格式判断
  return fieldType.value === 'LINK' || isValidLinkFieldId(props.fieldId)
})

// 获取实际的字段值（优先使用传入的 fieldValue，否则从 card 中获取）
const actualFieldValue = computed<FieldValue | null>(() => {
  if (props.fieldValue) {
    return props.fieldValue
  }
  if (props.card?.fieldValues?.[props.fieldId]) {
    return props.card.fieldValues[props.fieldId] ?? null
  }
  return null
})

// 获取枚举选中的选项列表
const enumOptions = computed<EnumOptionDTO[]>(() => {
  // 内置枚举字段
  if (isBuiltinEnum.value && props.fieldId && props.card) {
    return getBuiltinEnumOptions(props.card, props.fieldId, props.statusOptions)
  }
  // 普通枚举字段
  const enumValue = actualFieldValue.value?.type === 'ENUM' ? actualFieldValue.value : null
  if (enumValue) {
    return getEnumSelectedOptions(enumValue, props.renderConfig)
  }
  return []
})

// 获取关联卡片列表
const linkedCards = computed(() => {
  if (props.card?.linkedCards?.[props.fieldId]) {
    return props.card.linkedCards[props.fieldId]
  }
  return []
})

// 获取通用值
const value = computed(() => {
  return actualFieldValue.value?.value
})
</script>

<template>
  <FieldEnumDisplay
    v-if="isEnum"
    :options="enumOptions"
    :placeholder="placeholder"
  />
  <FieldLinkDisplay
    v-else-if="isLink"
    :linked-cards="linkedCards"
    :placeholder="placeholder"
  />
  <FieldNumberDisplay
    v-else-if="fieldType === 'NUMBER'"
    :value="value as number"
    :render-config="renderConfig as NumberRenderConfig"
    :placeholder="placeholder"
  />
  <FieldDateDisplay
    v-else-if="fieldType === 'DATE'"
    :value="value as number"
    :render-config="renderConfig as DateRenderConfig"
    :placeholder="placeholder"
  />
  <FieldStructureDisplay
    v-else-if="fieldType === 'STRUCTURE'"
    :value="value"
    :placeholder="placeholder"
  />
  <FieldWebUrlDisplay
    v-else-if="fieldType === 'WEB_URL'"
    :value="value as string"
    :placeholder="placeholder"
  />
  <FieldMarkdownDisplay
    v-else-if="fieldType === 'MARKDOWN'"
    :value="value as string"
    :placeholder="placeholder"
    :height="height"
  />
  <FieldTextDisplay
    v-else
    :value="value as string"
    :placeholder="placeholder"
  />
</template>
