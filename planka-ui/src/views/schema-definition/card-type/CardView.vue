<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconEdit,
  IconDelete,
  IconCheckCircle,
  IconMinusCircle,
  IconLink,
  IconHistory,
  IconTag,
  IconFile,
} from '@arco-design/web-vue/es/icon'
import { cardTypeApi } from '@/api'
import CreateButton from '@/components/common/CreateButton.vue'
import ViewSwitcher from '@/components/card-type/ViewSwitcher.vue'
import SchemaReferenceDrawer from '@/components/schema/SchemaReferenceDrawer.vue'
import SchemaChangelogDrawer from '@/components/schema/SchemaChangelogDrawer.vue'
import CardTypeFormDrawer from './CardTypeFormDrawer.vue'
import { handleReferenceConflictError } from '@/utils/error-handler'
import type { CardTypeDefinition } from '@/types/card-type'
import { SchemaSubType } from '@/types/schema'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

// 列表状态
const loading = ref(false)
const cardTypes = ref<CardTypeDefinition[]>([])
const searchKeyword = ref('')

// 表单抽屉状态
const drawerVisible = ref(false)
const drawerMode = ref<'create' | 'edit'>('create')
const editingCardType = ref<CardTypeDefinition | null>(null)

// 引用关系抽屉状态
const referenceDrawerVisible = ref(false)
const currentReferenceSchemaId = ref<string | undefined>(undefined)

// 审计日志抽屉状态
const changelogDrawerVisible = ref(false)
const currentChangelogSchemaId = ref<string | undefined>(undefined)
const currentChangelogSchemaName = ref<string | undefined>(undefined)

// 搜索过滤
const filteredCardTypes = computed(() => {
  if (!searchKeyword.value) return cardTypes.value
  const keyword = searchKeyword.value.toLowerCase()
  return cardTypes.value.filter(
    (ct) =>
      ct.name.toLowerCase().includes(keyword) ||
      ct.code?.toLowerCase().includes(keyword)
  )
})

// 分组数据
const traitTypes = computed(() => {
  return filteredCardTypes.value.filter(
    (ct) => ct.schemaSubType === SchemaSubType.TRAIT_CARD_TYPE
  )
})

const entityTypes = computed(() => {
  return filteredCardTypes.value.filter(
    (ct) => ct.schemaSubType === SchemaSubType.ENTITY_CARD_TYPE
  )
})

// 数据加载
async function fetchData() {
  if (loading.value) return
  loading.value = true
  try {
    cardTypes.value = await cardTypeApi.listAll()
  } catch (error) {
    console.error('Failed to fetch card types:', error)
  } finally {
    loading.value = false
  }
}

// 打开新建抽屉
function handleCreate() {
  drawerMode.value = 'create'
  editingCardType.value = null
  drawerVisible.value = true
}

// 打开编辑抽屉
function handleEdit(cardType: CardTypeDefinition) {
  drawerMode.value = 'edit'
  editingCardType.value = cardType
  drawerVisible.value = true
}

// 删除实体类型
async function handleDelete(cardType: CardTypeDefinition) {
  Modal.confirm({
    title: t('admin.message.confirmDelete'),
    content: t('admin.message.deleteConfirmContent', {
      type: t('admin.cardType.title'),
      name: cardType.name,
    }),
    okText: t('admin.action.delete'),
    okButtonProps: { status: 'danger' },
    modalClass: 'arco-modal-simple',
    async onOk() {
      try {
        await cardTypeApi.delete(cardType.id!)
        Message.success(t('admin.message.deleteSuccess'))
        await fetchData()
      } catch (error) {
        if (!handleReferenceConflictError(error)) {
          console.error('Failed to delete:', error)
        }
      }
    },
  })
}

// 启用/停用
async function handleToggleEnabled(cardType: CardTypeDefinition) {
  try {
    if (cardType.enabled) {
      await cardTypeApi.disable(cardType.id!)
      Message.success(t('admin.message.disableSuccess'))
    } else {
      await cardTypeApi.activate(cardType.id!)
      Message.success(t('admin.message.enableSuccess'))
    }
    await fetchData()
  } catch (error) {
    console.error('Failed to toggle enabled:', error)
  }
}

// 打开引用关系抽屉
function handleReference(cardType: CardTypeDefinition) {
  if (!cardType.id) return
  currentReferenceSchemaId.value = cardType.id
  referenceDrawerVisible.value = true
}

// 打开审计日志抽屉
function handleChangelog(cardType: CardTypeDefinition) {
  if (!cardType.id) return
  currentChangelogSchemaId.value = cardType.id
  currentChangelogSchemaName.value = cardType.name
  changelogDrawerVisible.value = true
}

// 表单保存成功
async function handleFormSuccess() {
  await fetchData()
}

// 处理路由参数中的编辑请求
async function handleEditFromRoute() {
  const editId = route.query.edit as string | undefined
  if (editId) {
    router.replace({ path: '/admin/card-type/card', query: {} })
    const cardType = cardTypes.value.find((ct) => ct.id === editId)
    if (cardType) {
      handleEdit(cardType)
    } else {
      handleEdit({ id: editId, name: '' } as CardTypeDefinition)
    }
  }
}

onMounted(async () => {
  await fetchData()
  await handleEditFromRoute()
})
</script>

<template>
  <div class="card-view-page">
    <div class="card-view-content">
      <div class="content-header">
        <ViewSwitcher />
        <div class="header-divider" />
        <a-input-search
          v-model="searchKeyword"
          :placeholder="t('admin.search.placeholder')"
          style="width: 260px"
          size="small"
          allow-clear
        />
        <div class="header-spacer" />
        <CreateButton @click="handleCreate">
          {{ t('admin.cardType.createButton') }}
        </CreateButton>
      </div>

      <div class="card-view-container">
        <a-spin :loading="loading" class="loading-wrapper">
          <!-- 特征类型分组 -->
          <div v-if="traitTypes.length > 0" class="card-group">
            <div class="group-header">
              <IconTag class="group-icon trait-color" />
              <span class="group-title">{{ t('admin.cardType.schemaSubType.TRAIT_CARD_TYPE') }}</span>
              <span class="group-count">({{ traitTypes.length }})</span>
            </div>
            <a-row :gutter="[16, 16]" class="card-grid">
              <a-col v-for="ct in traitTypes" :key="ct.id" :xs="24" :sm="12" :md="8" :lg="6">
                <a-card class="type-card trait-card" hoverable @click="handleEdit(ct)">
                  <div class="type-card-header">
                    <div class="type-card-icon trait-bg">
                      <IconTag />
                    </div>
                    <div class="type-card-info">
                      <div class="type-card-name">{{ ct.name }}</div>
                      <div v-if="ct.code" class="type-card-code">{{ ct.code }}</div>
                    </div>
                  </div>
                  <div class="type-card-tags">
                    <a-tag :color="ct.enabled ? 'green' : 'gray'" size="small">
                      {{ ct.enabled ? t('admin.status.enabled') : t('admin.status.disabled') }}
                    </a-tag>
                    <a-tag v-if="ct.systemCardType" color="blue" size="small">
                      {{ t('admin.table.systemBuiltin') }}
                    </a-tag>
                  </div>
                  <div class="type-card-actions" @click.stop>
                    <a-tooltip v-if="!ct.systemCardType" :content="t('admin.action.edit')" mini>
                      <a-button size="mini" type="text" @click="handleEdit(ct)">
                        <template #icon><IconEdit /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="t('admin.action.reference')" mini>
                      <a-button size="mini" type="text" @click="handleReference(ct)">
                        <template #icon><IconLink /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="t('admin.action.changelog')" mini>
                      <a-button size="mini" type="text" @click="handleChangelog(ct)">
                        <template #icon><IconHistory /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="ct.enabled ? t('admin.action.disable') : t('admin.action.enable')" mini>
                      <a-button size="mini" type="text" @click="handleToggleEnabled(ct)">
                        <template #icon>
                          <IconCheckCircle v-if="!ct.enabled" />
                          <IconMinusCircle v-else />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip v-if="!ct.systemCardType" :content="t('admin.action.delete')" mini>
                      <a-button size="mini" type="text" status="danger" @click="handleDelete(ct)">
                        <template #icon><IconDelete /></template>
                      </a-button>
                    </a-tooltip>
                  </div>
                </a-card>
              </a-col>
            </a-row>
          </div>

          <!-- 实体类型分组 -->
          <div v-if="entityTypes.length > 0" class="card-group">
            <div class="group-header">
              <IconFile class="group-icon entity-color" />
              <span class="group-title">{{ t('admin.cardType.schemaSubType.ENTITY_CARD_TYPE') }}</span>
              <span class="group-count">({{ entityTypes.length }})</span>
            </div>
            <a-row :gutter="[16, 16]" class="card-grid">
              <a-col v-for="ct in entityTypes" :key="ct.id" :xs="24" :sm="12" :md="8" :lg="6">
                <a-card class="type-card entity-card" hoverable @click="handleEdit(ct)">
                  <div class="type-card-header">
                    <div class="type-card-icon entity-bg">
                      <IconFile />
                    </div>
                    <div class="type-card-info">
                      <div class="type-card-name">{{ ct.name }}</div>
                      <div v-if="ct.code" class="type-card-code">{{ ct.code }}</div>
                    </div>
                  </div>
                  <div class="type-card-tags">
                    <a-tag :color="ct.enabled ? 'green' : 'gray'" size="small">
                      {{ ct.enabled ? t('admin.status.enabled') : t('admin.status.disabled') }}
                    </a-tag>
                    <a-tag v-if="ct.systemCardType" color="blue" size="small">
                      {{ t('admin.table.systemBuiltin') }}
                    </a-tag>
                  </div>
                  <div
                    v-if="'parentTypes' in ct && ct.parentTypes && ct.parentTypes.length > 0"
                    class="type-card-parents"
                  >
                    <a-tag v-for="p in ct.parentTypes" :key="p.id" size="small" color="arcoblue">
                      {{ p.name }}
                    </a-tag>
                  </div>
                  <div class="type-card-actions" @click.stop>
                    <a-tooltip v-if="!ct.systemCardType" :content="t('admin.action.edit')" mini>
                      <a-button size="mini" type="text" @click="handleEdit(ct)">
                        <template #icon><IconEdit /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="t('admin.action.reference')" mini>
                      <a-button size="mini" type="text" @click="handleReference(ct)">
                        <template #icon><IconLink /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="t('admin.action.changelog')" mini>
                      <a-button size="mini" type="text" @click="handleChangelog(ct)">
                        <template #icon><IconHistory /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip :content="ct.enabled ? t('admin.action.disable') : t('admin.action.enable')" mini>
                      <a-button size="mini" type="text" @click="handleToggleEnabled(ct)">
                        <template #icon>
                          <IconCheckCircle v-if="!ct.enabled" />
                          <IconMinusCircle v-else />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip v-if="!ct.systemCardType" :content="t('admin.action.delete')" mini>
                      <a-button size="mini" type="text" status="danger" @click="handleDelete(ct)">
                        <template #icon><IconDelete /></template>
                      </a-button>
                    </a-tooltip>
                  </div>
                </a-card>
              </a-col>
            </a-row>
          </div>

          <!-- 空状态 -->
          <div v-if="!loading && traitTypes.length === 0 && entityTypes.length === 0" class="empty-state">
            <a-empty :description="t('admin.cardType.emptyDescription')" />
          </div>
        </a-spin>
      </div>
    </div>

    <!-- 新建/编辑抽屉 -->
    <CardTypeFormDrawer
      v-model:visible="drawerVisible"
      :mode="drawerMode"
      :editing-card-type="editingCardType"
      @success="handleFormSuccess"
    />

    <!-- 引用关系抽屉 -->
    <SchemaReferenceDrawer
      v-model:visible="referenceDrawerVisible"
      :schema-id="currentReferenceSchemaId"
    />

    <!-- 审计日志抽屉 -->
    <SchemaChangelogDrawer
      v-model:visible="changelogDrawerVisible"
      :schema-id="currentChangelogSchemaId"
      :schema-name="currentChangelogSchemaName"
      :include-children="true"
    />
  </div>
</template>

<style scoped>
.card-view-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-view-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-2);
  border-radius: 4px;
  overflow: hidden;
}

.content-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-bottom: 1px solid var(--color-border-1);
}

.header-divider {
  width: 1px;
  height: 20px;
  background: var(--color-border-2);
  margin: 0 4px;
}

.header-spacer {
  flex: 1;
}

.card-view-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.loading-wrapper {
  width: 100%;
}

/* 分组 */
.card-group {
  margin-bottom: 24px;
}

.card-group:last-child {
  margin-bottom: 0;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border-1);
}

.group-icon {
  font-size: 16px;
}

.trait-color {
  color: #722ED1;
}

.entity-color {
  color: #3370FF;
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
}

.group-count {
  font-size: 12px;
  color: var(--color-text-3);
}

/* 卡片 */
.type-card {
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 8px;
}

.type-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.type-card :deep(.arco-card-body) {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.type-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.type-card-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
  flex-shrink: 0;
}

.trait-bg {
  background: linear-gradient(135deg, #722ED1, #9254DE);
}

.entity-bg {
  background: linear-gradient(135deg, #3370FF, #5B8FF9);
}

.type-card-info {
  min-width: 0;
  flex: 1;
}

.type-card-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-card-code {
  font-size: 12px;
  color: var(--color-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.type-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.type-card-parents {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.type-card-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-top: 6px;
  border-top: 1px solid var(--color-border-1);
  height: 0;
  padding: 0;
  border: none;
  overflow: hidden;
  opacity: 0;
  transition: height 0.2s, padding-top 0.2s, opacity 0.2s;
}

.type-card:hover .type-card-actions {
  height: auto;
  padding-top: 6px;
  border-top: 1px solid var(--color-border-1);
  overflow: visible;
  opacity: 1;
}

/* 空状态 */
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}
</style>
