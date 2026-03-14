/**
 * 时间单位配置 composable
 * 默认使用分钟作为单位
 */

export enum DurationUnit {
  MINUTE = 'MINUTE',
  HOUR = 'HOUR'
}

/**
 * 将分钟转换为指定单位的值
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

import { ref, computed } from 'vue'

export function useDurationUnit() {
  const durationUnit = ref<DurationUnit>(DurationUnit.MINUTE)
  const loading = ref(false)

  /**
   * 单位显示文本
   */
  const unitLabel = computed(() => getUnitLabel(durationUnit.value))

  /**
   * 转换分钟为当前单位的值
   */
  function formatDuration(minutes: number): string {
    const value = convertFromMinutes(minutes, durationUnit.value)
    return `${value} ${unitLabel.value}`
  }

  return {
    durationUnit,
    unitLabel,
    loading,
    formatDuration,
  }
}
