<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconEdit,
  IconDelete,
  IconMore,
  IconCheckCircle,
  IconMinusCircle,
  IconCopy,
  IconHistory,
  IconSearch,
} from '@arco-design/web-vue/es/icon'
import { bizRuleApi } from '@/api/biz-rule'
import CreateButton from '@/components/common/CreateButton.vue'
import BizRuleEditDrawer from './biz-rule/BizRuleEditDrawer.vue'
import RuleExecutionLogPanel from './biz-rule/RuleExecutionLogPanel.vue'
import type { BizRuleDefinition, TriggerEvent } from '@/types/biz-rule'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()

const props = defineProps<{
  cardTypeId: string
  cardTypeName?: string
}>()

const loading = ref(false)
const rules = ref<BizRuleDefinition[]>([])
const drawerVisible = ref(false)
const editingRule = ref<BizRuleDefinition | null>(null)
const logPanelVisible = ref(false)

// 筛选条件
const searchKeyword = ref('')

// 过滤后的规则列表
const filteredRules = computed(() => {
  if (!searchKeyword.value) return rules.value
  return rules.value.filter((rule) =>
    rule.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 加载规则列表
async function fetchRules() {
  if (!props.cardTypeId) return

  loading.value = true
  try {
    rules.value = await bizRuleApi.getByCardTypeId(props.cardTypeId)
  } catch (error: any) {
    console.error('Failed to fetch rules:', error)
    Message.error(error.message || t('admin.message.fetchFailed', { type: t('admin.bizRule.title') }))
  } finally {
    loading.value = false
  }
}

// 新建规则
function handleCreate() {
  editingRule.value = null
  drawerVisible.value = true
}

// 编辑规则
function handleEdit(rule: BizRuleDefinition) {
  editingRule.value = rule
  drawerVisible.value = true
}

// 复制规则
async function handleCopy(rule: BizRuleDefinition) {
  try {
    await bizRuleApi.copy(rule.id!)
    Message.success(t('admin.bizRule.message.copySuccess'))
    await fetchRules()
  } catch (error: any) {
    console.error('Failed to copy rule:', error)
    Message.error(error.message || t('admin.bizRule.message.copyFailed'))
  }
}

// 删除规则
function handleDelete(rule: BizRuleDefinition) {
  Modal.confirm({
    title: t('admin.message.confirmDelete'),
    content: t('admin.bizRule.message.confirmDelete', { name: rule.name }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await bizRuleApi.delete(rule.id!)
        Message.success(t('admin.bizRule.message.deleteSuccess'))
        await fetchRules()
      } catch (error: any) {
        console.error('Failed to delete rule:', error)
        Message.error(error.message || t('admin.bizRule.message.deleteFailed'))
      }
    },
  })
}

// 启用/停用规则
async function handleToggleEnabled(rule: BizRuleDefinition) {
  try {
    if (rule.enabled) {
      await bizRuleApi.disable(rule.id!)
      Message.success(t('admin.bizRule.message.disableSuccess'))
    } else {
      await bizRuleApi.enable(rule.id!)
      Message.success(t('admin.bizRule.message.enableSuccess'))
    }
    await fetchRules()
  } catch (error: any) {
    console.error('Failed to toggle enabled:', error)
    Message.error(error.message || t('admin.bizRule.message.toggleFailed'))
  }
}

// 保存成功后刷新列表
function handleSaveSuccess() {
  drawerVisible.value = false
  fetchRules()
}

// 获取触发事件标签
function getTriggerEventLabel(event: TriggerEvent): string {
  return t(`admin.bizRule.triggerEvent.${event}`)
}

// 获取动作数量标签
function getActionsCountLabel(rule: BizRuleDefinition): string {
  const count = rule.actions?.length || 0
  return t('admin.bizRule.actionsCount', { count })
}

// 监听 cardTypeId 变化
watch(
  () => props.cardTypeId,
  (newId) => {
    if (newId) {
      fetchRules()
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="biz-rules-tab">
    <!-- 头部操作栏 -->
    <div v-if="rules.length > 0" class="tab-header">
      <div class="tab-header-left">
        <a-input
          v-model="searchKeyword"
          :placeholder="t('admin.bizRule.searchPlaceholder')"
          allow-clear
          size="small"
          class="filter-search"
        >
          <template #suffix><IconSearch /></template>
        </a-input>
        <a-button type="text" size="small" @click="logPanelVisible = true">
          <template #icon><IconHistory /></template>
          {{ t('admin.bizRule.executionLog.viewButton') }}
        </a-button>
      </div>
      <CreateButton @click="handleCreate">{{ t('admin.bizRule.createButton') }}</CreateButton>
    </div>

    <!-- 规则列表 -->
    <a-spin :loading="loading" class="rule-list">
      <div v-if="rules.length > 0" class="table-wrapper">
        <a-table
          :data="filteredRules"
          :pagination="false"
          :bordered="false"
          row-key="id"
          size="small"
          :scroll="{ y: '100%' }"
        >
        <template #columns>
          <a-table-column :title="t('admin.bizRule.ruleName')" data-index="name" :width="200">
            <template #cell="{ record }">
              <a-link @click="handleEdit(record)">{{ record.name }}</a-link>
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.bizRule.triggerEventLabel')" :width="120">
            <template #cell="{ record }">
              {{ getTriggerEventLabel(record.triggerEvent) }}
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.bizRule.actionsLabel')" :width="80">
            <template #cell="{ record }">
              {{ getActionsCountLabel(record) }}
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.table.status')" :width="80" align="center">
            <template #cell="{ record }">
              <a-tag v-if="record.enabled" color="green" size="small">{{ t('admin.status.enabled') }}</a-tag>
              <a-tag v-else color="gray" size="small">{{ t('admin.status.disabled') }}</a-tag>
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.table.updatedAt')" data-index="updatedAt" :width="140">
            <template #cell="{ record }">
              {{ formatDateTime(record.updatedAt) }}
            </template>
          </a-table-column>
          <a-table-column :title="t('admin.table.operations')" :width="80" fixed="right">
            <template #cell="{ record }">
              <a-dropdown trigger="click">
                <a-button type="text" size="small">
                  <template #icon><IconMore /></template>
                </a-button>
                <template #content>
                  <a-doption @click="handleEdit(record)">
                    <IconEdit /> {{ t('admin.action.edit') }}
                  </a-doption>
                  <a-doption @click="handleCopy(record)">
                    <IconCopy /> {{ t('admin.action.copy') }}
                  </a-doption>
                  <a-doption @click="handleToggleEnabled(record)">
                    <IconMinusCircle v-if="record.enabled" />
                    <IconCheckCircle v-else />
                    {{ record.enabled ? t('admin.action.disable') : t('admin.action.enable') }}
                  </a-doption>
                  <a-divider style="margin: 4px 0" />
                  <a-doption
                    class="danger-option"
                    @click="handleDelete(record)"
                  >
                    <IconDelete /> {{ t('admin.action.delete') }}
                  </a-doption>
                </template>
              </a-dropdown>
            </template>
          </a-table-column>
        </template>
      </a-table>
      </div>

      <div v-else class="empty-state">
        <a-empty :description="t('admin.bizRule.emptyDescription')" />
        <CreateButton @click="handleCreate">{{ t('admin.bizRule.createButton') }}</CreateButton>
      </div>
    </a-spin>

    <!-- 编辑抽屉 -->
    <BizRuleEditDrawer
      v-model:visible="drawerVisible"
      :card-type-id="cardTypeId"
      :rule="editingRule"
      @success="handleSaveSuccess"
    />

    <!-- 执行日志面板 -->
    <RuleExecutionLogPanel
      v-model:visible="logPanelVisible"
      :card-type-id="cardTypeId"
    />
  </div>
</template>

<style scoped lang="scss">
.biz-rules-tab {
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.tab-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-search {
  width: 200px;
}

.rule-list {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.rule-list :deep(.arco-spin-children) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.rule-list :deep(.arco-table) {
  height: 100%;
}

.rule-list :deep(.arco-table-container) {
  height: 100%;
}

.rule-list :deep(.arco-table-body) {
  height: 100%;
}

.table-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px 0;
}

:deep(.danger-option) {
  color: rgb(var(--danger-6));
}
</style>
