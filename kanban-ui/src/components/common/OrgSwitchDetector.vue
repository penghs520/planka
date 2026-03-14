<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useOrgStore } from '@/stores/org'
import { showCompactModal } from '@/utils/compactModal'

const { t } = useI18n()
const orgStore = useOrgStore()

const isModalVisible = ref(false)

onMounted(() => {
  // 监听页面可见性变化（用户切换回此标签页）
  document.addEventListener('visibilitychange', handleVisibilityChange)
  // 监听窗口聚焦（用户点击此标签页）
  window.addEventListener('focus', handleWindowFocus)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('focus', handleWindowFocus)
})

/**
 * 处理页面可见性变化
 * 当用户从其他标签页切换回当前标签页时触发
 */
function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    checkOrgChanged()
  }
}

/**
 * 处理窗口聚焦
 * 当用户点击当前标签页时触发
 */
function handleWindowFocus() {
  checkOrgChanged()
}

/**
 * 检查组织是否发生变化
 * 比较 localStorage 中的 orgId 与当前 store 中的 orgId
 */
function checkOrgChanged() {
  // 如果弹窗已经在显示，不再重复检查
  if (isModalVisible.value) return

  const storedOrgId = localStorage.getItem('orgId')
  const currentOrgId = orgStore.currentOrgId

  // 如果 localStorage 中的组织与当前组织不同，说明在其他标签页切换了组织
  if (storedOrgId && storedOrgId !== currentOrgId) {
    // 获取组织名称
    const storedOrgName = localStorage.getItem('orgName') || t('common.unknown')
    showSwitchModal(storedOrgName)
  }
}

function showSwitchModal(orgName: string) {
  isModalVisible.value = true

  // 使用紧凑型 Modal 显示组织切换提示
  showCompactModal({
    title: t('common.hint'),
    content: t('common.org.switchConfirmMessage', { name: orgName }),
    okText: t('common.org.switchConfirmButton'),
    onOk: () => {
      window.location.reload()
    },
  })
}
</script>

<template>
  <div style="display: none"></div>
</template>
