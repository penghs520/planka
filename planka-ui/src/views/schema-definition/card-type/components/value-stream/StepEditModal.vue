<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { FormInstance } from '@arco-design/web-vue'
import { StepStatusKind, StepStatusKindConfig, type StepConfig, StatusWorkType } from '@/types/value-stream'

const props = defineProps<{
  visible: boolean
  step: StepConfig | null
  mode: 'create' | 'edit'
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: [step: StepConfig]
}>()

const formRef = ref<FormInstance>()
const formData = ref<Omit<StepConfig, 'statusList'>>({
  id: '',
  name: '',
  kind: StepStatusKind.TODO,
  sortOrder: 0,
})

const modalVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value),
})

const modalTitle = computed(() => (props.mode === 'create' ? '新建阶段' : '编辑阶段'))

// 阶段类别选项
const kindOptions = Object.entries(StepStatusKindConfig).map(([value, config]) => ({
  value,
  label: config.label,
}))

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      if (props.mode === 'edit' && props.step) {
        const { statusList: _statusList, ...rest } = props.step
        formData.value = { ...rest }
      } else {
        formData.value = {
          id: `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
          name: '',
          kind: StepStatusKind.TODO,
          sortOrder: 0,
        }
      }
    }
  },
)

async function handleConfirm() {
  const errors = await formRef.value?.validate()
  if (errors) return

  // 构建完整的 StepConfig
  const stepConfig: StepConfig = {
    ...formData.value,
    statusList: props.step?.statusList || [],
  }

  // 如果是新建且没有状态，创建一个默认状态
  if (props.mode === 'create' && stepConfig.statusList.length === 0) {
    const kindConfig = StepStatusKindConfig[formData.value.kind]
    stepConfig.statusList = [
      {
        id: `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
        name: formData.value.name || kindConfig.label,
        workType: StatusWorkType.WORKING,
        sortOrder: 0,
      },
    ]
  }

  emit('confirm', stepConfig)
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
        label="阶段名称"
        :rules="[{ required: true, message: '请输入阶段名称' }]"
      >
        <a-input v-model="formData.name" placeholder="请输入阶段名称" :max-length="50" />
      </a-form-item>

      <a-form-item
        field="kind"
        label="阶段类别"
        :rules="[{ required: true, message: '请选择阶段类别' }]"
      >
        <a-select v-model="formData.kind" placeholder="请选择阶段类别">
          <a-option v-for="opt in kindOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </a-option>
        </a-select>
      </a-form-item>

      <a-form-item field="desc" label="描述">
        <a-textarea
          v-model="formData.desc"
          placeholder="请输入阶段描述"
          :max-length="200"
          :auto-size="{ minRows: 2, maxRows: 4 }"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
