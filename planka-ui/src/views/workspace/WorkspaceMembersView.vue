<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  IconFilter,
  IconPlus,
  IconSort,
  IconSortAscending,
  IconSortDescending,
} from '@arco-design/web-vue/es/icon'
import AddMemberModal from '@/components/member/AddMemberModal.vue'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import 'dayjs/locale/en'
import { useOrgStore } from '@/stores/org'
import {
  fetchWorkspaceMembers,
  type WorkspaceMemberRow,
  type WorkspaceMembersSortField,
} from '@/api/workspace-members'
import { formatDateTime } from '@/utils/format'
import { OrganizationRole } from '@/types/member'

dayjs.extend(relativeTime)

const ONLINE_THRESHOLD_MS = 15 * 60 * 1000
const PAGE_SIZE = 30

const { t, locale } = useI18n()
const orgStore = useOrgStore()

const loading = ref(false)
const loadingMore = ref(false)
const rows = ref<WorkspaceMemberRow[]>([])
const keyword = ref('')
const sortField = ref<WorkspaceMembersSortField>('name')
const sortOrder = ref<'asc' | 'desc'>('asc')
const currentPage = ref(1)
const totalCount = ref(0)

const sentinelRef = ref<HTMLElement | null>(null)
const nameSearchOpen = ref(false)
const addMemberVisible = ref(false)
/** Arco Input 实例，用于展开筛选后聚焦 */
const nameSearchInputRef = ref<{ focus?: () => void } | null>(null)
let intersectionObserver: InstanceType<typeof window.IntersectionObserver> | null = null

const hasMore = computed(() => totalCount.value > rows.value.length)

const canShowMemberList = computed(
  () => Boolean(orgStore.currentOrgId && orgStore.currentMemberCardId),
)

watch(
  locale,
  (l) => {
    dayjs.locale(l === 'zh-CN' ? 'zh-cn' : 'en')
  },
  { immediate: true },
)

function memberInitials(name: string): string {
  const s = name.trim()
  if (!s) return '?'
  if (/[\u4e00-\u9fff]/.test(s) && !/\s/.test(s)) {
    return s.length >= 2 ? s.slice(0, 2) : s.slice(0, 1)
  }
  const parts = s.split(/\s+/).filter(Boolean)
  if (parts.length >= 2) {
    const a = parts[0]?.charAt(0)
    const b = parts[parts.length - 1]?.charAt(0)
    if (a && b) return (a + b).toUpperCase()
  }
  return s.length >= 2 ? s.slice(0, 2).toUpperCase() : (s.charAt(0) || '?').toUpperCase()
}

function formatJoinedShort(iso: string | null | undefined): string {
  if (!iso) return '—'
  const d = dayjs(iso)
  return locale.value === 'zh-CN' ? d.format('M月D日') : d.format('MMM D')
}

function isRecentlyOnline(iso: string | null | undefined): boolean {
  if (!iso) return false
  return Date.now() - new Date(iso).getTime() <= ONLINE_THRESHOLD_MS
}

function lastSeenRelative(iso: string): string {
  return dayjs(iso).fromNow()
}

function rolePillClass(role: string | null): string {
  if (role === OrganizationRole.OWNER) return 'wm-role-pill wm-role-pill--owner'
  if (role === OrganizationRole.ADMIN) return 'wm-role-pill wm-role-pill--admin'
  if (role === OrganizationRole.MEMBER) return 'wm-role-pill wm-role-pill--member'
  return 'wm-role-pill wm-role-pill--unknown'
}

function roleText(role: string | null): string {
  if (role === OrganizationRole.OWNER) return t('workspaceMembers.roleOwner')
  if (role === OrganizationRole.ADMIN) return t('workspaceMembers.roleAdmin')
  if (role === OrganizationRole.MEMBER) return t('workspaceMembers.roleMember')
  return role?.trim() ? role : t('workspaceMembers.roleUnknown')
}

function teamsLine(row: WorkspaceMemberRow): string {
  if (!row.teamNames?.length) return ''
  return row.teamNames.join(', ')
}

function columnTitle(field: WorkspaceMembersSortField): string {
  switch (field) {
    case 'name':
      return t('workspaceMembers.columnName')
    case 'email':
      return t('workspaceMembers.columnEmail')
    case 'joined':
      return t('workspaceMembers.columnJoined')
    case 'lastSeen':
      return t('workspaceMembers.columnLastSeen')
    default:
      return ''
  }
}

function sortAriaLabel(field: WorkspaceMembersSortField): string {
  const col = columnTitle(field)
  if (sortField.value !== field) {
    return t('workspaceMembers.sortByColumn', { column: col })
  }
  return t('workspaceMembers.sortActive', {
    column: col,
    order: sortOrder.value === 'asc' ? t('workspaceMembers.sortAsc') : t('workspaceMembers.sortDesc'),
  })
}

function ariaSortAttr(field: WorkspaceMembersSortField): 'ascending' | 'descending' | 'none' {
  if (sortField.value !== field) return 'none'
  return sortOrder.value === 'asc' ? 'ascending' : 'descending'
}

function sortGlyphComponent(field: WorkspaceMembersSortField) {
  if (sortField.value !== field) return IconSort
  return sortOrder.value === 'asc' ? IconSortAscending : IconSortDescending
}

async function load(reset: boolean) {
  const orgId = orgStore.currentOrgId
  const mid = orgStore.currentMemberCardId
  if (!orgId || !mid) {
    rows.value = []
    totalCount.value = 0
    return
  }

  if (!reset) {
    if (!hasMore.value || loadingMore.value || loading.value) return
    loadingMore.value = true
  } else {
    loading.value = true
  }

  const page = reset ? 1 : currentPage.value + 1

  try {
    const result = await fetchWorkspaceMembers({
      page,
      size: PAGE_SIZE,
      keyword: keyword.value.trim() || undefined,
      sort: sortField.value,
      order: sortOrder.value,
    })
    if (reset) {
      rows.value = result.content
    } else {
      rows.value = [...rows.value, ...result.content]
    }
    totalCount.value = result.total
    currentPage.value = page
  } finally {
    loading.value = false
    loadingMore.value = false
    if (reset) {
      await nextTick()
      if (rows.value.length > 0) setupIntersectionObserver()
    }
  }
}

function toggleSort(field: WorkspaceMembersSortField) {
  if (sortField.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortOrder.value = 'asc'
  }
  load(true)
}

function onSearch() {
  load(true)
}

function onAddMemberSuccess() {
  addMemberVisible.value = false
  load(true)
}

function toggleNameSearch() {
  nameSearchOpen.value = !nameSearchOpen.value
  if (nameSearchOpen.value) {
    nextTick(() => {
      nameSearchInputRef.value?.focus?.()
    })
  }
}

const filterButtonActive = computed(
  () => nameSearchOpen.value || Boolean(keyword.value.trim()),
)

function setupIntersectionObserver() {
  if (!sentinelRef.value) return
  if (!intersectionObserver) {
    intersectionObserver = new window.IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting) {
          load(false)
        }
      },
      { root: null, rootMargin: '120px', threshold: 0 },
    )
  }
  intersectionObserver.disconnect()
  intersectionObserver.observe(sentinelRef.value)
}

watch(
  () => [orgStore.currentOrgId, orgStore.currentMemberCardId],
  () => {
    load(true)
  },
)

onMounted(() => {
  load(true)
})

onUnmounted(() => {
  intersectionObserver?.disconnect()
  intersectionObserver = null
})
</script>

<template>
  <div class="wm-page">
    <header class="wm-header">
      <div class="wm-header-row">
        <h1 class="wm-title">
          <span class="wm-title-label">{{ t('sidebar.members') }}</span>
          <span class="wm-title-sep" aria-hidden="true"> · </span>
          <span class="wm-title-count">{{ totalCount }}</span>
        </h1>
        <a-button
          v-if="orgStore.canManageMembers && orgStore.currentOrgId"
          type="text"
          class="wm-invite-btn"
          :aria-label="t('workspaceMembers.inviteMember')"
          @click="addMemberVisible = true"
        >
          <template #icon>
            <IconPlus />
          </template>
        </a-button>
      </div>
    </header>

    <AddMemberModal
      v-if="orgStore.currentOrgId"
      v-model:visible="addMemberVisible"
      :org-id="orgStore.currentOrgId"
      @success="onAddMemberSuccess"
    />

    <a-spin :loading="loading" class="wm-spin">
      <div v-if="canShowMemberList" class="wm-list">
        <div class="wm-grid wm-grid--header" role="row">
          <div
            class="wm-col wm-col--name wm-col-header-name"
            role="columnheader"
            :class="{ 'wm-col--sorted': sortField === 'name' }"
            :aria-sort="ariaSortAttr('name')"
          >
            <div class="wm-name-header-row">
              <button
                type="button"
                class="wm-th-sort wm-th-sort--name"
                :aria-label="sortAriaLabel('name')"
                @click="toggleSort('name')"
              >
                <span class="wm-th-sort-text">{{ t('workspaceMembers.columnName') }}</span>
                <component :is="sortGlyphComponent('name')" class="wm-sort-glyph" />
              </button>
              <button
                type="button"
                class="wm-filter-btn"
                :class="{ 'wm-filter-btn--active': filterButtonActive }"
                :aria-label="t('workspaceMembers.filterByName')"
                :aria-expanded="nameSearchOpen"
                @click="toggleNameSearch"
              >
                <IconFilter />
              </button>
              <div v-show="nameSearchOpen" class="wm-name-filter-wrap">
                <a-input
                  ref="nameSearchInputRef"
                  v-model="keyword"
                  class="wm-name-filter-input"
                  size="mini"
                  :aria-label="t('workspaceMembers.filterByName')"
                  allow-clear
                  @press-enter="onSearch"
                  @clear="onSearch"
                />
              </div>
            </div>
          </div>
          <div
            class="wm-col wm-col--email"
            role="columnheader"
            :class="{ 'wm-col--sorted': sortField === 'email' }"
            :aria-sort="ariaSortAttr('email')"
          >
            <button
              type="button"
              class="wm-th-sort"
              :aria-label="sortAriaLabel('email')"
              @click="toggleSort('email')"
            >
              <span class="wm-th-sort-text">{{ t('workspaceMembers.columnEmail') }}</span>
              <component :is="sortGlyphComponent('email')" class="wm-sort-glyph" />
            </button>
          </div>
          <div class="wm-col wm-col--status">{{ t('workspaceMembers.columnStatus') }}</div>
          <div
            class="wm-col wm-col--joined"
            role="columnheader"
            :class="{ 'wm-col--sorted': sortField === 'joined' }"
            :aria-sort="ariaSortAttr('joined')"
          >
            <button
              type="button"
              class="wm-th-sort"
              :aria-label="sortAriaLabel('joined')"
              @click="toggleSort('joined')"
            >
              <span class="wm-th-sort-text">{{ t('workspaceMembers.columnJoined') }}</span>
              <component :is="sortGlyphComponent('joined')" class="wm-sort-glyph" />
            </button>
          </div>
          <div class="wm-col wm-col--teams">{{ t('workspaceMembers.columnTeams') }}</div>
          <div
            class="wm-col wm-col--seen"
            role="columnheader"
            :class="{ 'wm-col--sorted': sortField === 'lastSeen' }"
            :aria-sort="ariaSortAttr('lastSeen')"
          >
            <button
              type="button"
              class="wm-th-sort"
              :aria-label="sortAriaLabel('lastSeen')"
              @click="toggleSort('lastSeen')"
            >
              <span class="wm-th-sort-text">{{ t('workspaceMembers.columnLastSeen') }}</span>
              <component :is="sortGlyphComponent('lastSeen')" class="wm-sort-glyph" />
            </button>
          </div>
        </div>

        <template v-if="rows.length > 0">
          <div
            v-for="row in rows"
            :key="row.memberCardId"
            class="wm-grid wm-grid--row"
            role="row"
          >
            <div class="wm-col wm-col--name wm-name-cell">
              <div class="wm-avatar" :aria-hidden="true">{{ memberInitials(row.name) }}</div>
              <div class="wm-name-text">
                <div class="wm-name-primary">{{ row.name || '—' }}</div>
              </div>
            </div>
            <div
              class="wm-col wm-col--email wm-email-cell"
              :class="{ 'wm-email-cell--empty': !row.email?.trim() }"
              :title="row.email?.trim() || undefined"
            >
              {{ row.email?.trim() ? row.email : t('workspaceMembers.noEmail') }}
            </div>
            <div class="wm-col wm-col--status">
              <span :class="rolePillClass(row.role)">{{ roleText(row.role) }}</span>
            </div>
            <div class="wm-col wm-col--joined wm-dim">
              {{ formatJoinedShort(row.joinedAt) }}
            </div>
            <div
              class="wm-col wm-col--teams wm-teams"
              :title="teamsLine(row) || undefined"
            >
              {{ teamsLine(row) || '—' }}
            </div>
            <div class="wm-col wm-col--seen wm-last-seen">
              <span v-if="!row.lastLoginAt" class="wm-dim">{{ t('workspaceMembers.neverLoggedIn') }}</span>
              <span v-else-if="isRecentlyOnline(row.lastLoginAt)" class="wm-online">
                <span class="wm-online-dot" />
                {{ t('workspaceMembers.online') }}
              </span>
              <a-tooltip v-else :content="formatDateTime(row.lastLoginAt ?? undefined)">
                <span class="wm-relative">{{ lastSeenRelative(row.lastLoginAt) }}</span>
              </a-tooltip>
            </div>
          </div>

          <div ref="sentinelRef" class="wm-sentinel" aria-hidden="true" />

          <div v-if="loadingMore" class="wm-load-more" role="status" :aria-label="t('workspaceMembers.loadingMore')">
            <a-spin />
          </div>
        </template>

        <div v-else-if="!loading" class="wm-empty-wrap wm-empty-wrap--in-list">
          <a-empty :description="t('workspaceMembers.empty')" />
        </div>
      </div>
      <div v-else-if="!loading" class="wm-empty-wrap">
        <a-empty :description="t('workspaceMembers.empty')" />
      </div>
    </a-spin>
  </div>
</template>

<style scoped>
.wm-page {
  padding: 8px 24px 20px;
  min-height: 100%;
  overflow: auto;
  background: var(--color-main-panel);
  font-family: var(--font-family);
}

.wm-header {
  margin-bottom: 12px;
}

.wm-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.wm-invite-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 8px;
  color: var(--color-text-2);
}

.wm-invite-btn:hover {
  color: rgb(var(--primary-6));
  background: var(--color-fill-2);
}

.wm-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--color-text-1);
  line-height: 1.3;
}

.wm-title-count {
  font-weight: 500;
  color: var(--color-text-3);
}

.wm-spin {
  display: block;
  width: 100%;
  min-height: 120px;
}

.wm-list {
  border: none;
  border-radius: 0;
  background: transparent;
}

.wm-grid {
  display: grid;
  grid-template-columns:
    minmax(140px, 1.6fr) minmax(160px, 1.8fr) 100px 88px minmax(110px, 1.1fr) minmax(100px, 0.9fr);
  align-items: center;
  column-gap: 12px;
  padding: 0 4px;
}

.wm-grid--header {
  align-items: center;
  min-height: 26px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-3);
  letter-spacing: 0.02em;
  background: transparent;
  padding-bottom: 4px;
}

.wm-col-header-name {
  min-width: 0;
}

.wm-name-header-row {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 0;
  min-height: 22px;
  width: 100%;
}

.wm-th-sort {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: transparent;
  font: inherit;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-3);
  letter-spacing: 0.02em;
  cursor: pointer;
  transition: color 0.12s ease;
}

.wm-th-sort:hover {
  color: var(--color-text-1);
}

.wm-th-sort--name {
  flex: 0 0 auto;
  min-width: 0;
}

.wm-th-sort-text {
  white-space: nowrap;
}

.wm-sort-glyph {
  flex-shrink: 0;
  font-size: 12px;
  opacity: 0.4;
  color: var(--color-text-3);
}

.wm-grid--header .wm-col--sorted .wm-sort-glyph {
  opacity: 1;
  color: rgb(var(--primary-6));
}

.wm-filter-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  margin: 0 0 0 8px;
  padding: 0;
  border: none;
  border-radius: 3px;
  background: transparent;
  color: var(--color-text-3);
  cursor: pointer;
  transition:
    color 0.12s ease,
    background 0.12s ease;
}

.wm-filter-btn :deep(svg) {
  width: 13px;
  height: 13px;
}

.wm-filter-btn:hover {
  color: var(--color-text-1);
  background: var(--color-fill-2);
}

.wm-filter-btn--active {
  color: rgb(var(--primary-6));
  background: transparent;
}

.wm-filter-btn--active:hover {
  color: rgb(var(--primary-7));
  background: transparent;
}

.wm-name-filter-wrap {
  flex: 0 1 200px;
  width: 200px;
  max-width: 100%;
  margin-left: 6px;
}

.wm-name-filter-input {
  width: 100%;
  border-radius: 4px;
}

/* 表头内联：压扁 mini 输入，覆盖全局 14px 字号 */
.wm-name-filter-input.arco-input-wrapper {
  min-height: 22px !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
}

.wm-name-filter-input.arco-input-wrapper .arco-input {
  font-size: 12px !important;
  line-height: 20px !important;
  height: 20px !important;
  min-height: 20px !important;
  padding-top: 0 !important;
  padding-bottom: 0 !important;
}

.wm-name-filter-input.arco-input-wrapper .arco-input-clear-btn {
  font-size: 12px;
}

/* 覆盖全局 input 聚焦主色与光晕（style.css 使用 !important） */
.wm-name-filter-input.arco-input-wrapper:hover {
  border-color: var(--color-border-2) !important;
  box-shadow: none !important;
}

.wm-name-filter-input.arco-input-wrapper:focus-within,
.wm-name-filter-input.arco-input-wrapper.arco-input-focus {
  border-color: var(--color-border-2) !important;
  box-shadow: none !important;
}

.wm-grid--row {
  min-height: 44px;
  transition: background-color 0.12s ease;
}

.wm-grid--row:hover {
  background: var(--color-fill-1);
}

.wm-col {
  min-width: 0;
  font-size: 13px;
}

.wm-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}

.wm-avatar {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(145deg, rgb(var(--primary-5)), rgb(var(--primary-7)));
  letter-spacing: 0.02em;
}

.wm-name-text {
  min-width: 0;
}

.wm-name-primary {
  font-weight: 500;
  color: var(--color-text-1);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wm-email-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-1);
}

.wm-email-cell--empty {
  color: var(--color-text-3);
}

.wm-dim {
  color: var(--color-text-2);
}

.wm-teams {
  color: var(--color-text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wm-last-seen {
  font-size: 13px;
}

.wm-online {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-2);
  font-weight: 500;
}

.wm-online-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success);
  flex-shrink: 0;
}

.wm-relative {
  cursor: default;
  color: var(--color-text-2);
  border-bottom: 1px dashed var(--color-border-2);
}

.wm-role-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
}

.wm-role-pill--owner {
  background: var(--color-warning-light);
  color: var(--color-warning-active);
}

.wm-role-pill--admin {
  background: var(--color-primary-lighter);
  color: var(--color-primary-active);
}

.wm-role-pill--member {
  background: var(--color-fill-2);
  color: var(--color-text-2);
}

.wm-role-pill--unknown {
  background: var(--color-fill-2);
  color: var(--color-text-3);
}

.wm-sentinel {
  height: 1px;
  width: 100%;
  pointer-events: none;
}

.wm-load-more {
  display: flex;
  justify-content: center;
  padding: 16px 0 8px;
}

.wm-empty-wrap {
  padding: 48px 16px;
  display: flex;
  justify-content: center;
}

.wm-empty-wrap--in-list {
  padding: 32px 16px 48px;
}
</style>
