<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useOrgStore } from '@/stores/org'
import { IconMenuFold, IconMenuUnfold } from '@arco-design/web-vue/es/icon'
import UserDropdown from '@/components/auth/UserDropdown.vue'
import LocaleSwitcher from '@/components/common/LocaleSwitcher.vue'

defineProps<{
  collapsed: boolean
}>()

const emit = defineEmits<{
  'toggle-collapse': []
}>()

const userStore = useUserStore()
const orgStore = useOrgStore()

onMounted(async () => {
  // 获取用户信息和组织列表
  if (userStore.isLoggedIn && !userStore.user) {
    try {
      await userStore.fetchMe()
    } catch {
      // 错误已在拦截器处理
    }
  }
  if (userStore.isLoggedIn && orgStore.myOrgs.length === 0) {
    try {
      await orgStore.fetchMyOrganizations()
    } catch {
      // 错误已在拦截器处理
    }
  }
})
</script>

<template>
  <a-layout-header class="header">
    <div class="header-left">
      <a-button
        type="text"
        class="collapse-btn"
        @click="emit('toggle-collapse')"
      >
        <template #icon>
          <IconMenuFold v-if="!collapsed" />
          <IconMenuUnfold v-else />
        </template>
      </a-button>
    </div>

    <div class="header-right">
      <LocaleSwitcher />
      <a-divider direction="vertical" :margin="8" />
      <UserDropdown />
    </div>
  </a-layout-header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
  background-color: var(--color-bg-2);
  border-bottom: 1px solid var(--color-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.collapse-btn {
  color: var(--color-text-2);
}
</style>
