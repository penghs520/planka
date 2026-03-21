import request from './request'

export interface SidebarPreferencesDTO {
  pinnedStructureIds: string[]
}

export const sidebarPreferencesApi = {
  get(): Promise<SidebarPreferencesDTO> {
    return request.get('/api/v1/users/me/sidebar-preferences')
  },

  update(body: { pinnedStructureIds: string[] }): Promise<SidebarPreferencesDTO> {
    return request.put('/api/v1/users/me/sidebar-preferences', body)
  },
}
