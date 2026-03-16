import { ref, computed, type Ref } from 'vue'
import { conditionApi, type ConditionDisplayInfo, type CardDisplayInfo } from '@/api/condition'
import type { Condition } from '@/types/condition'
import type { EnumOption } from '@/types/field'

/**
 * 条件显示信息 Composable
 *
 * 用于加载和管理条件中所有需要显示名称的 ID -> Name 映射。
 * 页面加载时调用一次，后续编辑时使用本地状态更新（不再请求接口）。
 */
export function useConditionDisplayInfo(
  condition: Ref<Condition | null | undefined>
) {
  /**
   * 显示信息状态
   */
  const displayInfo = ref<ConditionDisplayInfo>({
    fieldNames: {},
    linkFieldNames: {},
    enumOptions: {},
    cards: {},
    statusNames: {},
  })

  /**
   * 加载状态
   */
  const loading = ref(false)

  /**
   * 是否已初始化
   */
  const initialized = ref(false)

  /**
   * 错误信息
   */
  const error = ref<string | null>(null)

  /**
   * 加载条件显示信息
   *
   * 页面加载时调用一次
   */
  async function loadDisplayInfo() {
    // 已初始化、正在加载或条件为空时不再请求
    if (initialized.value || loading.value || !condition.value?.root) {
      return
    }

    loading.value = true
    error.value = null

    try {
      const result = await conditionApi.getDisplayInfo(condition.value)
      displayInfo.value = result
      initialized.value = true
    } catch (e) {
      console.error('[useConditionDisplayInfo] Failed to load display info:', e)
      error.value = e instanceof Error ? e.message : 'Unknown error'
    } finally {
      loading.value = false
    }
  }

  // ==================== 获取方法 ====================

  /**
   * 获取字段名称
   * @param fieldId 字段 ID
   * @returns 字段名称，如果未找到则返回 fieldId
   */
  function getFieldName(fieldId: string): string {
    return displayInfo.value.fieldNames[fieldId] || fieldId
  }

  /**
   * 获取关联属性名称
   * @param linkFieldId 关联属性 ID
   * @returns 关联属性名称，如果未找到则返回 linkFieldId
   */
  function getLinkFieldName(linkFieldId: string): string {
    return displayInfo.value.linkFieldNames[linkFieldId] || linkFieldId
  }

  /**
   * 获取枚举选项
   * @param fieldId 枚举字段 ID
   * @returns 枚举选项列表
   */
  function getEnumOptions(fieldId: string): EnumOption[] {
    return displayInfo.value.enumOptions[fieldId] || []
  }

  /**
   * 获取卡片信息
   * @param cardId 卡片 ID
   * @returns 卡片显示信息
   */
  function getCardInfo(cardId: string): CardDisplayInfo | undefined {
    return displayInfo.value.cards[cardId]
  }

  /**
   * 获取状态名称
   * @param statusId 状态 ID
   * @returns 状态名称，如果未找到则返回 statusId
   */
  function getStatusName(statusId: string): string {
    return displayInfo.value.statusNames[statusId] || statusId
  }

  // ==================== 本地更新方法 ====================

  /**
   * 添加字段名称到缓存
   * @param fieldId 字段 ID
   * @param name 字段名称
   */
  function addFieldName(fieldId: string, name: string) {
    displayInfo.value.fieldNames[fieldId] = name
  }

  /**
   * 添加关联属性名称到缓存
   * @param linkFieldId 关联属性 ID
   * @param name 关联属性名称
   */
  function addLinkFieldName(linkFieldId: string, name: string) {
    displayInfo.value.linkFieldNames[linkFieldId] = name
  }

  /**
   * 添加枚举选项到缓存
   * @param fieldId 枚举字段 ID
   * @param options 枚举选项列表
   */
  function addEnumOptions(fieldId: string, options: EnumOption[]) {
    displayInfo.value.enumOptions[fieldId] = options
  }

  /**
   * 添加卡片信息到缓存
   * @param card 卡片显示信息
   */
  function addCard(card: CardDisplayInfo) {
    displayInfo.value.cards[card.id] = card
  }

  /**
   * 添加状态名称到缓存
   * @param statusId 状态 ID
   * @param name 状态名称
   */
  function addStatusName(statusId: string, name: string) {
    displayInfo.value.statusNames[statusId] = name
  }

  // ==================== 批量更新方法 ====================

  /**
   * 批量添加字段名称到缓存
   * @param names 字段名称映射
   */
  function addFieldNames(names: Record<string, string>) {
    Object.assign(displayInfo.value.fieldNames, names)
  }

  /**
   * 批量添加关联属性名称到缓存
   * @param names 关联属性名称映射
   */
  function addLinkFieldNames(names: Record<string, string>) {
    Object.assign(displayInfo.value.linkFieldNames, names)
  }

  /**
   * 重置状态
   */
  function reset() {
    displayInfo.value = {
      fieldNames: {},
      linkFieldNames: {},
      enumOptions: {},
      cards: {},
      statusNames: {},
    }
    initialized.value = false
    error.value = null
  }

  /**
   * 是否已有任何数据
   */
  const hasData = computed(() => {
    const info = displayInfo.value
    return (
      Object.keys(info.fieldNames).length > 0 ||
      Object.keys(info.linkFieldNames).length > 0 ||
      Object.keys(info.enumOptions).length > 0 ||
      Object.keys(info.cards).length > 0 ||
      Object.keys(info.statusNames).length > 0
    )
  })

  return {
    // 状态
    displayInfo,
    loading,
    initialized,
    error,
    hasData,

    // 加载方法
    loadDisplayInfo,

    // 获取方法
    getFieldName,
    getLinkFieldName,
    getEnumOptions,
    getCardInfo,
    getStatusName,

    // 本地更新方法
    addFieldName,
    addLinkFieldName,
    addEnumOptions,
    addCard,
    addStatusName,
    addFieldNames,
    addLinkFieldNames,

    // 重置方法
    reset,
  }
}

/**
 * 条件显示信息上下文类型
 */
export type ConditionDisplayInfoContext = ReturnType<typeof useConditionDisplayInfo>

/**
 * Provide/Inject Key
 */
export const CONDITION_DISPLAY_INFO_KEY = Symbol('conditionDisplayInfo')
