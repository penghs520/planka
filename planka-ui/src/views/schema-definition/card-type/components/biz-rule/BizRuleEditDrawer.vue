<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue'
import { bizRuleApi } from '@/api/biz-rule'
import { fieldOptionsApi } from '@/api/field-options'
import { valueStreamBranchApi, type StatusOption } from '@/api/value-stream'
import { useOrgStore } from '@/stores/org'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import ConditionEditor from '@/components/condition/ConditionEditor.vue'
import RuleActionsEditor from './RuleActionsEditor.vue'
import type { FieldOption } from '@/types/field-option'
import {
  type BizRuleDefinition,
  TriggerEvent,
  createEmptyBizRule,
  validateRuleAction,
} from '@/types/biz-rule'

const { t } = useI18n()
const orgStore = useOrgStore()

const props = defineProps<{
  visible: boolean
  cardTypeId: string
  rule: BizRuleDefinition | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const drawerVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

const isEditMode = computed(() => !!props.rule?.id)

const drawerTitle = computed(() => {
  return isEditMode.value
    ? t('admin.bizRule.editTitle')
    : t('admin.bizRule.createTitle')
})

// 表单状态
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const formData = ref<BizRuleDefinition | null>(null)

// 字段选项列表（用于条件编辑器）
const fieldOptions = ref<FieldOption[]>([])

// 状态选项列表
const statusOptions = ref<StatusOption[]>([])

// 加载字段选项
async function loadFieldOptions() {
  if (!props.cardTypeId) return
  try {
    fieldOptions.value = await fieldOptionsApi.getFields(props.cardTypeId)
  } catch (e) {
    console.error('Failed to load field options:', e)
  }
}

// 加载状态选项
async function loadStatusOptions() {
  if (!props.cardTypeId) return
  try {
    statusOptions.value = await valueStreamBranchApi.getStatusOptions(props.cardTypeId)
  } catch (e) {
    console.error('Failed to load status options:', e)
  }
}

// 目标状态下拉选项
const targetStatusOptions = computed(() =>
  statusOptions.value.map((s) => ({
    label: s.name,
    value: s.id,
  }))
)

// 监听字段下拉选项（仅自定义字段和关联字段，不包括内置字段）
const listenFieldOptions = computed(() =>
  fieldOptions.value
    .filter((f) => !f.systemField)
    .map((f) => ({
      label: f.name,
      value: f.id,
    }))
)

// 验证执行动作
function validateActions(_rule: any, value: any) {
  if (!value || value.length === 0) {
    return Promise.reject(t('admin.bizRule.form.actionsRequired'))
  }
  return Promise.resolve()
}

// 触发事件选项
const triggerEventOptions = computed(() => [
  { label: t('admin.bizRule.triggerEvent.ON_CREATE'), value: TriggerEvent.ON_CREATE },
  { label: t('admin.bizRule.triggerEvent.ON_DISCARD'), value: TriggerEvent.ON_DISCARD },
  { label: t('admin.bizRule.triggerEvent.ON_ARCHIVE'), value: TriggerEvent.ON_ARCHIVE },
  { label: t('admin.bizRule.triggerEvent.ON_RESTORE'), value: TriggerEvent.ON_RESTORE },
  { label: t('admin.bizRule.triggerEvent.ON_STATUS_MOVE'), value: TriggerEvent.ON_STATUS_MOVE },
  { label: t('admin.bizRule.triggerEvent.ON_STATUS_ROLLBACK'), value: TriggerEvent.ON_STATUS_ROLLBACK },
  { label: t('admin.bizRule.triggerEvent.ON_FIELD_CHANGE'), value: TriggerEvent.ON_FIELD_CHANGE },
  { label: t('admin.bizRule.triggerEvent.ON_SCHEDULE'), value: TriggerEvent.ON_SCHEDULE },
])

// 是否显示监听字段选择器
const showListenFields = computed(() => formData.value?.triggerEvent === TriggerEvent.ON_FIELD_CHANGE)

// 是否显示目标状态选择器
const showTargetStatus = computed(() =>
  formData.value?.triggerEvent === TriggerEvent.ON_STATUS_MOVE ||
  formData.value?.triggerEvent === TriggerEvent.ON_STATUS_ROLLBACK
)

// 是否显示定时配置
const showScheduleConfig = computed(() => formData.value?.triggerEvent === TriggerEvent.ON_SCHEDULE)

// 是否显示重试配置
const showRetryConfig = computed(() => !!formData.value)

// 初始化抽屉数据
async function initDrawer() {
  // 加载字段选项和状态选项
  loadFieldOptions()
  loadStatusOptions()

  if (props.rule?.id) {
    loading.value = true
    try {
      formData.value = await bizRuleApi.getById(props.rule.id)
    } catch (error) {
      console.error('Failed to fetch rule:', error)
      Message.error(t('admin.message.fetchFailed', { type: t('admin.bizRule.title') }))
      drawerVisible.value = false
    } finally {
      loading.value = false
    }
  } else {
    formData.value = createEmptyBizRule(props.cardTypeId, orgStore.currentOrgId!)
  }
}

// 提交表单
async function handleSubmit() {
  if (!formData.value) return

  // 手动验证执行动作
  if (!formData.value.actions || formData.value.actions.length === 0) {
    Message.error(t('admin.bizRule.form.actionsRequired'))
    return
  }

  // 校验每个动作的必填字段完整性
  for (let i = 0; i < formData.value.actions.length; i++) {
    const action = formData.value.actions[i]
    if (!action) continue
    const detail = validateRuleAction(action, t)
    if (detail) {
      const typeName = t(`admin.bizRule.actionType.${action.actionType}`)
      Message.error(
        t('admin.bizRule.actionConfig.actionIncomplete', { index: i + 1, type: typeName, detail }),
      )
      return
    }
  }

  const errors = await formRef.value?.validate()
  if (errors) return

  saving.value = true
  try {
    if (isEditMode.value) {
      await bizRuleApi.update(
        formData.value.id!,
        formData.value,
        formData.value.contentVersion,
      )
      Message.success(t('admin.message.saveSuccess'))
    } else {
      await bizRuleApi.create(formData.value)
      Message.success(t('admin.message.createSuccess'))
    }
    drawerVisible.value = false
    emit('success')
  } catch (error: any) {
    console.error('Failed to save rule:', error)
  } finally {
    saving.value = false
  }
}

// 监听 visible 变化，初始化数据
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      initDrawer()
    }
  }
)
</script>

<template>
  <a-drawer
    v-model:visible="drawerVisible"
    :title="drawerTitle"
    :width="900"
    :mask-closable="true"
    :esc-to-close="true"
    unmount-on-close
  >
    <a-spin :loading="loading" class="drawer-spin">
      <a-form
        v-if="formData"
        ref="formRef"
        :model="formData"
        layout="vertical"
        class="rule-form"
      >
        <!-- 触发配置 -->
        <div class="form-section">
          <a-form-item
            field="name"
            :label="t('admin.bizRule.form.name')"
            :rules="[{ required: true, message: t('admin.bizRule.form.nameRequired') }]"
          >
            <a-input
              v-model="formData.name"
              :placeholder="t('admin.bizRule.form.namePlaceholder')"
              :max-length="100"
              class="rule-name-input"
            />
          </a-form-item>

          <a-form-item
            field="triggerEvent"
            :label="t('admin.bizRule.form.triggerEvent')"
            :rules="[{ required: true, message: t('admin.bizRule.form.triggerEventRequired') }]"
          >
            <a-select
              v-model="formData.triggerEvent"
              :options="triggerEventOptions"
              :placeholder="t('admin.bizRule.form.triggerEventPlaceholder')"
              class="trigger-event-select"
            />
          </a-form-item>

          <!-- 监听字段（字段变更时） -->
          <a-form-item
            v-if="showListenFields"
            field="listenFieldList"
            :label="t('admin.bizRule.form.listenFields')"
          >
            <a-select
              v-model="formData.listenFieldList"
              :options="listenFieldOptions"
              multiple
              :placeholder="t('admin.bizRule.form.listenFieldsPlaceholder')"
              allow-search
            />
          </a-form-item>

          <!-- 目标状态（状态变更时） -->
          <a-form-item
            v-if="showTargetStatus"
            field="targetStatusId"
            :label="t('admin.bizRule.form.targetStatus')"
          >
            <a-select
              v-model="formData.targetStatusId"
              :options="targetStatusOptions"
              :placeholder="t('admin.bizRule.form.targetStatusPlaceholder')"
              allow-clear
            />
          </a-form-item>

          <!-- 定时配置（定时触发时） -->
          <a-form-item
            v-if="showScheduleConfig"
            :label="t('admin.bizRule.form.scheduleConfig')"
          >
            <span class="text-secondary">{{ t('admin.bizRule.scheduleConfigInDev') }}</span>
          </a-form-item>

          <!-- 重试配置（异步模式） -->
          <template v-if="showRetryConfig && formData.retryConfig">
            <div class="section-title mt-4">{{ t('admin.bizRule.retryConfig.title') }}</div>
            <div class="flex gap-3 items-end">
              <a-form-item field="retryConfig.maxRetries" :label="t('admin.bizRule.retryConfig.maxRetries')">
                <a-input-number
                  v-model="formData.retryConfig.maxRetries"
                  :min="0"
                  :max="10"
                  :style="{ width: '80px' }"
                />
              </a-form-item>
              <a-form-item field="retryConfig.retryIntervalMs" :label="t('admin.bizRule.retryConfig.retryInterval')">
                <a-input-number
                  v-model="formData.retryConfig.retryIntervalMs"
                  :min="100"
                  :max="300000"
                  :step="1000"
                  :style="{ width: '120px' }"
                >
                  <template #suffix>ms</template>
                </a-input-number>
              </a-form-item>
              <a-form-item field="retryConfig.exponentialBackoff" :label="t('admin.bizRule.retryConfig.exponentialBackoff')">
                <a-switch v-model="formData.retryConfig.exponentialBackoff" />
              </a-form-item>
              <a-form-item
                v-if="formData.retryConfig.exponentialBackoff"
                field="retryConfig.maxRetryIntervalMs"
                :label="t('admin.bizRule.retryConfig.maxRetryInterval')"
              >
                <a-input-number
                  v-model="formData.retryConfig.maxRetryIntervalMs"
                  :min="1000"
                  :max="300000"
                  :step="1000"
                  :style="{ width: '120px' }"
                >
                  <template #suffix>ms</template>
                </a-input-number>
              </a-form-item>
            </div>
          </template>
        </div>

        <!-- 触发条件 -->
        <div class="form-section">
          <div class="section-title">{{ t('admin.bizRule.section.condition') }}</div>
          <div class="section-hint">{{ t('admin.bizRule.section.conditionHint') }}</div>
          <ConditionEditor
            v-if="formData"
            v-model="formData.condition"
            :card-type-id="cardTypeId"
            :available-fields="fieldOptions"
          />
        </div>

        <!-- 执行动作 -->
        <div class="form-section">
          <a-form-item
            field="actions"
            :label="t('admin.bizRule.section.actions')"
            :rules="[{ required: true, validator: validateActions }]"
            validate-trigger="change"
          >
            <RuleActionsEditor
              :model-value="formData.actions ?? []"
              :card-type-id="cardTypeId"
              @update:model-value="formData.actions = $event"
            />
          </a-form-item>
        </div>
      </a-form>
    </a-spin>

    <template #footer>
      <a-space>
        <CancelButton @click="drawerVisible = false" />
        <SaveButton :loading="saving" @click="handleSubmit" />
      </a-space>
    </template>
  </a-drawer>
</template>

<style scoped lang="scss">
.drawer-spin {
  width: 100%;
  height: 100%;
}

.rule-form {
  padding: 0;

  :deep(.arco-input-number) {
    max-width: 120px;
  }

  :deep(.arco-form-item-label) {
    font-weight: 500;
  }

  :deep(.rule-name-input),
  :deep(.rule-name-input .arco-input-wrapper) {
    max-width: 320px;
  }

  :deep(.trigger-event-select),
  :deep(.trigger-event-select .arco-select-view) {
    max-width: 320px;
  }
}

.form-section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }

  .section-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-2);
    margin-bottom: 12px;
  }

  .section-hint {
    font-size: 12px;
    color: var(--color-text-3);
    margin-bottom: 8px;
  }
}

.text-secondary {
  color: var(--color-text-3);
  font-size: 13px;
}
</style>
