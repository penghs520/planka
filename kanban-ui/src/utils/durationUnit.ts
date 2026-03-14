/**
 * 时间单位转换工具
 */

export enum DurationUnit {
  MINUTE = 'MINUTE',
  HOUR = 'HOUR'
}

/**
 * 将分钟转换为指定单位的值
 * @param minutes 分钟值（存储值）
 * @param unit 目标单位
 * @returns 转换后的值
 */
export function convertFromMinutes(minutes: number, unit: DurationUnit): number {
  if (!unit) {
    unit = DurationUnit.MINUTE
  }

  switch (unit) {
    case DurationUnit.HOUR:
      return minutes / 60
    case DurationUnit.MINUTE:
    default:
      return minutes
  }
}

/**
 * 将指定单位的值转换为分钟（用于存储）
 * @param value 值
 * @param unit 单位
 * @returns 分钟值
 */
export function convertToMinutes(value: number, unit: DurationUnit): number {
  if (!unit) {
    unit = DurationUnit.MINUTE
  }

  switch (unit) {
    case DurationUnit.HOUR:
      return value * 60
    case DurationUnit.MINUTE:
    default:
      return value
  }
}

/**
 * 获取单位的显示名称
 * @param unit 单位
 * @returns 显示名称
 */
export function getUnitLabel(unit: DurationUnit): string {
  if (!unit) {
    return '分钟'
  }

  switch (unit) {
    case DurationUnit.HOUR:
      return '小时'
    case DurationUnit.MINUTE:
    default:
      return '分钟'
  }
}

/**
 * 获取单位的显示名称（英文）
 * @param unit 单位
 * @returns 显示名称
 */
export function getUnitLabelEn(unit: DurationUnit): string {
  if (!unit) {
    return 'minutes'
  }

  switch (unit) {
    case DurationUnit.HOUR:
      return 'hours'
    case DurationUnit.MINUTE:
    default:
      return 'minutes'
  }
}
