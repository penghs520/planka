import request from './request'

export interface PluginInfo {
  pluginId: string
  pluginName: string
  pluginState: string
  version: string
  provider: string
}

export interface ChannelInfo {
  channelId: string
  name: string
  version: string
  provider: string
  supportsRichContent: boolean
  supportsAttachment: boolean
  def: ChannelDef
}

export interface ChannelDef {
  id: string
  name: string
  description: string
  version: string
  provider: string
  supportsRichContent: boolean
  supportsAttachment: boolean
  configFields: ConfigField[]
}

export interface ConfigField {
  key: string
  label: string
  type: string
  required: boolean
  defaultValue?: string
  description?: string
  placeholder?: string
  options?: SelectOption[]
}

export interface SelectOption {
  label: string
  value: string
}

export const pluginApi = {
  // 获取插件列表
  list() {
    return request.get<PluginInfo[]>('/api/v1/notification/plugins')
  },

  // 上传插件
  upload(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/api/v1/notification/plugins/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 启动插件
  start(pluginId: string) {
    return request.post(`/api/v1/notification/plugins/${pluginId}/start`)
  },

  // 停止插件
  stop(pluginId: string) {
    return request.post(`/api/v1/notification/plugins/${pluginId}/stop`)
  },

  // 删除插件
  delete(pluginId: string) {
    return request.delete(`/api/v1/notification/plugins/${pluginId}`)
  },

  // 获取可用渠道列表
  listChannels() {
    return request.get<ChannelInfo[]>('/api/v1/notification/plugins/channels')
  },

  // 获取渠道定义
  getChannelDef(channelId: string) {
    return request.get<ChannelDef>(`/api/v1/notification/plugins/channels/${channelId}/def`)
  }
}

export const channelConfigApi = {
  // 测试渠道配置
  test(channelId: string, config: Record<string, any>) {
    return request.post(`/api/v1/schemas/notification-channels/${channelId}/test`, config)
  },

  // 创建渠道配置
  create(data: any) {
    return request.post('/api/v1/schemas/notification-channels', data)
  }
}
