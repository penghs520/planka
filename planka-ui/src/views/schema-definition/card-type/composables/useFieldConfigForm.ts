import { ref, computed, type Ref, type ComputedRef } from 'vue'
import { Message } from '@arco-design/web-vue'
import { structureApi, linkTypeApi, fieldOptionsApi } from '@/api'
import type { StructureDefinition, StructureLevel } from '@/types/structure'
import type { MatchingLinkFieldDTO } from '@/types/card-type'
import type { LinkPosition } from '@/types/link-type'
import type { EnumOptionDTO } from '@/types/view-data'

/** 层级绑定 */
export interface LevelBinding {
  /** 层级索引 */
  levelIndex: number
  /** 层级名称 */
  levelName?: string
  /** 层级对应的卡片类型名称列表 */
  levelCardTypeNames?: string[]
  /** 关联属性ID */
  linkFieldId?: string
  /** 关联属性名称 */
  linkFieldName?: string
  /** 是否必填 */
  required: boolean
}

/** 自动创建选项的特殊标识 */
export const AUTO_CREATE_VALUE = '__AUTO_CREATE__'

export interface UseFieldConfigFormOptions {
  cardTypeId: Ref<string>
  structureId: Ref<string | undefined>
  fieldName: Ref<string | undefined>
  t: (key: string, params?: Record<string, unknown>) => string
}

export interface UseFieldConfigFormReturn {
  // 架构线相关
  structureList: Ref<StructureDefinition[]>
  loadingStructures: Ref<boolean>
  currentStructureLevels: Ref<StructureLevel[]>
  levelBindings: Ref<LevelBinding[]>
  matchingLinksCache: Ref<Record<number, MatchingLinkFieldDTO[]>>
  creatingLinkLevelIndex: Ref<number | null>

  // 方法
  loadStructureList: () => Promise<void>
  loadMatchingLinks: () => Promise<void>
  handleStructureChange: (structureId: string) => Promise<void>
  getLevelLinkOptions: (levelIndex: number) => Array<{ label: string; value: string }>
  getLevelDisplayName: (record: LevelBinding) => string
  setLinkFieldId: (record: LevelBinding, value: string) => Promise<void>
  updateRequired: (record: LevelBinding, value: boolean) => void
  isCreatingLink: (levelIndex: number) => boolean
}

/**
 * 属性配置表单的共享逻辑
 */
export function useFieldConfigForm(options: UseFieldConfigFormOptions): UseFieldConfigFormReturn {
  const { cardTypeId, fieldName, t } = options

  // 架构线相关状态
  const structureList = ref<StructureDefinition[]>([])
  const loadingStructures = ref(false)
  const currentStructureLevels = ref<StructureLevel[]>([])
  const levelBindings = ref<LevelBinding[]>([])
  const matchingLinksCache = ref<Record<number, MatchingLinkFieldDTO[]>>({})
  const creatingLinkLevelIndex = ref<number | null>(null)

  /** 加载架构线列表 */
  async function loadStructureList(): Promise<void> {
    loadingStructures.value = true
    try {
      const result = await structureApi.list(1, 100)
      structureList.value = result.content || []
    } catch (error) {
      console.error('Failed to load structure list:', error)
      structureList.value = []
    } finally {
      loadingStructures.value = false
    }
  }

  /** 加载匹配的关联属性 */
  async function loadMatchingLinks(): Promise<void> {
    const cardTypeIdValue = cardTypeId.value
    if (!cardTypeIdValue) return

    for (const level of currentStructureLevels.value) {
      if (level.cardTypeIds && level.cardTypeIds.length > 0) {
        try {
          const res = await fieldOptionsApi.getMatchingLinkFields(
            [cardTypeIdValue],
            level.cardTypeIds
          )
          matchingLinksCache.value[level.index] = res.fields
        } catch (error) {
          console.error(`Failed to fetch matching links for level ${level.index}:`, error)
        }
      }
    }
  }

  /** 处理架构线变更 */
  async function handleStructureChange(newStructureId: string): Promise<void> {
    if (!newStructureId) {
      currentStructureLevels.value = []
      levelBindings.value = []
      matchingLinksCache.value = {}
      return
    }

    try {
      const structure = await structureApi.getById(newStructureId)
      currentStructureLevels.value = structure.levels || []

      // 初始化层级绑定配置
      levelBindings.value = structure.levels.map((level) => ({
        levelIndex: level.index,
        levelName: level.name,
        linkFieldId: undefined,
        required: false,
      }))

      // 加载匹配的关联属性
      await loadMatchingLinks()
    } catch (error) {
      console.error('Failed to fetch structure details:', error)
    }
  }

  /** 获取层级显示名称 */
  function getLevelDisplayName(record: LevelBinding): string {
    const level = currentStructureLevels.value.find((l) => l.index === record.levelIndex)
    if (level?.name) {
      return level.name
    }
    if (record.levelName) {
      return record.levelName
    }
    return t('admin.cardType.fieldConfig.levelIndex', { index: record.levelIndex + 1 })
  }

  /** 获取层级的关联选项 */
  function getLevelLinkOptions(levelIndex: number): Array<{ label: string; value: string }> {
    const fields = matchingLinksCache.value[levelIndex] || []
    const result = fields.map((f) => ({
      label: f.name || `${f.linkTypeId}:${f.linkPosition}`,
      value: `${f.linkTypeId}:${f.linkPosition}`,
    }))

    // 添加"自动创建"选项
    result.push({
      label: t('admin.cardType.fieldConfig.autoCreateLinkType'),
      value: AUTO_CREATE_VALUE,
    })

    return result
  }

  /** 自动创建关联类型 */
  async function handleAutoCreateLinkType(record: LevelBinding): Promise<void> {
    const levelIndex = record.levelIndex
    const level = currentStructureLevels.value.find((l) => l.index === levelIndex)
    if (!level) {
      Message.error('无法找到层级信息')
      return
    }

    const sourceCardTypeId = cardTypeId.value
    const targetCardTypeIds = level.cardTypeIds

    if (!targetCardTypeIds || targetCardTypeIds.length === 0) {
      Message.error('层级未配置卡片类型')
      return
    }

    creatingLinkLevelIndex.value = levelIndex

    try {
      const linkTypeName = `${fieldName.value || '架构'}-${level.name || `层级${levelIndex + 1}`}`
      const linkType = await linkTypeApi.create({
        name: linkTypeName,
        sourceName: `${level.name || `层级${levelIndex + 1}`}`,
        targetName: `关联卡片`,
        sourceCardTypeIds: [sourceCardTypeId],
        targetCardTypeIds: targetCardTypeIds,
        sourceMultiSelect: false,
        targetMultiSelect: true,
        sourceVisible: true,
        targetVisible: false,
      })

      record.linkFieldId = `${linkType.id}:SOURCE`

      const newField: MatchingLinkFieldDTO = {
        id: `${linkType.id}:SOURCE`,
        name: linkType.sourceName,
        linkTypeId: linkType.id,
        linkPosition: 'SOURCE' as LinkPosition,
        multiple: false,
        code: '',
        systemField: false,
        sortOrder: 0,
      }
      if (!matchingLinksCache.value[levelIndex]) {
        matchingLinksCache.value[levelIndex] = []
      }
      matchingLinksCache.value[levelIndex].push(newField)

      Message.success(`已自动创建关联类型: ${linkType.sourceName}`)
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : '未知错误'
      Message.error(`创建关联类型失败: ${errorMessage}`)
    } finally {
      creatingLinkLevelIndex.value = null
    }
  }

  /** 设置关联属性 */
  async function setLinkFieldId(record: LevelBinding, value: string): Promise<void> {
    if (!value) {
      record.linkFieldId = undefined
      return
    }

    if (value === AUTO_CREATE_VALUE) {
      await handleAutoCreateLinkType(record)
      return
    }

    record.linkFieldId = value
  }

  /** 更新必填设置 */
  function updateRequired(record: LevelBinding, value: boolean): void {
    record.required = value
  }

  /** 判断是否正在创建关联类型 */
  function isCreatingLink(levelIndex: number): boolean {
    return creatingLinkLevelIndex.value === levelIndex
  }

  return {
    structureList,
    loadingStructures,
    currentStructureLevels,
    levelBindings,
    matchingLinksCache,
    creatingLinkLevelIndex,
    loadStructureList,
    loadMatchingLinks,
    handleStructureChange,
    getLevelLinkOptions,
    getLevelDisplayName,
    setLinkFieldId,
    updateRequired,
    isCreatingLink,
  }
}

export interface UseEnumOptionsOptions {
  enumOptions: Ref<EnumOptionDTO[]>
}

export interface UseEnumOptionsReturn {
  localEnumOptions: ComputedRef<EnumOptionDTO[]>
  dragOptions: { animation: number; handle: string; ghostClass: string }
  addEnumOption: () => void
  removeEnumOption: (index: number) => void
  updateEnumOption: (index: number, key: keyof EnumOptionDTO, value: unknown) => void
  setEnumOptions: (options: EnumOptionDTO[]) => void
}

/**
 * 枚举选项管理的共享逻辑
 */
export function useEnumOptions(options: UseEnumOptionsOptions): UseEnumOptionsReturn {
  const { enumOptions } = options

  const dragOptions = {
    animation: 200,
    handle: '.drag-handle',
    ghostClass: 'enum-option-ghost',
  }

  const localEnumOptions = computed({
    get: () => enumOptions.value,
    set: (val: EnumOptionDTO[]) => {
      enumOptions.value = val.map((opt, index) => ({
        ...opt,
        order: index,
      }))
    },
  })

  function addEnumOption(): void {
    const options = enumOptions.value || []
    const newOption: EnumOptionDTO = {
      id: `opt_${Date.now()}`,
      value: '',
      label: '',
      order: options.length,
      enabled: true,
    }
    enumOptions.value = [...options, newOption]
  }

  function removeEnumOption(index: number): void {
    const options = [...enumOptions.value]
    options.splice(index, 1)
    enumOptions.value = options
  }

  function updateEnumOption(index: number, key: keyof EnumOptionDTO, value: unknown): void {
    const options = [...enumOptions.value]
    if (!options[index]) return
    options[index] = { ...options[index], [key]: value } as EnumOptionDTO
    enumOptions.value = options
  }

  function setEnumOptions(newOptions: EnumOptionDTO[]): void {
    enumOptions.value = newOptions.map((opt, index) => ({
      ...opt,
      order: index,
    }))
  }

  return {
    localEnumOptions,
    dragOptions,
    addEnumOption,
    removeEnumOption,
    updateEnumOption,
    setEnumOptions,
  }
}
