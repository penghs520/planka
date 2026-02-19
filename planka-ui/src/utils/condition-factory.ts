/**
 * 条件工厂函数
 * 用于创建默认的条件对象
 */

import type {
  Condition,
  ConditionGroup,
  ConditionItem,
  ConditionNode,
  NodeTypeValue,
  TextOperator,
  NumberOperator,
  DateOperator,
  EnumOperator,
  WebUrlOperator,
  DateConditionItem,
  LinkOperator,
  LinkConditionItem,
  LinkSubject,
  LinkValue,
  Path,
} from '@/types/condition'
import { NodeType, KeyDate, LifecycleState, SystemDateField, isConditionGroup } from '@/types/condition'

/**
 * 创建空的条件定义
 */
export function createEmptyCondition(): Condition {
  return {
    root: createEmptyConditionGroup(),
  }
}

/**
 * 创建空的条件组
 */
export function createEmptyConditionGroup(operator: 'AND' | 'OR' = 'AND'): ConditionGroup {
  return {
    nodeType: 'GROUP',
    operator,
    children: [],
  }
}

/**
 * 根据节点类型创建默认的条件项
 */
export function createDefaultConditionItem(nodeType: NodeTypeValue): ConditionItem {
  switch (nodeType) {
    case NodeType.TEXT:
    case NodeType.TITLE:
    case NodeType.CODE:
      return {
        nodeType: nodeType as any,
        subject: nodeType === NodeType.TEXT ? { fieldId: '' } : {},
        operator: { type: 'CONTAINS', value: '' },
      }

    case NodeType.NUMBER:
      return {
        nodeType: 'NUMBER',
        subject: { fieldId: '' },
        operator: { type: 'EQ', value: { type: 'STATIC', value: 0 } },
      }

    case NodeType.DATE:
      return {
        nodeType: 'DATE',
        subject: { type: 'FIELD', fieldId: '' },
        operator: { type: 'EQ', value: { type: 'KEY_DATE', keyDate: KeyDate.TODAY } },
      }

    case NodeType.ENUM:
      return {
        nodeType: 'ENUM',
        subject: { fieldId: '' },
        operator: { type: 'EQ', optionId: '' },
      }

    case NodeType.STATUS:
      return {
        nodeType: 'STATUS',
        subject: { streamId: '' },
        operator: { type: 'EQ', statusId: '' },
      }

    case NodeType.WEB_URL:
      return {
        nodeType: 'WEB_URL',
        subject: { fieldId: '' },
        operator: { type: 'IS_NOT_EMPTY' },
      }

    case NodeType.CREATED_BY:
    case NodeType.UPDATED_BY:
      return {
        nodeType: nodeType as any,
        subject: {},
        operator: { type: 'IS_CURRENT_USER' },
      }

    case NodeType.CARD_CYCLE:
      return {
        nodeType: 'CARD_CYCLE',
        subject: {},
        operator: { type: 'IN', values: [LifecycleState.ACTIVE] },
      }

    case NodeType.LINK:
      return {
        nodeType: 'LINK',
        subject: { linkFieldId: '' },
        operator: { type: 'HAS_ANY' },
      }

    default:
      // 默认返回TEXT类型
      return {
        nodeType: 'TEXT',
        subject: { fieldId: '' },
        operator: { type: 'CONTAINS', value: '' },
      }
  }
}

/**
 * 根据字段类型获取对应的条件节点类型
 */
export function getNodeTypeByFieldType(schemaSubType: string): NodeTypeValue {
  const mapping: Record<string, NodeTypeValue> = {
    // 字段配置类型映射（使用新的 _FIELD 后缀）
    TEXT_FIELD: NodeType.TEXT,
    MULTI_LINE_TEXT_FIELD: NodeType.TEXT,
    MARKDOWN_FIELD: NodeType.TEXT,
    NUMBER_FIELD: NodeType.NUMBER,
    DATE_FIELD: NodeType.DATE,
    ENUM_FIELD: NodeType.ENUM,
    WEB_URL_FIELD: NodeType.WEB_URL,
    ATTACHMENT_FIELD: NodeType.TEXT, // 附件当作文本处理
    STRUCTURE_FIELD: NodeType.TEXT, // 结构当作文本处理
    LINK_FIELD: NodeType.LINK, // 关联字段

    // 字段定义类型映射
    SINGLE_LINE_TEXT_FIELD_DEFINITION: NodeType.TEXT,
    MULTI_LINE_TEXT_FIELD_DEFINITION: NodeType.TEXT,
    MARKDOWN_FIELD_DEFINITION: NodeType.TEXT,
    NUMBER_FIELD_DEFINITION: NodeType.NUMBER,
    DATE_FIELD_DEFINITION: NodeType.DATE,
    ENUM_FIELD_DEFINITION: NodeType.ENUM,
    WEB_URL_FIELD_DEFINITION: NodeType.WEB_URL,
    ATTACHMENT_FIELD_DEFINITION: NodeType.TEXT,
    STRUCTURE_FIELD_DEFINITION: NodeType.TEXT,
  }

  return mapping[schemaSubType] || NodeType.TEXT
}

/**
 * 创建默认的文本操作符
 */
export function createDefaultTextOperator(): TextOperator {
  return { type: 'CONTAINS', value: '' }
}

/**
 * 创建默认的数字操作符
 */
export function createDefaultNumberOperator(): NumberOperator {
  return { type: 'EQ', value: { type: 'STATIC', value: 0 } }
}

/**
 * 创建默认的日期操作符
 */
export function createDefaultDateOperator(): DateOperator {
  return { type: 'EQ', value: { type: 'KEY_DATE', keyDate: KeyDate.TODAY } }
}

/**
 * 创建默认的枚举操作符
 */
export function createDefaultEnumOperator(): EnumOperator {
  return { type: 'EQ', optionId: '' }
}

/**
 * 创建默认的Web URL操作符
 */
export function createDefaultWebUrlOperator(): WebUrlOperator {
  return { type: 'IS_NOT_EMPTY' }
}

/**
 * 创建默认的关联操作符
 */
export function createDefaultLinkOperator(): LinkOperator {
  return { type: 'HAS_ANY' }
}

/**
 * 创建关联条件项
 * @param linkFieldId 关联字段ID，格式为 "{linkTypeId}:{SOURCE|TARGET}"
 * @param path 前置路径（可选）
 * @param operator 操作符（默认 HAS_ANY）
 */
export function createLinkConditionItem(
  linkFieldId: string,
  path?: Path,
  operator: LinkOperator = { type: 'HAS_ANY' }
): LinkConditionItem {
  const subject: LinkSubject = {
    linkFieldId,
  }
  if (path && path.linkNodes.length > 0) {
    subject.path = path
  }
  return {
    nodeType: 'LINK',
    subject,
    operator,
  }
}

/**
 * 创建路径
 * @param linkNodes 关联节点列表（linkFieldId 字符串数组）
 */
export function createPath(linkNodes: string[] = []): Path {
  return { linkNodes }
}

/**
 * 创建关联值（用于 IN/NOT_IN 操作符）
 * @param cardIds 卡片ID列表
 */
export function createLinkValue(cardIds: string[] = []): LinkValue {
  return { type: 'STATIC', cardIds }
}

/**
 * 判断条件是否为空
 */
export function isConditionEmpty(condition?: Condition): boolean {
  if (!condition || !condition.root) {
    return true
  }

  if (condition.root.nodeType === 'GROUP') {
    return condition.root.children.length === 0
  }

  return false
}

/**
 * 判断操作符是否需要值输入
 */
export function operatorNeedsValue(operatorType: string): boolean {
  const noValueOperators = [
    'IS_EMPTY',
    'IS_NOT_EMPTY',
    'HAS_ANY',
    'IS_CURRENT_USER',
    'IS_NOT_CURRENT_USER',
  ]
  return !noValueOperators.includes(operatorType)
}

/**
 * 创建系统日期字段条件项
 * @param systemField 系统日期字段类型
 */
export function createSystemDateConditionItem(systemField: SystemDateField): DateConditionItem {
  return {
    nodeType: 'DATE',
    subject: { type: 'SYSTEM', systemField },
    operator: { type: 'EQ', value: { type: 'KEY_DATE', keyDate: KeyDate.TODAY } },
  }
}

/**
 * 创建自定义日期字段条件项
 * @param fieldId 字段ID
 */
export function createFieldDateConditionItem(fieldId: string): DateConditionItem {
  return {
    nodeType: 'DATE',
    subject: { type: 'FIELD', fieldId },
    operator: { type: 'EQ', value: { type: 'KEY_DATE', keyDate: KeyDate.TODAY } },
  }
}

/**
 * 系统字段的 nodeType 列表（这些字段的 subject 中没有 fieldId）
 */
const SYSTEM_FIELD_NODE_TYPES = ['CARD_CYCLE', 'TITLE', 'CODE', 'CREATED_BY', 'UPDATED_BY']

/**
 * 验证条件项是否完整
 * @returns 如果条件项完整返回 true，否则返回 false
 */
export function isConditionItemComplete(item: ConditionItem): boolean {
  const subject = item.subject as any
  const operator = item.operator as any

  // 1. 验证 subject 是否完整
  if (item.nodeType === 'DATE') {
    // 日期类型：检查 subject.type
    if (subject.type === 'SYSTEM') {
      // 系统日期字段：需要有 systemField
      if (!subject.systemField) return false
    } else if (subject.type === 'FIELD') {
      // 自定义日期字段：需要有 fieldId
      if (!subject.fieldId) return false
    } else {
      // 没有 type，说明是未选择的状态
      return false
    }
  } else if (item.nodeType === 'STATUS') {
    // STATUS 字段：需要有 streamId
    if (!subject.streamId) return false
  } else if (item.nodeType === 'LINK') {
    // LINK 类型：需要有 linkFieldId
    if (!subject.linkFieldId) return false
  } else if (SYSTEM_FIELD_NODE_TYPES.includes(item.nodeType)) {
    // 系统字段：subject 可以为空或只有 path
    // 不需要额外验证
  } else {
    // 其他自定义字段：需要有 fieldId
    if (!subject.fieldId) return false
  }

  // 2. 验证 operator 是否完整（根据操作符类型）
  const opType = operator.type
  if (!opType) return false

  // 不需要值的操作符
  if (!operatorNeedsValue(opType)) {
    return true
  }

  // 需要值的操作符，检查值是否存在且有效
  switch (item.nodeType) {
    case 'TEXT':
    case 'TITLE':
    case 'CODE':
      // 文本类型：值不能为空字符串
      return typeof operator.value === 'string' && operator.value.trim().length > 0
    case 'NUMBER':
      if (opType === 'BETWEEN') {
        return operator.start?.value !== undefined && operator.end?.value !== undefined
      }
      return operator.value?.value !== undefined
    case 'DATE':
      if (opType === 'BETWEEN') {
        // BETWEEN 需要 start 和 end 都完整
        const isStartValid = isDateValueComplete(operator.start)
        const isEndValid = isDateValueComplete(operator.end)
        return isStartValid && isEndValid
      }
      // 单值操作符：检查 value 是否完整
      return isDateValueComplete(operator.value)
    case 'ENUM':
      if (opType === 'IN' || opType === 'NOT_IN') {
        return Array.isArray(operator.optionIds) && operator.optionIds.length > 0
      }
      // EQ/NE：需要有效的 optionId
      return typeof operator.optionId === 'string' && operator.optionId.length > 0
    case 'STATUS':
      if (opType === 'IN' || opType === 'NOT_IN') {
        return Array.isArray(operator.statusIds) && operator.statusIds.length > 0
      }
      return !!operator.statusId
    case 'CARD_CYCLE':
      return Array.isArray(operator.values) && operator.values.length > 0
    case 'CREATED_BY':
    case 'UPDATED_BY':
      if (opType === 'IN' || opType === 'NOT_IN') {
        return Array.isArray(operator.userIds) && operator.userIds.length > 0
      }
      if (opType === 'EQ' || opType === 'NE') {
        return typeof operator.userId === 'string' && operator.userId.trim().length > 0
      }
      return true
    case 'LINK':
      // LINK 类型的 IN/NOT_IN 需要有 cardIds
      if (opType === 'IN' || opType === 'NOT_IN') {
        return (
          operator.value?.type === 'STATIC' &&
          Array.isArray(operator.value.cardIds) &&
          operator.value.cardIds.length > 0
        )
      }
      // HAS_ANY 和 IS_EMPTY 不需要额外的值
      return true
    default:
      return true
  }
}

/**
 * 检查日期值是否完整
 */
function isDateValueComplete(dateValue: any): boolean {
  if (!dateValue) return false

  if (dateValue.type === 'KEY_DATE') {
    // 关键日期：需要有 keyDate
    return !!dateValue.keyDate
  } else if (dateValue.type === 'SPECIFIC') {
    // 具体日期：需要有非空的 value
    return typeof dateValue.value === 'string' && dateValue.value.length > 0
  }

  return false
}

/**
 * 验证条件节点是否完整（递归检查）
 * @returns 如果所有条件都完整返回 true，否则返回 false
 */
export function isConditionNodeComplete(node: ConditionNode): boolean {
  if (isConditionGroup(node)) {
    // 条件组：不能为空，且所有子节点都必须完整
    if (node.children.length === 0) {
      return false
    }
    return node.children.every((child) => isConditionNodeComplete(child))
  } else {
    // 条件项：检查是否完整
    return isConditionItemComplete(node as ConditionItem)
  }
}

/**
 * 验证整个条件是否完整
 * @returns 如果条件为空或所有条件都完整返回 true，否则返回 false
 */
export function isConditionComplete(condition?: Condition | null): boolean {
  // 空条件视为有效
  if (!condition || !condition.root) {
    return true
  }

  // 空条件组视为有效
  if (isConditionGroup(condition.root) && condition.root.children.length === 0) {
    return true
  }

  return isConditionNodeComplete(condition.root)
}

/**
 * 获取不完整的条件项数量（包含空条件组）
 */
export function getIncompleteConditionCount(condition?: Condition | null): number {
  if (!condition || !condition.root) {
    return 0
  }

  function countIncomplete(node: ConditionNode): number {
    if (isConditionGroup(node)) {
      // 空条件组算作一个不完整项
      if (node.children.length === 0) {
        return 1
      }
      return node.children.reduce((sum, child) => sum + countIncomplete(child), 0)
    } else {
      return isConditionItemComplete(node as ConditionItem) ? 0 : 1
    }
  }

  return countIncomplete(condition.root)
}
