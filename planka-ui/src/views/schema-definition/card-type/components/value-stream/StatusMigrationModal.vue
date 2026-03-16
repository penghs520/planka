<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from '@arco-design/web-vue'
import type { StatusConfig, StepConfig, ValueStreamDefinition } from '@/types/value-stream'
import { StepStatusKindConfig } from '@/types/value-stream'
import { valueStreamApi } from '@/api/value-stream'
import { useLoading } from '@/hooks/useLoading'

const { t } = useI18n()

interface StatusWithCardCount extends StatusConfig {
  cardCount?: number
  stepName?: string
  stepKind?: string
}

const props = defineProps<{
  visible: boolean
  mode: 'single' | 'multiple'
  deletingStatuses: StatusConfig[]
  availableTargetStatuses: StatusConfig[]
  valueStream: ValueStreamDefinition
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: [migrationMap: Record<string, string>]
}>()

const formRef = ref<FormInstance>()
const statusesWithInfo = ref<StatusWithCardCount[]>([])
const migrationMap = ref<Record<string, string>>({})
const { loading, withLoading } = useLoading()

const modalVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

const modalTitle = computed(() => {
  return props.mode === 'single'
    ? t('admin.cardType.valueStream.statusMigration.title')
    : t('admin.cardType.valueStream.statusMigration.stepMigrationTitle')
})

const description = computed(() => {
  return props.mode === 'single'
    ? t('admin.cardType.valueStream.statusMigration.description')
    : t('admin.cardType.valueStream.statusMigration.stepDescription')
})

// 为每个状态构建选项列表（排除自己）
function getTargetOptionsForStatus(statusId: string) {
  return props.availableTargetStatuses
    .filter((s) => s.id !== statusId)
    .map((status) => {
      // 找到状态所属的阶段
      const step = props.valueStream.stepList.find((st) =>
        st.statusList.some((s) => s.id === status.id),
      )
      const stepKindLabel = step ? StepStatusKindConfig[step.kind].label : ''
      return {
        value: status.id!,
        label: `${stepKindLabel} - ${status.name}`,
      }
    })
}

// 加载状态信息（包括卡片数量）
async function loadStatusInfo() {
  statusesWithInfo.value = []
  migrationMap.value = {}

  await withLoading(async () => {
    const promises = props.deletingStatuses.map(async (status) => {
      // 查询卡片数量
      let cardCount = 0
      try {
        cardCount = await valueStreamApi.getCardCountByStatus(
          status.id!,
          props.valueStream.id,
          props.valueStream.cardTypeId,
        )
      } catch (error) {
        console.error('Failed to get card count:', error)
      }

      // 找到状态所属的阶段
      const step = props.valueStream.stepList.find((st) =>
        st.statusList.some((s) => s.id === status.id),
      )

      return {
        ...status,
        cardCount,
        stepName: step?.name,
        stepKind: step ? StepStatusKindConfig[step.kind].label : '',
      }
    })

    statusesWithInfo.value = await Promise.all(promises)

    // 初始化迁移映射
    props.deletingStatuses.forEach((status) => {
      migrationMap.value[status.id!] = ''
    })
  })
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      loadStatusInfo()
    }
  },
)

async function handleConfirm() {
  const errors = await formRef.value?.validate()
  if (errors) return

  emit('confirm', { ...migrationMap.value })
  modalVisible.value = false
}
</script>

<template>
  <a-modal
    v-model:visible="modalVisible"
    :title="modalTitle"
    :width="600"
    :ok-loading="loading"
    @ok="handleConfirm"
    @cancel="modalVisible = false"
  >
    <div class="migration-modal">
      <div class="description">{{ description }}</div>

      <a-spin :loading="loading" style="width: 100%">
        <a-form
          ref="formRef"
          :model="migrationMap"
          :label-col-props="{ span: 8 }"
          :wrapper-col-props="{ span: 16 }"
        >
          <div
            v-for="status in statusesWithInfo"
            :key="status.id"
            class="status-migration-item"
          >
            <div class="status-info">
              <div class="status-header">
                <span class="status-label">
                  {{ t('admin.cardType.valueStream.statusMigration.sourceStatus') }}:
                </span>
                <span class="status-name">
                  <span v-if="status.stepKind" class="step-kind">{{ status.stepKind }}</span>
                  {{ status.name }}
                </span>
              </div>
              <div class="card-count">
                <span class="count-label">
                  {{ t('admin.cardType.valueStream.statusMigration.cardCount') }}:
                </span>
                <span class="count-value" :class="{ 'no-cards': status.cardCount === 0 }">
                  {{ status.cardCount }}
                  <span v-if="status.cardCount === 0" class="no-cards-hint">
                    ({{ t('admin.cardType.valueStream.statusMigration.noCards') }})
                  </span>
                </span>
              </div>
            </div>

            <a-form-item
              :field="`${status.id}`"
              :label="t('admin.cardType.valueStream.statusMigration.targetStatus')"
              :rules="[
                {
                  required: true,
                  message: t('admin.cardType.valueStream.statusMigration.selectTarget'),
                },
              ]"
            >
              <a-select
                v-model="migrationMap[status.id!]"
                :placeholder="t('admin.cardType.valueStream.statusMigration.selectTarget')"
                :options="getTargetOptionsForStatus(status.id!)"
              />
            </a-form-item>
          </div>
        </a-form>
      </a-spin>
    </div>
  </a-modal>
</template>

<style scoped>
.migration-modal {
  padding: 8px 0;
}

.description {
  margin-bottom: 20px;
  padding: 12px;
  background-color: var(--color-fill-2);
  border-radius: 6px;
  color: var(--color-text-2);
  font-size: 13px;
  line-height: 1.6;
}

.status-migration-item {
  margin-bottom: 24px;
  padding: 16px;
  background-color: var(--color-fill-1);
  border-radius: 8px;
  border: 1px solid var(--color-border-2);
}

.status-migration-item:last-child {
  margin-bottom: 0;
}

.status-info {
  margin-bottom: 16px;
}

.status-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.status-label {
  font-size: 13px;
  color: var(--color-text-3);
  margin-right: 8px;
}

.status-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-kind {
  display: inline-block;
  padding: 2px 8px;
  background-color: var(--color-fill-3);
  border-radius: 4px;
  font-size: 12px;
  color: var(--color-text-2);
}

.card-count {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.count-label {
  color: var(--color-text-3);
  margin-right: 8px;
}

.count-value {
  font-weight: 500;
  color: var(--color-text-1);
}

.count-value.no-cards {
  color: var(--color-text-3);
}

.no-cards-hint {
  font-weight: normal;
  font-size: 12px;
  margin-left: 4px;
}

:deep(.arco-form-item) {
  margin-bottom: 0;
}

:deep(.arco-form-item-label) {
  font-weight: 500;
}
</style>
