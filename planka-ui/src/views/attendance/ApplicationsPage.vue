<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { attendanceApi } from '@/api'
import { Message } from '@arco-design/web-vue'
import type { ApplicationRecord, ApprovalStatus } from '@/types/attendance'
import CardCreateModal from '@/views/workspace/components/shared/CardCreateModal.vue'

const { t } = useI18n()
const orgStore = useOrgStore()

const loading = ref(false)
const showCreateModal = ref(false)
const selectedCardTypeId = ref('')
const filterStatus = ref<ApprovalStatus | 'ALL'>('ALL')
const applications = ref<ApplicationRecord[]>([])

// 卡片类型ID
const cardTypeIds = computed(() => {
  if (!orgStore.currentOrgId) return {}
  return {
    leave: `${orgStore.currentOrgId}:leave-application`,
    overtime: `${orgStore.currentOrgId}:overtime-application`,
    makeup: `${orgStore.currentOrgId}:makeup-application`,
  }
})

// 过滤后的申请列表
const filteredApplications = computed(() => {
  if (filterStatus.value === 'ALL') {
    return applications.value
  }
  return applications.value.filter((app) => app.approvalStatus === filterStatus.value)
})

// 加载申请列表
async function loadApplications() {
  if (!orgStore.currentMemberCardId || !orgStore.currentOrgId) return

  loading.value = true
  try {
    // 加载所有类型的申请（请假、加班、补卡）
    applications.value = await attendanceApi.getApplications(
      orgStore.currentMemberCardId,
      orgStore.currentOrgId,
    )
  } catch (error) {
    console.error('Failed to load applications:', error)
  } finally {
    loading.value = false
  }
}

// 打开申请弹窗
function openApplicationModal(type: 'leave' | 'overtime' | 'makeup') {
  const typeId = cardTypeIds.value[type]
  if (!typeId) {
    Message.error(t('common.error.orgNotSelected'))
    return
  }
  selectedCardTypeId.value = typeId
  showCreateModal.value = true
}

// 申请成功
function handleApplicationSuccess() {
  showCreateModal.value = false
  Message.success(t('attendance.applicationsPage.applicationSuccess'))
  loadApplications()
}

// 格式化状态
function formatStatus(status: ApprovalStatus) {
  return t(`attendance.approvalStatus.${status}`)
}

// 获取状态颜色
function getStatusColor(status: ApprovalStatus) {
  const colorMap: Record<ApprovalStatus, string> = {
    PENDING: 'orange',
    APPROVED: 'green',
    REJECTED: 'red',
  }
  return colorMap[status] || 'gray'
}

// 初始化
onMounted(() => {
  loadApplications()
})
</script>

<template>
  <div class="applications-page">
    <!-- 快捷申请按钮 -->
    <a-card class="quick-actions">
      <template #title>{{ t('attendance.applicationsPage.newApplication') }}</template>
      <div class="action-buttons">
        <a-button type="primary" size="large" @click="openApplicationModal('leave')">
          <template #icon>
            <icon-calendar />
          </template>
          {{ t('attendance.applicationsPage.leaveApplication') }}
        </a-button>
        <a-button type="primary" size="large" @click="openApplicationModal('overtime')">
          <template #icon>
            <icon-clock-circle />
          </template>
          {{ t('attendance.applicationsPage.overtimeApplication') }}
        </a-button>
        <a-button type="primary" size="large" @click="openApplicationModal('makeup')">
          <template #icon>
            <icon-edit />
          </template>
          {{ t('attendance.applicationsPage.makeupApplication') }}
        </a-button>
      </div>
    </a-card>

    <!-- 申请列表 -->
    <a-card class="applications-list">
      <template #title>
        <div class="list-header">
          <span>{{ t('attendance.applicationsPage.myApplications') }}</span>
          <a-select
            v-model="filterStatus"
            :placeholder="t('attendance.applicationsPage.filterByStatus')"
            style="width: 200px"
          >
            <a-option value="ALL">{{ t('attendance.applicationsPage.allStatus') }}</a-option>
            <a-option value="PENDING">{{ t('attendance.approvalStatus.PENDING') }}</a-option>
            <a-option value="APPROVED">{{ t('attendance.approvalStatus.APPROVED') }}</a-option>
            <a-option value="REJECTED">{{ t('attendance.approvalStatus.REJECTED') }}</a-option>
          </a-select>
        </div>
      </template>

      <a-spin :loading="loading">
        <a-table :data="filteredApplications" :pagination="{ pageSize: 20 }" :bordered="false">
          <template #columns>
            <a-table-column :title="t('attendance.applicationType.type')" data-index="type" :width="100">
              <template #cell="{ record }">
                <a-tag :color="record.type === 'LEAVE' ? 'blue' : record.type === 'OVERTIME' ? 'orange' : 'purple'">
                  {{ t(`attendance.applicationType.${record.type}`) }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.leaveType')" data-index="leaveType" :width="100">
              <template #cell="{ record }">
                <a-tag v-if="record.type === 'LEAVE' && record.leaveType" color="cyan">{{ record.leaveType }}</a-tag>
                <span v-else>-</span>
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.title')" data-index="title">
              <template #cell="{ record }">
                <span class="font-medium">{{ record.title || '-' }}</span>
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.description')" data-index="description">
              <template #cell="{ record }">
                <span class="text-gray-500 truncate max-w-[200px] block" :title="record.description">
                  {{ record.description || '-' }}
                </span>
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.applyTime')" data-index="applyTime" />
            <a-table-column :title="t('attendance.field.startTime')" data-index="startTime" />
            <a-table-column :title="t('attendance.field.endTime')" data-index="endTime" />
            <a-table-column :title="t('attendance.field.duration')">
              <template #cell="{ record }">
                {{ record.duration ? `${record.duration} ${t('attendance.clockPage.hours')}` : '-' }}
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.reason')" data-index="reason" />
            <a-table-column :title="t('attendance.field.approvalStatus')">
              <template #cell="{ record }">
                <a-tag :color="getStatusColor(record.approvalStatus)">
                  {{ formatStatus(record.approvalStatus) }}
                </a-tag>
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.approver')">
              <template #cell="{ record }">
                {{ record.approverName || '-' }}
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.approvalComment')">
              <template #cell="{ record }">
                {{ record.approvalComment || '-' }}
              </template>
            </a-table-column>
          </template>
        </a-table>

        <a-empty
          v-if="!loading && filteredApplications.length === 0"
          :description="t('attendance.common.noData')"
        />
      </a-spin>
    </a-card>

    <!-- 申请弹窗 -->
    <CardCreateModal
      v-model:visible="showCreateModal"
      :card-type-id="selectedCardTypeId"
      @success="handleApplicationSuccess"
    />
  </div>
</template>

<style scoped>
.applications-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.action-buttons {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

/* 表格样式优化 */
.applications-list :deep(.arco-card-header) {
  padding: 16px 20px;
}

.applications-list :deep(.arco-card-body) {
  padding: 0;
}

.applications-list :deep(.arco-table) {
  margin: 0;
}

.applications-list :deep(.arco-table-container) {
  border-radius: 0;
}

.applications-list :deep(.arco-table-th),
.applications-list :deep(.arco-table-td) {
  padding: 12px 20px;
}

.applications-list :deep(.arco-table-th:first-child),
.applications-list :deep(.arco-table-td:first-child) {
  padding-left: 20px;
}

.applications-list :deep(.arco-table-th:last-child),
.applications-list :deep(.arco-table-td:last-child) {
  padding-right: 20px;
}

.font-medium {
  font-weight: 500;
}

.text-gray-500 {
  color: var(--color-text-3);
}

.truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.max-w-\[200px\] {
  max-width: 200px;
}

.block {
  display: block;
}
</style>
