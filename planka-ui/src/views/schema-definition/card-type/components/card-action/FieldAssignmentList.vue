<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { IconDelete, IconPlus } from '@arco-design/web-vue/es/icon'
import type { FieldOption } from '@/types/field-option'
import type { FieldAssignment } from '@/types/card-action'
import { AssignmentTypeEnum, FixedValueTypeEnum } from '@/types/card-action'
import FieldAssignmentEditor from './FieldAssignmentEditor.vue'

const { t } = useI18n()

defineProps<{
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

const modelValue = defineModel<FieldAssignment[]>({ default: () => [] })

// 添加新的赋值项
function addAssignment() {
  const newAssignment: FieldAssignment = {
    fieldId: '',
    assignmentType: AssignmentTypeEnum.FIXED_VALUE,
    value: { valueType: FixedValueTypeEnum.TEXT, text: '' },
  }
  modelValue.value = [...modelValue.value, newAssignment]
}

// 删除赋值项
function removeAssignment(index: number) {
  modelValue.value = modelValue.value.filter((_, i) => i !== index)
}

// 更新赋值项
function updateAssignment(index: number, assignment: FieldAssignment) {
  const newList = [...modelValue.value]
  newList[index] = assignment
  modelValue.value = newList
}
</script>

<template>
  <div class="field-assignment-list">
    <div class="section-header">
      <span class="section-title">{{ t('admin.cardAction.updateCard.fieldAssignments') }}</span>
    </div>

    <!-- 赋值项列表 -->
    <div v-if="modelValue.length > 0" class="assignment-items">
      <div
        v-for="(assignment, index) in modelValue"
        :key="index"
        class="assignment-item"
      >
        <FieldAssignmentEditor
          :model-value="assignment"
          :field-options="fieldOptions"
          :reference-field-options="referenceFieldOptions"
          :member-field-options="memberFieldOptions"
          :current-card-type-id="currentCardTypeId"
          :member-card-type-id="memberCardTypeId"
          :hide-user-input="hideUserInput"
          @update:model-value="updateAssignment(index, $event)"
        />
        <a-button
          type="text"
          size="small"
          class="delete-assignment-btn"
          @click="removeAssignment(index)"
        >
          <template #icon>
            <IconDelete />
          </template>
        </a-button>
      </div>
    </div>

    <!-- 添加按钮 -->
    <a-button
      type="outline"
      :disabled="fieldOptions.length === 0"
      class="add-assignment-btn"
      @click="addAssignment"
    >
      <template #icon>
        <IconPlus />
      </template>
      {{ t('admin.cardAction.updateCard.addAssignment') }}
    </a-button>
  </div>
</template>

<style scoped lang="scss">
.field-assignment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-2);
}

.assignment-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.assignment-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px;
  background: var(--color-fill-1);
  border-radius: 6px;

  .field-assignment-editor {
    flex: 1;
  }
}

.add-assignment-btn {
  align-self: flex-start;
  border-style: dashed;
  border-color: var(--color-border-2);
  color: var(--color-text-2);

  &:hover:not(:disabled) {
    border-color: var(--color-primary);
    color: var(--color-primary);
    background: var(--color-primary-light-1);
  }
}

.delete-assignment-btn {
  color: var(--color-text-3);
  opacity: 0;
  transition: opacity 0.2s;

  &:hover {
    color: rgb(var(--danger-6));
    background-color: rgb(var(--danger-1));
  }
}

.assignment-item:hover .delete-assignment-btn {
  opacity: 1;
}
</style>
