<template>
  <div class="attendance-change-config">
    <a-form :model="modelValue" layout="vertical" auto-label-width>
      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.attendanceChange.count')"
            field="count"
          >
            <a-input-number
              v-model="modelValue.count"
              :placeholder="t('outsourcingConfig.attendanceChange.countPlaceholder')"
              :min="0"
              :precision="0"
              style="width: 100%"
            >
              <template #suffix>
                {{ t('outsourcingConfig.attendanceChange.countUnit') }}
              </template>
            </a-input-number>
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.attendanceChange.window')"
            field="window"
          >
            <a-input-number
              v-model="modelValue.window"
              :placeholder="t('outsourcingConfig.attendanceChange.windowPlaceholder')"
              :min="0"
              :precision="0"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>

        <a-col :span="8">
          <a-form-item
            :label="t('outsourcingConfig.attendanceChange.windowUnit')"
            field="windowUnit"
          >
            <a-select
              v-model="modelValue.windowUnit"
              :placeholder="t('outsourcingConfig.attendanceChange.windowUnitPlaceholder')"
            >
              <a-option
                v-for="option in windowUnitOptions"
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
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendanceChange.allowWeekendOrHoliday')"
            field="allowWeekendOrHoliday"
          >
            <a-switch v-model="modelValue.allowWeekendOrHoliday" />
            <span class="form-item-tip">
              {{ t('outsourcingConfig.attendanceChange.allowWeekendOrHolidayTip') }}
            </span>
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider>{{ t('outsourcingConfig.attendanceChange.signIn') }}</a-divider>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendanceChange.timeStart')"
            field="signIn.start"
          >
            <a-time-picker
              v-model="modelValue.signIn!.start"
              :placeholder="t('outsourcingConfig.attendanceChange.timeStartPlaceholder')"
              format="HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendanceChange.timeEnd')"
            field="signIn.end"
          >
            <a-time-picker
              v-model="modelValue.signIn!.end"
              :placeholder="t('outsourcingConfig.attendanceChange.timeEndPlaceholder')"
              format="HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider>{{ t('outsourcingConfig.attendanceChange.signOut') }}</a-divider>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendanceChange.timeStart')"
            field="signOut.start"
          >
            <a-time-picker
              v-model="modelValue.signOut!.start"
              :placeholder="t('outsourcingConfig.attendanceChange.timeStartPlaceholder')"
              format="HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendanceChange.timeEnd')"
            field="signOut.end"
          >
            <a-time-picker
              v-model="modelValue.signOut!.end"
              :placeholder="t('outsourcingConfig.attendanceChange.timeEndPlaceholder')"
              format="HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { AttendanceChangeConf } from '@/types/outsourcing-config'
import { WindowUnit, getEnumOptions } from '@/types/outsourcing-config'

const { t } = useI18n()

const modelValue = defineModel<AttendanceChangeConf>({ required: true })

// 窗口单位选项
const windowUnitOptions = computed(() =>
  getEnumOptions(WindowUnit, {
    [WindowUnit.CALENDAR_DAY]: t('outsourcingConfig.enums.windowUnit.CALENDAR_DAY'),
    [WindowUnit.BUSINESS_DAY]: t('outsourcingConfig.enums.windowUnit.BUSINESS_DAY')
  })
)
</script>

<style scoped lang="scss">
.attendance-change-config {
  max-width: 100%;

  .form-item-tip {
    margin-left: 8px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}
</style>
