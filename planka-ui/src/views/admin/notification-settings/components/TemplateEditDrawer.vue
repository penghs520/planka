<template>
  <a-drawer
    :visible="visible"
    :title="isEdit ? t('admin.notificationSettings.template.editTitle') : t('admin.notificationSettings.template.createTitle')"
    :width="600"
    :mask-closable="false"
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
      <!-- 模板名称 -->
      <a-form-item field="name" :label="t('admin.notificationSettings.template.form.name')" required>
        <a-input
          v-model="formData.name"
          :placeholder="t('admin.notificationSettings.template.form.namePlaceholder')"
        />
      </a-form-item>

      <!-- 触发事件 -->
      <a-form-item field="triggerEvent" :label="t('admin.notificationSettings.template.form.triggerEvent')" required>
        <a-select
          v-model="formData.triggerEvent"
          :placeholder="t('admin.notificationSettings.template.form.triggerEventPlaceholder')"
          :options="triggerEventOptions"
        />
      </a-form-item>

      <!-- 卡片类型 -->
      <a-form-item field="cardTypeId" :label="t('admin.notificationSettings.template.form.cardType')" required>
        <a-select
          v-model="formData.cardTypeId"
          :placeholder="t('admin.notificationSettings.template.form.cardTypePlaceholder')"
          :options="cardTypeOptions"
          :loading="cardTypeLoading"
          @change="handleCardTypeChange"
        />
      </a-form-item>

      <!-- 通知对象类型 -->
      <a-form-item field="recipientType" :label="t('admin.notificationSettings.template.form.recipientType')" required>
        <a-radio-group v-model="formData.recipientType" type="button" @change="handleRecipientTypeChange">
          <a-radio value="MEMBER">{{ t('admin.notificationSettings.template.recipientType.member') }}</a-radio>
          <a-radio value="GROUP">{{ t('admin.notificationSettings.template.recipientType.group') }}</a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- 通知人 - 选择成员字段（多选） -->
      <a-form-item
        v-if="formData.recipientType === 'MEMBER'"
        field="recipientSelector.fieldIds"
        :label="t('admin.notificationSettings.template.form.memberFields')"
        required
      >
        <a-select
          v-model="formData.recipientSelector.fieldIds"
          :placeholder="t('admin.notificationSettings.template.form.memberFieldsPlaceholder')"
          :options="memberFieldSelectOptions"
          :loading="fieldLoading"
          multiple
          :max-tag-count="3"
          allow-clear
        />
      </a-form-item>

      <!-- 通知群 - 保留原有选择器逻辑 -->
      <template v-if="formData.recipientType === 'GROUP'">
        <!-- 接收者选择器 -->
        <a-form-item field="recipientSelector.selectorType" :label="t('admin.notificationSettings.template.form.selectorType')" required>
          <a-select
            v-model="formData.recipientSelector.selectorType"
            :placeholder="t('admin.notificationSettings.template.form.selectorTypePlaceholder')"
          >
            <a-option value="CURRENT_OPERATOR">{{ t('admin.notificationSettings.template.selectorType.currentOperator') }}</a-option>
            <a-option value="FIXED_MEMBERS">{{ t('admin.notificationSettings.template.selectorType.fixedMembers') }}</a-option>
            <a-option value="FROM_FIELD">{{ t('admin.notificationSettings.template.selectorType.fromField') }}</a-option>
            <a-option value="CARD_WATCHERS">{{ t('admin.notificationSettings.template.selectorType.cardWatchers') }}</a-option>
          </a-select>
        </a-form-item>

        <!-- 从字段选择 - 选择人员字段 -->
        <a-form-item
          v-if="formData.recipientSelector.selectorType === 'FROM_FIELD'"
          field="recipientSelector.fieldId"
          :label="t('admin.notificationSettings.template.form.fieldId')"
          required
        >
          <a-cascader
            v-model="recipientFieldValue"
            :options="memberFieldOptions"
            :placeholder="t('admin.notificationSettings.template.form.fieldIdPlaceholder')"
            :loading="fieldLoading"
            multiple
            :max-tag-count="3"
            @change="handleMemberFieldChange"
          />
        </a-form-item>

        <!-- 固定成员选择 -->
        <a-form-item
          v-if="formData.recipientSelector.selectorType === 'FIXED_MEMBERS'"
          field="recipientSelector.memberIds"
          :label="t('admin.notificationSettings.template.form.memberIds')"
          required
        >
          <a-select
            v-model="formData.recipientSelector.memberIds"
            :placeholder="t('admin.notificationSettings.template.form.memberIdsPlaceholder')"
            multiple
            allow-search
          >
            <a-option v-for="member in memberOptions" :key="member.id" :value="member.id">
              {{ member.name }}
            </a-option>
          </a-select>
        </a-form-item>
      </template>

      <!-- 系统人员选择 -->
      <a-form-item field="recipientSelector.includeSystemUsers" :label="t('admin.notificationSettings.template.form.includeSystemUsers')">
        <a-checkbox v-model="formData.recipientSelector.includeSystemUsers">
          {{ t('admin.notificationSettings.template.form.includeSystemUsersLabel') }}
        </a-checkbox>
      </a-form-item>

      <a-form-item
        v-if="formData.recipientSelector.includeSystemUsers"
        field="recipientSelector.systemUserIds"
        :label="t('admin.notificationSettings.template.form.systemUserIds')"
      >
        <a-select
          v-model="formData.recipientSelector.systemUserIds"
          :placeholder="t('admin.notificationSettings.template.form.systemUserIdsPlaceholder')"
          multiple
          allow-search
        >
          <a-option v-for="user in systemUserOptions" :key="user.id" :value="user.id">
            {{ user.name }}
          </a-option>
        </a-select>
      </a-form-item>

      <!-- 通知渠道 -->
      <a-form-item field="channels" :label="t('admin.notificationSettings.template.form.channels')" required>
        <a-checkbox-group v-model="formData.channels" direction="vertical">
          <a-checkbox value="builtin">{{ t('admin.notificationSettings.channel.type.builtin') }}</a-checkbox>
          <a-checkbox value="email">{{ t('admin.notificationSettings.channel.type.email') }}</a-checkbox>
          <a-checkbox value="feishu">{{ t('admin.notificationSettings.channel.type.feishu') }}</a-checkbox>
          <a-checkbox value="dingtalk">{{ t('admin.notificationSettings.channel.type.dingtalk') }}</a-checkbox>
          <a-checkbox value="wecom">{{ t('admin.notificationSettings.channel.type.wecom') }}</a-checkbox>
        </a-checkbox-group>
      </a-form-item>

      <!-- 标题模板 -->
      <a-form-item field="titleTemplate" :label="t('admin.notificationSettings.template.form.titleTemplate')" required>
        <a-input
          v-model="formData.titleTemplate"
          :placeholder="t('admin.notificationSettings.template.form.titleTemplatePlaceholder')"
        />
        <template #help>
          <div class="field-variables">
            <span class="help-text">{{ t('admin.notificationSettings.template.form.titleTemplateHelp') }}</span>
            <a-space wrap>
              <a-tag
                v-for="field in availableFields"
                :key="field.id"
                size="small"
                class="field-tag"
                @click="insertField('titleTemplate', field.id)"
              >
                {{ field.name }}
              </a-tag>
            </a-space>
          </div>
        </template>
      </a-form-item>

      <!-- 短内容模板 -->
      <a-form-item field="shortContent" :label="t('admin.notificationSettings.template.form.shortContent')">
        <a-textarea
          v-model="formData.shortContent"
          :placeholder="t('admin.notificationSettings.template.form.shortContentPlaceholder')"
          :auto-size="{ minRows: 3, maxRows: 6 }"
        />
        <template #help>
          <div class="field-variables">
            <span class="help-text">{{ t('admin.notificationSettings.template.form.contentHelp') }}</span>
            <a-space wrap>
              <a-tag
                v-for="field in availableFields"
                :key="field.id"
                size="small"
                class="field-tag"
                @click="insertField('shortContent', field.id)"
              >
                {{ field.name }}
              </a-tag>
            </a-space>
          </div>
        </template>
      </a-form-item>

      <!-- 长内容模板 -->
      <a-form-item field="longContent" :label="t('admin.notificationSettings.template.form.longContent')">
        <a-textarea
          v-model="formData.longContent"
          :placeholder="t('admin.notificationSettings.template.form.longContentPlaceholder')"
          :auto-size="{ minRows: 5, maxRows: 10 }"
        />
        <template #help>
          <div class="field-variables">
            <span class="help-text">{{ t('admin.notificationSettings.template.form.contentHelp') }}</span>
            <a-space wrap>
              <a-tag
                v-for="field in availableFields"
                :key="field.id"
                size="small"
                class="field-tag"
                @click="insertField('longContent', field.id)"
              >
                {{ field.name }}
              </a-tag>
            </a-space>
          </div>
        </template>
      </a-form-item>

      <!-- 优先级 -->
      <a-form-item field="priority" :label="t('admin.notificationSettings.template.form.priority')">
        <a-input-number v-model="formData.priority" :min="0" :max="1000" />
        <template #extra>{{ t('admin.notificationSettings.template.form.priorityHelp') }}</template>
      </a-form-item>

      <!-- 启用状态 -->
      <a-form-item v-if="isEdit" field="enabled" :label="t('admin.table.status')">
        <a-switch v-model="formData.enabled" />
      </a-form-item>
    </a-form>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
// FormInstance type from arco-design
interface FormInstance {
  validate: () => Promise<undefined | Error>
  resetFields: () => void
}
import {
  notificationTemplateApi,
  TRIGGER_EVENT_OPTIONS,
  type NotificationTemplateDefinition,
  type CreateNotificationTemplateRequest,
  type UpdateNotificationTemplateRequest,
  type SelectorType,
  type RecipientType,
} from '@/api/notification-template'
import { cardTypeApi } from '@/api/card-type'
import type { FieldOption } from '@/types/field-option'

const props = defineProps<{
  visible: boolean
  template: NotificationTemplateDefinition | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const { t } = useI18n()

// 表单引用
const formRef = ref<FormInstance | null>(null)

// 加载状态
const cardTypeLoading = ref(false)
const fieldLoading = ref(false)
const submitLoading = ref(false)

// 选项数据
const cardTypeOptions = ref<{ label: string; value: string }[]>([])
const memberFieldOptions = ref<any[]>([])
const availableFields = ref<FieldOption[]>([])
const memberOptions = ref<{ id: string; name: string }[]>([])
const systemUserOptions = ref<{ id: string; name: string }[]>([])

// 接收者字段值
const recipientFieldValue = ref<string[]>([])

// 是否是编辑模式
const isEdit = computed(() => !!props.template)

// 触发事件选项
const triggerEventOptions = computed(() =>
  TRIGGER_EVENT_OPTIONS.map((item) => ({
    label: t(item.label),
    value: item.value,
  }))
)

// 成员字段选项（用于通知人多选）- 过滤出 LINK 类型的字段（成员关联）
const memberFieldSelectOptions = computed(() =>
  availableFields.value
    .filter((f) => f.fieldType === 'LINK')
    .map((f) => ({
      label: f.name,
      value: f.id,
    }))
)

// 表单数据
const formData = ref({
  name: '',
  cardTypeId: '',
  triggerEvent: '' as string,
  recipientType: 'MEMBER' as RecipientType,
  recipientSelector: {
    selectorType: 'FROM_FIELD' as SelectorType,
    memberIds: [] as string[],
    fieldId: '',
    fieldIds: [] as string[],
    includeSystemUsers: false,
    systemUserIds: [] as string[],
  },
  channels: ['builtin'] as string[],
  titleTemplate: '',
  shortContent: '',
  longContent: '',
  priority: 100,
  enabled: true,
})

// 表单校验规则
const formRules = {
  name: [{ required: true, message: t('admin.notificationSettings.template.form.nameRequired') }],
  cardTypeId: [{ required: true, message: t('admin.notificationSettings.template.form.cardTypeRequired') }],
  triggerEvent: [{ required: true, message: t('admin.notificationSettings.template.form.triggerEventRequired') }],
  'recipientSelector.selectorType': [{ required: true, message: t('admin.notificationSettings.template.form.selectorTypeRequired') }],
  'recipientSelector.fieldId': [{
    required: false,
    validator: (value: string, callback: (error?: string) => void) => {
      if (formData.value.recipientSelector.selectorType === 'FROM_FIELD' && !value) {
        callback(t('admin.notificationSettings.template.form.fieldIdRequired'))
      } else {
        callback()
      }
    },
  }],
  channels: [{ required: true, message: t('admin.notificationSettings.template.form.channelsRequired'), type: 'array' }],
  titleTemplate: [{ required: true, message: t('admin.notificationSettings.template.form.titleTemplateRequired') }],
}

// 加载卡片类型选项
const loadCardTypes = async () => {
  cardTypeLoading.value = true
  try {
    const data = await cardTypeApi.listOptions()
    cardTypeOptions.value = data.map((item) => ({
      label: item.name,
      value: item.id,
    }))
  } finally {
    cardTypeLoading.value = false
  }
}

// 加载卡片类型的人员字段
const loadMemberFields = async (cardTypeId: string) => {
  if (!cardTypeId) return
  fieldLoading.value = true
  try {
    // 获取卡片类型的属性配置
    const fields: FieldOption[] = await cardTypeApi.getFieldOptions(cardTypeId)
    availableFields.value = fields

    // 构建级联选项（用于通知群的字段选择）
    memberFieldOptions.value = [
      {
        label: t('admin.notificationSettings.template.form.cardFields'),
        value: 'card',
        children: fields.map((f) => ({ label: f.name || f.id, value: f.id })),
      },
      {
        label: t('admin.notificationSettings.template.form.systemUsers'),
        value: 'system',
        children: systemUserOptions.value.map((u) => ({ label: u.name, value: `system_${u.id}` })),
      },
    ]
  } finally {
    fieldLoading.value = false
  }
}

// 处理卡片类型变化
const handleCardTypeChange = (value: string) => {
  formData.value.recipientSelector.fieldId = ''
  formData.value.recipientSelector.fieldIds = []
  recipientFieldValue.value = []
  loadMemberFields(value)
}

// 处理通知对象类型变化
const handleRecipientTypeChange = () => {
  // 切换类型时重置选择器
  formData.value.recipientSelector.fieldIds = []
  formData.value.recipientSelector.fieldId = ''
  formData.value.recipientSelector.memberIds = []
  recipientFieldValue.value = []
}

// 处理成员字段变化
const handleMemberFieldChange = (value: (string | string[])[]) => {
  const flatValue = value.flat()
  // 过滤出卡片字段
  const cardFields = flatValue.filter((v): v is string => typeof v === 'string' && !v.startsWith('system_'))
  if (cardFields.length > 0) {
    const firstField = cardFields[0]
    if (firstField) {
      formData.value.recipientSelector.fieldId = firstField
    }
  }
  // 过滤出系统用户
  const systemUsers = flatValue.filter((v): v is string => typeof v === 'string' && v.startsWith('system_'))
  if (systemUsers.length > 0) {
    formData.value.recipientSelector.systemUserIds = systemUsers.map((v) => v.replace('system_', ''))
  }
}

// 插入字段到模板
const insertField = (field: 'titleTemplate' | 'shortContent' | 'longContent', fieldId: string) => {
  const currentValue = formData.value[field] || ''
  const fieldRef = `\${${fieldId}}`
  formData.value[field] = currentValue + fieldRef
}

// 重置表单
const resetForm = () => {
  formData.value = {
    name: '',
    cardTypeId: '',
    triggerEvent: '',
    recipientType: 'MEMBER',
    recipientSelector: {
      selectorType: 'FROM_FIELD',
      memberIds: [],
      fieldId: '',
      fieldIds: [],
      includeSystemUsers: false,
      systemUserIds: [],
    },
    channels: ['builtin'],
    titleTemplate: '',
    shortContent: '',
    longContent: '',
    priority: 100,
    enabled: true,
  }
  recipientFieldValue.value = []
}

// 处理取消
const handleCancel = () => {
  emit('update:visible', false)
  resetForm()
}

// 处理提交
const handleSubmit = async () => {
  const valid = await formRef.value?.validate()
  if (valid) return

  submitLoading.value = true
  try {
    // 构建 recipientSelector
    const recipientSelector = formData.value.recipientType === 'MEMBER'
      ? {
          selectorType: 'FROM_FIELD' as SelectorType,
          fieldIds: formData.value.recipientSelector.fieldIds,
        }
      : {
          selectorType: formData.value.recipientSelector.selectorType,
          memberIds: formData.value.recipientSelector.memberIds,
          fieldId: formData.value.recipientSelector.fieldId,
          includeSystemUsers: formData.value.recipientSelector.includeSystemUsers,
          systemUserIds: formData.value.recipientSelector.systemUserIds,
        }

    if (isEdit.value && props.template) {
      const updateData: UpdateNotificationTemplateRequest = {
        name: formData.value.name,
        cardTypeId: formData.value.cardTypeId,
        triggerEvent: formData.value.triggerEvent as any,
        recipientType: formData.value.recipientType,
        recipientSelector,
        channels: formData.value.channels,
        titleTemplate: formData.value.titleTemplate,
        shortContent: formData.value.shortContent,
        longContent: formData.value.longContent,
        priority: formData.value.priority,
        enabled: formData.value.enabled,
        expectedVersion: props.template.contentVersion,
      }
      await notificationTemplateApi.update(props.template.id!, updateData)
      Message.success(t('admin.message.saveSuccess'))
    } else {
      const createData: CreateNotificationTemplateRequest = {
        name: formData.value.name,
        cardTypeId: formData.value.cardTypeId,
        triggerEvent: formData.value.triggerEvent as any,
        recipientType: formData.value.recipientType,
        recipientSelector,
        channels: formData.value.channels,
        titleTemplate: formData.value.titleTemplate,
        shortContent: formData.value.shortContent,
        longContent: formData.value.longContent,
        priority: formData.value.priority,
      }
      await notificationTemplateApi.create(createData)
      Message.success(t('admin.message.createSuccess'))
    }
    emit('success')
    emit('update:visible', false)
    resetForm()
  } catch {
    Message.error(isEdit.value ? t('admin.notificationSettings.template.saveFailed') : t('admin.notificationSettings.template.createFailed'))
  } finally {
    submitLoading.value = false
  }
}

// 监听编辑数据变化
watch(
  () => props.template,
  (newVal) => {
    if (newVal) {
      formData.value = {
        name: newVal.name,
        cardTypeId: newVal.cardTypeId,
        triggerEvent: newVal.triggerEvent,
        recipientType: newVal.recipientType || 'MEMBER',
        recipientSelector: {
          selectorType: newVal.recipientSelector?.selectorType || 'FROM_FIELD',
          memberIds: newVal.recipientSelector?.memberIds || [],
          fieldId: newVal.recipientSelector?.fieldId || '',
          fieldIds: newVal.recipientSelector?.fieldIds || [],
          includeSystemUsers: newVal.recipientSelector?.includeSystemUsers || false,
          systemUserIds: newVal.recipientSelector?.systemUserIds || [],
        },
        channels: newVal.channels || ['builtin'],
        titleTemplate: newVal.titleTemplate || '',
        shortContent: newVal.shortContent || '',
        longContent: newVal.longContent || '',
        priority: newVal.priority,
        enabled: newVal.enabled,
      }
      loadMemberFields(newVal.cardTypeId)
    } else {
      resetForm()
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadCardTypes()
  // TODO: 加载成员和系统用户选项
})
</script>

<style scoped lang="scss">
.field-variables {
  margin-top: 8px;

  .help-text {
    display: block;
    margin-bottom: 8px;
    color: var(--color-text-3);
    font-size: 12px;
  }

  .field-tag {
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      color: rgb(var(--primary-6));
      border-color: rgb(var(--primary-6));
    }
  }
}
</style>
