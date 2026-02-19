<template>
  <div class="attendance-config">
    <a-form :model="modelValue" layout="vertical" auto-label-width>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendance.workStart')"
            field="workStart"
          >
            <a-time-picker
              v-model="modelValue.workStart"
              :placeholder="t('outsourcingConfig.attendance.workStartPlaceholder')"
              format="HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendance.workEnd')"
            field="workEnd"
          >
            <a-time-picker
              v-model="modelValue.workEnd"
              :placeholder="t('outsourcingConfig.attendance.workEndPlaceholder')"
              format="HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendance.lunchStart')"
            field="lunchStart"
          >
            <a-time-picker
              v-model="modelValue.lunchStart"
              :placeholder="t('outsourcingConfig.attendance.lunchStartPlaceholder')"
              format="HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendance.lunchEnd')"
            field="lunchEnd"
          >
            <a-time-picker
              v-model="modelValue.lunchEnd"
              :placeholder="t('outsourcingConfig.attendance.lunchEndPlaceholder')"
              format="HH:mm"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendance.workDuration')"
            field="workDuration"
          >
            <a-input-number
              v-model="modelValue.workDuration"
              :placeholder="t('outsourcingConfig.attendance.workDurationPlaceholder')"
              :min="0"
              :precision="1"
              style="width: 100%"
            >
              <template #suffix>
                {{ t('outsourcingConfig.attendance.workDurationUnit') }}
              </template>
            </a-input-number>
          </a-form-item>
        </a-col>
      </a-row>

      <a-divider />

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendance.impactWm')"
            field="impactWm"
          >
            <a-switch v-model="modelValue.impactWm" />
            <span class="form-item-tip">
              {{ t('outsourcingConfig.attendance.impactWmTip') }}
            </span>
          </a-form-item>
        </a-col>

        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendance.accumulatedOvertime')"
            field="accumulatedOvertime"
          >
            <a-switch v-model="modelValue.accumulatedOvertime" />
            <span class="form-item-tip">
              {{ t('outsourcingConfig.attendance.accumulatedOvertimeTip') }}
            </span>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item
            :label="t('outsourcingConfig.attendance.absenceWhenNoSignInOrOut')"
            field="absenceWhenNoSignInOrOut"
          >
            <a-switch v-model="modelValue.absenceWhenNoSignInOrOut" />
            <span class="form-item-tip">
              {{ t('outsourcingConfig.attendance.absenceWhenNoSignInOrOutTip') }}
            </span>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { AttendanceConf } from '@/types/outsourcing-config'

const { t } = useI18n()

const modelValue = defineModel<AttendanceConf>({ required: true })
</script>

<style scoped lang="scss">
.attendance-config {
  max-width: 100%;

  .form-item-tip {
    margin-left: 8px;
    font-size: 12px;
    color: var(--color-text-3);
  }
}
</style>
