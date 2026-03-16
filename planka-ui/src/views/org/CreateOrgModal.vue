<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { useOrgStore } from '@/stores/org'
import type { CreateOrganizationRequest, OrganizationDTO } from '@/types/member'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: [org: OrganizationDTO]
}>()

const orgStore = useOrgStore()
const loading = ref(false)
const formRef = ref()

const formData = reactive<CreateOrganizationRequest>({
  name: '',
  description: '',
  logo: '',
})

const rules = {
  name: [
    { required: true, message: '请输入组织名称' },
    { maxLength: 200, message: '组织名称最多200个字符' },
  ],
  description: [{ maxLength: 1000, message: '组织描述最多1000个字符' }],
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      // 重置表单
      formData.name = ''
      formData.description = ''
      formData.logo = ''
      formRef.value?.clearValidate()
    }
  },
)

function handleClose() {
  emit('update:visible', false)
}

async function handleSubmit() {
  const errors = await formRef.value?.validate()
  if (errors) return

  loading.value = true
  try {
    const org = await orgStore.createOrganization({
      name: formData.name,
      description: formData.description || undefined,
      logo: formData.logo || undefined,
    })
    emit('success', org)
    handleClose()
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <a-modal
    :visible="visible"
    title="创建组织"
    :width="480"
    :mask-closable="false"
    :ok-loading="loading"
    @cancel="handleClose"
    @ok="handleSubmit"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      layout="vertical"
    >
      <a-form-item field="name" label="组织名称" required>
        <a-input
          v-model="formData.name"
          placeholder="请输入组织名称"
          :max-length="200"
          show-word-limit
        />
      </a-form-item>

      <a-form-item field="description" label="组织描述">
        <a-textarea
          v-model="formData.description"
          placeholder="请输入组织描述（可选）"
          :max-length="1000"
          show-word-limit
          :auto-size="{ minRows: 3, maxRows: 6 }"
        />
      </a-form-item>

      <a-form-item field="logo" label="组织 Logo">
        <a-input
          v-model="formData.logo"
          placeholder="请输入 Logo URL（可选）"
        />
      </a-form-item>
    </a-form>
    <template #footer>
      <CancelButton @click="handleClose" />
      <SaveButton :loading="loading" text="确定" @click="handleSubmit" />
    </template>
  </a-modal>
</template>
