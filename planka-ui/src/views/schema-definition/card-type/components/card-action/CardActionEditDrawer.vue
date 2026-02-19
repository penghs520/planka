<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { cardActionApi } from '@/api/card-action'
import type { CardActionConfigDefinition, UpdateCardExecution, CreateLinkedCardExecution } from '@/types/card-action'
import {
  ActionCategory,
  ExecutionTypeEnum,
  createEmptyCardAction,
} from '@/types/card-action'
import UpdateCardExecutionForm from './UpdateCardExecutionForm.vue'
import CreateLinkedCardExecutionForm from './CreateLinkedCardExecutionForm.vue'

const { t } = useI18n()

const props = defineProps<{
  cardTypeId: string
  action: CardActionConfigDefinition | null
}>()

const emit = defineEmits<{
  success: []
}>()

const visible = defineModel<boolean>('visible', { default: false })

const loading = ref(false)
const formRef = ref()
const formData = ref<CardActionConfigDefinition>(createEmptyCardAction('', ''))

const isEdit = computed(() => !!props.action?.id)
const isBuiltIn = computed(() => !!formData.value.builtIn)
const title = computed(() => {
  if (isEdit.value && props.action) {
    return t('admin.cardAction.editTitleWithName', { name: props.action.name })
  }
  return t('admin.cardAction.createTitle')
})

// 动作类别选项（编辑模式下显示所有类别）
const categoryOptions = computed(() => [
  { value: ActionCategory.LIFECYCLE, label: t('admin.cardAction.category.LIFECYCLE') },
  { value: ActionCategory.STATE_TOGGLE, label: t('admin.cardAction.category.STATE_TOGGLE') },
  { value: ActionCategory.CUSTOM, label: t('admin.cardAction.category.CUSTOM') },
])

// 执行类型选项（自定义动作不显示触发内置操作）
const executionTypeOptions = computed(() => {
  // 自定义动作只能使用这些执行类型
  return [
    {
      value: ExecutionTypeEnum.UPDATE_CARD,
      label: t('admin.cardAction.execution.UPDATE_CARD'),
      description: t('admin.cardAction.execution.UPDATE_CARD_DESC'),
    },
    {
      value: ExecutionTypeEnum.CREATE_LINKED_CARD,
      label: t('admin.cardAction.execution.CREATE_LINKED_CARD'),
      description: t('admin.cardAction.execution.CREATE_LINKED_CARD_DESC'),
    },
    {
      value: ExecutionTypeEnum.CALL_EXTERNAL_API,
      label: t('admin.cardAction.execution.CALL_EXTERNAL_API'),
      description: t('admin.cardAction.execution.CALL_EXTERNAL_API_DESC'),
    },
    {
      value: ExecutionTypeEnum.NAVIGATE_TO_PAGE,
      label: t('admin.cardAction.execution.NAVIGATE_TO_PAGE'),
      description: t('admin.cardAction.execution.NAVIGATE_TO_PAGE_DESC'),
    },
  ]
})

// 当前选中的执行类型
const selectedExecutionType = ref<ExecutionTypeEnum | null>(null)

// 表单验证规则
const rules = computed(() => ({
  name: [{ required: true, message: t('admin.cardAction.form.nameRequired') }],
}))

// 初始化表单数据
function initFormData() {
  if (props.action) {
    formData.value = { ...props.action }
    selectedExecutionType.value = props.action.executionType?.type || null
  } else {
    formData.value = createEmptyCardAction('', props.cardTypeId)
    formData.value.cardTypeId = props.cardTypeId
    // 新建模式默认为自定义动作
    formData.value.actionCategory = ActionCategory.CUSTOM
    selectedExecutionType.value = null
  }
}

// 执行类型变化时更新 formData
function handleExecutionTypeChange(type: ExecutionTypeEnum) {
  selectedExecutionType.value = type
  formData.value.executionType = { type } as UpdateCardExecution | CreateLinkedCardExecution
}

// 提交表单
async function handleSubmit() {
  try {
    const valid = await formRef.value?.validate()
    if (valid) return

    loading.value = true

    if (isEdit.value && formData.value.id) {
      await cardActionApi.update(formData.value.id, formData.value, formData.value.contentVersion)
      Message.success(t('admin.cardAction.message.saveSuccess'))
    } else {
      await cardActionApi.create(formData.value)
      Message.success(t('admin.cardAction.message.createSuccess'))
    }

    emit('success')
    visible.value = false
  } catch (error: any) {
    console.error('Failed to save action:', error)
    Message.error(error.message || t('admin.message.saveSuccess'))
  } finally {
    loading.value = false
  }
}

// 取消
function handleCancel() {
  visible.value = false
}

// 监听 visible 变化初始化表单
watch(
  () => visible.value,
  (newVisible) => {
    if (newVisible) {
      initFormData()
    }
  }
)

// 监听 action 变化
watch(
  () => props.action,
  () => {
    if (visible.value) {
      initFormData()
    }
  }
)
</script>

<template>
  <a-drawer
    :visible="visible"
    :title="title"
    :width="800"
    :mask-closable="true"
    unmount-on-close
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      layout="vertical"
      auto-label-width
    >
      <!-- 基础信息 -->
      <a-form-item :label="t('admin.cardAction.form.name')" field="name">
        <a-input
          v-model="formData.name"
          :placeholder="t('admin.cardAction.form.namePlaceholder')"
          :disabled="formData.builtIn"
          style="width: 280px"
        />
      </a-form-item>

      <!-- 动作类别（仅编辑内置动作时显示，自定义动作固定为 CUSTOM 不显示） -->
      <a-form-item v-if="isBuiltIn" :label="t('admin.cardAction.form.category')" field="actionCategory">
        <a-select
          v-model="formData.actionCategory"
          :placeholder="t('admin.cardAction.form.categoryPlaceholder')"
          disabled
        >
          <a-option
            v-for="option in categoryOptions"
            :key="option.value"
            :value="option.value"
            :label="option.label"
          />
        </a-select>
      </a-form-item>

      <!-- 执行类型（非内置动作） -->
      <template v-if="!formData.builtIn">
        <a-form-item :label="t('admin.cardAction.form.executionType')" field="executionType">
          <a-radio-group
            :model-value="selectedExecutionType"
            @change="handleExecutionTypeChange"
          >
            <a-radio
              v-for="option in executionTypeOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <!-- 更新卡片配置 -->
        <template v-if="selectedExecutionType === ExecutionTypeEnum.UPDATE_CARD">
          <UpdateCardExecutionForm
            v-model="(formData.executionType as UpdateCardExecution)"
            :card-type-id="cardTypeId"
          />
        </template>

        <!-- 新建关联卡片配置 -->
        <template v-if="selectedExecutionType === ExecutionTypeEnum.CREATE_LINKED_CARD">
          <CreateLinkedCardExecutionForm
            v-model="(formData.executionType as CreateLinkedCardExecution)"
            :card-type-id="cardTypeId"
          />
        </template>

        <!-- 调用外部接口配置 -->
        <template v-if="selectedExecutionType === ExecutionTypeEnum.CALL_EXTERNAL_API">
          <a-form-item :label="t('admin.cardAction.callApi.url')">
            <a-input
              v-model="(formData.executionType as any).url"
              :placeholder="t('admin.cardAction.callApi.urlPlaceholder')"
            />
          </a-form-item>
          <a-form-item :label="t('admin.cardAction.callApi.method')">
            <a-select v-model="(formData.executionType as any).method" :default-value="'POST'">
              <a-option value="GET">GET</a-option>
              <a-option value="POST">POST</a-option>
              <a-option value="PUT">PUT</a-option>
              <a-option value="DELETE">DELETE</a-option>
            </a-select>
          </a-form-item>
        </template>

        <!-- 跳转页面配置 -->
        <template v-if="selectedExecutionType === ExecutionTypeEnum.NAVIGATE_TO_PAGE">
          <a-form-item :label="t('admin.cardAction.navigate.targetUrl')">
            <a-input
              v-model="(formData.executionType as any).targetUrl"
              :placeholder="t('admin.cardAction.navigate.targetUrlPlaceholder')"
            />
          </a-form-item>
          <a-form-item :label="t('admin.cardAction.navigate.openInNewWindow')">
            <a-switch v-model="(formData.executionType as any).openInNewWindow" />
          </a-form-item>
        </template>
      </template>

      <!-- 确认和成功提示 -->
      <a-form-item :label="t('admin.cardAction.form.confirmMessage')" field="confirmMessage">
        <a-textarea
          v-model="formData.confirmMessage"
          :placeholder="t('admin.cardAction.form.confirmMessagePlaceholder')"
          :auto-size="{ minRows: 2, maxRows: 4 }"
        />
      </a-form-item>

      <a-form-item :label="t('admin.cardAction.form.successMessage')" field="successMessage">
        <a-textarea
          v-model="formData.successMessage"
          :placeholder="t('admin.cardAction.form.successMessagePlaceholder')"
          :auto-size="{ minRows: 2, maxRows: 4 }"
        />
      </a-form-item>

      <a-form-item :label="t('admin.cardAction.form.description')" field="description">
        <a-textarea
          v-model="formData.description"
          :placeholder="t('admin.cardAction.form.descriptionPlaceholder')"
          :auto-size="{ minRows: 2, maxRows: 4 }"
          :disabled="isBuiltIn"
        />
      </a-form-item>
    </a-form>

    <template #footer>
      <a-space>
        <a-button @click="handleCancel">{{ t('admin.action.cancel') }}</a-button>
        <a-button type="primary" :loading="loading" @click="handleSubmit">
          {{ t('admin.action.save') }}
        </a-button>
      </a-space>
    </template>
  </a-drawer>
</template>

<style scoped lang="scss">
</style>
