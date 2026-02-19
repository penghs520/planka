<template>
  <a-card class="condition-item" :class="{ 'is-incomplete': showError }" :bordered="false" hoverable>
    <a-row :gutter="12" >
      <!-- 1. 字段选择（支持多级关联） -->
      <a-col :span="8">
        <PathFieldSelector
          :available-fields="availableFields"
          :link-types="linkTypes"
          :any-trait-card-type-name="rootCardTypeName"
          :fetch-fields-by-link-field-id="fetchFieldsByLinkFieldId"
          :initial-field-id="getFieldId()"
          :initial-path="getPath()"
          :placeholder="t('common.condition.selectField')"
          @select="handleFieldSelect"
        />
      </a-col>

      <!-- 2. 操作符选择（LINK 类型由 LinkConditionValueEditor 内部处理） -->
      <a-col v-if="hasFieldSelected && localItem.nodeType !== 'LINK'" :span="4">
        <a-select
          :model-value="getOperatorType()"
          size="small"
          :placeholder="t('common.condition.selectOperator')"
          @change="handleOperatorChange"
        >
          <a-option
            v-for="op in availableOperators"
            :key="op.value"
            :value="op.value"
          >
            {{ op.label }}
          </a-option>
        </a-select>
      </a-col>

      <!-- 3. 值输入（根据条件类型和操作符动态渲染） -->
      <!-- LINK 类型占用更多空间（因为没有外部操作符选择器） -->
      <a-col v-if="hasFieldSelected" :span="localItem.nodeType === 'LINK' ? 14 : 10">
        <!-- 文本输入 (TEXT, TITLE, KEYWORD, CODE) -->
        <a-input
          v-if="needsValue && isTextType()"
          :model-value="getTextValue()"
          :placeholder="t('common.condition.inputText')"
          size="small"
          @update:model-value="handleTextValueChange"
        />

        <!-- 数字输入 (NUMBER) -->
        <a-input-number
          v-else-if="needsValue && localItem.nodeType === 'NUMBER'"
          :model-value="getNumberValue()"
          :placeholder="t('common.condition.inputNumber')"
          size="small"
          style="width: 100%"
          @update:model-value="handleNumberValueChange"
        />

        <!-- 日期输入 (DATE, 系统时间字段) -->
        <DateValueEditor
          v-else-if="needsValue && isDateType()"
          :model-value="localItem.operator as any"
          @update:model-value="handleDateOperatorChange"
        />

        <!-- 枚举选择 (ENUM) -->
        <EnumValueEditor
          v-else-if="needsValue && localItem.nodeType === 'ENUM'"
          :model-value="localItem.operator as any"
          :options="getEnumOptions()"
          @update:model-value="handleEnumOperatorChange"
        />

        <!-- 用户字段输入 (CREATED_BY, UPDATED_BY) -->
        <UserValueEditor
          v-else-if="needsValue && isUserType()"
          :model-value="localItem.operator as any"
          @update:model-value="handleUserOperatorChange"
        />

        <!-- 卡片生命周期状态选择 (CARD_CYCLE) -->
        <LifecycleValueEditor
          v-else-if="needsValue && localItem.nodeType === 'CARD_CYCLE'"
          :model-value="localItem.operator as any"
          @update:model-value="handleLifecycleOperatorChange"
        />

        <!-- 关联条件值编辑 (LINK) -->
        <LinkConditionValueEditor
          v-else-if="localItem.nodeType === 'LINK'"
          :model-value="getLinkOperator()"
          :link-field-id="getLinkFieldId()"
          @update:model-value="handleLinkOperatorChange"
        />

        <!-- 无需输入值 -->
        <span v-else-if="!needsValue" class="no-value-hint">{{ t('common.condition.noValueNeeded') }}</span>

        <!-- 其他情况 -->
        <span v-else class="no-value-hint">-</span>
      </a-col>

      <!-- 4. 删除按钮 -->
      <a-col :span="2">
        <a-button
          type="text"
          size="mini"
          class="delete-btn"
          @click="handleRemove"
        >
          <template #icon><icon-delete /></template>
        </a-button>
      </a-col>
    </a-row>
  </a-card>
</template>

<script setup lang="ts">
import { ref, watch, computed, inject, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconDelete } from '@arco-design/web-vue/es/icon'
import type { ConditionItem, LinkOperator, Path, DateOperator, EnumOperator, UserOperator, LifecycleOperator } from '@/types/condition'
import { LifecycleState, SystemDateField, SYSTEM_DATE_FIELD_PREFIX, KeyDate } from '@/types/condition'
import type { FieldOption } from '@/types/field-option'
import type { EnumOptionDTO } from '@/types/view-data'
import type { LinkTypeVO } from '@/types/link-type'
import PathFieldSelector, { type FieldSelectionResult } from './PathFieldSelector.vue'
import LinkConditionValueEditor from './LinkConditionValueEditor.vue'
import { DateValueEditor, EnumValueEditor, LifecycleValueEditor, UserValueEditor } from './value-editors'
import { SYSTEM_FIELD_NODE_TYPES } from './constants'
import { getNodeTypeByFieldType as _getNodeTypeByFieldType, createDefaultConditionItem, operatorNeedsValue, createSystemDateConditionItem, createLinkConditionItem, createPath as _createPath, isConditionItemComplete } from '@/utils/condition-factory'

const { t } = useI18n()

/**
 * Props定义
 */
const props = withDefaults(
  defineProps<{
    /** 条件项值 */
    modelValue: ConditionItem

    /** 可用字段列表 */
    availableFields: FieldOption[]

    /** 关联类型列表 */
    linkTypes?: LinkTypeVO[]

    /** 根卡片类型名称（用于路径面包屑显示） */
    rootCardTypeName?: string

    /** 根据关联字段ID获取级联字段的函数（用于多级关联） */
    fetchFieldsByLinkFieldId?: (linkFieldId: string) => Promise<FieldOption[]>
  }>(),
  {
    availableFields: () => [],
    linkTypes: () => [],
    rootCardTypeName: '',
    fetchFieldsByLinkFieldId: undefined,
  }
)

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: ConditionItem]
  'remove': []
}>()

/**
 * 本地条件项状态
 */
const localItem = ref<ConditionItem>(JSON.parse(JSON.stringify(props.modelValue)))

/**
 * 从父组件注入高亮状态
 */
const highlightIncomplete = inject<Ref<boolean>>('highlightIncomplete', ref(false))

/**
 * 当前条件项是否不完整
 */
const isIncomplete = computed(() => !isConditionItemComplete(localItem.value))

/**
 * 是否显示错误状态（高亮开启且当前项不完整）
 */
const showError = computed(() => highlightIncomplete.value && isIncomplete.value)

/**
 * 是否已选择字段
 */
const hasFieldSelected = computed(() => {
  const fieldId = getFieldId()
  return fieldId !== ''
})

/**
 * 监听props变化
 */
watch(
  () => props.modelValue,
  (newVal) => {
    localItem.value = JSON.parse(JSON.stringify(newVal))
  },
  { deep: true }
)

/**
 * 获取字段ID
 */
function getFieldId(): string {
  const item = localItem.value
  const subject = item.subject as any

  // 系统日期字段：DATE类型且subject.type为SYSTEM
  if (item.nodeType === 'DATE' && subject.type === 'SYSTEM' && subject.systemField) {
    return `${SYSTEM_DATE_FIELD_PREFIX}${subject.systemField}`
  }

  // 自定义日期字段：DATE类型且subject.type为FIELD
  if (item.nodeType === 'DATE' && subject.type === 'FIELD') {
    return subject.fieldId || ''
  }

  // LINK 类型：返回 linkFieldId
  if (item.nodeType === 'LINK' && subject.linkFieldId) {
    return subject.linkFieldId
  }

  // 有 fieldId 的是自定义字段
  if (subject.fieldId) {
    return subject.fieldId
  }

  // 有 streamId 的是 STATUS 字段
  if (subject.streamId) {
    return subject.streamId
  }

  // 系统字段（CARD_CYCLE, TITLE, CODE, CREATED_BY, UPDATED_BY）返回 nodeType
  if (SYSTEM_FIELD_NODE_TYPES.includes(item.nodeType)) {
    return item.nodeType
  }

  // 其他情况（如新建的 TEXT/NUMBER/ENUM 等条件，fieldId 为空）返回空字符串
  return ''
}

/**
 * 获取路径（用于多级关联回显）
 */
function getPath(): Path | undefined {
  const item = localItem.value
  const subject = item.subject as any
  return subject.path
}

/**
 * 获取操作符类型
 */
function getOperatorType(): string {
  return (localItem.value.operator as any).type || ''
}

/**
 * 获取当前选中字段的schemaSubType
 */
const currentFieldSchemaSubType = computed(() => {
  const fieldId = getFieldId()
  if (!fieldId) return null
  const field = props.availableFields.find((f) => f.id === fieldId)
  return field?.fieldType || null
})

/**
 * 获取操作符标签（使用 i18n 翻译）
 */
function getOperatorLabel(key: string): string {
  return t(`common.operator.${key}`, key)
}

/**
 * 当前可用的操作符列表
 */
const availableOperators = computed(() => {
  const nodeType = localItem.value.nodeType
  const schemaSubType = currentFieldSchemaSubType.value

  // 附件类型只支持：包含、为空、不为空
  if (schemaSubType === 'ATTACHMENT_FIELD') {
    return [
      { value: 'CONTAINS', label: getOperatorLabel('CONTAINS') },
      { value: 'IS_EMPTY', label: getOperatorLabel('IS_EMPTY') },
      { value: 'IS_NOT_EMPTY', label: getOperatorLabel('IS_NOT_EMPTY') },
    ]
  }

  const operatorMap: Record<string, { value: string; label: string }[]> = {
    TEXT: [
      { value: 'EQ', label: getOperatorLabel('EQ') },
      { value: 'NE', label: getOperatorLabel('NE') },
      { value: 'CONTAINS', label: getOperatorLabel('CONTAINS') },
      { value: 'NOT_CONTAINS', label: getOperatorLabel('NOT_CONTAINS') },
      { value: 'STARTS_WITH', label: getOperatorLabel('STARTS_WITH') },
      { value: 'ENDS_WITH', label: getOperatorLabel('ENDS_WITH') },
      { value: 'IS_EMPTY', label: getOperatorLabel('IS_EMPTY') },
      { value: 'IS_NOT_EMPTY', label: getOperatorLabel('IS_NOT_EMPTY') },
    ],
    NUMBER: [
      { value: 'EQ', label: getOperatorLabel('EQ') },
      { value: 'NE', label: getOperatorLabel('NE') },
      { value: 'GT', label: getOperatorLabel('GT') },
      { value: 'GE', label: getOperatorLabel('GE') },
      { value: 'LT', label: getOperatorLabel('LT') },
      { value: 'LE', label: getOperatorLabel('LE') },
      { value: 'IS_EMPTY', label: getOperatorLabel('IS_EMPTY') },
      { value: 'IS_NOT_EMPTY', label: getOperatorLabel('IS_NOT_EMPTY') },
    ],
    DATE: [
      { value: 'EQ', label: getOperatorLabel('EQ') },
      { value: 'NE', label: getOperatorLabel('NE') },
      { value: 'BEFORE', label: getOperatorLabel('BEFORE') },
      { value: 'AFTER', label: getOperatorLabel('AFTER') },
      { value: 'BETWEEN', label: getOperatorLabel('BETWEEN') },
      { value: 'IS_EMPTY', label: getOperatorLabel('IS_EMPTY') },
      { value: 'IS_NOT_EMPTY', label: getOperatorLabel('IS_NOT_EMPTY') },
    ],
    ENUM: [
      { value: 'EQ', label: getOperatorLabel('EQ') },
      { value: 'NE', label: getOperatorLabel('NE') },
      { value: 'IN', label: getOperatorLabel('IN') },
      { value: 'NOT_IN', label: getOperatorLabel('NOT_IN') },
      { value: 'IS_EMPTY', label: getOperatorLabel('IS_EMPTY') },
      { value: 'IS_NOT_EMPTY', label: getOperatorLabel('IS_NOT_EMPTY') },
    ],
    WEB_URL: [
      { value: 'IS_EMPTY', label: getOperatorLabel('IS_EMPTY') },
      { value: 'IS_NOT_EMPTY', label: getOperatorLabel('IS_NOT_EMPTY') },
    ],
    USER: [
      { value: 'EQ', label: getOperatorLabel('EQ') },
      { value: 'NE', label: getOperatorLabel('NE') },
      { value: 'IN', label: getOperatorLabel('IN') },
      { value: 'NOT_IN', label: getOperatorLabel('NOT_IN') },
      { value: 'IS_CURRENT_USER', label: getOperatorLabel('IS_CURRENT_USER') },
      { value: 'IS_NOT_CURRENT_USER', label: getOperatorLabel('IS_NOT_CURRENT_USER') },
      { value: 'IS_EMPTY', label: getOperatorLabel('IS_EMPTY') },
      { value: 'IS_NOT_EMPTY', label: getOperatorLabel('IS_NOT_EMPTY') },
    ],
    LIFECYCLE: [
      { value: 'IN', label: getOperatorLabel('IN') },
      { value: 'NOT_IN', label: getOperatorLabel('NOT_IN') },
    ],
    LINK: [
      { value: 'HAS_ANY', label: getOperatorLabel('HAS_ANY') },
      { value: 'IS_EMPTY', label: getOperatorLabel('IS_EMPTY') },
      { value: 'IN', label: getOperatorLabel('IN') },
      { value: 'NOT_IN', label: getOperatorLabel('NOT_IN') },
    ],
  }

  // TEXT操作符可被TITLE、CODE复用
  if (nodeType === 'TITLE' || nodeType === 'CODE') {
    return operatorMap.TEXT
  }

  // 系统用户字段使用USER操作符
  if (nodeType === 'CREATED_BY' || nodeType === 'UPDATED_BY') {
    return operatorMap.USER
  }

  // 卡片生命周期使用LIFECYCLE操作符
  if (nodeType === 'CARD_CYCLE') {
    return operatorMap.LIFECYCLE
  }

  // 关联字段使用LINK操作符
  if (nodeType === 'LINK') {
    return operatorMap.LINK
  }

  return operatorMap[nodeType] || []
})

/**
 * 判断当前操作符是否需要值输入
 */
const needsValue = computed(() => {
  return operatorNeedsValue(getOperatorType())
})

/**
 * 判断是否为文本类型
 */
function isTextType(): boolean {
  const nodeType = localItem.value.nodeType
  return nodeType === 'TEXT' || nodeType === 'TITLE' || nodeType === 'CODE'
}

/**
 * 判断是否为日期类型
 * 系统日期字段（创建时间、更新时间等）现在通过 DATE 类型的 DateSubject.systemField 支持
 */
function isDateType(): boolean {
  return localItem.value.nodeType === 'DATE'
}

/**
 * 判断是否为用户类型（系统用户字段）
 */
function isUserType(): boolean {
  const nodeType = localItem.value.nodeType
  return nodeType === 'CREATED_BY' || nodeType === 'UPDATED_BY'
}

/**
 * 获取文本值
 */
function getTextValue(): string {
  const op = localItem.value.operator as any
  return op.value || ''
}

/**
 * 处理文本值变化
 */
function handleTextValueChange(value: string) {
  const op = localItem.value.operator as any
  op.value = value
  emitChange()
}

/**
 * 获取数字值
 */
function getNumberValue(): number {
  const op = localItem.value.operator as any
  return op.value?.value || 0
}

/**
 * 处理数字值变化
 */
function handleNumberValueChange(value: number | undefined) {
  const op = localItem.value.operator as any
  if (!op.value) {
    op.value = { type: 'STATIC', value: 0 }
  }
  op.value.value = value || 0
  emitChange()
}

/**
 * 处理日期操作符变化（来自 DateValueEditor）
 */
function handleDateOperatorChange(operator: DateOperator) {
  localItem.value.operator = operator
  emitChange()
}

/**
 * 处理枚举操作符变化（来自 EnumValueEditor）
 */
function handleEnumOperatorChange(operator: EnumOperator) {
  localItem.value.operator = operator
  emitChange()
}

/**
 * 获取枚举选项
 * 支持 FieldConfig（使用 items）和 FieldOption（使用 enumOptions）两种类型
 */
function getEnumOptions(): EnumOptionDTO[] {
  const fieldId = getFieldId()
  const field = props.availableFields.find((f) => f.id === fieldId) as any
  // FieldConfig 使用 items，FieldOption 使用 enumOptions
  return field?.items || field?.enumOptions || []
}

/**
 * 处理用户操作符变化（来自 UserValueEditor）
 */
function handleUserOperatorChange(operator: UserOperator) {
  localItem.value.operator = operator
  emitChange()
}

/**
 * 处理生命周期操作符变化（来自 LifecycleValueEditor）
 */
function handleLifecycleOperatorChange(operator: LifecycleOperator) {
  localItem.value.operator = operator
  emitChange()
}

/**
 * 获取关联操作符
 */
function getLinkOperator(): LinkOperator {
  return localItem.value.operator as LinkOperator
}

/**
 * 获取关联字段ID（用于 LINK 类型条件）
 */
function getLinkFieldId(): string {
  if (localItem.value.nodeType === 'LINK') {
    const subject = localItem.value.subject as any
    return subject.linkFieldId || ''
  }
  return ''
}

/**
 * 处理关联操作符变化
 */
function handleLinkOperatorChange(operator: LinkOperator) {
  localItem.value.operator = operator
  emitChange()
}

/**
 * 处理字段选择（来自 PathFieldSelector）
 */
function handleFieldSelect(result: FieldSelectionResult) {
  let newItem: ConditionItem

  // 处理关联字段直接过滤
  if (result.isLinkFilter && result.linkFieldId) {
    newItem = createLinkConditionItem(
      result.linkFieldId,
      result.path,
      { type: 'HAS_ANY' }
    )
  }
  // 处理系统日期字段
  else if (result.fieldId?.startsWith(SYSTEM_DATE_FIELD_PREFIX)) {
    const systemFieldName = result.fieldId.substring(SYSTEM_DATE_FIELD_PREFIX.length) as SystemDateField
    newItem = createSystemDateConditionItem(systemFieldName)
    // 如果有路径，添加路径信息
    if (result.path && result.path.linkNodes.length > 0) {
      (newItem.subject as any).path = result.path
    }
  }
  // 处理其他字段
  else {
    newItem = createDefaultConditionItem(result.nodeType as any)

    // 设置 subject
    if (result.fieldId) {
      if ('fieldId' in newItem.subject) {
        (newItem.subject as any).fieldId = result.fieldId
      }
    }

    // 如果有路径，添加路径信息
    if (result.path && result.path.linkNodes.length > 0) {
      (newItem.subject as any).path = result.path
    }
  }

  localItem.value = newItem
  emitChange()
}

/**
 * 处理操作符变化
 */
function handleOperatorChange(newType: string) {
  const nodeType = localItem.value.nodeType
  const op = localItem.value.operator as any

  // 更新操作符类型
  op.type = newType

  // 根据新操作符类型初始化值
  if (!operatorNeedsValue(newType)) {
    // 删除值字段
    delete op.value
    delete op.values
    delete op.optionId
    delete op.optionIds
    delete op.userId
    delete op.userIds
  } else {
    // 确保值字段存在
    if (nodeType === 'TEXT' || nodeType === 'TITLE' || nodeType === 'CODE') {
      if (!op.value) op.value = ''
    } else if (nodeType === 'NUMBER') {
      if (!op.value) op.value = { type: 'STATIC', value: 0 }
    } else if (nodeType === 'CARD_CYCLE') {
      if (!op.values) op.values = [LifecycleState.ACTIVE]
    } else if (nodeType === 'DATE') {
      if (!op.value) op.value = { type: 'KEY_DATE', keyDate: KeyDate.TODAY }
    } else if (nodeType === 'ENUM') {
      if (newType === 'IN' || newType === 'NOT_IN') {
        if (!op.optionIds) op.optionIds = []
        delete op.optionId
      } else {
        if (!op.optionId) op.optionId = ''
        delete op.optionIds
      }
    } else if (nodeType === 'CREATED_BY' || nodeType === 'UPDATED_BY') {
      if (newType === 'IN' || newType === 'NOT_IN') {
        if (!op.userIds) op.userIds = []
        delete op.userId
      } else if (newType === 'EQ' || newType === 'NE') {
        if (!op.userId) op.userId = ''
        delete op.userIds
      }
    } else if (nodeType === 'LINK') {
      // LINK 类型的操作符变化由 LinkConditionValueEditor 处理
      if (newType === 'IN' || newType === 'NOT_IN') {
        if (!op.value) op.value = { type: 'STATIC', cardIds: [] }
      } else {
        delete op.value
      }
    }
  }

  emitChange()
}

/**
 * 处理删除
 */
function handleRemove() {
  emit('remove')
}

/**
 * 发送变更事件
 */
function emitChange() {
  emit('update:modelValue', JSON.parse(JSON.stringify(localItem.value)))
}
</script>

<style scoped lang="scss">
.condition-item {
  margin-bottom: 2px;
  transition: all 0.2s;
  background-color: transparent;
  width: 100%;
  box-sizing: border-box;

  &.is-incomplete {
    :deep(.arco-card-body) {
      background-color: rgba(var(--danger-6), 0.06);
      border-radius: 4px;
    }
  }

  :deep(.arco-card) {
    background-color: transparent;
    border: none;
    box-shadow: none;
    width: 100%;
  }

  :deep(.arco-card-body) {
    padding: 4px 0 !important;
    width: 100%;
  }

  :deep(.arco-row) {
    width: 100%;
  }

  /* 移除 select 和 input 的 hover 背景 */
  :deep(.arco-select-view-single:hover),
  :deep(.arco-select-view-multiple:hover),
  :deep(.arco-input-wrapper:hover) {
    background-color: transparent;
  }
}

.no-value-hint {
  color: var(--color-text-3);
  font-size: 13px;
  font-style: italic;
}

.delete-btn {
  color: var(--color-text-3);
  opacity: 0;
  transition: opacity 0.2s;

  &:hover {
    color: rgb(var(--danger-6));
    background-color: rgb(var(--danger-1));
  }
}

.condition-item:hover .delete-btn {
  opacity: 1;
}
</style>
