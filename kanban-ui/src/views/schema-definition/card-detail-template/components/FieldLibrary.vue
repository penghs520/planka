<script setup lang="ts">
import { ref, computed } from 'vue'
import { IconSearch, IconRefresh } from '@arco-design/web-vue/es/icon'
import FieldTypeIcon from '@/components/field/FieldTypeIcon.vue'
import type { FieldConfig } from '@/types/card-type'
import { getFieldTypeFromConfig } from '@/types/card-type'
import { isBuiltinField } from '@/types/builtin-field'

/** 扩展字段配置，支持内置字段标记 */
type ExtendedFieldConfig = FieldConfig & { builtin?: boolean }

const props = defineProps<{
  fields: FieldConfig[]
  loading: boolean
  usedFieldIds: string[]
}>()

const emit = defineEmits<{
  (e: 'drag-start', field: FieldConfig, event: DragEvent): void
  (e: 'refresh'): void
}>()

// 刷新字段列表
function handleRefresh() {
  emit('refresh')
}

// 搜索关键词
const searchKeyword = ref('')

// 字段类型配置：定义类型顺序和显示名称
const fieldTypeConfig: Record<string, { name: string; order: number }> = {
  TEXT: { name: '文本', order: 1 },
  NUMBER: { name: '数字', order: 2 },
  DATE: { name: '日期', order: 3 },
  ENUM: { name: '枚举', order: 4 },
  DESCRIPTION: { name: '描述', order: 5 },
  ATTACHMENT: { name: '附件', order: 6 },
  WEB_URL: { name: '网页链接', order: 7 },
  STRUCTURE: { name: '架构层级', order: 8 },
  LINK: { name: '关联', order: 9 },
  BUILTIN: { name: '内置属性', order: 99 },
}

// 按类型分组后的字段
const categorizedFields = computed(() => {
  const categories: Record<string, { name: string; order: number; fields: ExtendedFieldConfig[] }> = {}

  props.fields.forEach((field) => {
    const extField = field as ExtendedFieldConfig
    // 搜索过滤
    if (searchKeyword.value) {
      const keyword = searchKeyword.value.toLowerCase()
      if (!field.name.toLowerCase().includes(keyword)) {
        return
      }
    }
    // 内置字段单独分组
    const isBuiltin = extField.builtin || isBuiltinField(field.fieldId)
    const fieldType = isBuiltin ? 'BUILTIN' : getFieldTypeFromConfig(field.schemaSubType)
    if (!categories[fieldType]) {
      const config = fieldTypeConfig[fieldType] || { name: fieldType, order: 99 }
      categories[fieldType] = { name: config.name, order: config.order, fields: [] }
    }
    categories[fieldType].fields.push(extField)
  })

  // 每个类型内按名称排序
  Object.values(categories).forEach((category) => {
    category.fields.sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
  })

  // 按类型顺序排序后返回
  return Object.entries(categories)
    .sort((a, b) => a[1].order - b[1].order)
    .reduce(
      (acc, [key, value]) => {
        acc[key] = value
        return acc
      },
      {} as Record<string, { name: string; order: number; fields: ExtendedFieldConfig[] }>,
    )
})

// 展开的分类（默认展开所有类型）
const expandedKeys = ref(Object.keys(fieldTypeConfig))

// 是否已使用
function isUsed(fieldId: string): boolean {
  return props.usedFieldIds.includes(fieldId)
}

// 开始拖拽
function handleDragStart(field: FieldConfig, event: DragEvent) {
  if (isUsed(field.fieldId)) {
    event.preventDefault()
    return
  }
  event.dataTransfer?.setData('application/json', JSON.stringify({ type: 'field', field }))
  event.dataTransfer!.effectAllowed = 'copy'
  emit('drag-start', field, event)
}
</script>

<template>
  <div class="field-library">
    <div class="library-header">
      <div class="library-title-row">
        <h3 class="library-title">字段资源库</h3>
        <a-tooltip content="刷新字段列表">
          <a-button size="mini" type="text" :loading="loading" @click="handleRefresh">
            <template #icon><IconRefresh /></template>
          </a-button>
        </a-tooltip>
      </div>
      <a-input
        v-model="searchKeyword"
        placeholder="搜索字段"
        size="mini"
        allow-clear
      >
        <template #prefix>
          <IconSearch />
        </template>
      </a-input>
    </div>

    <div class="library-content">
      <a-spin :loading="loading" class="library-spin">
        <a-collapse v-model:active-key="expandedKeys" :bordered="false" expand-icon-position="right">
          <a-collapse-item
            v-for="(category, key) in categorizedFields"
            :key="key"
          >
            <template #header>
              <div class="category-header">
                <FieldTypeIcon :field-type="`${key}_FIELD_CONFIG`" class="category-icon" />
                <span>{{ category.name }} ({{ category.fields.length }})</span>
              </div>
            </template>
            <div class="field-list">
              <div
                v-for="field in category.fields"
                :key="field.fieldId"
                class="field-item"
                :class="{
                  'is-used': isUsed(field.fieldId),
                  'is-builtin': field.builtin || isBuiltinField(field.fieldId)
                }"
                :draggable="!isUsed(field.fieldId)"
                @dragstart="handleDragStart(field, $event)"
              >
                <span class="field-name">{{ field.name }}</span>
                <a-tag v-if="isUsed(field.fieldId)" size="small" color="gray">已使用</a-tag>
              </div>
              <a-empty
                v-if="category.fields.length === 0"
                description="暂无字段"
                class="empty-hint"
              />
            </div>
          </a-collapse-item>
        </a-collapse>

        <a-empty
          v-if="fields.length === 0 && !loading"
          description="请先选择卡片类型"
          class="empty-hint-main"
        />
      </a-spin>
    </div>
  </div>
</template>

<style scoped lang="scss">
.field-library {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.library-header {
  padding: 12px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.library-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;

  :deep(.arco-btn) {
    color: var(--color-text-3);

    &:hover {
      color: var(--color-text-2);
    }

    &:focus-visible,
    &:focus {
      outline: none;
      box-shadow: none;
    }
  }
}

.library-title {
  font-size: 13px;
  font-weight: 500;
  margin: 0;
  color: var(--color-text-1);
}

.library-content {
  flex: 1;
  overflow-y: auto;
  padding: 4px 4px 4px 0;
  width: 100%;
  box-sizing: border-box;
}

.library-spin {
  min-height: 100px;
  width: 100%;
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 22px;
  padding-right: 12px;
}

.field-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: grab;
  transition: all 0.15s;
  font-size: 12px;
  background: var(--color-fill-1);
  border: 1px solid var(--color-border-2);

  &:hover:not(.is-used) {
    background: var(--color-fill-2);
    border-color: var(--color-border-3);
  }

  &.is-used {
    cursor: not-allowed;
    opacity: 0.5;
  }

  &:active:not(.is-used) {
    cursor: grabbing;
  }

  &.is-builtin {
    border-style: dashed;
    border-color: var(--color-border-3);
  }
}

.category-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.category-icon {
  flex-shrink: 0;
}

.field-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-hint {
  padding: 16px 0;
}

.empty-hint-main {
  padding: 32px 0;
}

:deep(.arco-collapse) {
  width: 100%;
}

:deep(.arco-collapse-item) {
  width: 100%;
  border-bottom: none !important;
}

:deep(.arco-collapse-item-wrap) {
  width: 100%;
}

:deep(.arco-collapse-item-header) {
  font-size: 12px;
  border-bottom: none !important;
}

:deep(.arco-collapse-item-content) {
  padding: 0;
  background: transparent;
  width: 100%;
}

:deep(.arco-collapse-item-content-box) {
  background: transparent;
  width: 100%;
}
</style>
