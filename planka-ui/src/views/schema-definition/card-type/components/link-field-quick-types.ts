/** 新建「关联类型」字段时，第二步中「快速创建实体关系」表单状态（当前类型恒为源侧） */
export interface LinkFieldQuickCreateState {
  /** 对侧实体类型 */
  peerCardTypeIds: string[]
  targetName: string
  targetCode: string
  description: string
  /** 源侧多选，固定 false */
  sourceMultiSelect: boolean
  /** 对侧是否多选，由第一步入口决定，表单内不可改 */
  targetMultiSelect: boolean
}

export function defaultLinkFieldQuickCreateState(targetMultiSelect: boolean): LinkFieldQuickCreateState {
  return {
    peerCardTypeIds: [],
    targetName: '',
    targetCode: '',
    description: '',
    sourceMultiSelect: false,
    targetMultiSelect,
  }
}
