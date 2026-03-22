<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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
import { cardTypeApi } from '@/api/card-type'
import CreateButton from '@/components/common/CreateButton.vue'
import AdminTable from '@/components/table/AdminTable.vue'
import BizRuleEditDrawer from '../card-type/components/biz-rule/BizRuleEditDrawer.vue'
import RuleExecutionLogPanel from '../card-type/components/biz-rule/RuleExecutionLogPanel.vue'
import type { BizRuleDefinition, TriggerEvent } from '@/types/biz-rule'
import { formatDateTime } from '@/utils/format'

const { t } = useI18n()

const loading = ref(false)
const rules = ref<BizRuleDefinition[]>([])
const cardTypeOptions = ref<{ id: string; name: string; icon?: string; schemaSubType: string }[]>([])

const drawerVisible = ref(false)
const editingRule = ref<BizRuleDefinition | null>(null)
const drawerCardTypeId = ref('')

const logPanelVisible = ref(false)

const searchKeyword = ref('')
const filterCardTypeId = ref<string | undefined>(undefined)

const selectTypeModalVisible = ref(false)
const pendingCreateCardTypeId = ref<string | undefined>(undefined)

const cardTypeNameById = computed(() => {
  const m = new Map<string, string>()
  for (const o of cardTypeOptions.value) {
    m.set(o.id, o.name)
  }
  return m
})

function getCardTypeName(rule: BizRuleDefinition): string {
  return cardTypeNameById.value.get(rule.cardTypeId) ?? rule.cardTypeId
}

const filteredRules = computed(() => {
  let list = rules.value
  if (searchKeyword.value.trim()) {
    const k = searchKeyword.value.trim().toLowerCase()
    list = list.filter((r) => r.name.toLowerCase().includes(k))
  }
  if (filterCardTypeId.value) {
    list = list.filter((r) => r.cardTypeId === filterCardTypeId.value)
  }
  return list
})

async function fetchCardTypeOptions() {
  try {
    cardTypeOptions.value = await cardTypeApi.listOptions()
  } catch (e: any) {
    console.error('Failed to fetch card type options:', e)
    Message.error(e.message || t('admin.message.fetchFailed', { type: t('admin.menu.cardType') }))
  }
}

async function fetchRules() {
  loading.value = true
  try {
    rules.value = await bizRuleApi.list()
  } catch (error: any) {
    console.error('Failed to fetch biz rules:', error)
    Message.error(error.message || t('admin.message.fetchFailed', { type: t('admin.bizRule.title') }))
  } finally {
    loading.value = false
  }
}

function openCreateTypeModal() {
  pendingCreateCardTypeId.value = filterCardTypeId.value
  selectTypeModalVisible.value = true
}

function onBeforeCreateOk(): boolean {
  if (!pendingCreateCardTypeId.value) {
    Message.warning(t('admin.bizRuleDefinition.selectCardTypeRequired'))
    return false
  }
  drawerCardTypeId.value = pendingCreateCardTypeId.value
  editingRule.value = null
  drawerVisible.value = true
  return true
}

function handleEdit(rule: BizRuleDefinition) {
  drawerCardTypeId.value = rule.cardTypeId
  editingRule.value = rule
  drawerVisible.value = true
}

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

function handleSaveSuccess() {
  drawerVisible.value = false
  fetchRules()
}

function getTriggerEventLabel(event: TriggerEvent): string {
  return t(`admin.bizRule.triggerEvent.${event}`)
}

function getActionsCountLabel(rule: BizRuleDefinition): string {
  const count = rule.actions?.length || 0
  return t('admin.bizRule.actionsCount', { count })
}

function openExecutionLog() {
  logPanelVisible.value = true
}

onMounted(async () => {
  await fetchCardTypeOptions()
  await fetchRules()
})
</script>

<template>
  <div class="biz-rule-definition-page">
    <div class="page-toolbar">
      <div class="toolbar-filters">
        <a-input
          v-model="searchKeyword"
          :placeholder="t('admin.bizRuleDefinition.filterNamePlaceholder')"
          allow-clear
          size="small"
          class="filter-name"
        >
          <template #suffix>
            <IconSearch />
          </template>
        </a-input>
        <a-select
          v-model="filterCardTypeId"
          :placeholder="t('admin.bizRuleDefinition.filterCardType')"
          allow-clear
          allow-search
          size="small"
          class="filter-entity-type"
        >
          <a-option
            v-for="opt in cardTypeOptions"
            :key="opt.id"
            :value="opt.id"
          >
            {{ opt.name }}
          </a-option>
        </a-select>
      </div>
      <div class="toolbar-actions">
        <a-button
          type="text"
          size="small"
          class="execution-log-toolbar-btn"
          @click="openExecutionLog"
        >
          <template #icon>
            <IconHistory />
          </template>
          {{ t('admin.bizRule.executionLog.viewButton') }}
        </a-button>
        <CreateButton @click="openCreateTypeModal">{{ t('admin.bizRule.createButton') }}</CreateButton>
      </div>
    </div>

    <div class="biz-rule-table-wrap">
      <AdminTable
        :data="filteredRules"
        :loading="loading"
        size="mini"
        :empty-text="t('admin.bizRule.emptyDescription')"
        :scroll="{ x: 1000, y: 'calc(100vh - 220px)' }"
        row-key="id"
      >
      <a-table-column
        :title="t('admin.bizRule.ruleName')"
        data-index="name"
        :width="200"
        align="left"
        fixed="left"
      >
        <template #cell="{ record }">
          <a-link @click="handleEdit(record)">{{ record.name }}</a-link>
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.bizRuleDefinition.entityTypeColumn')" :width="160">
        <template #cell="{ record }">
          <span class="cell-text">{{ getCardTypeName(record) }}</span>
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.bizRule.triggerEventLabel')" :width="120">
        <template #cell="{ record }">
          {{ getTriggerEventLabel(record.triggerEvent) }}
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.bizRule.actionsLabel')" :width="88">
        <template #cell="{ record }">
          {{ getActionsCountLabel(record) }}
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.table.status')" :width="88" align="center">
        <template #cell="{ record }">
          <a-tag v-if="record.enabled" color="green" size="small">{{ t('admin.status.enabled') }}</a-tag>
          <a-tag v-else color="gray" size="small">{{ t('admin.status.disabled') }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.table.updatedAt')" data-index="updatedAt" :width="160">
        <template #cell="{ record }">
          <span class="time-column">{{ formatDateTime(record.updatedAt) }}</span>
        </template>
      </a-table-column>
      <a-table-column :title="t('admin.table.operations')" :width="72" fixed="right">
        <template #cell="{ record }">
          <a-dropdown trigger="click">
            <a-button type="text" size="small">
              <template #icon>
                <IconMore />
              </template>
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
              <a-doption class="danger-option" @click="handleDelete(record)">
                <IconDelete /> {{ t('admin.action.delete') }}
              </a-doption>
            </template>
          </a-dropdown>
        </template>
      </a-table-column>
      </AdminTable>
    </div>

    <BizRuleEditDrawer
      v-model:visible="drawerVisible"
      :card-type-id="drawerCardTypeId"
      :rule="editingRule"
      @success="handleSaveSuccess"
    />

    <RuleExecutionLogPanel
      v-model:visible="logPanelVisible"
      :card-type-id="filterCardTypeId"
      :card-type-options="cardTypeOptions"
    />

    <a-modal
      v-model:visible="selectTypeModalVisible"
      :title="t('admin.bizRuleDefinition.selectCardTypeTitle')"
      :ok-text="t('admin.action.confirm')"
      :on-before-ok="onBeforeCreateOk"
    >
      <a-select
        v-model="pendingCreateCardTypeId"
        :placeholder="t('admin.bizRuleDefinition.filterCardType')"
        allow-search
        style="width: 100%"
      >
        <a-option v-for="opt in cardTypeOptions" :key="opt.id" :value="opt.id">
          {{ opt.name }}
        </a-option>
      </a-select>
    </a-modal>
  </div>
</template>

<style scoped lang="scss">
.biz-rule-definition-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
  padding: 16px;
  box-sizing: border-box;
  font-size: 14px;
  font-family: inherit;
}

.page-toolbar {
  display: flex;
  flex-wrap: nowrap;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  gap: 12px;
}

.toolbar-filters {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.toolbar-actions {
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* text 按钮默认主色，改为正文色 */
.execution-log-toolbar-btn.arco-btn-text {
  color: var(--color-text-2);

  &:hover {
    color: var(--color-text-1);
    background-color: var(--color-fill-2);
  }
}

.filter-name {
  width: 200px;
  flex-shrink: 0;
}

.filter-entity-type {
  width: 220px;
  flex-shrink: 0;
}

.cell-text {
  color: var(--color-text-2);
}

/* 业务规则列表：紧凑行高；14px 正文（覆盖 table mini 默认较小字号） */
.biz-rule-table-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  font-size: 14px;

  /* 全局 admin-table.scss 给 .admin-table-container 设置了 padding: 0 12px 12px，表格会比上方筛选条多缩进 12px */
  :deep(.admin-table-container) {
    padding: 0 0 12px;
  }

  :deep(.arco-table) {
    font-size: 14px;
  }

  :deep(.arco-table-th .arco-table-cell) {
    padding: 2px 8px;
    font-size: 14px;
  }

  :deep(.arco-table-td .arco-table-cell) {
    padding: 1px 8px;
    font-size: 14px;
  }

  /* 首列与「名称」筛选框左缘对齐：全局 admin-table.scss 在 th/td 上有左右 padding（!important），首列去掉左侧一层 */
  :deep(.arco-table-th:first-child),
  :deep(.arco-table-td:first-child) {
    padding-left: 0 !important;
  }

  :deep(.arco-table-th:first-child .arco-table-cell),
  :deep(.arco-table-td:first-child .arco-table-cell) {
    padding-left: 0;
    text-align: left;
  }

  :deep(.arco-table-th.arco-table-cell-fix-left-first),
  :deep(.arco-table-td.arco-table-cell-fix-left-first) {
    padding-left: 0 !important;
  }

  :deep(.arco-table-cell) {
    line-height: 1.22;
  }

  :deep(.arco-table-tr) {
    height: auto;
  }

  :deep(.arco-link) {
    font-size: 14px;
  }

  :deep(.arco-tag) {
    font-size: 14px;
    line-height: 1.1;
    padding: 0 6px;
    margin: 0;
  }

  :deep(.arco-btn-size-small) {
    padding: 0 4px;
  }
}

:deep(.danger-option) {
  color: rgb(var(--danger-6));
}
</style>
