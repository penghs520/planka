<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from '@arco-design/web-vue'
import { Message } from '@arco-design/web-vue'
import { cardTypeApi } from '@/api'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import FieldTypeIcon from '@/components/field/FieldTypeIcon.vue'
import { SchemaSubType } from '@/types/schema'
import { DateFormat } from '@/types/field'
import { getFieldTypeLabelI18n, supportsValueSource } from './formatters'
import type { ValueSourceType } from '@/types/card-type'
import type { EnumOptionDTO } from '@/types/view-data'
import type { LevelBinding } from './composables/useFieldConfigForm'
import {
  ValueSourceConfig,
  CommonFieldConfig,
  TextFieldConfig,
  NumberFieldConfig,
  DateFieldConfig,
  EnumFieldConfig,
  AttachmentFieldConfig,
  WebUrlFieldConfig,
  StructureFieldConfig,
} from './components/field-config'

const { t } = useI18n()

const props = defineProps<{
  visible: boolean
  cardTypeId: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  created: []
}>()

const drawerVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

const saving = ref(false)
const formRef = ref<FormInstance>()
const selectedType = ref<SchemaSubType | null>(null)
const step = ref<1 | 2>(1)

// 表单验证规则
const formRules = {
  name: [{ required: true, message: t('admin.cardType.fieldConfig.nameRequired') }],
}

// 表单数据
const formData = ref<{
  name: string
  code: string
  required: boolean
  readOnly: boolean
  valueSource: ValueSourceType
  referenceFieldId?: string
  // 文本类型
  maxLength?: number
  defaultValue?: string | number
  // 数字类型
  minValue?: number
  maxValue?: number
  precision: number
  unit?: string
  displayFormat: 'NORMAL' | 'PERCENT' | 'THOUSAND_SEPARATOR'
  percentStyle: 'NUMBER' | 'PROGRESS_BAR'
  // 日期类型
  dateFormat: DateFormat
  useNowAsDefault: boolean
  // 枚举类型
  multiSelect: boolean
  enumOptions: EnumOptionDTO[]
  // 附件类型
  maxFileSize?: number
  maxFileCount: number
  // 网页链接类型
  validateUrl: boolean
  showPreview: boolean
  defaultUrl: string
  defaultLinkText: string
  // 架构层级类型
  structureId: string
  levelBindings: LevelBinding[]
}>({
  name: '',
  code: '',
  required: false,
  readOnly: false,
  valueSource: 'MANUAL',
  referenceFieldId: undefined,
  maxLength: undefined,
  defaultValue: '',
  minValue: undefined,
  maxValue: undefined,
  precision: 0,
  unit: '',
  displayFormat: 'NORMAL',
  percentStyle: 'NUMBER',
  dateFormat: DateFormat.DATE,
  useNowAsDefault: false,
  multiSelect: false,
  enumOptions: [],
  maxFileSize: undefined,
  maxFileCount: 10,
  validateUrl: true,
  showPreview: false,
  defaultUrl: '',
  defaultLinkText: '',
  structureId: '',
  levelBindings: [],
})

// 支持的字段类型列表
const fieldTypes = [
  SchemaSubType.TEXT_FIELD,
  SchemaSubType.MULTI_LINE_TEXT_FIELD,
  SchemaSubType.DATE_FIELD,
  SchemaSubType.NUMBER_FIELD,
  SchemaSubType.ENUM_FIELD,
  SchemaSubType.STRUCTURE_FIELD,
  SchemaSubType.MARKDOWN_FIELD,
  SchemaSubType.ATTACHMENT_FIELD,
  SchemaSubType.WEB_URL_FIELD,
]

function getFieldTypeLabel(subType: SchemaSubType): string {
  return getFieldTypeLabelI18n(subType, t)
}

// 引用属性选项
const referenceFieldOptions = computed(() => {
  return [] as Array<{ id: string; name: string }>
})

/** 处理值来源变更 */
function handleValueSourceChange(value: ValueSourceType): void {
  formData.value.valueSource = value
  if (value !== 'MANUAL') {
    formData.value.required = false
    formData.value.readOnly = true
  }
}

/** 动态生成抽屉标题 */
const drawerTitle = computed(() => {
  if (selectedType.value) {
    return t('admin.cardType.fieldConfig.createNewWithType', { type: getFieldTypeLabel(selectedType.value) })
  }
  return t('admin.cardType.fieldConfig.createNew')
})

/** 判断是否为文本类型 */
const isTextType = computed(() => {
  return selectedType.value && [
    SchemaSubType.TEXT_FIELD,
    SchemaSubType.MULTI_LINE_TEXT_FIELD,
    SchemaSubType.MARKDOWN_FIELD,
  ].includes(selectedType.value)
})

/** 值来源是否为手动输入 */
const valueSourceIsManual = computed(() => formData.value.valueSource === 'MANUAL')

function handleSelectFieldType(type: SchemaSubType): void {
  selectedType.value = type
  switch (type) {
    case SchemaSubType.NUMBER_FIELD:
      formData.value.precision = 0
      formData.value.displayFormat = 'NORMAL'
      formData.value.percentStyle = 'NUMBER'
      break
    case SchemaSubType.DATE_FIELD:
      formData.value.dateFormat = DateFormat.DATE
      formData.value.useNowAsDefault = false
      break
    case SchemaSubType.ENUM_FIELD:
      formData.value.multiSelect = false
      formData.value.enumOptions = []
      break
    case SchemaSubType.ATTACHMENT_FIELD:
      formData.value.maxFileCount = 10
      break
    case SchemaSubType.WEB_URL_FIELD:
      formData.value.validateUrl = true
      formData.value.showPreview = false
      break
    case SchemaSubType.STRUCTURE_FIELD:
      formData.value.structureId = ''
      formData.value.levelBindings = []
      break
  }
  step.value = 2
}

function resetForm(): void {
  step.value = 1
  selectedType.value = null
  formData.value = {
    name: '',
    code: '',
    required: false,
    readOnly: false,
    valueSource: 'MANUAL',
    referenceFieldId: undefined,
    maxLength: undefined,
    defaultValue: '',
    minValue: undefined,
    maxValue: undefined,
    precision: 0,
    unit: '',
    displayFormat: 'NORMAL',
    percentStyle: 'NUMBER',
    dateFormat: DateFormat.DATE,
    useNowAsDefault: false,
    multiSelect: false,
    enumOptions: [],
    maxFileSize: undefined,
    maxFileCount: 10,
    validateUrl: true,
    showPreview: false,
    defaultUrl: '',
    defaultLinkText: '',
    structureId: '',
    levelBindings: [],
  }
}

function handleCancel(): void {
  drawerVisible.value = false
  resetForm()
}

async function handleSave(): Promise<void> {
  const errors = await formRef.value?.validate()
  if (errors) return

  saving.value = true
  try {
    const fieldConfig: Record<string, unknown> = {
      schemaSubType: selectedType.value,
      name: formData.value.name,
      code: formData.value.code,
      required: formData.value.required,
      readOnly: formData.value.readOnly,
      cardTypeId: props.cardTypeId,
      systemField: false,
    }

    // 添加值来源配置
    if (selectedType.value && supportsValueSource(selectedType.value)) {
      fieldConfig.valueSource = formData.value.valueSource || 'MANUAL'
      if (formData.value.valueSource === 'REFERENCE' && formData.value.referenceFieldId) {
        fieldConfig.referenceFieldId = formData.value.referenceFieldId
      }
    }

    // 根据类型添加特有配置
    switch (selectedType.value) {
      case SchemaSubType.TEXT_FIELD:
      case SchemaSubType.MULTI_LINE_TEXT_FIELD:
      case SchemaSubType.MARKDOWN_FIELD:
        Object.assign(fieldConfig, {
          maxLength: formData.value.maxLength,
          defaultValue: formData.value.defaultValue,
        })
        break
      case SchemaSubType.NUMBER_FIELD:
        Object.assign(fieldConfig, {
          minValue: formData.value.minValue,
          maxValue: formData.value.maxValue,
          precision: formData.value.precision,
          unit: formData.value.displayFormat === 'PERCENT' ? undefined : formData.value.unit,
          displayFormat: formData.value.displayFormat,
          percentStyle: formData.value.percentStyle,
          showThousandSeparator: formData.value.displayFormat === 'THOUSAND_SEPARATOR',
          defaultValue: formData.value.defaultValue ? parseFloat(String(formData.value.defaultValue)) : undefined,
        })
        break
      case SchemaSubType.DATE_FIELD:
        Object.assign(fieldConfig, {
          dateFormat: formData.value.dateFormat,
          useNowAsDefault: formData.value.useNowAsDefault,
        })
        break
      case SchemaSubType.ENUM_FIELD:
        Object.assign(fieldConfig, {
          multiSelect: formData.value.multiSelect,
          options: formData.value.enumOptions?.map((opt) => ({
            id: opt.id,
            value: opt.value,
            label: opt.label,
            order: opt.order,
            enabled: opt.enabled,
            color: opt.color,
          })) || [],
        })
        break
      case SchemaSubType.ATTACHMENT_FIELD:
        Object.assign(fieldConfig, {
          maxFileSize: formData.value.maxFileSize,
          maxFileCount: formData.value.maxFileCount,
        })
        break
      case SchemaSubType.WEB_URL_FIELD:
        Object.assign(fieldConfig, {
          validateUrl: formData.value.validateUrl,
          showPreview: formData.value.showPreview,
          defaultUrl: formData.value.defaultUrl,
          defaultLinkText: formData.value.defaultLinkText,
        })
        break
      case SchemaSubType.STRUCTURE_FIELD:
        Object.assign(fieldConfig, {
          structureId: formData.value.structureId,
          levelBindings: formData.value.levelBindings,
        })
        break
    }

    await cardTypeApi.saveFieldConfig(props.cardTypeId, fieldConfig as unknown as Parameters<typeof cardTypeApi.saveFieldConfig>[1])
    Message.success(t('admin.cardType.fieldConfig.createSuccess'))
    emit('created')
    drawerVisible.value = false
    resetForm()
  } catch (error) {
    console.error('Failed to create field config:', error)
    // 错误提示已由 request 拦截器显示，这里不再重复显示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <!-- 步骤1：类型选择弹窗 -->
  <a-modal
    :visible="drawerVisible && step === 1"
    :title="t('admin.cardType.fieldConfig.selectFieldType')"
    :width="560"
    :mask-closable="true"
    :esc-to-close="true"
    :footer="false"
    @cancel="handleCancel"
  >
    <div class="field-type-selection-modal">
      <div class="field-type-grid-modal">
        <div
          v-for="type in fieldTypes"
          :key="type"
          class="field-type-card-modal"
          @click="handleSelectFieldType(type)"
        >
          <FieldTypeIcon :field-type="type" class="field-type-icon-modal" />
          <div class="field-type-name-modal">{{ getFieldTypeLabel(type) }}</div>
        </div>
      </div>
    </div>
  </a-modal>

  <!-- 步骤2：配置表单抽屉 -->
  <a-drawer
    :visible="drawerVisible && step === 2"
    :title="drawerTitle"
    :width="880"
    :mask-closable="true"
    :esc-to-close="true"
    unmount-on-close
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formData"
      layout="vertical"
      :rules="formRules"
      class="field-config-form"
    >
      <!-- 基础信息 -->
      <div class="form-section">
        <div class="basic-info-grid">
          <a-form-item
            field="name"
            :label="t('admin.cardType.fieldConfig.fieldName')"
            :validate-trigger="['change', 'blur']"
            class="form-field"
          >
            <a-input
              v-model="formData.name"
              :placeholder="t('admin.cardType.fieldConfig.enterFieldName')"
              :max-length="50"
            />
          </a-form-item>
          <a-form-item :label="t('admin.cardType.fieldConfig.fieldCode')" class="form-field">
            <a-input
              v-model="formData.code"
              :max-length="50"
            />
            <template #extra>
              <span class="field-extra">{{ t('admin.cardType.fieldConfig.codeHint') || '用于API调用，留空自动生成' }}</span>
            </template>
          </a-form-item>
        </div>
      </div>

      <!-- 值来源配置 -->
      <ValueSourceConfig
        v-if="selectedType && supportsValueSource(selectedType)"
        :model-value="formData.valueSource"
        :schema-sub-type="selectedType"
        :reference-field-id="formData.referenceFieldId"
        :reference-field-options="referenceFieldOptions"
        @update:model-value="handleValueSourceChange"
        @update:reference-field-id="(val) => formData.referenceFieldId = val"
      />

      <!-- 通用配置 -->
      <CommonFieldConfig
        v-model:required="formData.required"
        v-model:read-only="formData.readOnly"
        :value-source-is-manual="valueSourceIsManual"
      />

      <!-- 文本类型配置 -->
      <TextFieldConfig
        v-if="isTextType"
        v-model:max-length="formData.maxLength"
        v-model:default-value="formData.defaultValue as string"
      />

      <!-- 数字类型配置 -->
      <NumberFieldConfig
        v-if="selectedType === SchemaSubType.NUMBER_FIELD"
        v-model:default-value="formData.defaultValue as number"
        v-model:min-value="formData.minValue"
        v-model:max-value="formData.maxValue"
        v-model:precision="formData.precision"
        v-model:unit="formData.unit"
        v-model:display-format="formData.displayFormat"
        v-model:percent-style="formData.percentStyle"
      />

      <!-- 日期类型配置 -->
      <DateFieldConfig
        v-if="selectedType === SchemaSubType.DATE_FIELD"
        v-model:date-format="formData.dateFormat"
        v-model:use-now-as-default="formData.useNowAsDefault"
      />

      <!-- 枚举类型配置 -->
      <EnumFieldConfig
        v-if="selectedType === SchemaSubType.ENUM_FIELD"
        v-model:multi-select="formData.multiSelect"
        v-model:options="formData.enumOptions"
      />

      <!-- 附件类型配置 -->
      <AttachmentFieldConfig
        v-if="selectedType === SchemaSubType.ATTACHMENT_FIELD"
        v-model:max-file-size="formData.maxFileSize"
        v-model:max-file-count="formData.maxFileCount"
      />

      <!-- 网页链接类型配置 -->
      <WebUrlFieldConfig
        v-if="selectedType === SchemaSubType.WEB_URL_FIELD"
        v-model:validate-url="formData.validateUrl"
        v-model:show-preview="formData.showPreview"
        v-model:default-url="formData.defaultUrl"
        v-model:default-link-text="formData.defaultLinkText"
      />

      <!-- 架构层级类型配置 -->
      <StructureFieldConfig
        v-if="selectedType === SchemaSubType.STRUCTURE_FIELD"
        v-model:structure-id="formData.structureId"
        v-model:level-bindings="formData.levelBindings"
        :card-type-id="props.cardTypeId"
        :field-name="formData.name"
      />
    </a-form>

    <template #footer>
      <a-space>
        <CancelButton @click="handleCancel" />
        <SaveButton
          :loading="saving"
          :text="t('admin.action.create')"
          @click="handleSave"
        />
      </a-space>
    </template>
  </a-drawer>
</template>

<style scoped>
/* 类型选择弹窗 */
.field-type-selection-modal {
  padding: 8px 0;
}

.field-type-grid-modal {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.field-type-card-modal {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 16px;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--color-bg-2);
}

.field-type-card-modal:hover {
  border-color: rgb(var(--primary-5));
  background: rgb(var(--primary-1));
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.field-type-icon-modal {
  font-size: 28px !important;
  margin-bottom: 8px;
  color: var(--color-text-2) !important;
  transition: color 0.2s;
}

.field-type-card-modal:hover .field-type-icon-modal {
  color: rgb(var(--primary-6)) !important;
}

.field-type-name-modal {
  font-size: 14px;
  color: var(--color-text-1);
  white-space: nowrap;
}

.field-type-card-modal:hover .field-type-name-modal {
  color: rgb(var(--primary-6));
}

/* 表单配置样式 */
.field-config-form {
  padding: 4px 0;
}

.form-section {
  margin-bottom: 4px;
}

.basic-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.basic-info-grid :deep(.arco-form-item) {
  max-width: 320px;
}

.form-field {
  margin-bottom: 0;
}

.field-extra {
  font-size: 12px;
  color: var(--color-text-3);
  margin-top: 4px;
}

/* 表单项标签样式 */
:deep(.arco-form-item-label-col > label) {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-1);
}

/* 输入框聚焦样式增强 */
:deep(.arco-input-wrapper:focus-within),
:deep(.arco-select-view:focus-within) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(51, 112, 255, 0.1);
}
</style>
