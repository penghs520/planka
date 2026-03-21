import request from './request'
import type { PageResult } from '@/types/api'

export interface WorkspaceMemberRow {
  memberCardId: string
  name: string
  email: string | null
  teamNames: string[]
  role: string | null
  joinedAt: string | null
  lastLoginAt: string | null
}

export type WorkspaceMembersSortField = 'name' | 'email' | 'joined' | 'lastSeen'

export function fetchWorkspaceMembers(params: {
  page?: number
  size?: number
  keyword?: string
  sort?: WorkspaceMembersSortField
  order?: 'asc' | 'desc'
}): Promise<PageResult<WorkspaceMemberRow>> {
  return request.get('/api/v1/view-data/workspace/members', { params })
}
