/**
 * 业务规则类型定义
 */
import type { Condition } from './condition'

/**
 * 触发事件类型
 */
export enum TriggerEvent {
  /** 创建时 */
  ON_CREATE = 'ON_CREATE',
  /** 丢弃时 */
  ON_DISCARD = 'ON_DISCARD',
  /** 归档时 */
  ON_ARCHIVE = 'ON_ARCHIVE',
  /** 还原时 */
  ON_RESTORE = 'ON_RESTORE',
  /** 状态向前移动时 */
  ON_STATUS_MOVE = 'ON_STATUS_MOVE',
  /** 状态向后回滚时 */
  ON_STATUS_ROLLBACK = 'ON_STATUS_ROLLBACK',
  /** 字段变更时 */
  ON_FIELD_CHANGE = 'ON_FIELD_CHANGE',
  /** 定时触发 */
  ON_SCHEDULE = 'ON_SCHEDULE',
}

/**
 * 动作类型
 */
export enum RuleActionType {
  DISCARD_CARD = 'DISCARD_CARD',
  ARCHIVE_CARD = 'ARCHIVE_CARD',
  RESTORE_CARD = 'RESTORE_CARD',
  MOVE_CARD = 'MOVE_CARD',
  UPDATE_CARD = 'UPDATE_CARD',
  CREATE_CARD = 'CREATE_CARD',
  CREATE_LINKED_CARD = 'CREATE_LINKED_CARD',
  COMMENT_CARD = 'COMMENT_CARD',
  SEND_NOTIFICATION = 'SEND_NOTIFICATION',
  TRACK_USER_BEHAVIOR = 'TRACK_USER_BEHAVIOR',
  CALL_EXTERNAL_API = 'CALL_EXTERNAL_API',
}

/**
 * 目标类型
 */
export enum ActionTargetType {
  /** 当前触发规则的卡片 */
  CURRENT_CARD = 'CURRENT_CARD',
  /** 通过关联路径找到的卡片 */
  LINKED_CARD = 'LINKED_CARD',
}

/**
 * 目标选择器
 */
export interface ActionTargetSelector {
  targetType: ActionTargetType
  linkPath?: { linkNodes: string[] }
  filterCondition?: Condition
}

/**
 * 文本表达式模板 - 使用字符串形式，支持 ${card.fieldId}、${member.fieldId}、${system.currentTime} 等表达式
 */
export type TextExpressionTemplate = string

/**
 * 接收者选择类型
 */
export enum RecipientSelectorType {
  CURRENT_OPERATOR = 'CURRENT_OPERATOR',
  FIXED_MEMBERS = 'FIXED_MEMBERS',
  FROM_FIELD = 'FROM_FIELD',
  CARD_WATCHERS = 'CARD_WATCHERS',
}

/**
 * 接收者选择器
 */
export interface RecipientSelector {
  selectorType: RecipientSelectorType
  memberIds?: string[]
  fieldId?: string
  linkPath?: { linkNodes: string[] }
}

/**
 * 规则动作基础接口
 */
export interface BaseRuleAction {
  actionType: RuleActionType
  sortOrder?: number
}

/**
 * 丢弃卡片动作
 */
export interface DiscardCardAction extends BaseRuleAction {
  actionType: RuleActionType.DISCARD_CARD
  target?: ActionTargetSelector
  reasonTemplate?: TextExpressionTemplate
}

/**
 * 归档卡片动作
 */
export interface ArchiveCardAction extends BaseRuleAction {
  actionType: RuleActionType.ARCHIVE_CARD
  target?: ActionTargetSelector
}

/**
 * 还原卡片动作
 */
export interface RestoreCardAction extends BaseRuleAction {
  actionType: RuleActionType.RESTORE_CARD
  target?: ActionTargetSelector
}

/**
 * 移动卡片动作
 */
export interface MoveCardAction extends BaseRuleAction {
  actionType: RuleActionType.MOVE_CARD
  target?: ActionTargetSelector
  toStatusId: string
}

/**
 * 修改属性动作
 */
export interface UpdateCardAction extends BaseRuleAction {
  actionType: RuleActionType.UPDATE_CARD
  target?: ActionTargetSelector
  fieldAssignments?: any[] // 复用现有 FieldAssignment
}

/**
 * 创建卡片动作
 */
export interface CreateCardAction extends BaseRuleAction {
  actionType: RuleActionType.CREATE_CARD
  cardTypeId: string
  titleTemplate?: TextExpressionTemplate
  initialStatusId?: string
  fieldAssignments?: any[]
}

/**
 * 创建关联卡片动作
 */
export interface CreateLinkedCardAction extends BaseRuleAction {
  actionType: RuleActionType.CREATE_LINKED_CARD
  linkFieldId: string
  cardTypeId: string
  titleTemplate?: TextExpressionTemplate
  initialStatusId?: string
  fieldAssignments?: any[]
}

/**
 * 评论卡片动作
 */
export interface CommentCardAction extends BaseRuleAction {
  actionType: RuleActionType.COMMENT_CARD
  target?: ActionTargetSelector
  contentTemplate: TextExpressionTemplate
}

/**
 * 发送通知动作
 */
export interface SendNotificationAction extends BaseRuleAction {
  actionType: RuleActionType.SEND_NOTIFICATION
  /** 通知模板ID列表（多选） */
  templateIds: string[]
}

/**
 * 用户行为追踪动作
 */
export interface TrackUserBehaviorAction extends BaseRuleAction {
  actionType: RuleActionType.TRACK_USER_BEHAVIOR
  behaviorType: string
  properties?: Record<string, TextExpressionTemplate>
}

/**
 * 调用外部API动作
 */
export interface CallExternalApiAction extends BaseRuleAction {
  actionType: RuleActionType.CALL_EXTERNAL_API
  urlTemplate: TextExpressionTemplate
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  headers?: Record<string, string>
  bodyTemplate?: TextExpressionTemplate
  timeoutMs?: number
}

/**
 * 规则动作联合类型
 */
export type RuleAction =
  | DiscardCardAction
  | ArchiveCardAction
  | RestoreCardAction
  | MoveCardAction
  | UpdateCardAction
  | CreateCardAction
  | CreateLinkedCardAction
  | CommentCardAction
  | SendNotificationAction
  | TrackUserBehaviorAction
  | CallExternalApiAction

/**
 * 定时调度类型
 */
export enum ScheduleType {
  CRON = 'CRON',
  DAILY = 'DAILY',
  WEEKLY = 'WEEKLY',
  MONTHLY = 'MONTHLY',
}

/**
 * 定时配置
 */
export interface ScheduleConfig {
  scheduleType: ScheduleType
  cronExpression?: string
  executeTime?: string // LocalTime 格式
  daysOfWeek?: number[]
  daysOfMonth?: number[]
  timezone?: string
}

/**
 * 重试配置
 */
export interface RetryConfig {
  maxRetries?: number
  retryIntervalMs?: number
  exponentialBackoff?: boolean
  maxRetryIntervalMs?: number
}

/**
 * 业务规则定义
 */
export interface BizRuleDefinition {
  id?: string
  orgId?: string
  name: string
  description?: string
  schemaSubType?: string
  cardTypeId: string
  triggerEvent: TriggerEvent
  listenFieldList?: string[]
  targetStatusId?: string
  condition?: Condition
  actions?: RuleAction[]
  scheduleConfig?: ScheduleConfig
  enabled?: boolean
  retryConfig?: RetryConfig
  contentVersion?: number
  createdAt?: string
  updatedAt?: string
}

/**
 * 创建空的业务规则定义
 */
export function createEmptyBizRule(cardTypeId: string, orgId: string): BizRuleDefinition {
  return {
    orgId,
    name: '',
    schemaSubType: 'BIZ_RULE',
    cardTypeId,
    triggerEvent: TriggerEvent.ON_CREATE,
    enabled: true,
    retryConfig: {
      maxRetries: 3,
      retryIntervalMs: 1000,
      exponentialBackoff: true,
      maxRetryIntervalMs: 60000,
    },
    actions: [],
  }
}

/**
 * 创建默认的动作目标（当前卡片）
 */
export function createDefaultTarget(): ActionTargetSelector {
  return {
    targetType: ActionTargetType.CURRENT_CARD,
  }
}

/**
 * 校验单个规则动作的必填字段完整性，返回错误 i18n key 或 null
 */
export function validateRuleAction(
  action: RuleAction,
  t: (key: string) => string,
): string | null {
  switch (action.actionType) {
    case RuleActionType.MOVE_CARD: {
      const a = action as MoveCardAction
      if (!a.toStatusId) return t('admin.bizRule.actionConfig.toStatusRequired')
      break
    }
    case RuleActionType.UPDATE_CARD: {
      const a = action as UpdateCardAction
      if (!a.fieldAssignments || a.fieldAssignments.length === 0)
        return t('admin.bizRule.actionConfig.fieldAssignmentsRequired')
      break
    }
    case RuleActionType.CREATE_CARD: {
      const a = action as CreateCardAction
      if (!a.cardTypeId) return t('admin.bizRule.actionConfig.cardTypeIdRequired')
      if (!a.titleTemplate)
        return t('admin.bizRule.actionConfig.titleTemplateRequired')
      break
    }
    case RuleActionType.CREATE_LINKED_CARD: {
      const a = action as CreateLinkedCardAction
      if (!a.linkFieldId) return t('admin.bizRule.actionConfig.linkFieldIdRequired')
      if (!a.cardTypeId) return t('admin.bizRule.actionConfig.cardTypeIdRequired')
      if (!a.titleTemplate)
        return t('admin.bizRule.actionConfig.titleTemplateRequired')
      break
    }
    case RuleActionType.COMMENT_CARD: {
      const a = action as CommentCardAction
      if (!a.contentTemplate)
        return t('admin.bizRule.actionConfig.contentTemplateRequired')
      break
    }
    case RuleActionType.SEND_NOTIFICATION: {
      const a = action as SendNotificationAction
      if (!a.templateIds || a.templateIds.length === 0)
        return t('admin.bizRule.actionConfig.templateIdsRequired')
      break
    }
    case RuleActionType.TRACK_USER_BEHAVIOR: {
      const a = action as TrackUserBehaviorAction
      if (!a.behaviorType) return t('admin.bizRule.actionConfig.behaviorTypeRequired')
      break
    }
    case RuleActionType.CALL_EXTERNAL_API: {
      const a = action as CallExternalApiAction
      if (!a.urlTemplate)
        return t('admin.bizRule.actionConfig.urlTemplateRequired')
      break
    }
    // DISCARD_CARD, ARCHIVE_CARD, RESTORE_CARD 无需额外校验
    default:
      break
  }
  return null
}
