import type { EnumOption } from './field'

/**
 * 属性选项（用于列配置、条件编辑器等场景的属性选择）
 * <p>
 * 对于关联属性，id 格式为 "{linkTypeId}:{SOURCE|TARGET}"，已包含方向信息。
 */
export interface FieldOption {
  /**
   * 属性 ID
   * <p>
   * 对于关联属性，格式为 "{linkTypeId}:{SOURCE|TARGET}"
   */
  id: string

  /** 属性名称 */
  name: string

  /** 属性类型（SINGLE_LINE_TEXT, NUMBER, DATE, ENUM, LINK 等） */
  fieldType: string

  /** 排序顺序 */
  sortOrder?: number

  /** 是否必填 */
  required: boolean

  /** 属性编码 */
  code?: string

  /** 是否为系统字段 */
  systemField: boolean

  /** 枚举选项列表（仅枚举类型字段有效） */
  enumOptions?: EnumOption[]

  /**
   * 目标卡片类型ID列表（仅关联类型字段有效）
   * <p>
   * 对于 LINK 类型字段，包含该关联属性可以链接到的目标卡片类型ID。
   * SOURCE 端返回 TARGET 端卡片类型ID，TARGET 端返回 SOURCE 端卡片类型ID。
   */
  targetCardTypeIds?: string[]

  /** 架构线ID（仅架构类型字段有效） */
  structureId?: string

  /** 是否多选（仅关联类型字段有效） */
  multiple?: boolean
}

/**
 * 共同属性选项响应
 */
export interface CommonFieldOptionResponse {
  /** 共同属性列表 */
  fields: FieldOption[]
}
