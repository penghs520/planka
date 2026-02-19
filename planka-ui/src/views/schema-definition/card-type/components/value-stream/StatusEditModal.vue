<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { FormInstance } from '@arco-design/web-vue'
import {
  StatusWorkType,
  StatusWorkTypeConfig,
  type StatusConfig,
} from '@/types/value-stream'

const props = defineProps<{
  visible: boolean
  status: StatusConfig | null
  mode: 'create' | 'edit'
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: [status: StatusConfig]
}>()

const formRef = ref<FormInstance>()
const formData = ref<StatusConfig>({
  id: '',
  name: '',
  workType: StatusWorkType.WORKING,
  sortOrder: 0,
})

// workType 选项列表
const workTypeOptions = computed(() =>
  Object.entries(StatusWorkTypeConfig).map(([value, config]) => ({
    value,
    label: config.label,
  })),
)

const modalVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

const modalTitle = computed(() => (props.mode === 'create' ? '新建状态' : '编辑状态'))

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      if (props.mode === 'edit' && props.status) {
        formData.value = { ...props.status }
      } else {
        formData.value = {
          id: `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
          name: '',
          workType: StatusWorkType.WORKING,
          sortOrder: 0,
        }
      }
    }
  },
)

async function handleConfirm() {
  const errors = await formRef.value?.validate()
  if (errors) return

  emit('confirm', { ...formData.value })
  modalVisible.value = false
}
</script>

<template>
  <a-modal
    v-model:visible="modalVisible"
    :title="modalTitle"
    :width="480"
    @ok="handleConfirm"
    @cancel="modalVisible = false"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :label-col-props="{ span: 5 }"
      :wrapper-col-props="{ span: 19 }"
    >
      <a-form-item
        field="name"
        label="状态名称"
        :rules="[{ required: true, message: '请输入状态名称' }]"
      >
        <a-input v-model="formData.name" placeholder="请输入状态名称" :max-length="50" />
      </a-form-item>

      <a-form-item field="desc" label="描述">
        <a-textarea
          v-model="formData.desc"
          placeholder="请输入状态描述"
          :max-length="200"
          :auto-size="{ minRows: 2, maxRows: 4 }"
        />
      </a-form-item>

      <a-form-item field="workType" label="工作类型">
        <a-radio-group v-model="formData.workType">
          <a-radio v-for="opt in workTypeOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </a-radio>
        </a-radio-group>
        <span class="form-tip">等待：卡片处于等待状态；工作中：卡片正在被处理</span>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped>
.form-tip {
  margin-left: 8px;
  color: var(--color-text-3);
  font-size: 12px;
}
</style>
