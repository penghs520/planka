<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { attendanceApi } from '@/api'
import { Message } from '@arco-design/web-vue'
import type { TodayAttendance, AttendanceRecord } from '@/types/attendance'
import dayjs from 'dayjs'

const { t } = useI18n()
const orgStore = useOrgStore()

const loading = ref(false)
const clockingIn = ref(false)
const clockingOut = ref(false)
const todayAttendance = ref<TodayAttendance>({
  hasClockedIn: false,
  hasClockedOut: false,
})
const recentRecords = ref<AttendanceRecord[]>([])
const currentTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))

// 更新当前时间
setInterval(() => {
  currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
}, 1000)

// 计算属性
const statusText = computed(() => {
  if (todayAttendance.value.hasClockedOut) {
    return t('attendance.clockPage.hasClockedOut')
  }
  if (todayAttendance.value.hasClockedIn) {
    return t('attendance.clockPage.hasClockedIn')
  }
  return t('attendance.clockPage.notClockedIn')
})

const workHoursText = computed(() => {
  if (todayAttendance.value.workHours) {
    return `${todayAttendance.value.workHours} ${t('attendance.clockPage.hours')}`
  }
  return '-'
})

const canClockIn = computed(() => {
  return !todayAttendance.value.hasClockedIn
})

const canClockOut = computed(() => {
  return todayAttendance.value.hasClockedIn && !todayAttendance.value.hasClockedOut
})

// 加载今日考勤
async function loadTodayAttendance() {
  if (!orgStore.currentMemberCardId || !orgStore.currentOrgId) return

  loading.value = true
  try {
    todayAttendance.value = await attendanceApi.getTodayAttendance(
      orgStore.currentMemberCardId,
      orgStore.currentOrgId,
    )
  } catch (error) {
    console.error('Failed to load today attendance:', error)
  } finally {
    loading.value = false
  }
}

// 加载最近记录
async function loadRecentRecords() {
  if (!orgStore.currentMemberCardId || !orgStore.currentOrgId) return

  try {
    const startDate = dayjs().subtract(7, 'day').format('YYYY-MM-DD')
    const records = await attendanceApi.getRecords(
      orgStore.currentMemberCardId,
      orgStore.currentOrgId,
      startDate,
    )
    recentRecords.value = records.slice(0, 5)
  } catch (error) {
    console.error('Failed to load recent records:', error)
  }
}

// 签到
async function handleClockIn() {
  if (!canClockIn.value) {
    Message.warning(t('attendance.clockPage.alreadyClockedIn'))
    return
  }

  if (!orgStore.currentMemberCardId || !orgStore.currentOrgId) return

  clockingIn.value = true
  try {
    const response = await attendanceApi.clockIn(orgStore.currentMemberCardId, orgStore.currentOrgId)

    // 使用服务器返回的完整数据更新 UI
    todayAttendance.value.hasClockedIn = true
    todayAttendance.value.clockInTime = response.clockInTime
    todayAttendance.value.recordCardId = response.recordCardId

    Message.success(t('attendance.clockPage.clockInSuccess'))

    // 后台刷新最近记录
    await loadRecentRecords()
  } catch {
    Message.error(t('attendance.clockPage.clockInFailed'))
  } finally {
    clockingIn.value = false
  }
}

// 签退
async function handleClockOut() {
  if (!canClockOut.value) {
    if (!todayAttendance.value.hasClockedIn) {
      Message.warning(t('attendance.clockPage.pleaseClockInFirst'))
    } else {
      Message.warning(t('attendance.clockPage.alreadyClockedOut'))
    }
    return
  }

  if (!todayAttendance.value.recordCardId || !orgStore.currentOrgId || !orgStore.currentMemberCardId) return

  clockingOut.value = true
  try {
    const response = await attendanceApi.clockOut(
      todayAttendance.value.recordCardId,
      orgStore.currentMemberCardId,
      orgStore.currentOrgId
    )

    // 使用服务器返回的完整数据更新 UI
    todayAttendance.value.hasClockedOut = true
    todayAttendance.value.clockInTime = response.clockInTime
    todayAttendance.value.clockOutTime = response.clockOutTime
    todayAttendance.value.workHours = response.workDuration / 60 // 转换为小时

    Message.success(t('attendance.clockPage.clockOutSuccess'))

    // 后台刷新最近记录
    await loadRecentRecords()
  } catch {
    Message.error(t('attendance.clockPage.clockOutFailed'))
    // 失败时重新加载数据
    await loadTodayAttendance()
  } finally {
    clockingOut.value = false
  }
}

// 格式化状态
function formatStatus(status: string) {
  return t(`attendance.status.${status}`)
}

// 获取状态颜色
function getStatusColor(status: string) {
  const colorMap: Record<string, string> = {
    normal: 'green',
    late: 'orange',
    early: 'orange',
    absent: 'red',
    leave: 'blue',
    overtime: 'purple',
  }
  return colorMap[status] || 'gray'
}

// 初始化
onMounted(() => {
  loadTodayAttendance()
  loadRecentRecords()
})
</script>

<template>
  <div class="clock-page">
    <a-spin :loading="loading" class="clock-content">
      <!-- 当前时间卡片 -->
      <a-card class="time-card">
        <div class="time-display">
          <div class="current-time">{{ currentTime }}</div>
          <div class="today-status">
            <span class="status-label">{{ t('attendance.clockPage.todayStatus') }}:</span>
            <span class="status-value">{{ statusText }}</span>
          </div>
        </div>
      </a-card>

      <!-- 打卡按钮 -->
      <div class="clock-buttons">
        <a-button
          size="large"
          :loading="clockingIn"
          :disabled="!canClockIn"
          class="clock-button clock-in-button"
          @click="handleClockIn"
        >
          {{ t('attendance.clockPage.clockIn') }}
        </a-button>
        <a-button
          size="large"
          :loading="clockingOut"
          :disabled="!canClockOut"
          class="clock-button clock-out-button"
          @click="handleClockOut"
        >
          {{ t('attendance.clockPage.clockOut') }}
        </a-button>
      </div>

      <!-- 今日信息 -->
      <a-card v-if="todayAttendance.hasClockedIn" class="today-info">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item :label="t('attendance.field.clockInTime')">
            {{ todayAttendance.clockInTime || '-' }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('attendance.field.clockOutTime')">
            {{ todayAttendance.clockOutTime || '-' }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('attendance.clockPage.workHours')" :span="2">
            {{ workHoursText }}
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 最近记录 -->
      <a-card v-if="recentRecords.length > 0" class="recent-records">
        <template #title>{{ t('attendance.clockPage.recentRecords') }}</template>
        <a-table :data="recentRecords" :pagination="false" :bordered="false">
          <template #columns>
            <a-table-column :title="t('attendance.field.date')" data-index="date" />
            <a-table-column :title="t('attendance.field.clockInTime')" data-index="clockInTime" />
            <a-table-column :title="t('attendance.field.clockOutTime')" data-index="clockOutTime" />
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
          </template>
        </a-table>
      </a-card>
    </a-spin>
  </div>
</template>

<style scoped>
.clock-page {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.clock-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.time-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 100%);
  border: 1px solid var(--color-border-2);
}

.time-card :deep(.arco-card-body) {
  padding: 0;
}

.time-display {
  text-align: center;
  padding: 48px 32px;
}

.current-time {
  font-size: 56px;
  font-weight: 600;
  margin-bottom: 20px;
  letter-spacing: 2px;
  color: var(--color-text-1);
}

.today-status {
  font-size: 20px;
  color: var(--color-text-2);
}

.status-label {
  margin-right: 8px;
}

.status-value {
  font-weight: 600;
  padding: 4px 16px;
  background: var(--color-fill-2);
  border-radius: 20px;
  display: inline-block;
  color: var(--color-text-1);
}

.clock-buttons {
  display: flex;
  gap: 20px;
  justify-content: center;
}

.clock-button {
  min-width: 220px;
  height: 64px;
  font-size: 20px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.clock-in-button {
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  color: white;
}

.clock-in-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(17, 153, 142, 0.4);
}

.clock-in-button:disabled {
  background: #e5e6eb;
  color: #86909c;
  cursor: not-allowed;
}

.clock-out-button {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.clock-out-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(245, 87, 108, 0.4);
}

.clock-out-button:disabled {
  background: #e5e6eb;
  color: #86909c;
  cursor: not-allowed;
}

.today-info,
.recent-records {
  margin-top: 0;
}

.today-info {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.recent-records {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>




