<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { IconDown } from '@arco-design/web-vue/es/icon'
import { useOrgStore } from '@/stores/org'
import { useTeamNavStore } from '@/stores/teamNav'
import { cardApi } from '@/api/card'
import { pureTitle } from '@/types/card'
import { teamCardTypeId, teamMemberLinkTypeId } from '@/constants/systemSchemaIds'
import { buildLinkFieldId } from '@/utils/link-field-utils'
import SidebarTeamItem from './SidebarTeamItem.vue'

const { t } = useI18n()
const orgStore = useOrgStore()
const teamNav = useTeamNavStore()
const expanded = ref(true)
const creating = ref(false)

onMounted(() => {
  void teamNav.refreshMyTeams()
})

async function handleCreateTeam() {
  const orgId = orgStore.currentOrgId
  const mid = orgStore.currentMemberCardId
  if (!orgId || !mid) {
    return
  }
  const name = t('sidebar.newTeamDefaultName')
  creating.value = true
  try {
    await cardApi.create({
      orgId,
      typeId: teamCardTypeId(orgId),
      title: pureTitle(name),
      description: '',
      fieldValues: {},
      linkUpdates: [
        {
          linkFieldId: buildLinkFieldId(teamMemberLinkTypeId(orgId), 'SOURCE'),
          targetCardIds: [mid],
        },
      ],
    })
    await teamNav.refreshMyTeams()
  } catch {
    Message.error(t('sidebar.createTeamFailed'))
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <div class="sidebar-your-teams">
    <button
      type="button"
      class="section-header"
      @click="expanded = !expanded"
    >
      <span class="section-title">{{ t('sidebar.yourTeamsSection') }}</span>
      <IconDown
        class="chevron"
        :class="{ 'chevron--collapsed': !expanded }"
      />
    </button>
    <div
      v-show="expanded"
      class="section-body"
    >
      <SidebarTeamItem
        v-for="team in teamNav.myTeams"
        :key="team.id"
        :team="team"
      />
      <button
        type="button"
        class="create-team-btn"
        :disabled="creating || !orgStore.currentMemberCardId"
        @click="handleCreateTeam"
      >
        + {{ t('sidebar.createTeam') }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.sidebar-your-teams {
  padding: 4px 8px 12px;
}

.section-header {
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
  color: var(--sidebar-text-secondary);
  font-family: var(--sidebar-nav-font-family);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  cursor: pointer;
  text-align: left;
}

.section-header:hover {
  background: var(--sidebar-bg-hover);
  color: var(--sidebar-text-primary);
}

.chevron {
  width: 12px;
  height: 12px;
  transition: transform 0.15s ease;
  flex-shrink: 0;
}

.chevron--collapsed {
  transform: rotate(-90deg);
}

.section-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
}

.section-body {
  padding-top: 4px;
}

.create-team-btn {
  display: flex;
  align-items: center;
  width: 100%;
  height: 28px;
  margin-top: 4px;
  padding: 0 8px 0 8px;
  border: none;
  border-radius: 5px;
  background: transparent;
  font-family: var(--sidebar-nav-font-family);
  font-size: var(--sidebar-nav-font-size);
  font-weight: var(--sidebar-nav-font-weight);
  color: var(--sidebar-text-secondary);
  cursor: pointer;
  text-align: left;
}

.create-team-btn:hover:not(:disabled) {
  background: var(--sidebar-bg-hover);
  color: var(--sidebar-accent);
}

.create-team-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
