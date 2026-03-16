<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { cardTypeApi } from '@/api/card-type'
import { valueStreamBranchApi, type StatusOption } from '@/api/value-stream'
import type { FieldOption } from '@/types/field-option'
import type { UpdateCardExecution, FieldAssignment } from '@/types/card-action'
import FieldAssignmentList from './FieldAssignmentList.vue'

const { t } = useI18n()
const orgStore = useOrgStore()

const props = defineProps<{
  /** 当前卡片类型 ID */
  cardTypeId: string
}>()

const modelValue = defineModel<UpdateCardExecution>({ required: true })

// 字段选项
const fieldOptions = ref<FieldOption[]>([])
const loadingFields = ref(false)

// 成员卡片类型的字段选项
const memberFieldOptions = ref<FieldOption[]>([])

// 成员卡片类型ID
const memberCardTypeId = computed(() => {
  if (!orgStore.currentOrgId) return ''
  return `${orgStore.currentOrgId}:member`
})

// 过滤掉系统字段，只保留可赋值的字段
const assignableFieldOptions = computed(() =>
  fieldOptions.value.filter(f => !f.systemField)
)

// 状态选项
const statusOptions = ref<StatusOption[]>([])
const loadingStatus = ref(false)

// 加载字段选项
async function loadFieldOptions() {
  if (!props.cardTypeId) return

  loadingFields.value = true
  try {
    fieldOptions.value = await cardTypeApi.getFieldOptions(props.cardTypeId)
  } catch (e) {
    console.error('Failed to load field options:', e)
  } finally {
    loadingFields.value = false
  }
}

// 加载成员卡片类型的字段选项
async function loadMemberFieldOptions() {
  if (!memberCardTypeId.value) return
  try {
    memberFieldOptions.value = await cardTypeApi.getFieldOptions(memberCardTypeId.value)
  } catch (e) {
    console.error('Failed to load member field options:', e)
  }
}

// 加载状态选项
async function loadStatusOptions() {
  if (!props.cardTypeId) return

  loadingStatus.value = true
  try {
    statusOptions.value = await valueStreamBranchApi.getStatusOptions(props.cardTypeId)
  } catch (e) {
    console.error('Failed to load status options:', e)
  } finally {
    loadingStatus.value = false
  }
}

// 更新字段赋值列表
function handleFieldAssignmentsChange(assignments: FieldAssignment[]) {
  modelValue.value = {
    ...modelValue.value,
    fieldAssignments: assignments,
  }
}

// 更新目标状态
function handleTargetStatusChange(statusId: string | undefined) {
  modelValue.value = {
    ...modelValue.value,
    targetStatusId: statusId || undefined,
  }
}

// 初始化加载
onMounted(() => {
  loadFieldOptions()
  loadMemberFieldOptions()
  loadStatusOptions()
})

// 监听 cardTypeId 变化
watch(
  () => props.cardTypeId,
  () => {
    loadFieldOptions()
    loadStatusOptions()
  }
)

// 监听 memberCardTypeId 变化
watch(memberCardTypeId, () => {
  loadMemberFieldOptions()
})
</script>

<template>
  <div class="update-card-execution-form">
    <!-- 字段赋值 -->
    <div class="section">
      <FieldAssignmentList
        :model-value="modelValue.fieldAssignments || []"
        :field-options="assignableFieldOptions"
        :member-field-options="memberFieldOptions"
        :current-card-type-id="props.cardTypeId"
        :member-card-type-id="memberCardTypeId"
        @update:model-value="handleFieldAssignmentsChange"
      />
    </div>

    <!-- 目标状态 -->
    <div class="section">
      <a-form-item :label="t('admin.cardAction.updateCard.targetStatus')">
        <a-select
          :model-value="modelValue.targetStatusId"
          :placeholder="t('admin.cardAction.updateCard.targetStatusPlaceholder')"
          :loading="loadingStatus"
          allow-clear
          style="width: 280px"
          @change="handleTargetStatusChange"
        >
          <a-option
            v-for="status in statusOptions"
            :key="status.id"
            :value="status.id"
          >
            <div class="status-option">
              <span class="status-name">{{ status.name }}</span>
              <span class="status-kind">{{ status.stepKind }}</span>
            </div>
          </a-option>
        </a-select>
      </a-form-item>
    </div>
  </div>
</template>

<style scoped lang="scss">
.update-card-execution-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 16px;
}

.section {
  :deep(.arco-form-item) {
    margin-bottom: 0;
  }
}

.status-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .status-name {
    font-weight: 500;
  }

  .status-kind {
    font-size: 12px;
    color: var(--color-text-3);
  }
}
</style>
