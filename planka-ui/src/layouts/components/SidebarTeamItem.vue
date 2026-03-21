<script setup lang="ts">
import { ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { IconDown } from '@arco-design/web-vue/es/icon'
import type { CardDTO } from '@/types/card'
import { getCardTitle } from '@/types/card'
import { useTeamNavStore } from '@/stores/teamNav'

const props = defineProps<{
  team: CardDTO
}>()

const { t } = useI18n()
const route = useRoute()
const teamNav = useTeamNavStore()
const expanded = ref(false)

watch(expanded, (open) => {
  if (open) {
    void teamNav.ensureTeamProjects(props.team.id)
  }
})

function toggle() {
  expanded.value = !expanded.value
}

function projectActive(projectId: string) {
  return route.path.startsWith(`/project/${projectId}`)
}

function teamSubActive(segment: 'issues' | 'projects') {
  return route.path === `/team/${props.team.id}/${segment}`
}
</script>

<template>
  <div class="team-item">
    <button
      type="button"
      class="team-header"
      @click="toggle"
    >
      <IconDown
        class="chevron"
        :class="{ 'chevron--collapsed': !expanded }"
      />
      <span class="team-name">{{ getCardTitle(team) }}</span>
    </button>
    <div
      v-show="expanded"
      class="team-children"
    >
      <RouterLink
        :to="`/team/${team.id}/issues`"
        class="nav-row"
        :class="{ active: teamSubActive('issues') }"
      >
        {{ t('sidebar.teamIssues') }}
      </RouterLink>
      <RouterLink
        :to="`/team/${team.id}/projects`"
        class="nav-row"
        :class="{ active: teamSubActive('projects') }"
      >
        {{ t('sidebar.teamProjects') }}
      </RouterLink>
      <RouterLink
        v-for="p in (teamNav.teamProjects[team.id] || [])"
        :key="p.id"
        :to="`/project/${p.id}/issues`"
        class="nav-row nav-row--project"
        :class="{ active: projectActive(p.id) }"
      >
        {{ getCardTitle(p) }}
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.team-item {
  margin-bottom: 2px;
}

.team-header {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  height: 28px;
  padding: 0 8px;
  margin: 0;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: var(--sidebar-text-primary);
  font-family: var(--sidebar-nav-font-family);
  font-size: var(--sidebar-nav-font-size);
  font-weight: 600;
  cursor: pointer;
  text-align: left;
}

.team-header:hover {
  background: var(--sidebar-bg-hover);
}

.chevron {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
  transition: transform 0.15s ease;
  color: var(--sidebar-text-secondary);
}

.chevron--collapsed {
  transform: rotate(-90deg);
}

.team-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-children {
  padding-left: 6px;
}

.nav-row {
  display: block;
  height: 26px;
  line-height: 26px;
  padding: 0 8px 0 22px;
  border-radius: 5px;
  font-family: var(--sidebar-nav-font-family);
  font-size: var(--sidebar-nav-font-size);
  font-weight: var(--sidebar-nav-font-weight);
  color: var(--sidebar-text-secondary);
  text-decoration: none;
}

.nav-row:hover {
  background: var(--sidebar-bg-hover);
  color: var(--sidebar-text-primary);
}

.nav-row.active {
  background: var(--sidebar-bg-active);
  color: var(--sidebar-text-active);
}

.nav-row--project {
  padding-left: 28px;
}
</style>
