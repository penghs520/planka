import type { SchemaDefinition } from './schema'
import { SchemaSubType } from './schema'
import { EntityState } from './common'

// ==================== 枚举定义 ====================

/**
 * 动作类别
 */
export enum ActionCategory {
  /** 生命周期操作 */
  LIFECYCLE = 'LIFECYCLE',
  /** 状态切换操作 */
  STATE_TOGGLE = 'STATE_TOGGLE',
  /** 自定义操作 */
  CUSTOM = 'CUSTOM',
}

/**
 * 内置动作类型
 */
export enum BuiltInActionType {
  /** 丢弃 */
  DISCARD = 'DISCARD',
  /** 归档 */
  ARCHIVE = 'ARCHIVE',
  /** 还原 */
  RESTORE = 'RESTORE',
  /** 阻塞/解阻 */
  BLOCK_TOGGLE = 'BLOCK_TOGGLE',
  /** 点亮/暂停 */
  HIGHLIGHT_TOGGLE = 'HIGHLIGHT_TOGGLE',
}

/**
 * 执行类型标识
 */
export enum ExecutionTypeEnum {
  UPDATE_CARD = 'UPDATE_CARD',
  CREATE_LINKED_CARD = 'CREATE_LINKED_CARD',
  CALL_EXTERNAL_API = 'CALL_EXTERNAL_API',
  NAVIGATE_TO_PAGE = 'NAVIGATE_TO_PAGE',
  TRIGGER_BUILT_IN = 'TRIGGER_BUILT_IN',
}

/**
 * 赋值类型标识
 */
export enum AssignmentTypeEnum {
  USER_INPUT = 'USER_INPUT',
  FIXED_VALUE = 'FIXED_VALUE',
  REFERENCE_FIELD = 'REFERENCE_FIELD',
  CURRENT_TIME = 'CURRENT_TIME',
  CLEAR_VALUE = 'CLEAR_VALUE',
  INCREMENT = 'INCREMENT',
}

/**
 * 固定值类型标识
 */
export enum FixedValueTypeEnum {
  TEXT = 'TEXT',
  NUMBER = 'NUMBER',
  DATE = 'DATE',
  ENUM = 'ENUM',
  LINK = 'LINK',
}

/**
 * 引用来源
 */
export enum ReferenceSource {
  /** 当前操作用户 */
  CURRENT_USER = 'CURRENT_USER',
  /** 当前卡片 */
  CURRENT_CARD = 'CURRENT_CARD',
}

/**
 * 日期模式
 */
export enum DateMode {
  ABSOLUTE = 'ABSOLUTE',
  RELATIVE = 'RELATIVE',
}

// ==================== 固定值类型 ====================

/**
 * 固定值基类
 */
export interface FixedValueBase {
  valueType: FixedValueTypeEnum
}

/**
 * 文本值
 */
export interface TextValue extends FixedValueBase {
  valueType: FixedValueTypeEnum.TEXT
  text: string
}

/**
 * 数字值
 */
export interface NumberValue extends FixedValueBase {
  valueType: FixedValueTypeEnum.NUMBER
  number: number
}

/**
 * 日期值
 */
export interface DateValue extends FixedValueBase {
  valueType: FixedValueTypeEnum.DATE
  mode: DateMode
  absoluteDate?: string
  offsetDays?: number
}

/**
 * 枚举值
 */
export interface EnumValue extends FixedValueBase {
  valueType: FixedValueTypeEnum.ENUM
  enumValueIds: string[]
}

/**
 * 关联值（含人员）
 */
export interface LinkValue extends FixedValueBase {
  valueType: FixedValueTypeEnum.LINK
  ids: string[]
}

export type FixedValue = TextValue | NumberValue | DateValue | EnumValue | LinkValue

// ==================== 字段赋值策略 ====================

/**
 * 字段赋值基类
 */
export interface FieldAssignmentBase {
  assignmentType: AssignmentTypeEnum
  fieldId: string
}

/**
 * 用户输入赋值
 */
export interface UserInputAssignment extends FieldAssignmentBase {
  assignmentType: AssignmentTypeEnum.USER_INPUT
  placeholder?: string
  required?: boolean
}

/**
 * 固定值赋值
 */
export interface FixedValueAssignment extends FieldAssignmentBase {
  assignmentType: AssignmentTypeEnum.FIXED_VALUE
  value: FixedValue
}

/**
 * 关联路径
 */
export interface Path {
  linkNodes: string[]
}

/**
 * 字段引用赋值
 */
export interface ReferenceFieldAssignment extends FieldAssignmentBase {
  assignmentType: AssignmentTypeEnum.REFERENCE_FIELD
  source: ReferenceSource
  sourceFieldId?: string
  path?: Path
  appendMode?: boolean
}

/**
 * 当前时间赋值
 */
export interface CurrentTimeAssignment extends FieldAssignmentBase {
  assignmentType: AssignmentTypeEnum.CURRENT_TIME
  offsetDays?: number
}

/**
 * 清空值赋值
 */
export interface ClearValueAssignment extends FieldAssignmentBase {
  assignmentType: AssignmentTypeEnum.CLEAR_VALUE
}

/**
 * 数值增量赋值
 */
export interface IncrementAssignment extends FieldAssignmentBase {
  assignmentType: AssignmentTypeEnum.INCREMENT
  incrementValue: number
  allowNegative?: boolean
}

export type FieldAssignment =
  | UserInputAssignment
  | FixedValueAssignment
  | ReferenceFieldAssignment
  | CurrentTimeAssignment
  | ClearValueAssignment
  | IncrementAssignment

// ==================== 执行类型 ====================

/**
 * 执行类型基类
 */
export interface ActionExecutionTypeBase {
  type: ExecutionTypeEnum
}

/**
 * 更新卡片执行
 */
export interface UpdateCardExecution extends ActionExecutionTypeBase {
  type: ExecutionTypeEnum.UPDATE_CARD
  fieldAssignments?: FieldAssignment[]
  targetStatusId?: string
}

/**
 * 新建关联卡片执行
 */
export interface CreateLinkedCardExecution extends ActionExecutionTypeBase {
  type: ExecutionTypeEnum.CREATE_LINKED_CARD
  linkTypeId?: string
  targetCardTypeId?: string
  /** 标题模板，支持 ${fieldId} 和 ${$title} 等表达式 */
  titleTemplate?: string
  fieldAssignments?: FieldAssignment[]
  showCreateDialog?: boolean
}

/**
 * HTTP 方法
 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'

/**
 * 调用外部接口执行
 */
export interface CallExternalApiExecution extends ActionExecutionTypeBase {
  type: ExecutionTypeEnum.CALL_EXTERNAL_API
  url: string
  method?: HttpMethod
  headers?: Record<string, string>
  bodyTemplate?: string
  timeoutMs?: number
  waitForResponse?: boolean
}

/**
 * 跳转页面执行
 */
export interface NavigateToPageExecution extends ActionExecutionTypeBase {
  type: ExecutionTypeEnum.NAVIGATE_TO_PAGE
  targetUrl: string
  urlParams?: Record<string, string>
  openInNewWindow?: boolean
}

/**
 * 触发内置操作执行
 */
export interface TriggerBuiltInExecution extends ActionExecutionTypeBase {
  type: ExecutionTypeEnum.TRIGGER_BUILT_IN
  builtInActionType: BuiltInActionType
}

export type ActionExecutionType =
  | UpdateCardExecution
  | CreateLinkedCardExecution
  | CallExternalApiExecution
  | NavigateToPageExecution
  | TriggerBuiltInExecution

// ==================== 卡片动作配置定义 ====================

/**
 * 卡片动作配置定义
 */
export interface CardActionConfigDefinition extends SchemaDefinition {
  schemaSubType: SchemaSubType.CARD_ACTION_CONFIG
  /** 所属卡片类型 ID */
  cardTypeId: string
  /** 动作类别 */
  actionCategory?: ActionCategory
  /** 是否内置动作 */
  builtIn?: boolean
  /** 内置动作类型 */
  builtInActionType?: BuiltInActionType
  /** 图标 */
  icon?: string
  /** 颜色 */
  color?: string
  /** 执行类型（自定义动作） */
  executionType?: ActionExecutionType
  /** 确认提示 */
  confirmMessage?: string
  /** 成功提示 */
  successMessage?: string
}

// ==================== 动作执行结果 ====================

/**
 * 执行结果类型
 */
export enum ActionExecutionResultType {
  SUCCESS = 'SUCCESS',
  NAVIGATE = 'NAVIGATE',
  REQUIRE_INPUT = 'REQUIRE_INPUT',
  ERROR = 'ERROR',
}

/**
 * 需要用户输入的字段定义
 */
export interface RequiredInput {
  /** 字段ID */
  fieldId: string
  /** 字段标签（显示名称） */
  label: string
  /** 输入类型（text, textarea, number, date, enum 等） */
  inputType: string
  /** 输入提示文字 */
  placeholder?: string
  /** 是否必填 */
  required?: boolean
}

/**
 * 动作执行结果
 */
export interface ActionExecutionResult {
  type: ActionExecutionResultType
  message?: string
  data?: unknown
  navigateUrl?: string
  openInNewWindow?: boolean
  /** 需要用户输入的字段列表（当 type=REQUIRE_INPUT 时） */
  requiredInputs?: RequiredInput[]
}

// ==================== 辅助函数 ====================

/**
 * 创建空的卡片动作配置
 */
export function createEmptyCardAction(
  orgId: string,
  cardTypeId: string,
): CardActionConfigDefinition {
  return {
    schemaSubType: SchemaSubType.CARD_ACTION_CONFIG,
    orgId,
    name: '',
    enabled: true,
    contentVersion: 1,
    state: EntityState.ACTIVE,
    cardTypeId,
    actionCategory: ActionCategory.CUSTOM,
    builtIn: false,
  }
}

/**
 * 动作类别显示配置
 */
export const ActionCategoryConfig: Record<ActionCategory, { label: string }> = {
  [ActionCategory.LIFECYCLE]: { label: '生命周期' },
  [ActionCategory.STATE_TOGGLE]: { label: '状态切换' },
  [ActionCategory.CUSTOM]: { label: '自定义' },
}

/**
 * 内置动作类型显示配置
 */
export const BuiltInActionTypeConfig: Record<BuiltInActionType, { label: string; icon?: string }> = {
  [BuiltInActionType.DISCARD]: { label: '丢弃', icon: 'icon-delete' },
  [BuiltInActionType.ARCHIVE]: { label: '归档', icon: 'icon-archive' },
  [BuiltInActionType.RESTORE]: { label: '还原', icon: 'icon-undo' },
  [BuiltInActionType.BLOCK_TOGGLE]: { label: '阻塞/解阻', icon: 'icon-stop' },
  [BuiltInActionType.HIGHLIGHT_TOGGLE]: { label: '点亮/暂停', icon: 'icon-highlight' },
}

/**
 * 执行类型显示配置
 */
export const ExecutionTypeConfig: Record<ExecutionTypeEnum, { label: string; description: string }> = {
  [ExecutionTypeEnum.UPDATE_CARD]: { label: '更新卡片', description: '修改当前卡片的字段值或状态' },
  [ExecutionTypeEnum.CREATE_LINKED_CARD]: { label: '新建关联卡片', description: '创建新卡片并与当前卡片建立关联' },
  [ExecutionTypeEnum.CALL_EXTERNAL_API]: { label: '调用外部接口', description: '发送 HTTP 请求到外部系统' },
  [ExecutionTypeEnum.NAVIGATE_TO_PAGE]: { label: '跳转页面', description: '导航到指定的页面 URL' },
  [ExecutionTypeEnum.TRIGGER_BUILT_IN]: { label: '触发内置操作', description: '执行系统内置的操作' },
}

/**
 * 赋值类型显示配置
 */
export const AssignmentTypeConfig: Record<AssignmentTypeEnum, { label: string; description: string }> = {
  [AssignmentTypeEnum.USER_INPUT]: { label: '用户输入', description: '执行时弹窗让用户输入值' },
  [AssignmentTypeEnum.FIXED_VALUE]: { label: '固定值', description: '设置预定义的常量值' },
  [AssignmentTypeEnum.REFERENCE_FIELD]: { label: '字段引用', description: '引用当前用户或卡片字段的值' },
  [AssignmentTypeEnum.CURRENT_TIME]: { label: '当前时间', description: '设置为执行时的当前时间' },
  [AssignmentTypeEnum.CLEAR_VALUE]: { label: '清空值', description: '将字段置空' },
  [AssignmentTypeEnum.INCREMENT]: { label: '数值增量', description: '数字字段加减运算' },
}
