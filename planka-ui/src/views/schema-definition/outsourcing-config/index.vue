<template>
  <div class="outsourcing-config-container">
    <!-- 顶部操作栏 -->
    <div class="header">
      <div class="header-left">
        <h2>{{ t('outsourcingConfig.title') }}</h2>
        <a-tooltip :content="t('outsourcingConfig.helpTooltip')" position="right">
          <icon-question-circle class="help-icon" @click="showHelp = true" />
        </a-tooltip>
      </div>
      <a-space>
        <a-button :disabled="loading || saving || !attendanceEnabled" @click="handleReset">
          {{ t('outsourcingConfig.actions.reset') }}
        </a-button>
        <a-button
          type="primary"
          :loading="saving"
          :disabled="loading || !attendanceEnabled"
          @click="handleSave"
        >
          {{ t('outsourcingConfig.actions.save') }}
        </a-button>
      </a-space>
    </div>

    <!-- 加载状态 -->
    <a-spin :loading="loading" style="width: 100%">
      <!-- 考勤功能未启用的全局提示 -->
      <a-alert
        v-if="!attendanceEnabled"
        type="warning"
        banner
        closable
        style="margin-bottom: 16px"
      >
        <template #icon>
          <icon-exclamation-circle />
        </template>
        {{ t('outsourcingConfig.global.attendanceNotEnabled') }}
        <template #action>
          <a-link :href="`/admin/org-settings`">
            {{ t('outsourcingConfig.global.goToOrgSettings') }}
          </a-link>
        </template>
      </a-alert>

      <!-- 配置表单 -->
      <a-tabs v-model:active-key="activeTab" type="card-gutter" class="config-tabs">
        <a-tab-pane key="global" :title="t('outsourcingConfig.tabs.global')">
          <div :class="{ 'disabled-overlay': !attendanceEnabled }">
            <GlobalConfig v-model="config" />
          </div>
        </a-tab-pane>

        <a-tab-pane key="attendance" :title="t('outsourcingConfig.tabs.attendance')">
          <div :class="{ 'disabled-overlay': !attendanceEnabled }">
            <AttendanceConfig v-if="config.attendanceConf" v-model="config.attendanceConf" />
          </div>
        </a-tab-pane>

        <a-tab-pane key="leave" :title="t('outsourcingConfig.tabs.leave')">
          <div :class="{ 'disabled-overlay': !attendanceEnabled }">
            <LeaveConfig v-if="config.leaveConf" v-model="config.leaveConf" />
          </div>
        </a-tab-pane>

        <a-tab-pane key="overtime" :title="t('outsourcingConfig.tabs.overtime')">
          <div :class="{ 'disabled-overlay': !attendanceEnabled }">
            <OvertimeConfig
              v-if="config.overtimeConf"
              v-model="config.overtimeConf"
              :enabled-leave-types="config.leaveConf?.enabledLeaveTypes || []"
            />
          </div>
        </a-tab-pane>

        <a-tab-pane key="attendanceChange" :title="t('outsourcingConfig.tabs.attendanceChange')">
          <div :class="{ 'disabled-overlay': !attendanceEnabled }">
            <AttendanceChangeConfig
              v-if="config.attendanceChangeConf"
              v-model="config.attendanceChangeConf"
            />
          </div>
        </a-tab-pane>

        <a-tab-pane key="settlement" :title="t('outsourcingConfig.tabs.settlement')">
          <div :class="{ 'disabled-overlay': !attendanceEnabled }">
            <SettlementConfig
              v-if="config.settlementConf"
              v-model="config.settlementConf"
              :enabled-leave-types="config.leaveConf?.enabledLeaveTypes || []"
            />
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-spin>

    <!-- 帮助弹窗 -->
    <a-modal
      v-model:visible="showHelp"
      :title="t('outsourcingConfig.helpTitle')"
      :footer="false"
      width="600px"
    >
      <div class="help-content">
        <div v-for="(item, index) in helpItems" :key="index" class="help-item">
          <div class="help-item-header">
            <span class="help-item-icon">{{ item.icon }}</span>
            <span class="help-item-title">{{ item.title }}</span>
          </div>
          <div class="help-item-description">{{ item.description }}</div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal } from '@arco-design/web-vue'
import { IconQuestionCircle, IconExclamationCircle } from '@arco-design/web-vue/es/icon'
import { useOutsourcingConfig } from './composables/useOutsourcingConfig'
import { useOrgStore } from '@/stores/org'
import GlobalConfig from './components/GlobalConfig.vue'
import AttendanceConfig from './components/AttendanceConfig.vue'
import LeaveConfig from './components/LeaveConfig.vue'
import OvertimeConfig from './components/OvertimeConfig.vue'
import AttendanceChangeConfig from './components/AttendanceChangeConfig.vue'
import SettlementConfig from './components/SettlementConfig.vue'

const { t } = useI18n()
const orgStore = useOrgStore()
const activeTab = ref('global')
const showHelp = ref(false)

// 检查考勤功能是否启用
const attendanceEnabled = computed(() => orgStore.currentOrg?.attendanceEnabled || false)

// 帮助内容
const helpItems = computed(() => [
  {
    icon: '⚙️',
    title: t('outsourcingConfig.tabs.global'),
    description: t('outsourcingConfig.help.global')
  },
  {
    icon: '📝',
    title: t('outsourcingConfig.tabs.attendance'),
    description: t('outsourcingConfig.help.attendance')
  },
  {
    icon: '🏖️',
    title: t('outsourcingConfig.tabs.leave'),
    description: t('outsourcingConfig.help.leave')
  },
  {
    icon: '⏰',
    title: t('outsourcingConfig.tabs.overtime'),
    description: t('outsourcingConfig.help.overtime')
  },
  {
    icon: '🔄',
    title: t('outsourcingConfig.tabs.attendanceChange'),
    description: t('outsourcingConfig.help.attendanceChange')
  },
  {
    icon: '💰',
    title: t('outsourcingConfig.tabs.settlement'),
    description: t('outsourcingConfig.help.settlement')
  }
])

const {
  config,
  loading,
  saving,
  hasUnsavedChanges,
  loadConfig,
  saveConfig,
  resetConfig
} = useOutsourcingConfig()

// 初始化加载配置
onMounted(() => {
  loadConfig()
})

// 离开页面前检查未保存的更改
onBeforeUnmount(() => {
  if (hasUnsavedChanges.value) {
    // 注意：这里只是示例，实际的路由守卫应该在路由配置中处理
    console.warn('有未保存的更改')
  }
})

/**
 * 保存配置
 */
async function handleSave() {
  try {
    await saveConfig()
  } catch (error) {
    // 错误已在 composable 中处理
    console.error('保存失败:', error)
  }
}

/**
 * 重置配置
 */
function handleReset() {
  if (hasUnsavedChanges.value) {
    Modal.confirm({
      title: t('outsourcingConfig.actions.reset'),
      content: t('outsourcingConfig.messages.hasUnsavedChanges'),
      onOk: () => {
        resetConfig()
      }
    })
  } else {
    resetConfig()
  }
}
</script>

<style scoped lang="scss">
.outsourcing-config-container {
  padding: 0;
  background: #f7f8fa;
  min-height: calc(100vh - 60px);
  width: 100%;

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 24px 32px;
    max-width: 1200px;
    margin: 0 auto;
    background: #fff;
    border-bottom: 1px solid #e5e6eb;
    position: relative;

    // 全宽背景
    &::before {
      content: '';
      position: absolute;
      left: 50%;
      right: 50%;
      margin-left: -50vw;
      margin-right: -50vw;
      width: 100vw;
      top: 0;
      bottom: 0;
      background: #fff;
      border-bottom: 1px solid #e5e6eb;
      z-index: -1;
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    h2 {
      margin: 0;
      font-size: 22px;
      font-weight: 600;
      color: #1d2129;
      letter-spacing: -0.02em;
    }

    .help-icon {
      font-size: 20px;
      color: #86909c;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        color: #165dff;
        transform: scale(1.1);
      }
    }

    .a-space {
      gap: 12px;
    }
  }

  // 禁用遮罩层
  .disabled-overlay {
    position: relative;
    pointer-events: none;
    opacity: 0.6;
    user-select: none;

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.3);
      z-index: 1;
    }
  }

  .config-tabs {
    width: 100%;
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 32px;
    box-sizing: border-box;

    :deep(.arco-tabs-nav) {
      padding: 20px 0 0;
      margin-bottom: 0;
      background: transparent;

      &::before {
        display: none;
      }
    }

    :deep(.arco-tabs-nav-tab) {
      padding: 10px 20px;
      margin-right: 8px;
      font-size: 14px;
      font-weight: 500;
      color: #4e5969;
      border-radius: 8px 8px 0 0;
      transition: all 0.2s cubic-bezier(0.34, 0.69, 0.1, 1);
      background: transparent;
      border: none;

      &:hover {
        color: #1d2129;
        background: rgba(22, 93, 255, 0.04);
      }

      &.arco-tabs-nav-tab-active {
        color: #165dff;
        background: #fff;
        box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);

        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 0;
          right: 0;
          height: 2px;
          background: #165dff;
          border-radius: 2px 2px 0 0;
        }
      }
    }

    :deep(.arco-tabs-content) {
      padding: 0;
      background: #fff;
      border-radius: 0 12px 12px 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
      min-height: 500px;
      margin-bottom: 32px;
    }

    :deep(.arco-tabs-content-item) {
      padding: 32px;
      box-sizing: border-box;
    }
  }

  // 帮助弹窗样式
  .help-content {
    .help-item {
      margin-bottom: 24px;
      padding-bottom: 24px;
      border-bottom: 1px solid #e5e6eb;

      &:last-child {
        margin-bottom: 0;
        padding-bottom: 0;
        border-bottom: none;
      }

      .help-item-header {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        .help-item-icon {
          font-size: 24px;
          line-height: 1;
        }

        .help-item-title {
          font-size: 16px;
          font-weight: 600;
          color: #1d2129;
        }
      }

      .help-item-description {
        font-size: 14px;
        color: #4e5969;
        line-height: 1.6;
        padding-left: 36px;
      }
    }
  }

  // 加载状态优化
  :deep(.arco-spin) {
    width: 100%;

    .arco-spin-icon {
      color: #165dff;
    }
  }

  // 按钮样式优化
  :deep(.arco-btn) {
    border-radius: 8px;
    font-weight: 500;
    padding: 8px 20px;
    transition: all 0.2s cubic-bezier(0.34, 0.69, 0.1, 1);

    &.arco-btn-primary {
      background: #165dff;
      border-color: #165dff;
      box-shadow: 0 2px 4px rgba(22, 93, 255, 0.2);

      &:hover:not(:disabled) {
        background: #4080ff;
        border-color: #4080ff;
        box-shadow: 0 4px 8px rgba(22, 93, 255, 0.3);
        transform: translateY(-1px);
      }

      &:active {
        transform: translateY(0);
      }
    }

    &:not(.arco-btn-primary) {
      border-color: #e5e6eb;
      color: #4e5969;

      &:hover:not(:disabled) {
        border-color: #c9cdd4;
        background: #f7f8fa;
      }
    }
  }
}
</style>
