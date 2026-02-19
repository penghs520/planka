<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { attendanceApi } from '@/api'
import { Message } from '@arco-design/web-vue'
import type { ApplicationRecord } from '@/types/attendance'

const { t } = useI18n()
const orgStore = useOrgStore()

const loading = ref(false)
const approving = ref(false)
const approvals = ref<ApplicationRecord[]>([])
const selectedApplication = ref<ApplicationRecord | null>(null)
const showApprovalModal = ref(false)
const approvalComment = ref('')
const approvalAction = ref<'approve' | 'reject'>('approve')

// 加载待审批列表
async function loadApprovals() {
  if (!orgStore.currentMemberCardId || !orgStore.currentOrgId) return

  loading.value = true
  try {
    approvals.value = await attendanceApi.getPendingApprovals(
      orgStore.currentMemberCardId,
      orgStore.currentOrgId,
    )
  } catch (error) {
    console.error('Failed to load approvals:', error)
  } finally {
    loading.value = false
  }
}

// 打开审批弹窗
function openApprovalModal(application: ApplicationRecord, action: 'approve' | 'reject') {
  selectedApplication.value = application
  approvalAction.value = action
  approvalComment.value = ''
  showApprovalModal.value = true
}

// 确认审批
async function handleConfirmApproval() {
  if (!selectedApplication.value) return

  approving.value = true
  try {
    await attendanceApi.approveApplication(
      selectedApplication.value.cardId,
      orgStore.currentOrgId!,
      approvalAction.value === 'approve',
      approvalComment.value || undefined,
      selectedApplication.value.type,
    )

    const successMsg =
      approvalAction.value === 'approve'
        ? t('attendance.approvalsPage.approveSuccess')
        : t('attendance.approvalsPage.rejectSuccess')
    Message.success(successMsg)

    showApprovalModal.value = false
    await loadApprovals()
  } catch {
    const errorMsg =
      approvalAction.value === 'approve'
        ? t('attendance.approvalsPage.approveFailed')
        : t('attendance.approvalsPage.rejectFailed')
    Message.error(errorMsg)
  } finally {
    approving.value = false
  }
}

// 初始化
onMounted(() => {
  loadApprovals()
})
</script>

<template>
  <div class="approvals-page">
    <a-card class="approvals-list">
      <template #title>{{ t('attendance.approvalsPage.pendingApprovals') }}</template>

      <a-spin :loading="loading">
        <a-table :data="approvals" :pagination="{ pageSize: 20 }" :bordered="false">
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
            <a-table-column :title="t('attendance.field.applicant')" data-index="applicantName" />
            <a-table-column :title="t('attendance.field.applyTime')" data-index="applyTime" />
            <a-table-column :title="t('attendance.field.startTime')" data-index="startTime" />
            <a-table-column :title="t('attendance.field.endTime')" data-index="endTime" />
            <a-table-column :title="t('attendance.field.duration')">
              <template #cell="{ record }">
                {{ record.duration ? `${record.duration} ${t('attendance.clockPage.hours')}` : '-' }}
              </template>
            </a-table-column>
            <a-table-column :title="t('attendance.field.reason')" data-index="reason" />
            <a-table-column :title="t('common.action.actions')" :width="200">
              <template #cell="{ record }">
                <a-space>
                  <a-button
                    type="primary"
                    size="small"
                    @click="openApprovalModal(record, 'approve')"
                  >
                    {{ t('attendance.approvalsPage.approve') }}
                  </a-button>
                  <a-button
                    type="outline"
                    status="danger"
                    size="small"
                    @click="openApprovalModal(record, 'reject')"
                  >
                    {{ t('attendance.approvalsPage.reject') }}
                  </a-button>
                </a-space>
              </template>
            </a-table-column>
          </template>
        </a-table>

        <a-empty
          v-if="!loading && approvals.length === 0"
          :description="t('attendance.common.noData')"
        />
      </a-spin>
    </a-card>

    <!-- 审批弹窗 -->
    <a-modal
      v-model:visible="showApprovalModal"
      :title="
        approvalAction === 'approve'
          ? t('attendance.approvalsPage.confirmApprove')
          : t('attendance.approvalsPage.confirmReject')
      "
      :ok-loading="approving"
      @ok="handleConfirmApproval"
    >
      <a-form :model="{ comment: approvalComment }">
        <a-form-item
          :label="t('attendance.approvalsPage.approvalComment')"
          field="comment"
        >
          <a-textarea
            v-model="approvalComment"
            :placeholder="t('attendance.approvalsPage.pleaseEnterComment')"
            :rows="4"
          />
        </a-form-item>
      </a-form>

      <a-alert
        v-if="approvalAction === 'approve'"
        type="info"
        :message="t('attendance.approvalsPage.approveConfirmMessage')"
        style="margin-top: 16px"
      />
      <a-alert
        v-else
        type="warning"
        :message="t('attendance.approvalsPage.rejectConfirmMessage')"
        style="margin-top: 16px"
      />
    </a-modal>
  </div>
</template>

<style scoped>
.approvals-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.approvals-list :deep(.arco-card-header) {
  padding: 16px 20px;
}

.approvals-list :deep(.arco-card-body) {
  padding: 0;
}

.approvals-list :deep(.arco-table) {
  margin: 0;
}

.approvals-list :deep(.arco-table-container) {
  border-radius: 0;
}

.approvals-list :deep(.arco-table-th),
.approvals-list :deep(.arco-table-td) {
  padding: 12px 20px;
}

.approvals-list :deep(.arco-table-th:first-child),
.approvals-list :deep(.arco-table-td:first-child) {
  padding-left: 20px;
}

.approvals-list :deep(.arco-table-th:last-child),
.approvals-list :deep(.arco-table-td:last-child) {
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
