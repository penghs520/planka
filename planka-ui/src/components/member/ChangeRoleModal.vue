<script setup lang="ts">
import { ref, watch } from 'vue'
import { memberApi } from '@/api/member'
import { Message } from '@arco-design/web-vue'
import type { MemberDTO } from '@/types/member'
import { OrganizationRole, OrganizationRoleConfig } from '@/types/member'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'

const props = defineProps<{
  visible: boolean
  member: MemberDTO
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const loading = ref(false)
const selectedRole = ref<OrganizationRole>(OrganizationRole.MEMBER)

const roleOptions = [
  { value: OrganizationRole.ADMIN, label: OrganizationRoleConfig[OrganizationRole.ADMIN].label },
  { value: OrganizationRole.MEMBER, label: OrganizationRoleConfig[OrganizationRole.MEMBER].label },
]

watch(
  () => props.visible,
  (val) => {
    if (val && props.member) {
      selectedRole.value = props.member.role
    }
  },
)

function handleClose() {
  emit('update:visible', false)
}

async function handleSubmit() {
  if (selectedRole.value === props.member.role) {
    handleClose()
    return
  }

  loading.value = true
  try {
    await memberApi.changeRole(props.member.id, { role: selectedRole.value })
    Message.success('角色修改成功')
    emit('success')
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
    title="修改成员角色"
    :width="400"
    :mask-closable="false"
    :ok-loading="loading"
    @cancel="handleClose"
    @ok="handleSubmit"
  >
    <div class="member-info">
      <a-avatar :size="40" :image-url="member.avatar || undefined">
        {{ member.nickname?.charAt(0) }}
      </a-avatar>
      <div class="member-details">
        <div class="member-name">{{ member.nickname }}</div>
        <div class="member-email">{{ member.email }}</div>
      </div>
    </div>

    <a-divider />

    <a-form layout="vertical">
      <a-form-item label="选择角色">
        <a-radio-group v-model="selectedRole" direction="vertical">
          <a-radio
            v-for="option in roleOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
    <template #footer>
      <CancelButton @click="handleClose" />
      <SaveButton :loading="loading" text="确定" @click="handleSubmit" />
    </template>
  </a-modal>
</template>

<style scoped>
.member-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.member-details {
  flex: 1;
}

.member-name {
  font-weight: 500;
  color: var(--color-text-1);
}

.member-email {
  font-size: 13px;
  color: var(--color-text-3);
}
</style>
