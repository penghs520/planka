<script setup lang="ts">
/**
 * 卡片新建 Modal
 * 
 * 根据 CreatePageFormVO 动态渲染表单，支持各种字段类型
 */
import { ref, computed, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import dayjs from 'dayjs'
import { cardApi, cardCreatePageTemplateApi, attendanceApi } from '@/api'
import { outsourcingConfigApi } from '@/api/outsourcing-config'
import { useOrgStore } from '@/stores/org'
import { LeaveUnit, type OutsourcingConfig } from '@/types/outsourcing-config'
import type { CreateCardRequest, LinkFieldUpdate } from '@/types/card'
import { pureTitle } from '@/types/card'
import type {
  CreatePageFormVO,
  CreatePageFieldVO,
  TextFieldVO,
  NumberFieldVO,
  EnumFieldVO,
  LinkFieldVO,
} from '@/types/card-create-page-template'
import FieldStructureEditor from '@/components/field/editor/FieldStructureEditor.vue'
import FieldLinkEditor from '@/components/field/editor/FieldLinkEditor.vue'
import type { LinkedCard } from '@/types/card'

const props = defineProps<{
  /** 是否显示 */
  visible: boolean
  /** 卡片类型 ID */
  cardTypeId: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'success'): void
}>()

const orgStore = useOrgStore()

// 状态
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const formConfig = ref<CreatePageFormVO | null>(null)
const formData = ref<Record<string, any>>({})
const outsourcingConfig = ref<OutsourcingConfig | null>(null)

// 是否请假申请卡片类型
const isLeaveApplication = computed(() => {
  return props.cardTypeId?.includes('leave-application')
})

// 是否加班申请卡片类型
const isOvertimeApplication = computed(() => {
  return props.cardTypeId?.includes('overtime-application')
})

// 是否补卡申请卡片类型
const isMakeupApplication = computed(() => {
  return props.cardTypeId?.includes('makeup-application')
})

// 是否考勤申请卡片类型（需要验证）
const isAttendanceApplication = computed(() => {
  return isLeaveApplication.value || isOvertimeApplication.value || isMakeupApplication.value
})

// 日期字段ID（根据实际卡片类型动态识别）
const startDateFieldId = computed(() => {
  return fields.value.find(f => f.fieldId.includes('start-date'))?.fieldId
})

const endDateFieldId = computed(() => {
  return fields.value.find(f => f.fieldId.includes('end-date'))?.fieldId
})

const durationFieldId = computed(() => {
  return fields.value.find(f => f.fieldId.includes('duration'))?.fieldId
})

// 计算属性
const modalTitle = computed(() =>
  formConfig.value?.cardTypeName ? `新建${formConfig.value.cardTypeName}` : '新建卡片'
)

const fields = computed(() => formConfig.value?.fields || [])

// 获取字段标签
function getFieldLabel(field: CreatePageFieldVO): string {
  return field.name
}

// 监听可见性变化
watch(
  () => props.visible,
  async (visible) => {
    if (visible && props.cardTypeId) {
      await loadFormConfig()
    }
  }
)

// 加载表单配置
async function loadFormConfig() {
  loading.value = true
  try {
    // 先获取当前成员信息（用于设置申请人默认值）
    if (!orgStore.currentMember) {
      await orgStore.fetchCurrentOrgRole()
    }

    const config = await cardCreatePageTemplateApi.getForm(props.cardTypeId)
    formConfig.value = config
    console.log('[CardCreateModal] 表单配置:', config)
    console.log('[CardCreateModal] 字段列表:', config.fields)
    config.fields.forEach((field: any) => {
      if (field.fieldType === 'ENUM') {
        console.log(`[CardCreateModal] 枚举字段 ${field.name}(${field.fieldId}):`, field.options)
      }
    })
    initFormData(config)
    // 加载考勤配置（用于请假时长计算）
    await loadOutsourcingConfig()
  } catch (error: any) {
    console.error('Failed to load form config:', error)
    Message.error('加载表单配置失败')
    handleClose()
  } finally {
    loading.value = false
  }
}

// 加载考勤配置
async function loadOutsourcingConfig() {
  if (!orgStore.currentOrgId || !isLeaveApplication.value) return
  try {
    const config = await outsourcingConfigApi.getByOrgId(orgStore.currentOrgId)
    outsourcingConfig.value = config
  } catch (error) {
    console.error('[CardCreateModal] 加载考勤配置失败:', error)
  }
}

// 监听日期变化，自动计算请假时长
watch(
  () => {
    const startId = startDateFieldId.value
    const endId = endDateFieldId.value
    if (!startId || !endId) return []
    return [formData.value[startId], formData.value[endId]]
  },
  ([startDate, endDate]) => {
    if (!isLeaveApplication.value || !durationFieldId.value) return
    if (!startDate || !endDate) return

    const start = dayjs(startDate)
    const end = dayjs(endDate)

    if (!start.isValid() || !end.isValid()) return
    if (end.isBefore(start)) return

    // 计算天数差（包含起始日）
    const daysDiff = end.diff(start, 'day') + 1

    // 根据请假单位计算时长
    const leaveUnit = outsourcingConfig.value?.leaveConf?.leaveUnit ?? LeaveUnit.DAY
    let duration: number

    if (leaveUnit === LeaveUnit.HALF_DAY) {
      // 半天单位：每半天算0.5
      duration = daysDiff * 2 * 0.5 // 每天2个半天
    } else {
      // 天单位
      duration = daysDiff
    }

    // 设置时长字段值
    formData.value[durationFieldId.value] = duration
  },
  { immediate: false }
)

// 初始化表单数据
function initFormData(config: CreatePageFormVO) {
  const data: Record<string, any> = {}
  for (const field of config.fields) {
    data[field.fieldId] = getFieldDefaultValue(field)
  }
  // 为申请人字段设置当前成员作为默认值
  const applicantField = config.fields.find(f => f.fieldId.includes('link:applicant'))
  if (applicantField && orgStore.currentMemberCardId) {
    const memberTitle = orgStore.currentMember?.nickname
      || orgStore.currentMember?.email
      || '当前用户'
    data[applicantField.fieldId] = [{
      cardId: orgStore.currentMemberCardId,
      title: memberTitle
    }]
    console.log('[CardCreateModal] 设置申请人默认值:', {
      fieldId: applicantField.fieldId,
      cardId: orgStore.currentMemberCardId,
      title: memberTitle
    })
  } else {
    console.log('[CardCreateModal] 无法设置申请人默认值:', {
      hasApplicantField: !!applicantField,
      currentMemberCardId: orgStore.currentMemberCardId,
      currentMember: orgStore.currentMember
    })
  }
  formData.value = data
}

// 获取字段默认值
function getFieldDefaultValue(field: CreatePageFieldVO): any {
  switch (field.fieldType) {
    case 'TEXT':
      return (field as TextFieldVO).defaultValue || ''
    case 'NUMBER':
      return (field as NumberFieldVO).defaultValue ?? null
    case 'DATE':
      return null
    case 'ENUM':
      const enumField = field as EnumFieldVO
      if (enumField.multiSelect) {
        return enumField.defaultOptionIds || []
      }
      return enumField.defaultOptionIds?.[0] || null
    case 'LINK':
      return []
    default:
      return null
  }
}

// 关闭对话框
function handleClose() {
  emit('update:visible', false)
  // 清理状态
  formConfig.value = null
  formData.value = {}
}

// 提交表单
async function handleSubmit() {
  // 表单验证
  const errors = await formRef.value?.validate()
  if (errors) return

  submitting.value = true
  try {
    const fieldValues: Record<string, any> = {}
    const linkUpdates: LinkFieldUpdate[] = []

    // 构建 fieldValues（排除 $title 和 LINK 类型）
    for (const field of fields.value) {
      if (field.fieldId === '$title') continue
      const value = formData.value[field.fieldId]

      if (field.fieldType === 'LINK') {
        // 关联属性：组装为 linkUpdates
        const selectedCards = value as LinkedCard[] | undefined
        if (selectedCards && selectedCards.length > 0) {
          linkUpdates.push({
            linkFieldId: field.fieldId,
            targetCardIds: selectedCards.map(c => c.cardId),
          })
        }
        continue
      }

      if (value != null && value !== '') {
        fieldValues[field.fieldId] = buildFieldValue(field, value)
      }
    }

    console.log('[CardCreateModal] 提交表单', {
      cardTypeId: props.cardTypeId,
      isAttendanceApplication: isAttendanceApplication.value,
      fieldValues
    })

    // 如果是考勤申请类型，先调用验证接口
    if (isAttendanceApplication.value) {
      console.log('[CardCreateModal] 开始验证考勤申请')

      // 提取申请人ID
      const applicantField = fields.value.find(f => f.fieldId.includes('link:applicant'))
      const applicantCards = applicantField ? formData.value[applicantField.fieldId] : []
      const applicantId = applicantCards?.[0]?.cardId || orgStore.currentMemberCardId

      console.log('[CardCreateModal] 申请人信息', {
        applicantField: applicantField?.fieldId,
        applicantCards,
        applicantId
      })

      if (!applicantId) {
        Message.error('无法获取申请人信息')
        return
      }

      // 调用验证接口
      console.log('[CardCreateModal] 调用验证接口', {
        orgId: orgStore.currentOrgId,
        applicantId,
        cardTypeId: props.cardTypeId,
        fieldValues
      })

      const validationResult = await attendanceApi.validateApplication({
        orgId: orgStore.currentOrgId!,
        applicantId,
        cardTypeId: props.cardTypeId,
        fieldValues,
      })

      console.log('[CardCreateModal] 验证结果', validationResult)

      // 如果验证失败，显示错误信息并返回
      if (!validationResult.valid) {
        Message.error(validationResult.message || '验证失败')
        return
      }
    }

    const request: CreateCardRequest = {
      orgId: orgStore.currentOrgId!,
      typeId: props.cardTypeId,
      title: pureTitle(formData.value['$title'] || ''),
      fieldValues,
      linkUpdates: linkUpdates.length > 0 ? linkUpdates : undefined,
    }

    await cardApi.create(request)
    Message.success('创建成功')
    emit('success')
    handleClose()
  } catch (error: any) {
    console.error('Create card failed:', error)
    Message.error(error.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

// 构建字段值（符合 FieldValue 类型）
function buildFieldValue(field: CreatePageFieldVO, value: any): any {
  const base = { fieldId: field.fieldId, readable: true }
  switch (field.fieldType) {
    case 'TEXT':
    case 'TEXTAREA':
    case 'MARKDOWN':
      // 所有文本类型都使用 TEXT 类型
      return { ...base, type: 'TEXT', value }
    case 'NUMBER':
      return { ...base, type: 'NUMBER', value }
    case 'DATE':
      // 日期字段需要转换为时间戳（毫秒）
      const timestamp = value ? dayjs(value).valueOf() : null
      return { ...base, type: 'DATE', value: timestamp }
    case 'ENUM':
      const enumField = field as EnumFieldVO
      // 枚举字段值统一存储为数组
      const optionIds = enumField.multiSelect 
        ? value 
        : (value ? [value] : [])
      return { ...base, type: 'ENUM', value: optionIds }
    case 'STRUCTURE':
      // 架构字段值直接使用 FieldStructureEditor 构建的值
      if (value && typeof value === 'object' && value.type === 'STRUCTURE') {
        return value
      }
      return { ...base, type: 'STRUCTURE', value: value }
    case 'LINK':
      // LINK 字段值：返回关联卡片ID数组
      const linkedCards = value as LinkedCard[]
      return linkedCards.map(c => c.cardId)
    default:
      return { ...base, type: 'TEXT', value }
  }
}

// 生成表单验证规则
function getFieldRules(field: CreatePageFieldVO) {
  const rules: any[] = []
  if (field.required) {
    rules.push({ required: true, message: `请输入${field.name}` })
  }
  return rules
}

// 判断是否需要换行（100% 宽度或前一个字段是 100%）
function shouldStartNewRow(index: number): boolean {
  if (index === 0) return false
  const field = fields.value[index]
  const prevField = fields.value[index - 1]
  if (!field || !prevField) return false
  return field.widthPercent === 100 || prevField.widthPercent === 100
}
</script>

<template>
  <a-modal
    :visible="visible"
    :title="modalTitle"
    :width="640"
    :mask-closable="false"
    :closable="!submitting"
    unmount-on-close
    @cancel="handleClose"
  >
    <a-spin :loading="loading" class="form-loading">
      <a-form
        ref="formRef"
        :model="formData"
        layout="vertical"
        size="medium"
      >
        <div class="form-grid">
          <template v-for="(field, index) in fields" :key="field.fieldId">
            <!-- 100% 宽度字段 -->
            <div
              v-if="field.widthPercent === 100"
              class="form-field full-width"
              :class="{ 'new-row': shouldStartNewRow(index) }"
            >
              <a-form-item
                :field="field.fieldId"
                :label="getFieldLabel(field)"
                :rules="getFieldRules(field)"
              >
                <!-- 单行文本字段 -->
                <a-input
                  v-if="field.fieldType === 'TEXT'"
                  v-model="formData[field.fieldId]"
                  :placeholder="field.placeholder || `请输入${field.name}`"
                  :max-length="(field as TextFieldVO).maxLength"
                  :disabled="field.readOnly"
                  allow-clear
                />

                <!-- 多行文本字段 -->
                <a-textarea
                  v-else-if="field.fieldType === 'TEXTAREA' || field.fieldType === 'MARKDOWN'"
                  v-model="formData[field.fieldId]"
                  :placeholder="field.placeholder || `请输入${field.name}`"
                  :max-length="(field as TextFieldVO).maxLength"
                  :disabled="field.readOnly"
                  :auto-size="{ minRows: 2, maxRows: 6 }"
                  allow-clear
                />

                <!-- 数字字段 -->
                <a-input-number
                  v-else-if="field.fieldType === 'NUMBER'"
                  v-model="formData[field.fieldId]"
                  :placeholder="`请输入${field.name}`"
                  :min="(field as NumberFieldVO).minValue"
                  :max="(field as NumberFieldVO).maxValue"
                  :precision="(field as NumberFieldVO).precision"
                  :disabled="field.readOnly"
                  class="full-width-input"
                />

                <!-- 日期字段 -->
                <a-date-picker
                  v-else-if="field.fieldType === 'DATE'"
                  v-model="formData[field.fieldId]"
                  :placeholder="`请选择${field.name}`"
                  :disabled="field.readOnly"
                  class="full-width-input"
                />

                <!-- 枚举字段 -->
                <a-select
                  v-else-if="field.fieldType === 'ENUM'"
                  v-model="formData[field.fieldId]"
                  :placeholder="`请选择${field.name}`"
                  :multiple="(field as EnumFieldVO).multiSelect"
                  :disabled="field.readOnly"
                  allow-clear
                >
                  <a-option
                    v-for="opt in (field as EnumFieldVO).options?.filter(o => o.enabled !== false)"
                    :key="opt.id"
                    :value="opt.id"
                  >
                    {{ opt.label }}
                  </a-option>
                </a-select>

                <!-- 关联字段 (LINK) -->
                <FieldLinkEditor
                  v-else-if="field.fieldType === 'LINK'"
                  v-model:selected-cards="formData[field.fieldId]"
                  :link-field-id="field.fieldId"
                  :render-config="(field as LinkFieldVO).renderConfig"
                  class="full-width-input"
                  @save="() => {}"
                />

                <!-- 架构属性字段 -->
                <FieldStructureEditor
                  v-else-if="field.fieldType === 'STRUCTURE'"
                  :field-id="field.fieldId"
                  :model-value="formData[field.fieldId]"
                  :placeholder="`请选择${field.name}`"
                  :disabled="field.readOnly"
                  class="full-width-input"
                  @update:model-value="formData[field.fieldId] = $event"
                />

                <!-- 其他字段类型 - 占位 -->
                <a-input
                  v-else
                  v-model="formData[field.fieldId]"
                  :placeholder="`${field.name}（暂不支持）`"
                  disabled
                />
              </a-form-item>
            </div>

            <!-- 50% 宽度字段 -->
            <div
              v-else
              class="form-field half-width"
              :class="{ 'new-row': shouldStartNewRow(index) }"
            >
              <a-form-item
                :field="field.fieldId"
                :label="getFieldLabel(field)"
                :rules="getFieldRules(field)"
              >
                <!-- 单行文本字段 -->
                <a-input
                  v-if="field.fieldType === 'TEXT'"
                  v-model="formData[field.fieldId]"
                  :placeholder="field.placeholder || `请输入${field.name}`"
                  :max-length="(field as TextFieldVO).maxLength"
                  :disabled="field.readOnly"
                  allow-clear
                />

                <!-- 多行文本字段 -->
                <a-textarea
                  v-else-if="field.fieldType === 'TEXTAREA' || field.fieldType === 'MARKDOWN'"
                  v-model="formData[field.fieldId]"
                  :placeholder="field.placeholder || `请输入${field.name}`"
                  :max-length="(field as TextFieldVO).maxLength"
                  :disabled="field.readOnly"
                  :auto-size="{ minRows: 2, maxRows: 4 }"
                  allow-clear
                />

                <!-- 数字字段 -->
                <a-input-number
                  v-else-if="field.fieldType === 'NUMBER'"
                  v-model="formData[field.fieldId]"
                  :placeholder="`请输入${field.name}`"
                  :min="(field as NumberFieldVO).minValue"
                  :max="(field as NumberFieldVO).maxValue"
                  :precision="(field as NumberFieldVO).precision"
                  :disabled="field.readOnly"
                  class="full-width-input"
                />

                <!-- 日期字段 -->
                <a-date-picker
                  v-else-if="field.fieldType === 'DATE'"
                  v-model="formData[field.fieldId]"
                  :placeholder="`请选择${field.name}`"
                  :disabled="field.readOnly"
                  class="full-width-input"
                />

                <!-- 枚举字段 -->
                <a-select
                  v-else-if="field.fieldType === 'ENUM'"
                  v-model="formData[field.fieldId]"
                  :placeholder="`请选择${field.name}`"
                  :multiple="(field as EnumFieldVO).multiSelect"
                  :disabled="field.readOnly"
                  allow-clear
                >
                  <a-option
                    v-for="opt in (field as EnumFieldVO).options?.filter(o => o.enabled !== false)"
                    :key="opt.id"
                    :value="opt.id"
                  >
                    {{ opt.label }}
                  </a-option>
                </a-select>

                <!-- 关联字段 (LINK) -->
                <FieldLinkEditor
                  v-else-if="field.fieldType === 'LINK'"
                  v-model:selected-cards="formData[field.fieldId]"
                  :link-field-id="field.fieldId"
                  :render-config="(field as LinkFieldVO).renderConfig"
                  class="full-width-input"
                  @save="() => {}"
                />

                <!-- 架构属性字段 -->
                <FieldStructureEditor
                  v-else-if="field.fieldType === 'STRUCTURE'"
                  :field-id="field.fieldId"
                  :model-value="formData[field.fieldId]"
                  :placeholder="`请选择${field.name}`"
                  :disabled="field.readOnly"
                  class="full-width-input"
                  @update:model-value="formData[field.fieldId] = $event"
                />

                <!-- 其他字段类型 - 占位 -->
                <a-input
                  v-else
                  v-model="formData[field.fieldId]"
                  :placeholder="`${field.name}（暂不支持）`"
                  disabled
                />
              </a-form-item>
            </div>
          </template>
        </div>
      </a-form>
    </a-spin>

    <template #footer>
      <a-space>
        <a-button :disabled="submitting" @click="handleClose">取消</a-button>
        <a-button type="primary" :loading="submitting" @click="handleSubmit">
          创建
        </a-button>
      </a-space>
    </template>
  </a-modal>
</template>

<style scoped lang="scss">
.form-loading {
  min-height: 200px;
  width: 100%;
}

.form-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0 24px;
}

.form-field {
  box-sizing: border-box;

  &.full-width {
    width: 100%;
  }

  &.half-width {
    width: calc(50% - 12px);
  }
}

.full-width-input {
  width: 100%;
}

:deep(.arco-form-item) {
  margin-bottom: 16px;
}

:deep(.arco-form-item-label) {
  font-weight: 500;
}
</style>
