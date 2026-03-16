<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrgStore } from '@/stores/org'
import { Message } from '@arco-design/web-vue'
import MemberListTab from './MemberListTab.vue'

const router = useRouter()
const orgStore = useOrgStore()

const loading = ref(false)

const currentOrg = computed(() => orgStore.currentOrg)
const isOwner = computed(() => orgStore.isOwner)
const isAdmin = computed(() => orgStore.isAdmin)

onMounted(async () => {
  if (!orgStore.currentOrgId) {
    router.push('/select-org')
    return
  }
  // 获取当前用户在组织中的角色
  loading.value = true
  try {
    await orgStore.fetchCurrentOrgRole()

    if (!isAdmin.value) {
      Message.warning('只有管理员可见')
      router.push('/')
      return
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="member-management-container">
    <a-spin :loading="loading" class="member-spin">
      <MemberListTab
        v-if="currentOrg"
        :org-id="currentOrg.id"
        :is-owner="isOwner"
        :is-admin="isAdmin"
      />
    </a-spin>
  </div>
</template>

<style scoped>
.member-management-container {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.member-spin {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.member-spin :deep(.arco-spin-content) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
</style>
