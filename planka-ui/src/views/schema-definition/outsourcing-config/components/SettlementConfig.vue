<template>
  <div class="settlement-config">
    <a-form :model="modelValue" layout="vertical" auto-label-width>
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.settlement.method')"
            field="method"
          >
            <a-select
              v-model="modelValue.method"
              :placeholder="t('outsourcingConfig.settlement.methodPlaceholder')"
            >
              <a-option
                v-for="option in settlementMethodOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </a-option>
            </a-select>
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.settlement.absenteeismDeductionCoefficient')"
            field="absenteeismDeductionCoefficient"
          >
            <a-input-number
              v-model="modelValue.absenteeismDeductionCoefficient"
              :placeholder="t('outsourcingConfig.settlement.absenteeismDeductionCoefficientPlaceholder')"
              :min="0"
              :precision="0"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.settlement.durationUnit')"
            field="durationUnit"
          >
            <a-select
              v-model="modelValue.durationUnit"
              :placeholder="t('outsourcingConfig.settlement.durationUnitPlaceholder')"
            >
              <a-option
                v-for="option in durationUnitOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </a-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.settlement.decimalScale')"
            field="decimalScale"
          >
            <a-input-number
              v-model="modelValue.decimalScale"
              :placeholder="t('outsourcingConfig.settlement.decimalScalePlaceholder')"
              :min="0"
              :precision="0"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.settlement.vutIds')"
            field="vutIds"
          >
            <a-select
              v-model="selectedCardTypeId"
              :placeholder="t('outsourcingConfig.settlement.vutIdsPlaceholder')"
              :loading="loadingMemberCardTypes"
              allow-clear
              allow-search
            >
              <a-option
                v-for="option in memberCardTypeOptions"
                :key="option.id"
                :value="option.id"
              >
                {{ option.name }}
              </a-option>
            </a-select>
            <template #extra>
              <span class="form-item-tip">
                选择参与结算的成员卡片类型
              </span>
            </template>
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.settlement.leaveDateFieldId')"
            field="leaveDateFieldId"
          >
            <a-select
              v-model="modelValue.leaveDateFieldId"
              :placeholder="t('outsourcingConfig.settlement.leaveDateFieldIdPlaceholder')"
              :loading="loadingFields"
              :disabled="!modelValue.vutIds || modelValue.vutIds.length === 0"
              allow-clear
              allow-search
            >
              <a-option
                v-for="field in dateFieldOptions"
                :key="field.id"
                :value="field.id"
              >
                {{ field.name }}
              </a-option>
            </a-select>
            <template #extra>
              <span v-if="!modelValue.vutIds || modelValue.vutIds.length === 0" class="form-item-tip warning">
                请先选择成员卡片类型
              </span>
              <span v-else-if="dateFieldOptions.length === 0" class="form-item-tip warning">
                所选卡片类型中没有日期类型字段
              </span>
              <span v-else class="form-item-tip">
                选择成员卡片中的离职日期字段
              </span>
            </template>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.settlement.specialLeaveItemIds')"
            field="specialLeaveItemIds"
          >
            <a-select
              v-model="modelValue.specialLeaveItemIds"
              :placeholder="t('outsourcingConfig.settlement.specialLeaveItemIdsPlaceholder')"
              multiple
              allow-clear
              :disabled="!enabledLeaveTypes || enabledLeaveTypes.length === 0"
            >
              <a-option
                v-for="option in enabledLeaveTypeOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </a-option>
            </a-select>
            <template #extra>
              <span v-if="!enabledLeaveTypes || enabledLeaveTypes.length === 0" class="form-item-tip warning">
                请先在"请假配置"中选择启用的请假类型
              </span>
              <span v-else class="form-item-tip">
                选择需要单独计算的请假类型
              </span>
            </template>
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider>{{ t('outsourcingConfig.settlement.personalServiceFeeConf') }}</a-divider>

      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.settlement.baseFeeFieldId')"
            field="personalServiceFeeConf.baseFeeFieldId"
          >
            <a-select
              v-model="modelValue.personalServiceFeeConf!.baseFeeFieldId"
              :placeholder="t('outsourcingConfig.settlement.baseFeeFieldIdPlaceholder')"
              :loading="loadingFields"
              :disabled="!modelValue.vutIds || modelValue.vutIds.length === 0"
              allow-clear
              allow-search
            >
              <a-option
                v-for="field in numberFieldOptions"
                :key="field.id"
                :value="field.id"
              >
                {{ field.name }}
              </a-option>
            </a-select>
            <template #extra>
              <span v-if="!modelValue.vutIds || modelValue.vutIds.length === 0" class="form-item-tip warning">
                请先选择成员卡片类型
              </span>
              <span v-else-if="numberFieldOptions.length === 0" class="form-item-tip warning">
                所选卡片类型中没有数字类型字段
              </span>
            </template>
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.settlement.overtimeFeeFieldId')"
            field="personalServiceFeeConf.overtimeFeeFieldId"
          >
            <a-select
              v-model="modelValue.personalServiceFeeConf!.overtimeFeeFieldId"
              :placeholder="t('outsourcingConfig.settlement.overtimeFeeFieldIdPlaceholder')"
              :loading="loadingFields"
              :disabled="!modelValue.vutIds || modelValue.vutIds.length === 0"
              allow-clear
              allow-search
            >
              <a-option
                v-for="field in numberFieldOptions"
                :key="field.id"
                :value="field.id"
              >
                {{ field.name }}
              </a-option>
            </a-select>
            <template #extra>
              <span v-if="!modelValue.vutIds || modelValue.vutIds.length === 0" class="form-item-tip warning">
                请先选择成员卡片类型
              </span>
              <span v-else-if="numberFieldOptions.length === 0" class="form-item-tip warning">
                所选卡片类型中没有数字类型字段
              </span>
            </template>
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.settlement.subsidyFieldId')"
            field="personalServiceFeeConf.subsidyFieldId"
          >
            <a-select
              v-model="modelValue.personalServiceFeeConf!.subsidyFieldId"
              :placeholder="t('outsourcingConfig.settlement.subsidyFieldIdPlaceholder')"
              :loading="loadingFields"
              :disabled="!modelValue.vutIds || modelValue.vutIds.length === 0"
              allow-clear
              allow-search
            >
              <a-option
                v-for="field in numberFieldOptions"
                :key="field.id"
                :value="field.id"
              >
                {{ field.name }}
              </a-option>
            </a-select>
            <template #extra>
              <span v-if="!modelValue.vutIds || modelValue.vutIds.length === 0" class="form-item-tip warning">
                请先选择成员卡片类型
              </span>
              <span v-else-if="numberFieldOptions.length === 0" class="form-item-tip warning">
                所选卡片类型中没有数字类型字段
              </span>
            </template>
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider>{{ t('outsourcingConfig.settlement.projectServiceFeeConf') }}</a-divider>

      <!-- 分摊维度列表 -->
      <div
        v-if="modelValue.projectServiceFeeConf?.columns && modelValue.projectServiceFeeConf.columns.length > 0"
        class="column-list"
      >
        <div
          v-for="(column, index) in modelValue.projectServiceFeeConf.columns"
          :key="index"
          class="column-item"
        >
          <a-row :gutter="16">
            <a-col :span="10">
              <a-form-item
                :label="t('outsourcingConfig.settlement.column')"
                :field="`projectServiceFeeConf.columns.${index}.column`"
              >
                <a-input
                  v-model="column.column"
                  :placeholder="t('outsourcingConfig.settlement.columnPlaceholder')"
                />
              </a-form-item>
            </a-col>

            <a-col :span="10">
              <a-form-item
                :label="t('outsourcingConfig.settlement.active')"
                :field="`projectServiceFeeConf.columns.${index}.active`"
              >
                <a-switch v-model="column.active" />
              </a-form-item>
            </a-col>

            <a-col :span="4">
              <a-form-item label=" ">
                <a-button type="text" status="danger" @click="removeColumn(index)">
                  <template #icon>
                    <icon-delete />
                  </template>
                </a-button>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
      </div>

      <a-empty v-else :description="t('outsourcingConfig.settlement.columnsEmpty')" />

      <a-button type="dashed" long @click="addColumn">
        <template #icon>
          <icon-plus />
        </template>
        {{ t('outsourcingConfig.settlement.addColumn') }}
      </a-button>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { computed, watch, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconPlus, IconDelete } from '@arco-design/web-vue/es/icon'
import type { SettlementConf } from '@/types/outsourcing-config'
import { DurationUnit, SettlementMethod, LeaveType, getEnumOptions } from '@/types/outsourcing-config'
import { cardTypeApi } from '@/api'
import { SchemaSubType } from '@/types/schema'
import type { FieldOption } from '@/types/field-option'

const { t } = useI18n()

const modelValue = defineModel<SettlementConf>({ required: true })

// 接收启用的请假类型
const props = defineProps<{
  enabledLeaveTypes: LeaveType[]
}>()

// 结算方式选项
const settlementMethodOptions = computed(() =>
  getEnumOptions(SettlementMethod, {
    [SettlementMethod.AUTO]: t('outsourcingConfig.enums.settlementMethod.AUTO'),
    [SettlementMethod.MANUAL]: t('outsourcingConfig.enums.settlementMethod.MANUAL')
  })
)

// 时间单位选项
const durationUnitOptions = computed(() =>
  getEnumOptions(DurationUnit, {
    [DurationUnit.HOUR]: t('outsourcingConfig.enums.durationUnit.HOUR'),
    [DurationUnit.MINUTE]: t('outsourcingConfig.enums.durationUnit.MINUTE')
  })
)

// 启用的请假类型选项（用于特殊请假类型多选框）
const enabledLeaveTypeOptions = computed(() => {
  if (!props.enabledLeaveTypes || props.enabledLeaveTypes.length === 0) {
    return []
  }
  return getEnumOptions(LeaveType, {
    [LeaveType.SICK_LEAVE]: t('outsourcingConfig.enums.leaveType.SICK_LEAVE'),
    [LeaveType.TIME_OFF]: t('outsourcingConfig.enums.leaveType.TIME_OFF'),
    [LeaveType.ANNUAL_LEAVE]: t('outsourcingConfig.enums.leaveType.ANNUAL_LEAVE'),
    [LeaveType.PERSONAL_LEAVE]: t('outsourcingConfig.enums.leaveType.PERSONAL_LEAVE')
  }).filter(option => props.enabledLeaveTypes.includes(option.value))
})

// 监听启用的请假类型变化，自动清理无效的特殊请假类型
watch(
  () => props.enabledLeaveTypes,
  (newEnabledTypes) => {
    if (!modelValue.value.specialLeaveItemIds) return

    if (!newEnabledTypes || newEnabledTypes.length === 0) {
      // 如果没有启用的请假类型，清空特殊请假类型
      modelValue.value.specialLeaveItemIds = []
      return
    }

    // 过滤掉未启用的请假类型
    modelValue.value.specialLeaveItemIds = modelValue.value.specialLeaveItemIds.filter(itemId =>
      newEnabledTypes.includes(itemId as LeaveType)
    )
  },
  { deep: true }
)

// 成员卡片类型选项
const memberCardTypeOptions = ref<{ id: string; name: string; code?: string }[]>([])
const loadingMemberCardTypes = ref(false)

// 字段选项
const availableFields = ref<FieldOption[]>([])
const loadingFields = ref(false)

// 单选的成员卡片类型（用于 UI 绑定）
const selectedCardTypeId = computed({
  get: () => modelValue.value.vutIds?.[0] || '',
  set: (value: string) => {
    if (value) {
      modelValue.value.vutIds = [value]
    } else {
      modelValue.value.vutIds = []
    }
  }
})

// 日期字段选项（用于离职日期字段）
const dateFieldOptions = computed(() => {
  return availableFields.value.filter(field => field.fieldType === 'DATE')
})

// 数字字段选项（用于个人服务费配置）
const numberFieldOptions = computed(() => {
  return availableFields.value.filter(field => field.fieldType === 'NUMBER')
})

// 加载成员卡片类型及其子类型
async function loadMemberCardTypes() {
  loadingMemberCardTypes.value = true
  try {
    const allCardTypes = await cardTypeApi.list()
    const memberAbstract = allCardTypes.find(
      (ct) => ct.code === 'member-trait'
    )

    if (!memberAbstract || !memberAbstract.id) {
      console.warn('member-trait card type not found')
      memberCardTypeOptions.value = allCardTypes
        .filter((ct) => ct.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE)
        .map((ct) => ({
          id: ct.id!,
          name: ct.name,
          code: ct.code
        }))
      return
    }

    const childCardTypes = await cardTypeApi.getByParent(memberAbstract.id)

    memberCardTypeOptions.value = [
      {
        id: memberAbstract.id,
        name: memberAbstract.name,
        code: memberAbstract.code
      },
      ...childCardTypes.map((ct) => ({
        id: ct.id!,
        name: ct.name,
        code: ct.code
      }))
    ]
  } catch (error) {
    console.error('Failed to load member card types:', error)
  } finally {
    loadingMemberCardTypes.value = false
  }
}

// 加载字段列表
async function fetchFields(cardTypeId: string) {
  loadingFields.value = true
  try {
    availableFields.value = await cardTypeApi.getFieldOptions(cardTypeId)
  } catch (error) {
    console.error('Failed to fetch fields:', error)
    availableFields.value = []
  } finally {
    loadingFields.value = false
  }
}

// 监听 vutIds 变化，加载卡片类型的字段
watch(
  () => modelValue.value.vutIds,
  (newVutIds) => {
    if (newVutIds && newVutIds.length > 0) {
      // 加载选中卡片类型的字段
      const firstVutId = newVutIds[0]
      if (firstVutId) {
        fetchFields(firstVutId)
      }
    } else {
      availableFields.value = []
      loadingFields.value = false
      // 清空字段选择
      if (modelValue.value.leaveDateFieldId) {
        modelValue.value.leaveDateFieldId = undefined
      }
      if (modelValue.value.personalServiceFeeConf) {
        modelValue.value.personalServiceFeeConf.baseFeeFieldId = undefined
        modelValue.value.personalServiceFeeConf.overtimeFeeFieldId = undefined
        modelValue.value.personalServiceFeeConf.subsidyFieldId = undefined
      }
    }
  },
  { deep: true }
)

/**
 * 添加分摊维度
 */
function addColumn() {
  if (!modelValue.value.projectServiceFeeConf) {
    modelValue.value.projectServiceFeeConf = { columns: [] }
  }
  if (!modelValue.value.projectServiceFeeConf.columns) {
    modelValue.value.projectServiceFeeConf.columns = []
  }
  modelValue.value.projectServiceFeeConf.columns.push({
    column: '',
    active: true
  })
}

/**
 * 删除分摊维度
 */
function removeColumn(index: number) {
  modelValue.value.projectServiceFeeConf?.columns?.splice(index, 1)
}

onMounted(() => {
  loadMemberCardTypes()
  // 如果已经有选中的卡片类型，加载字段
  if (modelValue.value.vutIds && modelValue.value.vutIds.length > 0) {
    const firstVutId = modelValue.value.vutIds[0]
    if (firstVutId) {
      fetchFields(firstVutId)
    }
  }
})
</script>

<style scoped lang="scss">
.settlement-config {
  max-width: 100%;

  .form-item-tip {
    font-size: 13px;
    color: var(--color-text-3);
    line-height: 1.5;

    &.warning {
      color: var(--color-warning-6);
    }
  }

  .column-list {
    margin-bottom: 16px;

    .column-item {
      padding: 16px;
      margin-bottom: 12px;
      background: var(--color-fill-2);
      border-radius: 4px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}
</style>
