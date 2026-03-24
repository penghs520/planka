<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { useCascadeRelationNavStore } from '@/stores/cascadeRelationNav'
import { useOrgStore } from '@/stores/org'

const { t } = useI18n()
const router = useRouter()
const cascadeRelationNav = useCascadeRelationNavStore()
const orgStore = useOrgStore()

const activeTab = ref<'user' | 'workspace'>('user')
const draftUserIds = ref<string[]>([])
const draftWorkspaceIds = ref<string[]>([])

const sortedCatalog = computed(() =>
  [...cascadeRelationNav.cascadeRelationCatalog].sort((a, b) => a.name.localeCompare(b.name, 'zh-CN')),
)

/**
 * 个人偏好草稿：已保存的个人侧栏偏好 + 工作空间已勾选项（去重）。
 * 工作空间勾选的会在个人页默认勾选，便于在侧栏实际展示基础上继续调整。
 */
function initialDraftUserPinnedIds(): string[] {
  const user = [...cascadeRelationNav.pinnedCascadeRelationIds]
  const ws = [...cascadeRelationNav.workspacePinnedCascadeRelationIds]
  const seen = new Set<string>()
  const out: string[] = []
  for (const id of user) {
    if (!seen.has(id)) {
      seen.add(id)
      out.push(id)
    }
  }
  for (const id of ws) {
    if (!seen.has(id)) {
      seen.add(id)
      out.push(id)
    }
  }
  return out
}

onMounted(async () => {
  await cascadeRelationNav.refreshCatalog()
  draftUserIds.value = initialDraftUserPinnedIds()
  draftWorkspaceIds.value = [...cascadeRelationNav.workspacePinnedCascadeRelationIds]
})

function toggleUserId(id: string, checked: boolean) {
  if (checked) {
    if (!draftUserIds.value.includes(id)) {
      draftUserIds.value = [...draftUserIds.value, id]
    }
  } else {
    draftUserIds.value = draftUserIds.value.filter((x) => x !== id)
  }
}

function toggleWorkspaceId(id: string, checked: boolean) {
  if (!orgStore.isAdmin) {
    return
  }
  if (checked) {
    if (!draftWorkspaceIds.value.includes(id)) {
      draftWorkspaceIds.value = [...draftWorkspaceIds.value, id]
    }
  } else {
    draftWorkspaceIds.value = draftWorkspaceIds.value.filter((x) => x !== id)
  }
}

function isUserChecked(id: string) {
  return draftUserIds.value.includes(id)
}

function isWorkspaceChecked(id: string) {
  return draftWorkspaceIds.value.includes(id)
}

function orderedUserPinnedIds(): string[] {
  return sortedCatalog.value.map((s) => String(s.id)).filter((id) => draftUserIds.value.includes(id))
}

function orderedWorkspacePinnedIds(): string[] {
  return sortedCatalog.value.map((s) => String(s.id)).filter((id) => draftWorkspaceIds.value.includes(id))
}

async function handleSaveUser() {
  try {
    await cascadeRelationNav.setPinnedCascadeRelationIds(orderedUserPinnedIds())
    Message.success(t('sidebar.sidebarSettingsSaved'))
    router.back()
  } catch {
    /* 请求失败时由全局拦截器提示 */
  }
}

async function handleSaveWorkspace() {
  if (!orgStore.isAdmin) {
    return
  }
  try {
    await cascadeRelationNav.setWorkspacePinnedCascadeRelationIds(orderedWorkspacePinnedIds())
    Message.success(t('sidebar.sidebarWorkspaceSettingsSaved'))
    router.back()
  } catch {
    /* 请求失败时由全局拦截器提示 */
  }
}

function handleCancel() {
  router.back()
}
</script>

<template>
  <div class="sidebar-settings-page">
    <h1 class="page-title">{{ t('common.route.sidebarSettings') }}</h1>

    <a-tabs
      v-model:active-key="activeTab"
      type="line"
      class="settings-tabs"
    >
      <a-tab-pane
        key="user"
        :title="t('sidebar.sidebarSettingsTabUser')"
      >
        <section class="section">
          <h2 class="section-title">{{ t('sidebar.cascadeRelationSidebarUserTitle') }}</h2>
          <p class="section-hint">{{ t('sidebar.cascadeRelationSidebarUserHint') }}</p>
          <a-spin :loading="cascadeRelationNav.loadingCatalog">
            <div
              v-if="!cascadeRelationNav.loadingCatalog && sortedCatalog.length === 0"
              class="empty"
            >
              {{ t('sidebar.cascadeRelationSidebarEditorEmpty') }}
            </div>
            <ul
              v-else
              class="check-list"
            >
              <li
                v-for="s in sortedCatalog"
                :key="String(s.id)"
              >
                <label class="row">
                  <input
                    type="checkbox"
                    class="cb"
                    :checked="isUserChecked(String(s.id))"
                    @change="toggleUserId(String(s.id), ($event.target as HTMLInputElement).checked)"
                  >
                  <span class="name">{{ s.name }}</span>
                </label>
              </li>
            </ul>
          </a-spin>
        </section>
        <div class="page-actions">
          <a-button @click="handleCancel">
            {{ t('common.action.cancel') }}
          </a-button>
          <a-button
            type="primary"
            :disabled="cascadeRelationNav.loadingCatalog"
            @click="handleSaveUser"
          >
            {{ t('common.action.save') }}
          </a-button>
        </div>
      </a-tab-pane>

      <a-tab-pane
        key="workspace"
        :title="t('sidebar.sidebarSettingsTabWorkspace')"
      >
        <section class="section">
          <h2 class="section-title">{{ t('sidebar.workspaceSidebarEditorTitle') }}</h2>
          <p class="section-hint">{{ t('sidebar.workspaceSidebarEditorHint') }}</p>
          <p
            v-if="!orgStore.isAdmin"
            class="section-hint readonly-hint"
          >
            {{ t('sidebar.workspaceSidebarReadonlyHint') }}
          </p>
          <a-spin :loading="cascadeRelationNav.loadingCatalog">
            <div
              v-if="!cascadeRelationNav.loadingCatalog && sortedCatalog.length === 0"
              class="empty"
            >
              {{ t('sidebar.cascadeRelationSidebarEditorEmpty') }}
            </div>
            <ul
              v-else
              class="check-list"
            >
              <li
                v-for="s in sortedCatalog"
                :key="String(s.id)"
              >
                <label
                  class="row"
                  :class="{ 'row-readonly': !orgStore.isAdmin }"
                >
                  <input
                    type="checkbox"
                    class="cb"
                    :disabled="!orgStore.isAdmin"
                    :checked="isWorkspaceChecked(String(s.id))"
                    @change="toggleWorkspaceId(String(s.id), ($event.target as HTMLInputElement).checked)"
                  >
                  <span class="name">{{ s.name }}</span>
                </label>
              </li>
            </ul>
          </a-spin>
        </section>
        <div class="page-actions">
          <a-button @click="handleCancel">
            {{ t('common.action.cancel') }}
          </a-button>
          <a-button
            v-if="orgStore.isAdmin"
            type="primary"
            :disabled="cascadeRelationNav.loadingCatalog"
            @click="handleSaveWorkspace"
          >
            {{ t('common.action.save') }}
          </a-button>
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<style scoped>
.sidebar-settings-page {
  padding: 20px 24px;
  height: 100%;
  overflow: auto;
  background: var(--color-main-panel);
  max-width: 640px;
}

.page-title {
  margin: 0 0 16px;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-1);
}

.settings-tabs :deep(.arco-tabs-nav) {
  margin-left: 0;

  &::before {
    display: none;
  }
}

.settings-tabs :deep(.arco-tabs-nav-tab-list) {
  justify-content: flex-start;
  padding-left: 0;
}

.settings-tabs :deep(.arco-tabs-tab) {
  margin-left: 0;
}

.settings-tabs :deep(.arco-tabs-tab:first-child) {
  padding-left: 0;
}

.settings-tabs :deep(.arco-tabs-tab + .arco-tabs-tab) {
  margin-left: 24px;
}

.settings-tabs :deep(.arco-tabs-content) {
  padding-top: 16px;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-1);
}

.section-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--color-text-3);
  line-height: 1.5;
}

.readonly-hint {
  color: var(--color-text-2);
}

.empty {
  font-size: 13px;
  color: var(--color-text-3);
  padding: 8px 0;
}

.check-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border: 1px solid var(--color-border-2);
  border-radius: 8px;
  background: var(--color-bg-2);
  max-height: min(420px, 50vh);
  overflow-y: auto;
}

.row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--color-border-1);
  cursor: pointer;
  font-size: 14px;
  color: var(--color-text-1);
}

.row-readonly {
  cursor: default;
}

.row:last-child {
  border-bottom: none;
}

.row:hover:not(.row-readonly) {
  background: var(--color-fill-2);
}

.cb {
  flex-shrink: 0;
}

.cb:disabled {
  cursor: not-allowed;
}

.name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-actions {
  display: flex;
  gap: 8px;
  padding-top: 8px;
}
</style>
