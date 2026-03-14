import type { SchemaDefinition } from './schema'
import { SchemaSubType } from './schema'
import type { CardTypeInfo } from './field'

/**
 * 时间点数据源类型枚举
 */
export enum TimePointSourceType {
  /** 卡片创建时间 */
  CARD_CREATED_TIME = 'CARD_CREATED_TIME',
  /** 卡片更新时间 */
  CARD_UPDATED_TIME = 'CARD_UPDATED_TIME',
  /** 自定义日期字段的值 */
  CUSTOM_DATE_FIELD = 'CUSTOM_DATE_FIELD',
  /** 卡片进入某个具体价值流状态的时间 */
  STATUS_ENTER_TIME = 'STATUS_ENTER_TIME',
  /** 卡片离开某个具体价值流状态的时间 */
  STATUS_EXIT_TIME = 'STATUS_EXIT_TIME',
  /** 卡片进入当前价值流状态的时间 */
  CURRENT_STATUS_ENTER_TIME = 'CURRENT_STATUS_ENTER_TIME',
  /** 当前时间（仅用于时间段公式的结束时间） */
  CURRENT_TIME = 'CURRENT_TIME',
}

/**
 * 时间段统计精度枚举
 */
export enum TimeRangePrecision {
  /** 按天统计 */
  DAY = 'DAY',
  /** 按小时统计 */
  HOUR = 'HOUR',
  /** 按分钟统计 */
  MINUTE = 'MINUTE',
}

/**
 * 日期汇集方式枚举
 */
export enum DateAggregationType {
  /** 最早日期 */
  EARLIEST = 'EARLIEST',
  /** 最晚日期 */
  LATEST = 'LATEST',
}

/**
 * 卡片汇集方式枚举
 */
export enum CardAggregationType {
  /** 计数（统计关联卡片个数） */
  COUNT = 'COUNT',
  /** 去重计数（统计关联卡片个数，去重） */
  DISTINCT_COUNT = 'DISTINCT_COUNT',
  /** 求和 */
  SUM = 'SUM',
  /** 平均值 */
  AVG = 'AVG',
  /** 最小值 */
  MIN = 'MIN',
  /** 最大值 */
  MAX = 'MAX',
  /** P85分位数 */
  P85 = 'P85',
}

/**
 * 计算公式定义基类
 */
export interface AbstractFormulaDefinition extends SchemaDefinition {
  /** 公式编码 */
  code?: string
  /** 关联的卡片类型 ID 列表（创建/编辑时使用） */
  cardTypeIds?: string[]
  /** 关联的卡片类型列表（列表查询返回） */
  cardTypes?: CardTypeInfo[]
}

/**
 * 时间点公式定义
 */
export interface TimePointFormulaDefinition extends AbstractFormulaDefinition {
  schemaSubType: SchemaSubType.TIME_POINT_FORMULA_DEFINITION
  /** 数据源类型 */
  sourceType: TimePointSourceType
  /** 源日期字段ID */
  sourceFieldId?: string
  /** 价值流ID */
  streamId?: string
  /** 价值流状态ID */
  statusId?: string
}

/**
 * 时间段公式定义
 */
export interface TimeRangeFormulaDefinition extends AbstractFormulaDefinition {
  schemaSubType: SchemaSubType.TIME_RANGE_FORMULA_DEFINITION
  /** 开始时间数据源类型 */
  startSourceType: TimePointSourceType
  /** 开始时间字段ID */
  startFieldId?: string
  /** 开始时间价值流ID */
  startStreamId?: string
  /** 开始时间价值流状态ID */
  startStatusId?: string
  /** 结束时间数据源类型 */
  endSourceType: TimePointSourceType
  /** 结束时间字段ID */
  endFieldId?: string
  /** 结束时间价值流ID */
  endStreamId?: string
  /** 结束时间价值流状态ID */
  endStatusId?: string
  /** 统计精度 */
  precision: TimeRangePrecision
}

/**
 * 日期汇集公式定义
 */
export interface DateCollectionFormulaDefinition extends AbstractFormulaDefinition {
  schemaSubType: SchemaSubType.DATE_COLLECTION_FORMULA_DEFINITION
  /** 关联属性ID */
  linkFieldId: string
  /** 目标卡片类型ID列表 */
  targetCardTypeIds?: string[]
  /** 关联卡片中的源日期字段ID */
  sourceFieldId: string
  /** 汇集方式 */
  aggregationType: DateAggregationType
  /** 过滤条件（JSON格式） */
  filterCondition?: string
}

/**
 * 卡片汇集公式定义
 */
export interface CardCollectionFormulaDefinition extends AbstractFormulaDefinition {
  schemaSubType: SchemaSubType.CARD_COLLECTION_FORMULA_DEFINITION
  /** 关联属性ID */
  linkFieldId: string
  /** 目标卡片类型ID列表 */
  targetCardTypeIds?: string[]
  /** 关联卡片中的源数值字段ID */
  sourceFieldId?: string
  /** 汇集方式 */
  aggregationType: CardAggregationType
  /** 过滤条件（JSON格式） */
  filterCondition?: string
}

/**
 * 数值运算公式定义
 */
export interface NumberCalculationFormulaDefinition extends AbstractFormulaDefinition {
  schemaSubType: SchemaSubType.NUMBER_CALCULATION_FORMULA_DEFINITION
  /** SPEL表达式 */
  expression: string
  /** 结构化表达式定义（JSON格式） */
  expressionStructure?: string
  /** 结果精度（小数位数） */
  precision?: number
}

/**
 * 计算公式定义联合类型
 */
export type FormulaDefinition =
  | TimePointFormulaDefinition
  | TimeRangeFormulaDefinition
  | DateCollectionFormulaDefinition
  | CardCollectionFormulaDefinition
  | NumberCalculationFormulaDefinition
