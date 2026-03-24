<script setup lang="ts">
/**
 * 字段值显示分发组件
 */
import { computed } from 'vue'
import type { CardDTO, FieldValue } from '@/types/card'
import type { FieldRenderConfig, NumberRenderConfig, DateRenderConfig, EnumOptionDTO, StatusOption } from '@/types/view-data'
import { isBuiltinEnumField, getBuiltinEnumOptions, getEnumSelectedOptions, renderFieldValue } from '@/utils/field-render'
import { isValidLinkFieldId } from '@/utils/link-field-utils'
import FieldTextDisplay from './FieldTextDisplay.vue'
import FieldNumberDisplay from './FieldNumberDisplay.vue'
import FieldDateDisplay from './FieldDateDisplay.vue'
import FieldEnumDisplay from './FieldEnumDisplay.vue'
import FieldLinkDisplay from './FieldLinkDisplay.vue'
import FieldCascadeDisplay from './FieldCascadeDisplay.vue'
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

// 判断是否无权限
const isNoPermission = computed(() => {
  if (props.fieldValue?.permissionStatus === 'NO_PERMISSION') return true
  if (isLink.value && props.card?.linkedCardPermissions?.[props.fieldId] === 'NO_PERMISSION') return true
  return false
})

// 是否为内置日期字段
const isBuiltinDate = computed(() => {
  return ['$createdAt', '$updatedAt', '$archivedAt', '$discardedAt'].includes(props.fieldId)
})

// 是否为内置文本字段
const isBuiltinText = computed(() => {
  return ['$code', '$description'].includes(props.fieldId)
})

// 判断字段类型
const fieldType = computed(() => {
  if (actualFieldValue.value?.type) {
    return actualFieldValue.value.type
  }
  // 如果没有 fieldValue，尝试从 renderConfig 或 fieldId 推断
  if (props.renderConfig?.type) {
    return props.renderConfig.type
  }
  // 内置日期字段
  if (isBuiltinDate.value) {
    return 'DATE'
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
  // 对于内置字段，使用 renderFieldValue 获取显示值（因为值存储在 card 的一级属性中）
  if ((isBuiltinDate.value || isBuiltinText.value) && props.card) {
    const displayValue = renderFieldValue(props.card, props.fieldId)
    // 对于日期字段，需要将 ISO 字符串转换为时间戳
    if (isBuiltinDate.value) {
      const dateStr = props.fieldId === '$createdAt'
        ? props.card.createdAt
        : props.fieldId === '$updatedAt'
          ? props.card.updatedAt
          : props.fieldId === '$archivedAt'
            ? props.card.archivedAt
            : props.card.abandonedAt
      return dateStr ? new Date(dateStr).getTime() : null
    }
    return displayValue
  }
  return actualFieldValue.value?.value
})
</script>

<template>
  <span v-if="isNoPermission" class="field-no-permission">***</span>
  <FieldEnumDisplay
    v-else-if="isEnum"
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
  <FieldCascadeDisplay
    v-else-if="fieldType === 'CASCADE'"
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

<style scoped>
.field-no-permission {
  color: var(--color-text-3);
  font-style: italic;
}
</style>
