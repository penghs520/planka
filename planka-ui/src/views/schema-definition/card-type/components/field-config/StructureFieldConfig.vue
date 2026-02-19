<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { structureApi, linkTypeApi, fieldOptionsApi } from '@/api'
import { Message } from '@arco-design/web-vue'
import type { StructureDefinition, StructureLevel } from '@/types/structure'
import type { MatchingLinkFieldDTO } from '@/types/card-type'
import type { LinkPosition } from '@/types/link-type'
import type { LevelBinding } from '../../composables/useFieldConfigForm'

const AUTO_CREATE_VALUE = '__AUTO_CREATE__'

interface Props {
  structureId?: string
  levelBindings: LevelBinding[]
  cardTypeId: string
  fieldName?: string
  disabled?: boolean
  /** 是否为只读模式（仅显示，不可编辑架构线和层级绑定） */
  readonly?: boolean
  /** 只读模式下显示的架构线名称 */
  structureName?: string
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  readonly: false,
})

const emit = defineEmits<{
  'update:structureId': [value: string]
  'update:levelBindings': [value: LevelBinding[]]
}>()

const { t } = useI18n()

// 内部状态
const structureList = ref<StructureDefinition[]>([])
const loadingStructures = ref(false)
const currentStructureLevels = ref<StructureLevel[]>([])
const matchingLinksCache = ref<Record<number, MatchingLinkFieldDTO[]>>({})
const creatingLinkLevelIndex = ref<number | null>(null)
const linkTypeNames = ref<Record<string, { sourceName: string; targetName: string }>>({})

const structureIdValue = computed({
  get: () => props.structureId || '',
  set: (value: string) => emit('update:structureId', value),
})

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
  const cardTypeIdValue = props.cardTypeId
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
  emit('update:structureId', newStructureId)

  if (!newStructureId) {
    currentStructureLevels.value = []
    emit('update:levelBindings', [])
    matchingLinksCache.value = {}
    return
  }

  try {
    const structure = await structureApi.getById(newStructureId)
    currentStructureLevels.value = structure.levels || []

    // 初始化层级绑定配置
    const newBindings = structure.levels.map((level: StructureLevel) => ({
      levelIndex: level.index,
      levelName: level.name,
      linkFieldId: undefined,
      required: false,
    }))
    emit('update:levelBindings', newBindings)

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

  const sourceCardTypeId = props.cardTypeId
  const targetCardTypeIds = level.cardTypeIds

  if (!targetCardTypeIds || targetCardTypeIds.length === 0) {
    Message.error('层级未配置卡片类型')
    return
  }

  creatingLinkLevelIndex.value = levelIndex

  try {
    const linkTypeName = `${props.fieldName || '架构'}-${level.name || `层级${levelIndex + 1}`}`
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

    // 更新 levelBindings
    const newBindings: LevelBinding[] = [...props.levelBindings]
    const bindingIndex = newBindings.findIndex((b) => b.levelIndex === levelIndex)
    const existing = newBindings[bindingIndex]
    if (bindingIndex >= 0 && existing) {
      newBindings[bindingIndex] = {
        levelIndex: existing.levelIndex,
        required: existing.required,
        levelName: existing.levelName,
        levelCardTypeNames: existing.levelCardTypeNames,
        linkFieldId: `${linkType.id}:SOURCE`,
        linkFieldName: existing.linkFieldName,
      }
      emit('update:levelBindings', newBindings)
    }

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
  if (value === AUTO_CREATE_VALUE) {
    await handleAutoCreateLinkType(record)
    return
  }

  const newBindings: LevelBinding[] = [...props.levelBindings]
  const bindingIndex = newBindings.findIndex((b) => b.levelIndex === record.levelIndex)
  const existing = newBindings[bindingIndex]
  if (bindingIndex >= 0 && existing) {
    newBindings[bindingIndex] = {
      levelIndex: existing.levelIndex,
      required: existing.required,
      levelName: existing.levelName,
      levelCardTypeNames: existing.levelCardTypeNames,
      linkFieldId: value || undefined,
      linkFieldName: existing.linkFieldName,
    }
    emit('update:levelBindings', newBindings)
  }
}

/** 更新必填设置 */
function updateRequired(record: LevelBinding, value: boolean): void {
  const newBindings: LevelBinding[] = [...props.levelBindings]
  const bindingIndex = newBindings.findIndex((b) => b.levelIndex === record.levelIndex)
  const existing = newBindings[bindingIndex]
  if (bindingIndex >= 0 && existing) {
    newBindings[bindingIndex] = {
      levelIndex: existing.levelIndex,
      required: value,
      levelName: existing.levelName,
      levelCardTypeNames: existing.levelCardTypeNames,
      linkFieldId: existing.linkFieldId,
      linkFieldName: existing.linkFieldName,
    }
    emit('update:levelBindings', newBindings)
  }
}

/** 判断是否正在创建关联类型 */
function isCreatingLink(levelIndex: number): boolean {
  return creatingLinkLevelIndex.value === levelIndex
}

/** 获取关联关系显示名称（只读模式） */
function getLinkRelationDisplayName(record: LevelBinding): string {
  if (!record.linkFieldId) return '-'
  const parts = record.linkFieldId.split(':')
  const linkTypeId = parts[0]
  const position = parts[1] as 'SOURCE' | 'TARGET' | undefined
  if (!linkTypeId) return record.linkFieldId
  const linkTypeInfo = linkTypeNames.value[linkTypeId]
  if (linkTypeInfo) {
    if (position === 'TARGET') {
      return linkTypeInfo.targetName
    }
    return linkTypeInfo.sourceName
  }
  return record.linkFieldId
}

/** 加载关联类型名称（只读模式） */
async function loadLinkTypeNames(): Promise<void> {
  for (const binding of props.levelBindings) {
    if (binding.linkFieldId) {
      const linkTypeId = binding.linkFieldId.split(':')[0]
      if (linkTypeId && !linkTypeNames.value[linkTypeId]) {
        try {
          const linkType = await linkTypeApi.getById(linkTypeId)
          linkTypeNames.value[linkTypeId] = {
            sourceName: linkType.sourceName,
            targetName: linkType.targetName,
          }
        } catch {
          console.error('Failed to load link type:', linkTypeId)
        }
      }
    }
  }
}

// 初始化加载
if (!props.readonly) {
  loadStructureList()
}

// 监听 structureId 变化，加载层级信息
watch(
  () => props.structureId,
  async (newId) => {
    if (newId && !props.readonly) {
      try {
        const structure = await structureApi.getById(newId)
        currentStructureLevels.value = structure.levels || []
        await loadMatchingLinks()
      } catch (error) {
        console.error('Failed to fetch structure details:', error)
      }
    }
  },
  { immediate: true }
)

// 只读模式下加载关联类型名称
watch(
  () => props.levelBindings,
  () => {
    if (props.readonly) {
      loadLinkTypeNames()
    }
  },
  { immediate: true, deep: true }
)
</script>

<template>
  <!-- 架构线选择 -->
  <a-row :gutter="16">
    <a-col :span="24">
      <a-form-item :label="t('admin.cardType.fieldConfig.structureLine')" required>
        <!-- 编辑模式 -->
        <a-select
          v-if="!readonly"
          v-model="structureIdValue"
          :loading="loadingStructures"
          :placeholder="t('admin.cardType.fieldConfig.selectStructureLine')"
          :disabled="disabled"
          allow-clear
          @update:model-value="handleStructureChange"
        >
          <a-option
            v-for="structure in structureList"
            :key="structure.id"
            :value="structure.id"
          >
            {{ structure.name }}
          </a-option>
        </a-select>
        <!-- 只读模式 -->
        <span v-else class="readonly-value">{{ structureName || '-' }}</span>
      </a-form-item>
    </a-col>
  </a-row>

  <!-- 层级绑定配置 -->
  <a-form-item
    v-if="levelBindings.length > 0"
    :label="t('admin.cardType.fieldConfig.levelConfig')"
    :required="!readonly"
  >
    <a-table
      :data="levelBindings"
      :pagination="false"
      size="small"
      :bordered="false"
    >
      <template #columns>
        <a-table-column :title="t('admin.cardType.fieldConfig.levelName')" :width="180">
          <template #cell="{ record }">
            {{ getLevelDisplayName(record) }}
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.cardType.fieldConfig.linkRelation')" :width="220">
          <template #cell="{ record }">
            <!-- 编辑模式 -->
            <a-select
              v-if="!readonly"
              :model-value="record.linkFieldId"
              :options="getLevelLinkOptions(record.levelIndex)"
              :placeholder="t('admin.cardType.fieldConfig.selectLinkType')"
              :loading="isCreatingLink(record.levelIndex)"
              :disabled="disabled"
              size="small"
              allow-search
              style="width: 100%"
              @update:model-value="(val: string) => setLinkFieldId(record, val)"
            />
            <!-- 只读模式 -->
            <span v-else class="readonly-value">{{ getLinkRelationDisplayName(record) }}</span>
          </template>
        </a-table-column>
        <a-table-column :title="t('admin.cardType.fieldConfig.isRequired')" :width="80">
          <template #cell="{ record }">
            <a-switch
              :model-value="record.required"
              size="small"
              :disabled="disabled || readonly"
              @update:model-value="(val: string | number | boolean) => updateRequired(record, Boolean(val))"
            />
          </template>
        </a-table-column>
      </template>
    </a-table>
  </a-form-item>
</template>

<style scoped>
.readonly-value {
  color: var(--color-text-2);
  font-size: 13px;
  line-height: 32px;
}
</style>
