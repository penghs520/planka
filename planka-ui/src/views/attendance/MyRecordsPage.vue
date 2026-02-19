<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { attendanceApi } from '@/api'
import type { AttendanceRecord, AttendanceStats } from '@/types/attendance'
import dayjs from 'dayjs'

const { t } = useI18n()
const router = useRouter()
const orgStore = useOrgStore()

const loading = ref(false)
const selectedMonth = ref(dayjs().format('YYYY-MM'))
const records = ref<AttendanceRecord[]>([])

// 计算统计数据
const stats = computed<AttendanceStats>(() => {
  const result: AttendanceStats = {
    attendanceDays: 0,
    lateDays: 0,
    earlyLeaveDays: 0,
    absentDays: 0,
    leaveDays: 0,
    overtimeHours: 0,
    totalWorkHours: 0,
  }

  records.value.forEach((record) => {
    if (record.status === 'NORMAL') result.attendanceDays++
    if (record.status === 'LATE') result.lateDays++
    if (record.status === 'EARLY_LEAVE') result.earlyLeaveDays++
    if (record.status === 'ABSENT') result.absentDays++
    if (record.status === 'LEAVE') result.leaveDays++
    if (record.workHours) result.totalWorkHours += record.workHours
  })

  return result
})

// 加载考勤记录
async function loadRecords() {
  if (!orgStore.currentMemberCardId || !orgStore.currentOrgId) return

  loading.value = true
  try {
    const startDate = dayjs(selectedMonth.value).startOf('month').format('YYYY-MM-DD')
    const endDate = dayjs(selectedMonth.value).endOf('month').format('YYYY-MM-DD')

    records.value = await attendanceApi.getRecords(
      orgStore.currentMemberCardId,
      orgStore.currentOrgId,
      startDate,
      endDate,
    )
  } catch (error) {
    console.error('Failed to load records:', error)
  } finally {
    loading.value = false
  }
}

// 格式化状态
function formatStatus(status: string) {
  return t(`attendance.status.${status}`)
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    NORMAL: 'green',
    LATE: 'orange',
    EARLY_LEAVE: 'orange',
    ABSENT: 'red',
    LEAVE: 'blue',
    OVERTIME: 'purple',
  }
  return colorMap[status] || 'gray'
}

// 月份变化
function handleMonthChange(value: string) {
  selectedMonth.value = value
  loadRecords()
}

// 点击行跳转到考勤记录详情
function handleRowClick(record: AttendanceRecord) {
  if (record.cardId) {
    router.push({ name: 'CardDetail', params: { cardId: record.cardId } })
  }
}

// 初始化
onMounted(() => {
  loadRecords()
})
</script>

<template>
  <div class="my-records-page">
    <!-- 统计卡片 -->
    <a-card class="stats-card">
      <template #title>{{ t('attendance.myRecordsPage.stats') }}</template>
      <a-row :gutter="16">
        <a-col :span="6">
          <a-statistic
            :title="t('attendance.myRecordsPage.attendanceDays')"
            :value="stats.attendanceDays"
            :value-style="{ color: 'var(--color-success)' }"
          >
            <template #suffix>{{ t('attendance.myRecordsPage.days') }}</template>
          </a-statistic>
        </a-col>
        <a-col :span="6">
          <a-statistic
            :title="t('attendance.myRecordsPage.lateDays')"
            :value="stats.lateDays"
            :value-style="{ color: 'var(--color-warning)' }"
          >
            <template #suffix>{{ t('attendance.myRecordsPage.times') }}</template>
          </a-statistic>
        </a-col>
        <a-col :span="6">
          <a-statistic
            :title="t('attendance.myRecordsPage.leaveDays')"
            :value="stats.leaveDays"
            :value-style="{ color: 'var(--color-primary)' }"
          >
            <template #suffix>{{ t('attendance.myRecordsPage.days') }}</template>
          </a-statistic>
        </a-col>
        <a-col :span="6">
          <a-statistic
            :title="t('attendance.myRecordsPage.totalWorkHours')"
            :value="stats.totalWorkHours"
            :precision="1"
          >
            <template #suffix>{{ t('attendance.clockPage.hours') }}</template>
          </a-statistic>
        </a-col>
      </a-row>
    </a-card>

    <!-- 记录列表 -->
    <a-card class="records-card">
      <template #title>
        <div class="records-header">
          <span>{{ t('attendance.myRecordsPage.title') }}</span>
          <a-month-picker
            v-model="selectedMonth"
            :placeholder="t('attendance.myRecordsPage.selectMonth')"
            @change="handleMonthChange"
          />
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table
          :data="records"
          :pagination="{ pageSize: 20 }"
          :bordered="false"
          :hoverable="true"
          class="clickable-table"
          @row-click="handleRowClick"
        >
          <template #columns>
            <a-table-column :title="t('attendance.field.date')" data-index="date" />
            <a-table-column :title="t('attendance.field.clockInTime')" data-index="clockInTime">
              <template #cell="{ record }">
                {{ record.clockInTime || '-' }}
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.clockOutTime')" data-index="clockOutTime">
              <template #cell="{ record }">
                {{ record.clockOutTime || '-' }}
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.workHours')">
              <template #cell="{ record }">
                {{ record.workHours ? `${record.workHours} ${t('attendance.clockPage.hours')}` : '-' }}
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.status')">
              <template #cell="{ record }">
                <a-tag :color="getStatusColor(record.status)">
                  {{ formatStatus(record.status) }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.remark')" data-index="remark">
              <template #cell="{ record }">
                {{ record.remark || '-' }}
              </template>
            </a-table-column>
          </template>
        </a-table>

        <a-empty v-if="!loading && records.length === 0" :description="t('attendance.common.noData')" />
      </a-spin>
    </a-card>
  </div>
</template>

<style scoped>
.my-records-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

/* 表格样式优化 */
.records-card :deep(.arco-card-header) {
  padding: 16px 20px;
}

.records-card :deep(.arco-card-body) {
  padding: 0;
}

.records-card :deep(.arco-table) {
  margin: 0;
}

.records-card :deep(.arco-table-container) {
  border-radius: 0;
}

.records-card :deep(.arco-table-th),
.records-card :deep(.arco-table-td) {
  padding: 12px 20px;
}

.records-card :deep(.arco-table-th:first-child),
.records-card :deep(.arco-table-td:first-child) {
  padding-left: 20px;
}

.records-card :deep(.arco-table-th:last-child),
.records-card :deep(.arco-table-td:last-child) {
  padding-right: 20px;
}

/* 表格行可点击样式 */
.clickable-table :deep(.arco-table-tr) {
  cursor: pointer;
  transition: background-color 0.2s;
}

.clickable-table :deep(.arco-table-tr:hover) {
  background-color: var(--color-fill-2);
}
</style>


