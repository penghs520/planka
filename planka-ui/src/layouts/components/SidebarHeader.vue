<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import { IconSearch } from '@arco-design/web-vue/es/icon'
import { ref } from 'vue'
import type { OrganizationDTO } from '@/types/member'
import CreateOrgModal from '@/views/org/CreateOrgModal.vue'

const { t } = useI18n()

const router = useRouter()
const orgStore = useOrgStore()
const userStore = useUserStore()

const showCreateModal = ref(false)
const searchKeyword = ref('')

const currentOrg = computed(() => orgStore.currentOrg)
const organizations = computed(() => orgStore.myOrgs)
const displayName = computed(() => currentOrg.value?.name || t('common.org.select'))

// 取组织名首字母
const orgInitial = computed(() => {
  const name = displayName.value
  return name ? name.charAt(0).toUpperCase() : 'O'
})

const filteredOrganizations = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return organizations.value
  return organizations.value.filter(org => org.name.toLowerCase().includes(keyword))
})

function handleDropdownVisibleChange(visible: boolean) {
  if (!visible) {
    searchKeyword.value = ''
  }
}

const switching = ref(false)

async function selectOrg(org: OrganizationDTO) {
  if (org.id === orgStore.currentOrgId) return
  if (switching.value) return

  switching.value = true
  try {
    await orgStore.switchOrganization(org.id)
    Message.success(t('common.org.switchedTo', { name: org.name }))
    router.go(0)
  } catch (error: unknown) {
    const err = error as { message?: string }
    Message.error(err.message || t('common.org.switchFailed'))
  } finally {
    switching.value = false
  }
}

function handleCreateSuccess() {
  showCreateModal.value = false
  Message.success(t('common.org.createSuccess'))
}
</script>

<template>
  <a-dropdown
    trigger="click"
    position="bl"
    popup-container=".sidebar-header"
    :popup-max-height="false"
    @popup-visible-change="handleDropdownVisibleChange"
  >
    <div class="sidebar-header">
      <div class="org-icon">
        <span class="org-icon-text">{{ orgInitial }}</span>
      </div>
      <span class="org-name">{{ displayName }}</span>
    </div>
    <template #content>
      <div class="org-dropdown-content">
        <div class="org-dropdown-header">
          <span class="org-dropdown-title">{{ t('common.layout.switchOrg') }}</span>
        </div>
        <div class="org-search">
          <a-input
            v-model="searchKeyword"
            size="small"
            allow-clear
            @click.stop
          >
            <template #suffix>
              <IconSearch />
            </template>
          </a-input>
        </div>
        <div class="org-list">
          <a-doption
            v-for="org in filteredOrganizations"
            :key="org.id"
            :class="{ 'org-active': org.id === currentOrg?.id }"
            @click="selectOrg(org)"
          >
            <div class="org-option">
              <span class="org-option-name">{{ org.name }}</span>
              <icon-check v-if="org.id === currentOrg?.id" class="org-check" />
            </div>
          </a-doption>
          <div v-if="filteredOrganizations.length === 0" class="org-empty">
            {{ t('common.org.noMatch') }}
          </div>
        </div>
        <a-divider :margin="4" />
        <a-doption v-if="userStore.isSuperAdmin" class="create-org-option" @click="showCreateModal = true">
          {{ t('common.org.create') }}
        </a-doption>
      </div>
    </template>
  </a-dropdown>

  <CreateOrgModal
    v-model:visible="showCreateModal"
    @success="handleCreateSuccess"
  />
</template>

<style scoped>
.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 12px;
  cursor: pointer;
  transition: background-color 0.15s;
  flex-shrink: 0;
}

.sidebar-header:hover {
  background-color: var(--sidebar-bg-hover);
}

.org-icon {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, #5E6AD2 0%, #8B5CF6 100%);
}

.org-icon-text {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  line-height: 1;
}

.org-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
  color: var(--sidebar-text-primary);
}

/* Dropdown 保持亮色主题 */
.org-dropdown-content {
  min-width: 220px;
}

.org-dropdown-header {
  padding: 8px 12px;
}

.org-dropdown-title {
  font-size: 12px;
  color: var(--color-text-3);
}

.org-search {
  padding: 0 12px 4px;
}

.org-search :deep(.arco-input-wrapper) {
  height: 24px;
}

.org-list {
  max-height: 360px;
  overflow-y: auto;
}

.org-option {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.org-option-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.org-check {
  color: rgb(var(--primary-6));
  flex-shrink: 0;
}

.org-active {
  background-color: rgb(var(--primary-1));
  color: rgb(var(--primary-6));
  font-weight: 500;
}

.org-empty {
  padding: 16px 12px;
  text-align: center;
  color: var(--color-text-3);
  font-size: 13px;
}

.org-dropdown-content :deep(.arco-dropdown-option-content) {
  justify-content: flex-start;
  text-align: left;
  width: 100%;
}

.org-dropdown-content :deep(.arco-dropdown-option) {
  padding: 0 12px;
}

.create-org-option {
  font-weight: 500;
}
</style>
