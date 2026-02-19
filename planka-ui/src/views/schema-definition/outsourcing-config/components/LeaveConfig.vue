<template>
  <div class="leave-config">
    <a-form :model="modelValue" layout="vertical" auto-label-width>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.leave.leaveUnit')"
            field="leaveUnit"
          >
            <a-select
              v-model="modelValue.leaveUnit"
              :placeholder="t('outsourcingConfig.leave.leaveUnitPlaceholder')"
            >
              <a-option
                v-for="option in leaveUnitOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </a-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 启用的请假类型 -->
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item
            :label="t('outsourcingConfig.leave.enabledLeaveTypes')"
            field="enabledLeaveTypes"
          >
            <a-checkbox-group v-model="modelValue.enabledLeaveTypes">
              <a-checkbox
                v-for="option in leaveTypeOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </a-checkbox>
            </a-checkbox-group>
            <template #extra>
              <a-alert type="info" :show-icon="false" style="margin-top: 8px">
                <template #icon>
                  <icon-info-circle />
                </template>
                {{ t('outsourcingConfig.leave.enabledLeaveTypesTooltip') }}
              </a-alert>
            </template>
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider>{{ t('outsourcingConfig.leave.limitRules') }}</a-divider>

      <!-- 未启用请假类型的提示 -->
      <a-alert
        v-if="!modelValue.enabledLeaveTypes || modelValue.enabledLeaveTypes.length === 0"
        type="warning"
        style="margin-bottom: 16px"
      >
        请先选择启用的请假类型，才能配置请假限制规则
      </a-alert>

      <!-- 请假限制规则列表 -->
      <div v-if="modelValue.limitRules && modelValue.limitRules.length > 0" class="rule-list">
        <div
          v-for="(rule, index) in modelValue.limitRules"
          :key="index"
          class="rule-item"
        >
          <a-row :gutter="16">
            <a-col :span="7">
              <a-form-item
                :label="t('outsourcingConfig.leave.range')"
                :field="`limitRules.${index}.range`"
              >
                <a-select
                  v-model="rule.range"
                  :placeholder="t('outsourcingConfig.leave.rangePlaceholder')"
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

            <a-col :span="7">
              <a-form-item
                :label="t('outsourcingConfig.leave.leaveType')"
                :field="`limitRules.${index}.leaveType`"
              >
                <a-select
                  v-model="rule.leaveType"
                  :placeholder="t('outsourcingConfig.leave.leaveTypePlaceholder')"
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

            <a-col :span="7">
              <a-form-item
                :label="t('outsourcingConfig.leave.limit')"
                :field="`limitRules.${index}.limit`"
              >
                <a-input-number
                  v-model="rule.limit"
                  :placeholder="t('outsourcingConfig.leave.limitPlaceholder')"
                  :min="0"
                  :precision="0"
                  style="width: 100%"
                >
                  <template #suffix>
                    {{ t('outsourcingConfig.leave.limitUnit') }}
                  </template>
                </a-input-number>
              </a-form-item>
            </a-col>

            <a-col :span="3">
              <a-form-item label=" ">
                <a-button type="text" status="danger" @click="removeRule(index)">
                  <template #icon>
                    <icon-delete />
                  </template>
                  {{ t('outsourcingConfig.actions.delete') }}
                </a-button>
              </a-form-item>
            </a-col>
          </a-row>
        </div>
      </div>

      <a-empty v-else :description="t('outsourcingConfig.leave.limitRulesEmpty')" />

      <a-button
        type="dashed"
        long
        :disabled="!modelValue.enabledLeaveTypes || modelValue.enabledLeaveTypes.length === 0"
        @click="addRule"
      >
        <template #icon>
          <icon-plus />
        </template>
        {{ t('outsourcingConfig.leave.addLimitRule') }}
      </a-button>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconPlus, IconDelete, IconInfoCircle } from '@arco-design/web-vue/es/icon'
import type { LeaveConf } from '@/types/outsourcing-config'
import { LeaveUnit, LeaveType, DateUnit, getEnumOptions } from '@/types/outsourcing-config'

const { t } = useI18n()

const modelValue = defineModel<LeaveConf>({ required: true })

// 请假单位选项
const leaveUnitOptions = computed(() =>
  getEnumOptions(LeaveUnit, {
    [LeaveUnit.HALF_DAY]: t('outsourcingConfig.enums.leaveUnit.HALF_DAY'),
    [LeaveUnit.DAY]: t('outsourcingConfig.enums.leaveUnit.DAY')
  })
)

// 请假类型选项（所有类型）
const leaveTypeOptions = computed(() =>
  getEnumOptions(LeaveType, {
    [LeaveType.SICK_LEAVE]: t('outsourcingConfig.enums.leaveType.SICK_LEAVE'),
    [LeaveType.TIME_OFF]: t('outsourcingConfig.enums.leaveType.TIME_OFF'),
    [LeaveType.ANNUAL_LEAVE]: t('outsourcingConfig.enums.leaveType.ANNUAL_LEAVE'),
    [LeaveType.PERSONAL_LEAVE]: t('outsourcingConfig.enums.leaveType.PERSONAL_LEAVE')
  })
)

// 已启用的请假类型选项（用于规则下拉框）
const enabledLeaveTypeOptions = computed(() => {
  const enabledTypes = modelValue.value.enabledLeaveTypes || []
  if (enabledTypes.length === 0) {
    return []
  }
  return leaveTypeOptions.value.filter(option => enabledTypes.includes(option.value))
})

// 日期单位选项
const dateUnitOptions = computed(() =>
  getEnumOptions(DateUnit, {
    [DateUnit.DAY]: t('outsourcingConfig.enums.dateUnit.DAY'),
    [DateUnit.WEEK]: t('outsourcingConfig.enums.dateUnit.WEEK'),
    [DateUnit.MONTH]: t('outsourcingConfig.enums.dateUnit.MONTH')
  })
)

// 监听启用的请假类型变化，自动清理无效规则
watch(
  () => modelValue.value.enabledLeaveTypes,
  (newEnabledTypes) => {
    if (!newEnabledTypes || newEnabledTypes.length === 0) {
      // 如果没有启用的请假类型，清空所有规则
      modelValue.value.limitRules = []
      return
    }

    if (!modelValue.value.limitRules) return

    // 过滤掉未启用类型的规则
    modelValue.value.limitRules = modelValue.value.limitRules.filter(rule =>
      newEnabledTypes.includes(rule.leaveType)
    )
  },
  { deep: true }
)

/**
 * 添加规则
 */
function addRule() {
  if (!modelValue.value.limitRules) {
    modelValue.value.limitRules = []
  }

  // 默认使用第一个启用的请假类型
  const defaultLeaveType = modelValue.value.enabledLeaveTypes?.[0] || LeaveType.SICK_LEAVE

  modelValue.value.limitRules.push({
    range: DateUnit.MONTH,
    leaveType: defaultLeaveType,
    limit: 0
  })
}

/**
 * 删除规则
 */
function removeRule(index: number) {
  modelValue.value.limitRules?.splice(index, 1)
}
</script>

<style scoped lang="scss">
.leave-config {
  max-width: 100%;

  .form-item-tip {
    font-size: 13px;
    color: var(--color-text-3);
    line-height: 1.5;
  }

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
