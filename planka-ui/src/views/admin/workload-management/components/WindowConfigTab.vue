<template>
  <div class="window-config-tab">
    <a-spin :loading="loading">
      <a-form :model="form" layout="vertical" @submit="handleSubmit">
        <!-- 新增工时窗口期 -->
        <a-card :title="t('admin.workloadManagement.windowConfig.createOperation')" :bordered="false" class="mb-4">
          <template #extra>
            <a-switch v-model="form.createEnabled" />
          </template>

          <a-form-item v-if="form.createEnabled">
            <div class="flex items-center gap-2">
              <span>{{ t('admin.workloadManagement.windowConfig.pastDays') }}</span>
              <a-input-number
                v-model="form.createPastDays"
                :min="0"
                :max="9999"
                :style="{ width: '120px' }"
                :placeholder="t('admin.workloadManagement.windowConfig.pastDaysPlaceholder')"
              />
              <span>{{ t('admin.workloadManagement.windowConfig.days') }}</span>
              <span>{{ t('admin.workloadManagement.windowConfig.to') }}</span>
              <a-input-number
                v-model="form.createFutureDays"
                :min="0"
                :max="9999"
                :style="{ width: '120px' }"
                :placeholder="t('admin.workloadManagement.windowConfig.futureDaysPlaceholder')"
              />
              <span>{{ t('admin.workloadManagement.windowConfig.days') }}</span>
            </div>
          </a-form-item>
        </a-card>

        <!-- 修改工时窗口期 -->
        <a-card :title="t('admin.workloadManagement.windowConfig.updateOperation')" :bordered="false" class="mb-4">
          <template #extra>
            <a-switch v-model="form.updateEnabled" />
          </template>

          <a-form-item v-if="form.updateEnabled">
            <div class="flex items-center gap-2">
              <span>{{ t('admin.workloadManagement.windowConfig.pastDays') }}</span>
              <a-input-number
                v-model="form.updatePastDays"
                :min="0"
                :max="9999"
                :style="{ width: '120px' }"
                :placeholder="t('admin.workloadManagement.windowConfig.pastDaysPlaceholder')"
              />
              <span>{{ t('admin.workloadManagement.windowConfig.days') }}</span>
              <span>{{ t('admin.workloadManagement.windowConfig.to') }}</span>
              <a-input-number
                v-model="form.updateFutureDays"
                :min="0"
                :max="9999"
                :style="{ width: '120px' }"
                :placeholder="t('admin.workloadManagement.windowConfig.futureDaysPlaceholder')"
              />
              <span>{{ t('admin.workloadManagement.windowConfig.days') }}</span>
            </div>
          </a-form-item>
        </a-card>

        <!-- 删除工时窗口期 -->
        <a-card :title="t('admin.workloadManagement.windowConfig.deleteOperation')" :bordered="false" class="mb-4">
          <template #extra>
            <a-switch v-model="form.deleteEnabled" />
          </template>

          <a-form-item v-if="form.deleteEnabled">
            <div class="flex items-center gap-2">
              <span>{{ t('admin.workloadManagement.windowConfig.pastDays') }}</span>
              <a-input-number
                v-model="form.deletePastDays"
                :min="0"
                :max="9999"
                :style="{ width: '120px' }"
                :placeholder="t('admin.workloadManagement.windowConfig.pastDaysPlaceholder')"
              />
              <span>{{ t('admin.workloadManagement.windowConfig.days') }}</span>
              <span>{{ t('admin.workloadManagement.windowConfig.to') }}</span>
              <a-input-number
                v-model="form.deleteFutureDays"
                :min="0"
                :max="9999"
                :style="{ width: '120px' }"
                :placeholder="t('admin.workloadManagement.windowConfig.futureDaysPlaceholder')"
              />
              <span>{{ t('admin.workloadManagement.windowConfig.days') }}</span>
            </div>
          </a-form-item>
        </a-card>

        <!-- 周期锁定 -->
        <a-card :title="t('admin.workloadManagement.windowConfig.lockConfig')" :bordered="false" class="mb-4">
          <a-form-item>
            <div class="flex items-center gap-2">
              <span>{{ t('admin.workloadManagement.windowConfig.lockDay') }}</span>
              <a-input-number
                v-model="form.lockDay"
                :min="1"
                :max="28"
                :style="{ width: '120px' }"
                :placeholder="t('admin.workloadManagement.windowConfig.lockDayPlaceholder')"
              />
              <a-tooltip :content="t('admin.workloadManagement.windowConfig.lockDayHelp')">
                <icon-question-circle class="text-gray-400 cursor-help" />
              </a-tooltip>
            </div>
          </a-form-item>
        </a-card>

        <!-- 管理员白名单 -->
        <a-card :title="t('admin.workloadManagement.windowConfig.whitelistConfig')" :bordered="false" class="mb-4">
          <template #extra>
            <a-switch v-model="form.managerWhitelistEnable">
              <template #checked>{{ t('admin.workloadManagement.windowConfig.enabled') }}</template>
              <template #unchecked>{{ t('admin.workloadManagement.windowConfig.disabled') }}</template>
            </a-switch>
          </template>

          <a-form-item v-if="form.managerWhitelistEnable">
            <a-textarea
              v-model="whitelistText"
              :placeholder="t('admin.workloadManagement.windowConfig.managerWhiteListPlaceholder')"
              :rows="4"
            />
            <template #extra>
              <span class="text-gray-500">{{ t('admin.workloadManagement.windowConfig.managerWhitelistEnableHelp') }}</span>
            </template>
          </a-form-item>
        </a-card>

        <!-- 操作按钮 -->
        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="saving">
              {{ t('common.action.save') }}
            </a-button>
            <a-button @click="handleReset">{{ t('common.action.reset') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconQuestionCircle } from '@arco-design/web-vue/es/icon'
import { useI18n } from 'vue-i18n'
import { workloadApi } from '@/api/workload'
import { useOrgStore } from '@/stores/org'

const { t } = useI18n()
const orgStore = useOrgStore()
const orgId = computed(() => orgStore.currentOrgId || '')

const loading = ref(false)
const saving = ref(false)

interface WindowConfigForm {
  orgId: string
  createEnabled: boolean
  createPastDays: number
  createFutureDays: number
  updateEnabled: boolean
  updatePastDays: number
  updateFutureDays: number
  deleteEnabled: boolean
  deletePastDays: number
  deleteFutureDays: number
  lockDay: number | undefined
  managerWhitelistEnable: boolean
  managerWhitelist?: string
}

const form = ref<WindowConfigForm>({
  orgId: '',
  createEnabled: true,
  createPastDays: 7,
  createFutureDays: 7,
  updateEnabled: true,
  updatePastDays: 3,
  updateFutureDays: 3,
  deleteEnabled: true,
  deletePastDays: 1,
  deleteFutureDays: 1,
  lockDay: undefined,
  managerWhitelistEnable: false,
  managerWhitelist: '',
})

const whitelistText = ref('')

// 加载配置
async function loadConfig() {
  if (!orgId.value) {
    Message.error(t('admin.workloadManagement.windowConfig.loadFailed'))
    return
  }

  loading.value = true
  try {
    const config = await workloadApi.getWindowConfig(orgId.value)

    // 将旧的配置格式转换为新的格式
    form.value = {
      orgId: config.orgId,
      createEnabled: true,
      createPastDays: config.createWindow || 7,
      createFutureDays: config.createWindow || 7,
      updateEnabled: true,
      updatePastDays: config.updateWindow || 3,
      updateFutureDays: config.updateWindow || 3,
      deleteEnabled: true,
      deletePastDays: config.deleteWindow || 1,
      deleteFutureDays: config.deleteWindow || 1,
      lockDay: config.lockDay,
      managerWhitelistEnable: config.managerWhitelistEnable,
      managerWhitelist: config.managerWhitelist,
    }

    // 解析白名单
    if (config.managerWhitelist) {
      try {
        const list = JSON.parse(config.managerWhitelist)
        whitelistText.value = list.join('\n')
      } catch {
        whitelistText.value = ''
      }
    }
  } catch (error: any) {
    Message.error(error.message || t('admin.workloadManagement.windowConfig.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 保存配置
async function handleSubmit() {
  if (!orgId.value) {
    Message.error(t('admin.workloadManagement.windowConfig.loadFailed'))
    return
  }

  // 处理白名单
  let managerWhitelist = ''
  if (form.value.managerWhitelistEnable && whitelistText.value) {
    const list = whitelistText.value
      .split('\n')
      .map(id => id.trim())
      .filter(id => id.length > 0)
    managerWhitelist = JSON.stringify(list)
  }

  // 转换为后端期望的格式
  const config = {
    orgId: orgId.value,
    createWindow: Math.max(form.value.createPastDays, form.value.createFutureDays),
    updateWindow: Math.max(form.value.updatePastDays, form.value.updateFutureDays),
    deleteWindow: Math.max(form.value.deletePastDays, form.value.deleteFutureDays),
    lockDay: form.value.lockDay || 5,
    lockHour: 23,
    managerWhitelistEnable: form.value.managerWhitelistEnable,
    managerWhitelist,
  }

  saving.value = true
  try {
    await workloadApi.updateWindowConfig(orgId.value, config)
    Message.success(t('admin.workloadManagement.windowConfig.saveSuccess'))
    await loadConfig()
  } catch (error: any) {
    Message.error(error.message || t('admin.workloadManagement.windowConfig.saveFailed'))
  } finally {
    saving.value = false
  }
}

// 重置
function handleReset() {
  loadConfig()
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.window-config-tab {
  max-width: 900px;
}

.window-config-tab :deep(.arco-card) {
  margin-bottom: 16px;
}

.window-config-tab :deep(.arco-card:last-child) {
  margin-bottom: 0;
}

.window-config-tab :deep(.arco-card-body) {
  padding: 16px 20px;
}

.window-config-tab :deep(.arco-card-header) {
  padding: 12px 20px;
  border-bottom: 1px solid var(--color-border-2);
}

.window-config-tab :deep(.arco-card-header-title) {
  font-size: 14px;
  font-weight: 500;
}

.window-config-tab :deep(.arco-form-item) {
  margin-bottom: 0;
}

.window-config-tab :deep(.arco-form-item-label-col) {
  padding-bottom: 0;
}

.window-config-tab :deep(.arco-textarea-wrapper) {
  margin-top: 8px;
}
</style>
