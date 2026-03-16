<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { memberApi } from '@/api/member'
import { Message, Modal } from '@arco-design/web-vue'
import { IconPlus } from '@arco-design/web-vue/es/icon'
import type { MemberDTO } from '@/types/member'
import MemberRoleTag from '@/components/member/MemberRoleTag.vue'
import AddMemberModal from '@/components/member/AddMemberModal.vue'
import ChangeRoleModal from '@/components/member/ChangeRoleModal.vue'
import { AdminTable, AdminTableActions } from '@/components/table'
import type { ActionItem } from '@/types/table'
import { handleReferenceConflictError } from '@/utils/error-handler'

const { t } = useI18n()

const props = defineProps<{
  orgId: string
  isOwner: boolean
  isAdmin: boolean
}>()

const loading = ref(false)
const members = ref<MemberDTO[]>([])
const pagination = ref({
  current: 1,
  pageSize: 20,
  total: 0,
})
const searchKeyword = ref('')

const showAddModal = ref(false)
const showRoleModal = ref(false)
const selectedMember = ref<MemberDTO | null>(null)

// 过滤后的成员列表
const filteredMembers = computed(() => {
  if (!searchKeyword.value) {
    return members.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return members.value.filter(
    (member) =>
      member.nickname?.toLowerCase().includes(keyword) ||
      member.email?.toLowerCase().includes(keyword)
  )
})

// 获取操作项
function getActions(record: MemberDTO): ActionItem[] {
  const actions: ActionItem[] = []

  if (props.isOwner && record.role !== 'OWNER') {
    actions.push({
      key: 'changeRole',
      label: t('admin.members.changeRole'),
      onClick: () => handleChangeRole(record),
    })
  }

  if (props.isAdmin && record.role !== 'OWNER') {
    actions.push({
      key: 'remove',
      label: t('admin.members.remove'),
      danger: true,
      onClick: () => handleRemoveMember(record),
    })
  }

  return actions
}

// 处理操作
function handleAction(key: string, record: MemberDTO) {
  switch (key) {
    case 'changeRole':
      handleChangeRole(record)
      break
    case 'remove':
      handleRemoveMember(record)
      break
  }
}

onMounted(() => {
  fetchMembers()
})

async function fetchMembers() {
  loading.value = true
  try {
    const result = await memberApi.list(pagination.value.current, pagination.value.pageSize)
    members.value = result.content
    pagination.value.total = result.total
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pagination.value.current = page
  fetchMembers()
}

function handleAddMember() {
  showAddModal.value = true
}

function handleAddSuccess() {
  showAddModal.value = false
  fetchMembers()
}

function handleChangeRole(member: MemberDTO) {
  selectedMember.value = member
  showRoleModal.value = true
}

function handleRoleChangeSuccess() {
  showRoleModal.value = false
  selectedMember.value = null
  fetchMembers()
}

async function handleRemoveMember(member: MemberDTO) {
  Modal.confirm({
    title: t('admin.members.removeConfirmTitle'),
    content: t('admin.members.removeConfirmContent', { name: member.nickname }),
    okText: t('admin.members.removeConfirmButton'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await memberApi.remove(member.id)
        Message.success(t('admin.members.removeSuccess'))
        fetchMembers()
      } catch (error: any) {
        if (!handleReferenceConflictError(error)) {
          // 错误已在拦截器处理或按默认方式处理
        }
      }
    },
  })
}
</script>

<template>
  <div class="member-list-tab">
    <!-- 顶部工具栏 -->
    <div class="content-header">
      <a-button v-if="isAdmin" type="primary" size="small" @click="handleAddMember">
        <template #icon>
          <IconPlus />
        </template>
        {{ t('admin.members.addButton') }}
      </a-button>
      <a-input-search
        v-model="searchKeyword"
        :placeholder="t('admin.search.namePlaceholder')"
        style="width: 260px"
        size="small"
        allow-clear
      />
    </div>

    <!-- 成员表格 -->
    <AdminTable
      :data="filteredMembers"
      :loading="loading"
      :scroll="{ x: 800, y: '100%' }"
      :pagination="{
        current: pagination.current,
        pageSize: pagination.pageSize,
        total: pagination.total,
        showTotal: true,
      }"
      row-key="id"
      @page-change="handlePageChange"
    >
      <a-table-column
        :title="t('admin.table.name')"
        data-index="nickname"
        :width="240"
        fixed="left"
      >
        <template #cell="{ record }">
          <div class="member-cell">
            <a-avatar :size="32" :image-url="record.avatar || undefined">
              {{ record.nickname?.charAt(0) || record.email?.charAt(0) }}
            </a-avatar>
            <span class="member-name">{{ record.nickname }}</span>
          </div>
        </template>
      </a-table-column>

      <a-table-column
        :title="t('admin.table.email')"
        data-index="email"
        ellipsis
      >
        <template #cell="{ record }">
          <span class="cell-text">{{ record.email }}</span>
        </template>
      </a-table-column>

      <a-table-column
        :title="t('admin.table.role')"
        :width="120"
      >
        <template #cell="{ record }">
          <MemberRoleTag :role="record.role" />
        </template>
      </a-table-column>

      <a-table-column
        :title="t('admin.table.joinedAt')"
        data-index="joinedAt"
        :width="180"
      >
        <template #cell="{ record }">
          <span class="nowrap">{{ record.joinedAt }}</span>
        </template>
      </a-table-column>

      <a-table-column
        :title="t('admin.table.operations')"
        :width="120"
        fixed="right"
      >
        <template #cell="{ record }">
          <template v-if="record.role === 'OWNER'">
            <span class="owner-label">{{ t('admin.members.owner') }}</span>
          </template>
          <template v-else>
            <AdminTableActions
              :actions="getActions(record)"
              @action="handleAction($event, record)"
            />
          </template>
        </template>
      </a-table-column>

      <template #empty>
        <a-empty :description="t('common.state.empty')" />
      </template>
    </AdminTable>

    <AddMemberModal
      v-model:visible="showAddModal"
      :org-id="orgId"
      @success="handleAddSuccess"
    />

    <ChangeRoleModal
      v-if="selectedMember"
      v-model:visible="showRoleModal"
      :member="selectedMember"
      @success="handleRoleChangeSuccess"
    />
  </div>
</template>

<style scoped>
.member-list-tab {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 12px 12px;
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-name {
  font-weight: 500;
  font-size: 13px;
}

.cell-text {
  font-size: 13px;
  color: var(--color-text-1);
}

.nowrap {
  white-space: nowrap;
  font-size: 13px;
}

.owner-label {
  font-size: 12px;
  color: var(--color-text-3);
}
</style>
