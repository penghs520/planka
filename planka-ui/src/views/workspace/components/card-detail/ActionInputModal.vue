<script setup lang="ts">
/**
 * 动作执行用户输入弹窗
 * 当动作配置中包含 USER_INPUT 类型的字段赋值时，弹出此弹窗让用户输入值
 */
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CardActionConfigDefinition, FieldAssignment, UserInputAssignment } from '@/types/card-action'
import { AssignmentTypeEnum, ExecutionTypeEnum } from '@/types/card-action'
import { fieldOptionsApi } from '@/api/field-options'
import { linkCardApi } from '@/api/link-card'
import type { FieldOption } from '@/types/field-option'
import type { LinkedCard } from '@/types/card'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  action: CardActionConfigDefinition | null
  cardTypeId: string
}>()

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  confirm: [userInputs: Record<string, unknown>]
  cancel: []
}>()

const loading = ref(false)
const submitting = ref(false)
const fieldOptions = ref<FieldOption[]>([])
const formValues = ref<Record<string, unknown>>({})

// 关联卡片选项缓存
const linkableCardsMap = ref<Record<string, { loading: boolean; options: { label: string; value: string }[] }>>({})


// 获取需要用户输入的字段赋值列表
const userInputAssignments = computed<UserInputAssignment[]>(() => {
  if (!props.action?.executionType) return []

  const execType = props.action.executionType
  let assignments: FieldAssignment[] = []

  if (execType.type === ExecutionTypeEnum.UPDATE_CARD && execType.fieldAssignments) {
    assignments = execType.fieldAssignments
  } else if (execType.type === ExecutionTypeEnum.CREATE_LINKED_CARD && execType.fieldAssignments) {
    assignments = execType.fieldAssignments
  }

  return assignments.filter(
    (a): a is UserInputAssignment => a.assignmentType === AssignmentTypeEnum.USER_INPUT
  )
})

// 根据字段ID获取字段信息
function getFieldOption(fieldId: string): FieldOption | undefined {
  return fieldOptions.value.find(f => f.id === fieldId)
}

// 获取应该加载字段选项的卡片类型ID
const targetCardTypeId = computed<string | null>(() => {
  if (!props.action?.executionType) return props.cardTypeId

  const execType = props.action.executionType
  // UPDATE_CARD 使用当前卡片类型
  if (execType.type === ExecutionTypeEnum.UPDATE_CARD) {
    return props.cardTypeId
  }
  // CREATE_LINKED_CARD 使用目标卡片类型
  if (execType.type === ExecutionTypeEnum.CREATE_LINKED_CARD && execType.targetCardTypeId) {
    return execType.targetCardTypeId
  }
  return props.cardTypeId
})

// 加载字段选项
async function loadFieldOptions() {
  const cardTypeId = targetCardTypeId.value
  if (!cardTypeId) return

  loading.value = true
  try {
    fieldOptions.value = await fieldOptionsApi.getFields(cardTypeId)
  } catch (error) {
    console.error('Failed to load field options:', error)
  } finally {
    loading.value = false
  }
}

// 初始化表单值
function initFormValues() {
  const values: Record<string, unknown> = {}
  for (const assignment of userInputAssignments.value) {
    values[assignment.fieldId] = undefined
  }
  formValues.value = values
  // 清空关联卡片缓存
  linkableCardsMap.value = {}
}

/**
 * 搜索可关联的卡片
 * @param linkFieldId 关联属性ID
 * @param keyword 搜索关键字
 */
async function searchLinkableCards(linkFieldId: string, keyword?: string) {
  // 初始化缓存项
  if (!linkableCardsMap.value[linkFieldId]) {
    linkableCardsMap.value[linkFieldId] = { loading: false, options: [] }
  }
  
  linkableCardsMap.value[linkFieldId].loading = true
  try {
    const result = await linkCardApi.queryLinkableCards({
      linkFieldId,
      keyword: keyword || '',
      page: 0,
      size: 50
    })
    linkableCardsMap.value[linkFieldId].options = result.content.map((card: LinkedCard) => ({
      label: card.title?.displayValue || '',
      value: card.cardId
    }))
  } catch (error) {
    console.error('Failed to search linkable cards:', error)
    linkableCardsMap.value[linkFieldId].options = []
  } finally {
    linkableCardsMap.value[linkFieldId].loading = false
  }
}

/**
 * 获取关联字段的选项
 */
function getLinkFieldOptions(fieldId: string) {
  return linkableCardsMap.value[fieldId]?.options || []
}

/**
 * 获取关联字段的加载状态
 */
function getLinkFieldLoading(fieldId: string) {
  return linkableCardsMap.value[fieldId]?.loading || false
}

// 验证表单
function validateForm(): boolean {
  for (const assignment of userInputAssignments.value) {
    if (assignment.required !== false) {
      const value = formValues.value[assignment.fieldId]
      if (value === undefined || value === null || value === '') {
        return false
      }
    }
  }
  return true
}

// 提交
function handleSubmit() {
  if (!validateForm()) return

  submitting.value = true
  
  // 将用户输入的值转换为 FixedValue 格式
  const userInputs: Record<string, unknown> = {}
  for (const assignment of userInputAssignments.value) {
    const fieldId = assignment.fieldId
    const value = formValues.value[fieldId]
    const fieldOption = getFieldOption(fieldId)
    
    if (value === undefined || value === null) continue
    
    // 根据字段类型转换为对应的 FixedValue
    if (fieldOption) {
      userInputs[fieldId] = convertToFixedValue(value, fieldOption.fieldType)
    } else {
      // 兜底处理为文本值
      userInputs[fieldId] = { valueType: 'TEXT', text: String(value) }
    }
  }
  
  emit('confirm', userInputs)
  submitting.value = false
}

/**
 * 将原始值转换为 FixedValue 格式
 */
function convertToFixedValue(value: unknown, fieldType: string): unknown {
  switch (fieldType) {
    case 'SINGLE_LINE_TEXT':
    case 'MULTI_LINE_TEXT':
    case 'MARKDOWN':
    case 'WEB_URL':
      return { valueType: 'TEXT', text: String(value) }
      
    case 'NUMBER':
      return { valueType: 'NUMBER', number: Number(value) }
      
    case 'DATE':
    case 'DATETIME':
      // 日期值使用绝对日期模式
      // 确保格式为完整的 ISO 日期时间格式 (LocalDateTime)
      let dateStr: string
      if (value instanceof Date) {
        dateStr = value.toISOString().replace('Z', '')
      } else if (typeof value === 'string') {
        // Arco DatePicker 返回的格式可能是 "2026-01-02" 或完整格式
        dateStr = value.includes('T') ? value : `${value}T00:00:00`
      } else {
        dateStr = String(value)
      }
      return { 
        valueType: 'DATE', 
        mode: 'ABSOLUTE', 
        absoluteDate: dateStr
      }
      
    case 'ENUM':
    case 'SINGLE_ENUM':
      // 单选枚举转为数组
      return { valueType: 'ENUM', enumValueIds: [String(value)] }
      
    case 'MULTI_ENUM':
      // 多选枚举已经是数组
      return { valueType: 'ENUM', enumValueIds: Array.isArray(value) ? value : [String(value)] }
      
    case 'LINK':
    case 'STRUCTURE':
    case 'MEMBER':
      // 关联/人员字段
      return { valueType: 'LINK', ids: Array.isArray(value) ? value : [String(value)] }
      
    default:
      // 默认当作文本处理
      return { valueType: 'TEXT', text: String(value) }
  }
}

// 取消
function handleCancel() {
  emit('update:visible', false)
  emit('cancel')
}

// 监听弹窗打开
watch(() => props.visible, (visible) => {
  if (visible) {
    loadFieldOptions()
    initFormValues()
  }
})

// 监听 action 变化
watch(() => props.action, () => {
  if (props.visible) {
    initFormValues()
  }
})
</script>

<template>
  <a-modal
    :visible="visible"
    :title="t('admin.cardAction.userInputModal.title')"
    :ok-text="t('admin.cardAction.userInputModal.submit')"
    :cancel-text="t('admin.cardAction.userInputModal.cancel')"
    :ok-loading="submitting"
    :ok-button-props="{ disabled: loading }"
    :mask-closable="false"
    modal-class="arco-modal-simple"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-spin :loading="loading" style="width: 100%">
      <a-form v-if="!loading && fieldOptions.length > 0" :model="formValues" layout="vertical">
        <a-form-item
          v-for="assignment in userInputAssignments"
          :key="assignment.fieldId"
          :label="getFieldOption(assignment.fieldId)?.name || assignment.fieldId"
          :required="assignment.required !== false"
        >
          <!-- 根据字段类型渲染不同的输入控件 -->
          <template v-if="getFieldOption(assignment.fieldId)">
            <!-- 文本类型 -->
            <a-input
              v-if="['SINGLE_LINE_TEXT', 'WEB_URL'].includes(getFieldOption(assignment.fieldId)!.fieldType)"
              v-model="formValues[assignment.fieldId]"
              :placeholder="assignment.placeholder || t('admin.cardType.fieldConfig.enterDefaultValue')"
            />
            <!-- 多行文本 -->
            <a-textarea
              v-else-if="['MULTI_LINE_TEXT', 'MARKDOWN'].includes(getFieldOption(assignment.fieldId)!.fieldType)"
              :model-value="formValues[assignment.fieldId] as string"
              :placeholder="assignment.placeholder || t('admin.cardType.fieldConfig.enterDefaultValue')"
              :auto-size="{ minRows: 2, maxRows: 4 }"
              @update:model-value="formValues[assignment.fieldId] = $event"
            />
            <!-- 数字类型 -->
            <a-input-number
              v-else-if="getFieldOption(assignment.fieldId)!.fieldType === 'NUMBER'"
              v-model="formValues[assignment.fieldId]"
              :placeholder="assignment.placeholder || t('admin.cardType.fieldConfig.enterValue')"
              style="width: 100%"
            />
            <!-- 日期类型 -->
            <a-date-picker
              v-else-if="['DATE', 'DATETIME'].includes(getFieldOption(assignment.fieldId)!.fieldType)"
              v-model="formValues[assignment.fieldId]"
              :show-time="getFieldOption(assignment.fieldId)!.fieldType === 'DATETIME'"
              style="width: 100%"
            />
            <!-- 枚举类型 -->
            <a-select
              v-else-if="['ENUM', 'SINGLE_ENUM', 'MULTI_ENUM'].includes(getFieldOption(assignment.fieldId)!.fieldType)"
              v-model="formValues[assignment.fieldId]"
              :placeholder="t('admin.cardType.fieldConfig.selectPlease')"
              :multiple="getFieldOption(assignment.fieldId)!.fieldType === 'MULTI_ENUM'"
              :options="getFieldOption(assignment.fieldId)!.enumOptions?.map(o => ({ label: o.label, value: o.value })) || []"
              style="width: 100%"
            />
            <!-- 关联类型 -->
            <a-select
              v-else-if="getFieldOption(assignment.fieldId)!.fieldType === 'LINK'"
              v-model="formValues[assignment.fieldId]"
              :placeholder="t('admin.cardType.fieldConfig.selectPlease')"
              :multiple="getFieldOption(assignment.fieldId)!.multiple !== false"
              :options="getLinkFieldOptions(assignment.fieldId)"
              :loading="getLinkFieldLoading(assignment.fieldId)"
              :filter-option="false"
              allow-search
              allow-clear
              style="width: 100%"
              @search="(keyword: string) => searchLinkableCards(assignment.fieldId, keyword)"
              @focus="() => searchLinkableCards(assignment.fieldId)"
            />
            <!-- 其他类型默认使用文本输入 -->
            <a-input
              v-else
              v-model="formValues[assignment.fieldId]"
              :placeholder="assignment.placeholder || t('admin.cardType.fieldConfig.enterDefaultValue')"
            />
          </template>
          <!-- 字段信息未加载时显示加载中 -->
          <a-input v-else disabled placeholder="Loading..." />
        </a-form-item>
      </a-form>
      <!-- 加载中的占位 -->
      <div v-else-if="loading" style="min-height: 80px"></div>
    </a-spin>
  </a-modal>
</template>
