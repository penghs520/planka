import type { ListViewDefinition } from '@/types/view'

/** 与可见性旧字段对齐，保存前写入 viewVisibilityScope / shared */
export function syncVisibilityForSave(d: ListViewDefinition): void {
  const scope =
    d.viewVisibilityScope ?? (d.shared === false ? 'PRIVATE' : 'WORKSPACE')
  d.viewVisibilityScope = scope
  d.shared = scope !== 'PRIVATE'
}

/** 列表分页配置不在表单中编辑时补全默认值 */
export function ensureListViewPageDefaults(d: ListViewDefinition): void {
  if (!d.pageConfig) {
    d.pageConfig = {
      defaultPageSize: 20,
      pageSizeOptions: [10, 20, 50, 100],
      enableVirtualScroll: false,
    }
    return
  }
  if (!Array.isArray(d.pageConfig.pageSizeOptions) || d.pageConfig.pageSizeOptions.length === 0) {
    d.pageConfig.pageSizeOptions = [10, 20, 50, 100]
  }
}
