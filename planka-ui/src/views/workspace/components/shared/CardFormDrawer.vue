<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { cardApi } from '@/api'
import { useOrgStore } from '@/stores/org'
import type { CardDTO, CreateCardRequest, UpdateCardRequest } from '@/types/card'
import { pureTitle } from '@/types/card'

const props = defineProps<{
  /** 是否显示 */
  visible: boolean
  /** 模式：create 或 edit */
  mode: 'create' | 'edit'
  /** 卡片类型 ID（创建时必填） */
  cardTypeId?: string
  /** 卡片数据（编辑时必填） */
  card?: CardDTO | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', visible: boolean): void
  (e: 'success'): void
}>()

const orgStore = useOrgStore()

// 表单状态
const loading = ref(false)
const formRef = ref()
const formData = ref({
  title: '',
  description: '',
})

// 计算属性
const drawerTitle = computed(() => (props.mode === 'create' ? '新建卡片' : '编辑卡片'))
const isEdit = computed(() => props.mode === 'edit')

// 监听可见性变化，初始化表单
watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      initForm()
    }
  },
)

// 初始化表单数据
function initForm() {
  if (props.mode === 'edit' && props.card) {
    formData.value = {
      title: props.card.title?.displayValue || '',
      description: props.card.description?.value || '',
    }
  } else {
    formData.value = {
      title: '',
      description: '',
    }
  }
}

// 关闭抽屉
function handleClose() {
  emit('update:visible', false)
}

// 提交表单
async function handleSubmit() {
  // 表单验证
  const errors = await formRef.value?.validate()
  if (errors) return

  loading.value = true
  try {
    if (props.mode === 'create') {
      await createCard()
    } else {
      await updateCard()
    }
    Message.success(props.mode === 'create' ? '创建成功' : '保存成功')
    emit('success')
    handleClose()
  } catch (error: any) {
    console.error('Save card failed:', error)
  } finally {
    loading.value = false
  }
}

// 创建卡片
async function createCard() {
  if (!props.cardTypeId) {
    throw new Error('缺少卡片类型 ID')
  }

  const request: CreateCardRequest = {
    orgId: orgStore.currentOrgId!,
    typeId: props.cardTypeId,
    title: pureTitle(formData.value.title),
    description: formData.value.description || undefined,
    fieldValues: {},
  }

  await cardApi.create(request)
}

// 更新卡片
async function updateCard() {
  if (!props.card?.id) {
    throw new Error('缺少卡片 ID')
  }

  const request: UpdateCardRequest = {
    cardId: props.card.id,
    title: formData.value.title || undefined,
    description: formData.value.description || undefined,
    fieldValues: {},
  }

  await cardApi.update(request)
}

// 表单验证规则
const formRules = {
  title: [{ required: true, message: '请输入卡片标题' }],
}
</script>

<template>
  <a-drawer
    :visible="visible"
    :title="drawerTitle"
    :width="480"
    :mask-closable="false"
    unmount-on-close
    @cancel="handleClose"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      layout="vertical"
      size="small"
    >
      <a-form-item field="title" label="标题">
        <a-input
          v-model="formData.title"
          placeholder="请输入卡片标题"
          :max-length="200"
          show-word-limit
        />
      </a-form-item>

      <a-form-item field="description" label="描述">
        <a-textarea
          v-model="formData.description"
          placeholder="请输入卡片描述"
          :max-length="2000"
          :auto-size="{ minRows: 3, maxRows: 8 }"
          show-word-limit
        />
      </a-form-item>

      <!-- TODO: 动态属性字段渲染 -->
    </a-form>

    <template #footer>
      <a-space>
        <a-button @click="handleClose">取消</a-button>
        <a-button type="primary" :loading="loading" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </a-button>
      </a-space>
    </template>
  </a-drawer>
</template>
