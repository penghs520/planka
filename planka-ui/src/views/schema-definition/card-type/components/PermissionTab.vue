<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { cardTypeApi } from '@/api'
import { permissionConfigApi } from '@/api/permission-config'
import type { PermissionConfig, PermissionConfigDefinition } from '@/types/permission'
import type { FieldOption } from '@/types/field-option'
import { SchemaSubType } from '@/types/schema'
import { EntityState } from '@/types/common'
import CardOperationPermissionPanel from './permission/CardOperationPermissionPanel.vue'
import FieldPermissionPanel from './permission/FieldPermissionPanel.vue'
import AttachmentPermissionPanel from './permission/AttachmentPermissionPanel.vue'

const { t } = useI18n()

// 历史记录栈（用于撤销）
interface HistorySnapshot {
  config: PermissionConfigDefinition | null
}
const historyStack = ref<HistorySnapshot[]>([])
const MAX_HISTORY = 50

// 原始配置快照
const originalConfig = ref<string>('')

const props = defineProps<{
  /** 卡片类型ID */
  cardTypeId: string
  /** 组织ID */
  orgId: string
}>()

// 权限配置
const permissionConfig = ref<PermissionConfigDefinition | null>(null)

// 字段列表（用于属性权限和附件权限的属性选择）
const fieldList = ref<FieldOption[]>([])
const loading = ref(false)
const saving = ref(false)

// 计算附件类型的属性列表
const attachmentFields = computed(() => {
  return fieldList.value.filter((f) => f.fieldType === 'ATTACHMENT')
})

// 计算普通属性列表（非附件）
const normalFields = computed(() => {
  return fieldList.value.filter((f) => f.fieldType !== 'ATTACHMENT')
})

// 当前配置的权限内容
const currentPermission = computed<PermissionConfig>(() => {
  const config = permissionConfig.value
  if (!config) {
    return {
      cardOperations: [],
      fieldPermissions: [],
      attachmentPermissions: [],
    }
  }
  return {
    cardOperations: config.cardOperations ?? [],
    fieldPermissions: config.fieldPermissions ?? [],
    attachmentPermissions: config.attachmentPermissions ?? [],
  }
})

// 序列化配置用于比较
function serializeConfig(config: PermissionConfigDefinition | null): string {
  if (!config) return ''
  return JSON.stringify({
    cardOperations: config.cardOperations ?? [],
    fieldPermissions: config.fieldPermissions ?? [],
    attachmentPermissions: config.attachmentPermissions ?? [],
  })
}

// 保存原始配置快照
function saveOriginalSnapshot() {
  originalConfig.value = serializeConfig(permissionConfig.value)
}

// 深拷贝配置
function cloneConfig(config: PermissionConfigDefinition | null): PermissionConfigDefinition | null {
  if (!config) return null
  return JSON.parse(JSON.stringify(config))
}

// 保存到历史记录（用于撤销）
function pushToHistory() {
  const snapshot: HistorySnapshot = {
    config: cloneConfig(permissionConfig.value),
  }
  historyStack.value.push(snapshot)
  if (historyStack.value.length > MAX_HISTORY) {
    historyStack.value.shift()
  }
}

// 撤销操作
function undo() {
  if (historyStack.value.length === 0) {
    Message.info(t('admin.cardType.permission.noMoreUndo'))
    return
  }
  const snapshot = historyStack.value.pop()!
  permissionConfig.value = snapshot.config
}

// 加载权限配置
async function loadPermissionConfigs() {
  if (!props.cardTypeId) return

  loading.value = true
  try {
    const configs = await permissionConfigApi.listByCardType(props.cardTypeId)
    permissionConfig.value = configs[0] || null
    // 保存原始快照
    saveOriginalSnapshot()
    // 清空历史记录
    historyStack.value = []
  } catch (error) {
    console.error('Failed to fetch permission configs:', error)
    Message.error(t('admin.cardType.permission.fetchFailed'))
  } finally {
    loading.value = false
  }
}

// 加载字段列表
async function loadFieldList() {
  if (!props.cardTypeId) return

  try {
    fieldList.value = await cardTypeApi.getFieldOptions(props.cardTypeId)
  } catch (error) {
    console.error('Failed to fetch field options:', error)
    Message.error(t('admin.cardType.fetchFieldConfigFailed'))
  }
}

// 创建新的权限配置定义
function createNewConfig(): PermissionConfigDefinition {
  return {
    schemaSubType: SchemaSubType.CARD_PERMISSION,
    orgId: props.orgId,
    name: '组织权限配置',
    enabled: true,
    state: EntityState.ACTIVE,
    contentVersion: 1,
    cardTypeId: props.cardTypeId,
    cardOperations: [],
    fieldPermissions: [],
    attachmentPermissions: [],
  }
}

// 更新当前配置的权限内容
async function updateCurrentPermission(permission: PermissionConfig) {
  // 先保存历史
  pushToHistory()

  if (!permissionConfig.value) {
    permissionConfig.value = createNewConfig()
  }
  permissionConfig.value.cardOperations = permission.cardOperations
  permissionConfig.value.fieldPermissions = permission.fieldPermissions
  permissionConfig.value.attachmentPermissions = permission.attachmentPermissions

  // 自动保存
  await saveCurrentConfig()
}

// 保存当前配置
async function saveCurrentConfig() {
  const config = permissionConfig.value

  if (!config) {
    Message.warning(t('admin.cardType.permission.noConfig'))
    return
  }

  saving.value = true
  try {
    if (config.id) {
      // 更新
      const updated = await permissionConfigApi.update(config.id, config, config.contentVersion)
      permissionConfig.value = updated
    } else {
      // 创建
      const created = await permissionConfigApi.create(config)
      permissionConfig.value = created
    }
    // 保存成功后更新原始快照
    saveOriginalSnapshot()
    Message.success(t('common.message.saveSuccess'))
  } catch (error) {
    console.error('Failed to save permission config:', error)
  } finally {
    saving.value = false
  }
}

// 键盘事件处理
function handleKeydown(e: KeyboardEvent) {
  const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0
  const modifierKey = isMac ? e.metaKey : e.ctrlKey

  if (modifierKey && e.key === 'z' && !e.shiftKey) {
    e.preventDefault()
    undo()
  }
}

// 监听卡片类型ID变化
watch(
  () => props.cardTypeId,
  () => {
    loadPermissionConfigs()
    loadFieldList()
  },
)

onMounted(() => {
  loadPermissionConfigs()
  loadFieldList()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="permission-tab">
    <a-spin :loading="loading" style="width: 100%; display: block;">
      <!-- 权限配置面板 -->
      <a-collapse :default-active-key="['card', 'field', 'attachment']" :bordered="false" style="width: 100%;">
        <!-- 卡片操作权限 -->
        <a-collapse-item key="card" :header="t('admin.cardType.permission.cardOperation')">
          <CardOperationPermissionPanel
            :model-value="currentPermission.cardOperations"
            :card-type-id="cardTypeId"
            :available-fields="fieldList"
            @update:model-value="async (v) => await updateCurrentPermission({ ...currentPermission, cardOperations: v })"
          />
        </a-collapse-item>

        <!-- 属性权限 -->
        <a-collapse-item key="field" :header="t('admin.cardType.permission.fieldPermission')">
          <FieldPermissionPanel
            :model-value="currentPermission.fieldPermissions"
            :card-type-id="cardTypeId"
            :field-list="normalFields"
            :available-fields="fieldList"
            @update:model-value="async (v) => await updateCurrentPermission({ ...currentPermission, fieldPermissions: v })"
          />
        </a-collapse-item>

        <!-- 附件权限 -->
        <a-collapse-item
          key="attachment"
          :header="t('admin.cardType.permission.attachmentPermission')"
        >
          <AttachmentPermissionPanel
            :model-value="currentPermission.attachmentPermissions"
            :card-type-id="cardTypeId"
            :attachment-fields="attachmentFields"
            :available-fields="fieldList"
            @update:model-value="async (v) => await updateCurrentPermission({ ...currentPermission, attachmentPermissions: v })"
          />
        </a-collapse-item>
      </a-collapse>
    </a-spin>
  </div>
</template>

<style scoped>
.permission-tab {
  padding: 0;
  width: 100%;
  height: 100%;
  overflow-y: auto;
}

.permission-tab :deep(.arco-collapse) {
  width: 100%;
}

.permission-tab :deep(.arco-collapse-item) {
  width: 100%;
}

.permission-tab :deep(.arco-collapse-item-content) {
  padding: 4px 8px;
  background-color: transparent !important;
}

.permission-tab :deep(.arco-collapse) {
  border: none !important;
}

.permission-tab :deep(.arco-collapse-item) {
  border: none !important;
}

.permission-tab :deep(.arco-collapse-item-header) {
  border: none !important;
}

.permission-tab :deep(.arco-collapse-item-content) {
  border: none !important;
}

.permission-tab :deep(.arco-collapse-item-content-box) {
  /* Default block behavior */
}

.permission-tab :deep(.arco-empty) {
  width: 100%;
  padding: 40px 0;
}

.permission-tab :deep(.permission-panel) {
  width: 100%;
}

.permission-tab :deep(.rule-list) {
  width: 100%;
}

.permission-tab :deep(.rule-item) {
  width: 100%;
}
</style>
