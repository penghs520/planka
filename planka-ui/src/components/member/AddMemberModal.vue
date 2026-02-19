<script setup lang="ts">
import { ref, reactive, watch, computed, onMounted } from 'vue'
import { memberApi } from '@/api/member'
import { Message } from '@arco-design/web-vue'
import type { AddMemberRequest, MemberCardTypeOption } from '@/types/member'
import { OrganizationRole, OrganizationRoleConfig } from '@/types/member'
import SaveButton from '@/components/common/SaveButton.vue'
import CancelButton from '@/components/common/CancelButton.vue'

const props = defineProps<{
  visible: boolean
  orgId: string
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const loading = ref(false)
const formRef = ref()

// 成员卡片类型列表
const memberCardTypes = ref<MemberCardTypeOption[]>([])

// 表单数据
const formData = reactive<AddMemberRequest>({
  email: '',
  nickname: '',
  role: OrganizationRole.MEMBER,
  cardTypeId: undefined,
})

// 是否需要显示类型选择
const showCardTypeSelect = computed(() => memberCardTypes.value.length > 1)

const roleOptions = [
  { value: OrganizationRole.ADMIN, label: OrganizationRoleConfig[OrganizationRole.ADMIN].label },
  { value: OrganizationRole.MEMBER, label: OrganizationRoleConfig[OrganizationRole.MEMBER].label },
]

// 构造成员属性集ID
const memberAbstractTypeId = computed(() => `${props.orgId}:member-trait`)

// 加载成员卡片类型列表
async function loadMemberCardTypes() {
  try {
    const res = await memberApi.getMemberCardTypes(memberAbstractTypeId.value)
    memberCardTypes.value = res
    // 如果只有一个类型，自动选中
    if (res.length === 1 && res[0]) {
      formData.cardTypeId = res[0].id
    }
  } catch {
    // 错误已在拦截器处理
    memberCardTypes.value = []
  }
}

onMounted(() => {
  loadMemberCardTypes()
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱' },
    { type: 'email' as const, message: '请输入正确的邮33箱格式' },
  ],
  nickname: [
    { required: true, message: '请输入昵称' },
    { maxLength: 100, message: '昵称最多100个字符' },
  ],
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      formData.email = ''
      formData.nickname = ''
      formData.role = OrganizationRole.MEMBER
      // 如果只有一个类型，自动选中；否则清空
      const firstType = memberCardTypes.value[0]
      formData.cardTypeId = memberCardTypes.value.length === 1 && firstType ? firstType.id : undefined
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
    await memberApi.add(formData)
    Message.success('成员添加成功')
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
    title="添加成员"
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
      <a-form-item field="email" label="邮箱" required>
        <a-input
          v-model="formData.email"
          placeholder="请输入成员邮箱"
        />
        <template #extra>
          如果用户不存在，将自动创建账号并使用默认密码登录，首次登录后需要修改密码
        </template>
      </a-form-item>

      <a-form-item field="nickname" label="昵称" required>
        <a-input
          v-model="formData.nickname"
          placeholder="请输入成员昵称"
          :max-length="100"
          show-word-limit
        />
      </a-form-item>

      <a-form-item field="role" label="角色">
        <a-select v-model="formData.role" placeholder="请选择角色">
          <a-option
            v-for="option in roleOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </a-option>
        </a-select>
      </a-form-item>

      <!-- 成员类型选择（当有多个类型时显示） -->
      <a-form-item
        v-if="showCardTypeSelect"
        field="cardTypeId"
        label="成员类型"
        required
      >
        <a-select v-model="formData.cardTypeId" placeholder="请选择成员类型">
          <a-option
            v-for="option in memberCardTypes"
            :key="option.id"
            :value="option.id"
          >
            <template v-if="option.icon">
              <icon :name="option.icon" />
            </template>
            {{ option.name }}
          </a-option>
        </a-select>
      </a-form-item>
    </a-form>
    <template #footer>
      <CancelButton @click="handleClose" />
      <SaveButton :loading="loading" text="确定" @click="handleSubmit" />
    </template>
  </a-modal>
</template>
