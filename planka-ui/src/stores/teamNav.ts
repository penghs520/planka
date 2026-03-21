import { defineStore } from 'pinia'
import { ref, shallowRef, watch } from 'vue'
import type { CardDTO } from '@/types/card'
import { fetchMyTeams, fetchProjectsForTeam } from '@/api/team'
import { useOrgStore } from '@/stores/org'

/**
 * 侧栏「你的团队」与团队下项目缓存
 */
export const useTeamNavStore = defineStore('teamNav', () => {
  const orgStore = useOrgStore()
  const myTeams = shallowRef<CardDTO[]>([])
  /** teamCardId -> projects */
  const teamProjects = shallowRef<Record<string, CardDTO[]>>({})
  const loading = ref(false)

  async function refreshMyTeams() {
    const orgId = orgStore.currentOrgId
    const mid = orgStore.currentMemberCardId
    if (!orgId || !mid) {
      myTeams.value = []
      teamProjects.value = {}
      return
    }
    loading.value = true
    try {
      myTeams.value = await fetchMyTeams(orgId, mid)
    } finally {
      loading.value = false
    }
  }

  async function ensureTeamProjects(teamCardId: string) {
    const orgId = orgStore.currentOrgId
    const mid = orgStore.currentMemberCardId
    if (!orgId || !mid || teamProjects.value[teamCardId]) {
      return
    }
    const list = await fetchProjectsForTeam(orgId, mid, teamCardId)
    teamProjects.value = { ...teamProjects.value, [teamCardId]: list }
  }

  watch(
    () => [orgStore.currentOrgId, orgStore.currentMemberCardId] as const,
    () => {
      myTeams.value = []
      teamProjects.value = {}
      void refreshMyTeams()
    },
  )

  return {
    myTeams,
    teamProjects,
    loading,
    refreshMyTeams,
    ensureTeamProjects,
  }
})
