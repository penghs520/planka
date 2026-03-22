import { SchemaSubType } from '@/types/schema'

/**
 * 工作区「新建视图」可选类型（与后端 AbstractViewDefinition / schemaSubType 扩展对齐）
 */
export type WorkspaceCreateViewKind = 'LIST' | 'PLANKA' | 'GANTT'

export interface WorkspaceCreateViewTypeOption {
  kind: WorkspaceCreateViewKind
  /** 是否可在当前版本创建 */
  enabled: boolean
  /** 创建成功后写入的 schemaSubType（仅 enabled 时有效） */
  schemaSubType: SchemaSubType
  /** 后端视图类型标识，如 LIST / planka */
  viewType: string
  titleKey: string
  descriptionKey: string
}

export const WORKSPACE_CREATE_VIEW_TYPE_OPTIONS: readonly WorkspaceCreateViewTypeOption[] = [
  {
    kind: 'LIST',
    enabled: true,
    schemaSubType: SchemaSubType.LIST_VIEW,
    viewType: 'LIST',
    titleKey: 'common.viewTypeName.list',
    descriptionKey: 'sidebar.createViewTypeListHelp',
  },
  {
    kind: 'PLANKA',
    enabled: false,
    schemaSubType: SchemaSubType.LIST_VIEW,
    viewType: 'planka',
    titleKey: 'common.viewTypeName.planka',
    descriptionKey: 'sidebar.createViewTypeComingSoon',
  },
  {
    kind: 'GANTT',
    enabled: false,
    schemaSubType: SchemaSubType.LIST_VIEW,
    viewType: 'gantt',
    titleKey: 'common.viewTypeName.gantt',
    descriptionKey: 'sidebar.createViewTypeComingSoon',
  },
] as const
