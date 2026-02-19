<script setup lang="ts">
import { computed } from 'vue'
import {
  IconFontColors,
  IconCalendar,
  IconMenu,
  IconAttachment,
  IconLink,
} from '@arco-design/web-vue/es/icon'
import type { Component } from 'vue'
import IconMarkdown from '@/components/icons/IconMarkdown.vue'
import IconMultiLineText from '@/components/icons/IconMultiLineText.vue'
import IconStructure from '@/components/icons/IconStructure.vue'
import IconNumber from '@/components/icons/IconNumber.vue'
import IconLinkType from '@/components/icons/IconLinkType.vue'

export interface FieldTypeIconProps {
  /** 字段类型（SchemaSubType） */
  fieldType: string
  /** 图标大小 */
  size?: 'small' | 'medium' | 'large'
  /** 图标颜色 */
  color?: string
}

const props = withDefaults(defineProps<FieldTypeIconProps>(), {
  size: 'medium',
  color: 'var(--color-text-3)',
})

// 基础字段类型到图标组件的映射
const baseIconMap: Record<string, Component> = {
  SINGLE_LINE_TEXT: IconFontColors,
  MULTI_LINE_TEXT: IconMultiLineText,
  MARKDOWN: IconMarkdown,
  NUMBER: IconNumber,
  DATE: IconCalendar,
  ENUM: IconMenu,
  ATTACHMENT: IconAttachment,
  WEB_URL: IconLink,
  STRUCTURE: IconStructure,
  LINK: IconLinkType,
}

// 字段类型到图标组件的映射（支持 FieldType、SchemaSubType）
const fieldTypeIconMap: Record<string, Component> = {
  // FieldType（基础类型）
  ...baseIconMap,

  // FieldDefinition 类型
  SINGLE_LINE_TEXT_FIELD_DEFINITION: IconFontColors,
  MULTI_LINE_TEXT_FIELD_DEFINITION: IconMultiLineText,
  MARKDOWN_FIELD_DEFINITION: IconMarkdown,
  NUMBER_FIELD_DEFINITION: IconNumber,
  DATE_FIELD_DEFINITION: IconCalendar,
  ENUM_FIELD_DEFINITION: IconMenu,
  ATTACHMENT_FIELD_DEFINITION: IconAttachment,
  WEB_URL_FIELD_DEFINITION: IconLink,
  STRUCTURE_FIELD_DEFINITION: IconStructure,
  LINK_FIELD_DEFINITION: IconLinkType,

  // FieldConfig 类型（使用新的 _FIELD 后缀）
  TEXT_FIELD: IconFontColors,
  MULTI_LINE_TEXT_FIELD: IconMultiLineText,
  MARKDOWN_FIELD: IconMarkdown,
  NUMBER_FIELD: IconNumber,
  DATE_FIELD: IconCalendar,
  ENUM_FIELD: IconMenu,
  ATTACHMENT_FIELD: IconAttachment,
  WEB_URL_FIELD: IconLink,
  STRUCTURE_FIELD: IconStructure,
  LINK_FIELD: IconLinkType,
}

// 获取字段类型对应的图标组件（改为 computed，使其响应式）
const iconComponent = computed(() => {
  return fieldTypeIconMap[props.fieldType] || IconFontColors
})

// 大小映射
const sizeMap = {
  small: '14px',
  medium: '16px',
  large: '20px',
}
</script>

<template>
  <component
    :is="iconComponent"
    class="field-type-icon"
    :style="{
      fontSize: sizeMap[size],
      color: color,
    }"
  />
</template>

<style scoped lang="scss">
.field-type-icon {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
