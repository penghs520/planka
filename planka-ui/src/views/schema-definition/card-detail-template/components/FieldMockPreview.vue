<script setup lang="ts">
import { inject } from 'vue'
import type { FieldConfig, DateFieldConfig, EnumFieldConfig, NumberFieldConfig } from '@/types/card-type'
import { getFieldTypeFromConfig } from '@/types/card-type'

const props = defineProps<{
  fieldConfigId: string
}>()

// props 在模板中使用
void props

// 注入字段列表
const fields = inject<{ value: FieldConfig[] }>('fields', { value: [] })

// 获取字段信息
function getFieldInfo(fieldConfigId: string): FieldConfig | undefined {
  return fields.value.find((f) => f.fieldId === fieldConfigId)
}

// 获取字段类型
function getFieldType(fieldConfigId: string): string {
  const field = getFieldInfo(fieldConfigId)
  return field ? getFieldTypeFromConfig(field.schemaSubType) : 'TEXT'
}

// 获取日期字段的模拟显示值
function getMockDateValue(fieldConfigId: string): string {
  const field = getFieldInfo(fieldConfigId) as DateFieldConfig | undefined
  const dateFormat = field?.dateFormat || 'DATE'
  switch (dateFormat) {
    case 'DATE':
      return '2025-07-08'
    case 'DATETIME':
    case 'DATETIME_SECOND':
      return '2025-07-08 14:30'
    case 'YEAR_MONTH':
      return '2025-07'
    default:
      return '2025-07-08'
  }
}

// 获取数字字段的模拟显示值
function getMockNumberValue(fieldConfigId: string): string {
  const field = getFieldInfo(fieldConfigId) as NumberFieldConfig | undefined
  const precision = field?.precision ?? 2
  const showThousandSeparator = field?.showThousandSeparator ?? true
  const unit = field?.unit || ''

  let value = '12345'
  if (precision > 0) {
    value = (12345).toFixed(precision)
  }
  if (showThousandSeparator) {
    const parts = value.split('.')
    const intPart = parts[0]
    if (intPart) {
      parts[0] = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    }
    value = parts.join('.')
  }
  return unit ? `${value} ${unit}` : value
}

// 获取枚举字段的模拟显示值
function getMockEnumOption(fieldConfigId: string): { name: string; color: string } {
  const field = getFieldInfo(fieldConfigId) as EnumFieldConfig | undefined
  const options = field?.options || []
  if (options.length > 0) {
    const item = options[0]
    // EnumOption 使用 label 而不是 name
    return { name: item?.label || '选项', color: item?.color || '#165dff' }
  }
  return { name: '选项', color: '#165dff' }
}
</script>

<template>
  <div class="field-preview-content">
    <!-- 文本类型 -->
    <template v-if="getFieldType(fieldConfigId) === 'TEXT'">
      <div class="mock-text">这是一段示例文本内容</div>
    </template>
    <!-- 数字类型 -->
    <template v-else-if="getFieldType(fieldConfigId) === 'NUMBER'">
      <div class="mock-number">{{ getMockNumberValue(fieldConfigId) }}</div>
    </template>
    <!-- 日期类型 -->
    <template v-else-if="getFieldType(fieldConfigId) === 'DATE'">
      <div class="mock-date">{{ getMockDateValue(fieldConfigId) }}</div>
    </template>
    <!-- 枚举类型 -->
    <template v-else-if="getFieldType(fieldConfigId) === 'ENUM'">
      <div class="mock-enum">
        <span
          class="enum-tag"
          :style="{ backgroundColor: getMockEnumOption(fieldConfigId).color }"
        >
          {{ getMockEnumOption(fieldConfigId).name }}
        </span>
      </div>
    </template>
    <!-- 附件类型 -->
    <template v-else-if="getFieldType(fieldConfigId) === 'ATTACHMENT'">
      <div class="mock-attachment">
        <div class="attachment-item">
          <span class="attachment-icon">📄</span>
          <span class="attachment-name">需求文档.docx</span>
        </div>
      </div>
    </template>
    <!-- 链接类型 -->
    <template v-else-if="getFieldType(fieldConfigId) === 'WEB_URL'">
      <a class="mock-link" href="javascript:void(0)">
        <span class="link-icon">🔗</span>
        <span>https://example.com/page</span>
      </a>
    </template>
    <!-- 级联属性类型 -->
    <template v-else-if="getFieldType(fieldConfigId) === 'CASCADE'">
      <div class="mock-cascade-field">
        <span class="cascade-path">部门 / 研发中心 / 前端组</span>
      </div>
    </template>
    <!-- 关联类型 - 与实际详情页样式一致 -->
    <template v-else-if="getFieldType(fieldConfigId) === 'LINK'">
      <div class="mock-relation">
        <span class="linked-card-tag">TASK-123 关联的任务标题</span>
      </div>
    </template>
    <!-- 描述/Markdown 类型 - 与实际详情页样式一致 -->
    <template v-else-if="getFieldType(fieldConfigId) === 'DESCRIPTION' || getFieldType(fieldConfigId) === 'MARKDOWN'">
      <div class="mock-description">
        <div class="tiptap-display">
          <div class="ProseMirror">
            <h2>功能需求</h2>
            <ul>
              <li>支持 Markdown 格式</li>
              <li>支持实时预览</li>
            </ul>
          </div>
        </div>
      </div>
    </template>
    <!-- 默认 -->
    <template v-else>
      <div class="mock-text">示例值</div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.mock-text {
  font-size: 14px;
  color: var(--color-text-1);
  word-break: break-word;
}

.mock-number {
  font-family: 'Roboto Mono', monospace;
  font-size: 14px;
  color: var(--color-text-1);
}

.mock-date {
  font-size: 14px;
  color: var(--color-text-1);
}

.mock-enum {
  .enum-tag {
    display: inline-block;
    padding: 1px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
    line-height: 18px;
    background-color: var(--color-bg-1);
    border: 1px solid var(--color-border-2);
    color: #333 !important;
    white-space: nowrap;
  }
}

.mock-attachment {
  .attachment-item {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    color: var(--color-text-1);
    cursor: pointer;

    &:hover {
      color: rgb(var(--primary-6));
    }
  }
}

.mock-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: rgb(var(--primary-6));
  font-size: 13px;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.mock-cascade-field {
  .cascade-path {
    color: var(--color-text-1);
    font-size: 14px;
  }
}

.mock-relation {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;

  .linked-card-tag {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    background-color: var(--color-bg-1);
    border: 1px solid var(--color-border-2);
    border-radius: 4px;
    font-size: 13px;
    color: var(--color-text-1);
    cursor: default;
    transition: border-color 0.2s;

    &:hover {
      border-color: var(--color-border-3);
    }
  }
}

.mock-description {
  width: 100%;

  .tiptap-display {
    width: 100%;
    border: 1px solid var(--color-border);
    border-radius: 4px;
    background: #fff;
    padding: 12px 16px;
    box-sizing: border-box;

    .ProseMirror {
      outline: none;

      h2 {
        margin-top: 16px;
        margin-bottom: 8px;
        font-size: 1.2em;
        font-weight: 600;
        line-height: 1.4;
      }

      ul {
        padding-left: 20px;
        margin: 8px 0;
        list-style-type: disc;
      }

      li {
        margin: 4px 0;
      }

      p {
        margin: 8px 0;
        line-height: 1.6;
      }
    }
  }
}
</style>
