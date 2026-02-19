<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { valueStreamApi } from '@/api/value-stream'

const { t } = useI18n()
import { useOrgStore } from '@/stores/org'
import {
  createDefaultValueStream,
  type ValueStreamDefinition,
  type StepConfig,
} from '@/types/value-stream'
import ValueStreamBoard from './value-stream/ValueStreamBoard.vue'
import CreateButton from '@/components/common/CreateButton.vue'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'
import DeleteButton from '@/components/common/DeleteButton.vue'

const props = defineProps<{
  cardTypeId: string
}>()

const orgStore = useOrgStore()

// 状态
const loading = ref(false)
const saving = ref(false)
const hasChanges = ref(false)

// 价值流状态
const valueStream = ref<ValueStreamDefinition | null>(null)
const localStepList = ref<StepConfig[]>([])

// 计算属性
const hasValueStream = computed(() => valueStream.value !== null)

// 加载价值流定义
async function loadValueStream() {
  if (!props.cardTypeId) return

  loading.value = true
  try {
    const result = await valueStreamApi.getByCardType(props.cardTypeId)
    valueStream.value = result
    if (result) {
      localStepList.value = JSON.parse(JSON.stringify(result.stepList))
    }
    hasChanges.value = false
  } catch (error) {
    console.error('Failed to load value stream:', error)
    Message.error(t('admin.cardType.fetchFieldConfigFailed'))
  } finally {
    loading.value = false
  }
}

// 创建默认价值流定义
async function handleCreateValueStream() {
  if (!orgStore.currentOrgId) {
    Message.error(t('admin.cardType.valueStream.operationFailed'))
    return
  }

  saving.value = true
  try {
    const defaultValueStream = createDefaultValueStream(
      orgStore.currentOrgId,
      props.cardTypeId,
    )
    const created = await valueStreamApi.create(defaultValueStream)
    valueStream.value = created
    localStepList.value = JSON.parse(JSON.stringify(created.stepList))
    hasChanges.value = false
    Message.success(t('admin.cardType.valueStream.createValueStreamSuccess'))
  } catch (error) {
    console.error('Failed to create value stream:', error)
    Message.error(t('admin.cardType.valueStream.createFailed'))
  } finally {
    saving.value = false
  }
}

// 保存价值流定义
async function handleSaveValueStream() {
  if (!valueStream.value) return

  saving.value = true
  try {
    const updated = await valueStreamApi.update(
      valueStream.value.id!,
      {
        ...valueStream.value,
        stepList: localStepList.value,
      },
      valueStream.value.contentVersion,
    )
    valueStream.value = updated
    localStepList.value = JSON.parse(JSON.stringify(updated.stepList))
    hasChanges.value = false
    Message.success(t('admin.cardType.valueStream.saveValueStreamSuccess'))
  } catch (error) {
    console.error('Failed to save value stream:', error)
    Message.error(t('admin.cardType.valueStream.saveFailed'))
  } finally {
    saving.value = false
  }
}

// 删除价值流定义
async function handleDeleteValueStream() {
  if (!valueStream.value?.id) return

  saving.value = true
  try {
    await valueStreamApi.delete(valueStream.value.id)
    valueStream.value = null
    localStepList.value = []
    hasChanges.value = false
    Message.success(t('admin.cardType.valueStream.deleteValueStreamSuccess'))
  } catch (error) {
    console.error('Failed to delete value stream:', error)
    Message.error(t('admin.cardType.valueStream.deleteFailed'))
  } finally {
    saving.value = false
  }
}

// 处理步骤列表更新
function handleStepListUpdate(newStepList: StepConfig[]) {
  localStepList.value = newStepList
  hasChanges.value = true
}

// 重置更改
function handleResetValueStream() {
  if (valueStream.value) {
    localStepList.value = JSON.parse(JSON.stringify(valueStream.value.stepList))
    hasChanges.value = false
  }
}

onMounted(() => {
  loadValueStream()
})

// 暴露给父组件
defineExpose({
  hasChanges,
})
</script>

<template>
  <div class="value-stream-tab">
    <a-spin :loading="loading" class="tab-spin">
      <!-- 未创建价值流 -->
      <template v-if="!hasValueStream">
        <div class="empty-state">
          <a-empty :description="t('admin.cardType.valueStream.emptyValueStream')" />
          <CreateButton :loading="saving" @click="handleCreateValueStream">
            {{ t('admin.cardType.valueStream.createValueStream') }}
          </CreateButton>
        </div>
      </template>

      <!-- 已有价值流 -->
      <template v-else>
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <span class="title">{{ t('admin.cardType.valueStream.valueStreamDefinition') }}</span>
            <a-tag v-if="hasChanges" color="arcoblue" size="small">{{ t('admin.cardType.valueStream.unsavedChanges') }}</a-tag>
          </div>
          <div class="toolbar-right">
            <CancelButton v-if="hasChanges" size="mini" @click="handleResetValueStream" />
            <SaveButton size="mini" :loading="saving" :disabled="!hasChanges" @click="handleSaveValueStream" />
            <a-popconfirm
              :content="t('admin.cardType.valueStream.confirmDeleteValueStream')"
              @ok="handleDeleteValueStream"
            >
              <DeleteButton size="mini" />
            </a-popconfirm>
          </div>
        </div>

        <!-- 看板 -->
        <ValueStreamBoard
          :step-list="localStepList"
          @update:step-list="handleStepListUpdate"
        />
      </template>
    </a-spin>
  </div>
</template>

<style scoped>
.value-stream-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.tab-spin {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 16px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--color-border-2);
  flex-shrink: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-1);
}

.toolbar-right {
  display: flex;
  gap: 8px;
}
</style>
