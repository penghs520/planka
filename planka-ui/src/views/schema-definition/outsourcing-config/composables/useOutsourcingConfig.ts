/**
 * 考勤配置管理 Composable
 *
 * 提供配置的加载、保存、重置等功能
 */

import { ref, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import { useOrgStore } from '@/stores/org'
import { outsourcingConfigApi } from '@/api/outsourcing-config'
import { outsourcingConfigSchema } from '@/types/outsourcing-config.schema'
import type { OutsourcingConfig } from '@/types/outsourcing-config'
import {
  DurationUnit,
  LeaveUnit,
  OvertimeCalWay,
  WindowUnit,
  SettlementMethod
} from '@/types/outsourcing-config'

/**
 * 获取默认配置
 */
function getDefaultConfig(orgId: string): OutsourcingConfig {
  return {
    orgId,
    name: '考勤配置',
    durationUnit: DurationUnit.MINUTE,
    decimalScale: 0,
    cardAttendanceRequired: false,

    // 签到配置默认值
    attendanceConf: {
      workStart: '08:30',
      workEnd: '17:30',
      lunchStart: '11:30',
      lunchEnd: '13:30',
      workDuration: 8.0,
      impactWm: false,
      accumulatedOvertime: true,
      absenceWhenNoSignInOrOut: false
    },

    // 请假配置默认值
    leaveConf: {
      limitRules: [],
      leaveUnit: LeaveUnit.HALF_DAY,
      enabledLeaveTypes: []
    },

    // 加班配置默认值
    overtimeConf: {
      calWay: OvertimeCalWay.ACTUAL_ATTENDANCE,
      startDuration: 0,
      limitRules: [],
      nonWorkOvertime: {
        limit: undefined
      },
      calRule: {
        rules: []
      }
    },

    // 补卡配置默认值
    attendanceChangeConf: {
      count: 5,
      window: 10,
      windowUnit: WindowUnit.CALENDAR_DAY,
      allowWeekendOrHoliday: false,
      signIn: {
        start: '',
        end: ''
      },
      signOut: {
        start: '',
        end: ''
      }
    },

    // 结算配置默认值
    settlementConf: {
      method: SettlementMethod.MANUAL,
      absenteeismDeductionCoefficient: 2,
      durationUnit: DurationUnit.MINUTE,
      decimalScale: 0,
      vutIds: [],
      personalServiceFeeConf: {
        baseFeeFieldId: undefined,
        overtimeFeeFieldId: undefined,
        subsidyFieldId: undefined
      },
      projectServiceFeeConf: {
        columns: []
      }
    }
  }
}

/**
 * 考勤配置管理 Hook
 */
export function useOutsourcingConfig() {
  const orgStore = useOrgStore()
  const orgId = computed(() => orgStore.currentOrgId || '')

  // 状态
  const config = ref<OutsourcingConfig>(getDefaultConfig(orgId.value))
  const originalConfig = ref<OutsourcingConfig | null>(null)
  const loading = ref(false)
  const saving = ref(false)

  // 是否有未保存的更改
  const hasUnsavedChanges = computed(() => {
    if (!originalConfig.value) return false
    return JSON.stringify(config.value) !== JSON.stringify(originalConfig.value)
  })

  /**
   * 加载配置
   */
  async function loadConfig() {
    console.log('[useOutsourcingConfig] loadConfig start, orgId:', orgId.value)
    if (!orgId.value) {
      Message.error('组织ID不能为空')
      loading.value = false
      return
    }

    loading.value = true
    console.log('[useOutsourcingConfig] loading set to true')
    try {
      const data = await outsourcingConfigApi.getByOrgId(orgId.value)
      console.log('[useOutsourcingConfig] config loaded:', data)

      // 如果配置不存在（返回 null），使用默认配置
      if (data === null) {
        console.log('配置不存在，使用默认配置')
        config.value = getDefaultConfig(orgId.value)
        originalConfig.value = null
        return
      }

      // 清理可能存在的旧字段
      if ('memberFilterId' in data) {
        delete (data as any).memberFilterId
      }

      config.value = data
      originalConfig.value = JSON.parse(JSON.stringify(data))
    } catch (error: any) {
      console.error('[useOutsourcingConfig] loadConfig error:', error)
      // 如果配置不存在（404），使用默认配置
      if (error.status === 404 || error.code === 'DATA_NOT_FOUND') {
        console.log('配置不存在（错误），使用默认配置')
        config.value = getDefaultConfig(orgId.value)
        originalConfig.value = null
      } else {
        Message.error(error.message || '加载配置失败')
        throw error
      }
    } finally {
      loading.value = false
      console.log('[useOutsourcingConfig] loading set to false')
    }
  }

  /**
   * 保存配置
   */
  async function saveConfig() {
    if (!orgId.value) {
      Message.error('组织ID不能为空')
      return
    }

    // 校验配置
    const result = outsourcingConfigSchema.safeParse(config.value)
    if (!result.success) {
      const firstError = result.error.issues[0]
      const errorMessage = firstError?.message || '配置校验失败'
      Message.error(errorMessage)
      throw new Error(errorMessage)
    }

    saving.value = true
    try {
      // 创建一个干净的配置副本，移除可能存在的旧字段
      const configToSave = JSON.parse(JSON.stringify(config.value))
      // 删除可能存在的旧字段 memberFilterId
      if ('memberFilterId' in configToSave) {
        delete configToSave.memberFilterId
      }
      // 确保包含必要的 schema 字段
      if (!configToSave.schemaType) {
        configToSave.schemaType = 'OUTSOURCING_CONFIG'
      }
      if (!configToSave.schemaSubType) {
        configToSave.schemaSubType = 'OUTSOURCING_CONFIG'
      }

      const saved = await outsourcingConfigApi.saveOrUpdate(orgId.value, configToSave)
      config.value = saved
      originalConfig.value = JSON.parse(JSON.stringify(saved))
      Message.success('保存成功')
    } catch (error: any) {
      const errorMessage = error.message || '保存失败'
      Message.error(errorMessage)
      throw error
    } finally {
      saving.value = false
    }
  }

  /**
   * 重置配置
   */
  function resetConfig() {
    if (originalConfig.value) {
      config.value = JSON.parse(JSON.stringify(originalConfig.value))
    } else {
      config.value = getDefaultConfig(orgId.value)
    }
    Message.info('已重置')
  }

  /**
   * 验证配置
   */
  function validateConfig(): { valid: boolean; errors: string[] } {
    const result = outsourcingConfigSchema.safeParse(config.value)
    if (result.success) {
      return { valid: true, errors: [] }
    }

    const errors = result.error.issues.map((err) => {
      const path = err.path.join('.')
      return `${path}: ${err.message}`
    })

    return { valid: false, errors }
  }

  return {
    // 状态
    config,
    originalConfig,
    loading,
    saving,
    hasUnsavedChanges,

    // 方法
    loadConfig,
    saveConfig,
    resetConfig,
    validateConfig,
    getDefaultConfig: () => getDefaultConfig(orgId.value)
  }
}
