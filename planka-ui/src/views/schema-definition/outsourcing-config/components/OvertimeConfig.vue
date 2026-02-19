<template>
  <div class="overtime-config">
    <a-form :model="modelValue" layout="vertical" auto-label-width>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.overtime.startDuration')"
            field="startDuration"
          >
            <a-input-number
              v-model="modelValue.startDuration"
              :placeholder="t('outsourcingConfig.overtime.startDurationPlaceholder')"
              :min="0"
              :precision="0"
              style="width: 100%"
            >
              <template #suffix>
                {{ t('outsourcingConfig.overtime.startDurationUnit') }}
              </template>
            </a-input-number>
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.overtime.minDuration')"
            field="minDuration"
          >
            <a-input-number
              v-model="modelValue.minDuration"
              :placeholder="t('outsourcingConfig.overtime.minDurationPlaceholder')"
              :min="0"
              :precision="0"
              style="width: 100%"
            >
              <template #suffix>
                {{ t('outsourcingConfig.overtime.minDurationUnit') }}
              </template>
            </a-input-number>
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider>{{ t('outsourcingConfig.overtime.limitRules') }}</a-divider>

      <!-- 加班时长限制列表 -->
      <div v-if="modelValue.limitRules && modelValue.limitRules.length > 0" class="rule-list">
        <div
          v-for="(rule, index) in modelValue.limitRules"
          :key="index"
          class="rule-item"
        >
          <a-row :gutter="16">
            <a-col :span="10">
              <a-form-item
                :label="t('outsourcingConfig.overtime.range')"
                :field="`limitRules.${index}.range`"
              >
                <a-select
                  v-model="rule.range"
                  :placeholder="t('outsourcingConfig.overtime.rangePlaceholder')"
                >
                  <a-option
                    v-for="option in dateUnitOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </a-option>
                </a-select>
              </a-form-item>
            </a-col>

            <a-col :span="10">
              <a-form-item
                :label="t('outsourcingConfig.overtime.limit')"
                :field="`limitRules.${index}.limit`"
              >
                <a-input-number
                  v-model="rule.limit"
                  :placeholder="t('outsourcingConfig.overtime.limitPlaceholder')"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                >
                  <template #suffix>
                    {{ t('outsourcingConfig.overtime.limitUnit') }}
                  </template>
                </a-input-number>
              </a-form-item>
            </a-col>

            <a-col :span="4">
              <a-form-item label=" ">
                <a-button type="text" status="danger" @click="removeLimitRule(index)">
                  <template #icon>
                    <icon-delete />
                  </template>
                </a-button>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
      </div>

      <a-empty v-else :description="t('outsourcingConfig.overtime.limitRulesEmpty')" />

      <a-button type="dashed" long style="margin-bottom: 24px" @click="addLimitRule">
        <template #icon>
          <icon-plus />
        </template>
        {{ t('outsourcingConfig.overtime.addLimitRule') }}
      </a-button>

      <a-divider>{{ t('outsourcingConfig.overtime.nonWorkOvertime') }}</a-divider>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.overtime.nonWorkOvertimeLimit')"
            field="nonWorkOvertime.limit"
          >
            <a-input-number
              v-model="modelValue.nonWorkOvertime!.limit"
              :placeholder="t('outsourcingConfig.overtime.nonWorkOvertimeLimitPlaceholder')"
              :min="0"
              :precision="0"
              style="width: 100%"
            >
              <template #suffix>
                {{ t('outsourcingConfig.overtime.nonWorkOvertimeLimitUnit') }}
              </template>
            </a-input-number>
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider>{{ t('outsourcingConfig.overtime.calRule') }}</a-divider>

      <!-- 未启用请假类型的提示 -->
      <a-alert
        v-if="!enabledLeaveTypes || enabledLeaveTypes.length === 0"
        type="warning"
        style="margin-bottom: 16px"
      >
        请先在"请假配置"中选择启用的请假类型，才能配置加班换算规则
      </a-alert>

      <!-- 加班换算规则列表 -->
      <div
        v-if="modelValue.calRule?.rules && modelValue.calRule.rules.length > 0"
        class="rule-list"
      >
        <div
          v-for="(rule, index) in modelValue.calRule.rules"
          :key="index"
          class="rule-item"
        >
          <a-row :gutter="16">
            <a-col :span="7">
              <a-form-item
                :label="t('outsourcingConfig.overtime.type')"
                :field="`calRule.rules.${index}.type`"
              >
                <a-select
                  v-model="rule.type"
                  :placeholder="t('outsourcingConfig.overtime.typePlaceholder')"
                >
                  <a-option
                    v-for="option in overtimeTypeOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </a-option>
                </a-select>
              </a-form-item>
            </a-col>

            <a-col :span="7">
              <a-form-item
                :label="t('outsourcingConfig.overtime.ratio')"
                :field="`calRule.rules.${index}.ratio`"
              >
                <a-input-number
                  v-model="rule.ratio"
                  :placeholder="t('outsourcingConfig.overtime.ratioPlaceholder')"
                  :min="0"
                  :precision="1"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>

            <a-col :span="7">
              <a-form-item
                :label="t('outsourcingConfig.overtime.leaveItemId')"
                :field="`calRule.rules.${index}.leaveItemId`"
              >
                <a-select
                  v-model="rule.leaveItemId"
                  :placeholder="t('outsourcingConfig.overtime.leaveItemIdPlaceholder')"
                >
                  <a-option
                    v-for="option in enabledLeaveTypeOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </a-option>
                </a-select>
              </a-form-item>
            </a-col>

            <a-col :span="3">
              <a-form-item label=" ">
                <a-button type="text" status="danger" @click="removeCalRule(index)">
                  <template #icon>
                    <icon-delete />
                  </template>
                </a-button>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
      </div>

      <a-empty v-else :description="t('outsourcingConfig.overtime.calRuleEmpty')" />

      <a-button
        type="dashed"
        long
        :disabled="!enabledLeaveTypes || enabledLeaveTypes.length === 0"
        @click="addCalRule"
      >
        <template #icon>
          <icon-plus />
        </template>
        {{ t('outsourcingConfig.overtime.addCalRule') }}
      </a-button>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconPlus, IconDelete } from '@arco-design/web-vue/es/icon'
import type { OvertimeConf } from '@/types/outsourcing-config'
import { DateUnit, OvertimeType, LeaveType, getEnumOptions } from '@/types/outsourcing-config'

const { t } = useI18n()

const modelValue = defineModel<OvertimeConf>({ required: true })

// 接收启用的请假类型
const props = defineProps<{
  enabledLeaveTypes: LeaveType[]
}>()

// 日期单位选项
const dateUnitOptions = computed(() =>
  getEnumOptions(DateUnit, {
    [DateUnit.DAY]: t('outsourcingConfig.enums.dateUnit.DAY'),
    [DateUnit.WEEK]: t('outsourcingConfig.enums.dateUnit.WEEK'),
    [DateUnit.MONTH]: t('outsourcingConfig.enums.dateUnit.MONTH')
  })
)

// 加班类型选项
const overtimeTypeOptions = computed(() =>
  getEnumOptions(OvertimeType, {
    [OvertimeType.WORKDAY]: t('outsourcingConfig.enums.overtimeType.WORKDAY'),
    [OvertimeType.WEEKEND]: t('outsourcingConfig.enums.overtimeType.WEEKEND'),
    [OvertimeType.HOLIDAY]: t('outsourcingConfig.enums.overtimeType.HOLIDAY')
  })
)

// 启用的请假类型选项（用于换算规则的关联假期下拉框）
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

// 监听启用的请假类型变化，自动清理无效的换算规则
watch(
  () => props.enabledLeaveTypes,
  (newEnabledTypes) => {
    if (!modelValue.value.calRule?.rules) return

    if (!newEnabledTypes || newEnabledTypes.length === 0) {
      // 如果没有启用的请假类型，清空所有换算规则
      modelValue.value.calRule.rules = []
      return
    }

    // 过滤掉未启用类型的换算规则
    modelValue.value.calRule.rules = modelValue.value.calRule.rules.filter(rule =>
      newEnabledTypes.includes(rule.leaveItemId as LeaveType)
    )
  },
  { deep: true }
)

/**
 * 添加时长限制规则
 */
function addLimitRule() {
  if (!modelValue.value.limitRules) {
    modelValue.value.limitRules = []
  }
  modelValue.value.limitRules.push({
    range: DateUnit.DAY,
    limit: 0
  })
}

/**
 * 删除时长限制规则
 */
function removeLimitRule(index: number) {
  modelValue.value.limitRules?.splice(index, 1)
}

/**
 * 添加换算规则
 */
function addCalRule() {
  if (!modelValue.value.calRule) {
    modelValue.value.calRule = { rules: [] }
  }
  if (!modelValue.value.calRule.rules) {
    modelValue.value.calRule.rules = []
  }

  // 默认使用第一个启用的请假类型
  const defaultLeaveType = props.enabledLeaveTypes?.[0] || LeaveType.SICK_LEAVE

  modelValue.value.calRule.rules.push({
    type: OvertimeType.WORKDAY,
    ratio: 1.0,
    leaveItemId: defaultLeaveType
  })
}

/**
 * 删除换算规则
 */
function removeCalRule(index: number) {
  modelValue.value.calRule?.rules?.splice(index, 1)
}
</script>

<style scoped lang="scss">
.overtime-config {
  max-width: 100%;

  .rule-list {
    margin-bottom: 16px;

    .rule-item {
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
