<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FieldOption } from '@/types/field-option'
import type { FieldAssignment } from '@/types/card-action'
import { AssignmentTypeEnum } from '@/types/card-action'
import AssignmentValueSelector from './AssignmentValueSelector.vue'

const { t } = useI18n()

const props = defineProps<{
  /** 可选字段列表（目标卡片的字段，用于选择要赋值的目标字段） */
  fieldOptions: FieldOption[]
  /** 当前卡片的字段列表（用于"当前卡片"引用来源，默认使用 fieldOptions） */
  referenceFieldOptions?: FieldOption[]
  /** 成员卡片类型的字段列表（用于"当前用户"引用） */
  memberFieldOptions?: FieldOption[]
  /** 当前卡片类型ID（用于判断引用来源本身是否可选） */
  currentCardTypeId?: string
  /** 成员卡片类型ID（用于判断引用来源本身是否可选） */
  memberCardTypeId?: string
  /** 是否隐藏用户输入选项（业务规则等自动执行场景使用） */
  hideUserInput?: boolean
}>()

const modelValue = defineModel<FieldAssignment>({ required: true })

// 当前卡片引用来源使用的字段列表
const effectiveReferenceFields = computed(() =>
  props.referenceFieldOptions ?? props.fieldOptions
)

// 当前选中的字段
const selectedField = computed(() =>
  props.fieldOptions.find(f => f.id === modelValue.value.fieldId)
)

// 字段变化时重置赋值配置
function handleFieldChange(fieldId: string) {
  const field = props.fieldOptions.find(f => f.id === fieldId)
  if (!field) return

  // 重置为空的赋值配置
  modelValue.value = {
    fieldId,
    assignmentType: AssignmentTypeEnum.CLEAR_VALUE,
  }
}
</script>

<template>
  <div class="field-assignment-editor">
    <!-- 目标字段选择 -->
    <a-select
      :model-value="modelValue.fieldId"
      :placeholder="t('admin.cardAction.updateCard.targetFieldPlaceholder')"
      class="field-select"
      @change="handleFieldChange"
    >
      <a-option
        v-for="field in fieldOptions"
        :key="field.id"
        :value="field.id"
        :label="field.name"
      />
    </a-select>

    <!-- 赋值方式和值选择（合并为一个选择器，仅在选择字段后显示） -->
    <AssignmentValueSelector
      v-if="selectedField"
      v-model="modelValue"
      :target-field="selectedField"
      :reference-fields="effectiveReferenceFields"
      :member-fields="memberFieldOptions"
      :current-card-type-id="currentCardTypeId"
      :member-card-type-id="memberCardTypeId"
      :hide-user-input="hideUserInput"
      class="value-selector"
    />
  </div>
</template>

<style scoped lang="scss">
.field-assignment-editor {
  display: flex;
  gap: 12px;
  align-items: center;
  flex: 1;

  :deep(.field-select) {
    flex: 0 0 200px !important;
    width: 200px !important;
  }

  :deep(.assignment-value-trigger) {
    flex: 1;
  }
}
</style>
