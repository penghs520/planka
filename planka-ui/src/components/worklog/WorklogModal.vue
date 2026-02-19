<script setup lang="ts">
/**
 * 工时填报弹窗组件
 * 在卡片详情页中点击报工按钮时弹出
 */
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { Message } from '@arco-design/web-vue'
import { workloadApi } from '@/api'
import dayjs from 'dayjs'
import type { WindowConfig } from '@/types/workload'

const { t: _t } = useI18n()
const orgStore = useOrgStore()

const props = defineProps<{
  visible: boolean
  cardId: string
  cardTitle: string
}>()

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  success: []
}>()

// 状态
const submitting = ref(false)
const windowConfig = ref<WindowConfig | null>(null)

// 表单数据
const formData = ref({
  date: dayjs().format('YYYY-MM-DD'),
  hours: 8,
  description: '',
  type: 'NORMAL' as 'NORMAL' | 'OVERTIME' | 'LEAVE',
})

// 工时类型选项
const worklogTypes = [
  { label: '正常工时', value: 'NORMAL', color: 'green' },
  { label: '加班工时', value: 'OVERTIME', color: 'orange' },
  { label: '请假扣减', value: 'LEAVE', color: 'red' },
]

// 表单验证规则
const rules = {
  date: [{ required: true, message: '请选择日期' }],
  hours: [
    { required: true, message: '请输入工时' },
    {
      validator: (value: number) => {
        return value > 0 && value <= 24
      },
      message: '工时必须在 0-24 之间',
    },
  ],
  type: [{ required: true, message: '请选择工时类型' }],
}

// 检查是否有权限填报
const canSubmit = computed(() => {
  if (!windowConfig.value) return true
  const date = dayjs(formData.value.date)
  const today = dayjs()
  const diffDays = today.diff(date, 'day')
  return diffDays <= (windowConfig.value.createWindow || 7)
})

// 窗口期提示文本
const windowHint = computed(() => {
  if (!windowConfig.value) return ''
  return `只能填报最近 ${windowConfig.value.createWindow || 7} 天的工时`
})

// 获取窗口期配置
const fetchWindowConfig = async () => {
  if (!orgStore.currentOrgId) return
  try {
    windowConfig.value = await workloadApi.getWindowConfig(orgStore.currentOrgId)
  } catch (error) {
    console.error('获取窗口期配置失败:', error)
  }
}

// 检查填报权限
const checkPermission = async () => {
  if (!orgStore.currentOrgId || !orgStore.currentMemberCardId) return false
  try {
    const result = await workloadApi.checkPermission({
      orgId: orgStore.currentOrgId,
      memberId: orgStore.currentMemberCardId,
      date: formData.value.date,
      operation: 'CREATE',
    })
    return result.allowed
  } catch (error) {
    console.error('检查权限失败:', error)
    return false
  }
}

// 提交工时
const handleSubmit = async () => {
  if (!orgStore.currentOrgId) {
    Message.error('未选择组织')
    return
  }
  if (!orgStore.currentMemberCardId) {
    Message.error('未获取到成员信息')
    return
  }

  const hasPermission = await checkPermission()
  if (!hasPermission) {
    Message.error('当前不在窗口期内，无法填报')
    return
  }

  submitting.value = true
  try {
    await workloadApi.createWorklog({
      orgId: orgStore.currentOrgId,
      cardId: props.cardId,
      memberId: orgStore.currentMemberCardId,
      date: formData.value.date,
      hours: formData.value.hours,
      type: formData.value.type,
      description: formData.value.description,
    })

    Message.success('工时填报成功')
    emit('success')
    emit('update:visible', false)
    resetForm()
  } catch (error: any) {
    console.error('提交工时失败:', error)
    Message.error(error.message || '工时填报失败')
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  emit('update:visible', false)
  resetForm()
}

// 重置表单
const resetForm = () => {
  formData.value = {
    date: dayjs().format('YYYY-MM-DD'),
    hours: 8,
    description: '',
    type: 'NORMAL',
  }
}

// 日期变化时检查权限
const handleDateChange = () => {
  checkPermission()
}

// 监听弹窗显示
watch(() => props.visible, (visible) => {
  if (visible) {
    fetchWindowConfig()
    resetForm()
  }
})
</script>

<template>
  <a-modal
    :visible="visible"
    :title="`填报工时 - ${cardTitle}`"
    :width="480"
    :mask-closable="false"
    :esc-to-close="false"
    @ok="handleSubmit"
    @cancel="handleCancel"
    @update:visible="$emit('update:visible', $event)"
  >
    <a-form :model="formData" :rules="rules" layout="vertical">
      <!-- 日期选择 -->
      <a-form-item field="date" label="日期" required>
        <a-date-picker
          v-model="formData.date"
          style="width: 100%"
          :disabled-date="(current: Date) => dayjs(current).isAfter(dayjs())"
          @change="handleDateChange"
        />
      </a-form-item>

      <!-- 工时类型 -->
      <a-form-item field="type" label="工时类型" required>
        <a-radio-group v-model="formData.type" type="button" style="width: 100%">
          <a-radio
            v-for="type in worklogTypes"
            :key="type.value"
            :value="type.value"
            style="flex: 1; text-align: center"
          >
            <a-tag :color="type.color" size="small">
              {{ type.label }}
            </a-tag>
          </a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- 工时数 -->
      <a-form-item field="hours" label="工时数" required>
        <a-input-number
          v-model="formData.hours"
          :min="0"
          :max="24"
          :step="0.5"
          style="width: 100%"
          placeholder="请输入工时"
        >
          <template #append>
            <span>小时</span>
          </template>
        </a-input-number>
      </a-form-item>

      <!-- 工作内容 -->
      <a-form-item field="description" label="工作内容">
        <a-textarea
          v-model="formData.description"
          placeholder="请描述工作内容..."
          :max-length="500"
          show-word-limit
          :auto-size="{ minRows: 3, maxRows: 5 }"
        />
      </a-form-item>
    </a-form>

    <!-- 窗口期提示 -->
    <a-alert v-if="windowHint" type="info" :content="windowHint" style="margin-top: 8px" />

    <template #footer>
      <a-button @click="handleCancel">取消</a-button>
      <a-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
        提交
      </a-button>
    </template>
  </a-modal>
</template>
