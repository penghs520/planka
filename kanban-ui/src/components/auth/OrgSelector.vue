<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { useUserStore } from '@/stores/user'
import { Message } from '@arco-design/web-vue'
import { IconSearch } from '@arco-design/web-vue/es/icon'
import type { OrganizationDTO } from '@/types/member'
import CreateOrgModal from '@/views/org/CreateOrgModal.vue'

const { t } = useI18n()
const defaultLogo = '/favicon.svg'

const router = useRouter()
const orgStore = useOrgStore()
const userStore = useUserStore()

defineProps<{
  collapsed?: boolean
}>()

const showCreateModal = ref(false)
const searchKeyword = ref('')

const currentOrg = computed(() => orgStore.currentOrg)
const organizations = computed(() => orgStore.myOrgs)
const displayName = computed(() => currentOrg.value?.name || t('common.org.select'))

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
    // Refresh current page to load new organization data
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
  <a-dropdown trigger="click" position="bl" popup-container=".org-selector-trigger" :popup-max-height="false" @popup-visible-change="handleDropdownVisibleChange">
    <div class="org-selector-trigger" :class="{ 'is-collapsed': collapsed }">
      <div class="org-avatar">
        <img :src="currentOrg?.logo || defaultLogo" alt="" />
      </div>
      <template v-if="!collapsed">
        <span class="org-name">{{ displayName }}</span>
        <icon-down />
      </template>
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
.org-selector-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  height: 40px;
  padding: 0 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.org-selector-trigger:hover {
  background-color: var(--color-fill-2);
}

.org-selector-trigger.is-collapsed {
  width: 40px;
  padding: 0;
  justify-content: center;
}

.org-avatar {
  width: 26px;
  height: 26px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.org-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.org-avatar-text {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.org-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
}

.org-dropdown-content {
  min-width: 200px;
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

.org-search :deep(.arco-icon-search) {
  color: var(--color-text-3);
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
