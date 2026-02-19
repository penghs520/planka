<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/hooks/useAuth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { logout } = useAuth()

const user = computed(() => userStore.user)
const displayName = computed(() => user.value?.nickname || user.value?.email || '用户')
const avatarText = computed(() => displayName.value.charAt(0).toUpperCase())

// 判断是否在管理后台
const isInAdmin = computed(() => route.path.startsWith('/admin'))

function goToProfile() {
  // 根据当前所在布局决定跳转路径
  router.push(isInAdmin.value ? '/admin/profile' : '/profile')
}

async function handleLogout() {
  await logout()
}
</script>

<template>
  <a-dropdown trigger="click" position="br">
    <div class="user-dropdown-trigger">
      <a-avatar :size="32" :image-url="user?.avatar || undefined">
        {{ avatarText }}
      </a-avatar>
      <span class="user-name">{{ displayName }}</span>
      <icon-down />
    </div>
    <template #content>
      <a-doption @click="goToProfile">
        <template #icon>
          <icon-user />
        </template>
        个人设置
      </a-doption>
      <a-divider :margin="4" />
      <a-doption @click="handleLogout">
        <template #icon>
          <icon-export />
        </template>
        退出登录
      </a-doption>
    </template>
  </a-dropdown>
</template>

<style scoped>
.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-dropdown-trigger:hover {
  background-color: var(--color-fill-2);
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--color-text-1);
}
</style>
