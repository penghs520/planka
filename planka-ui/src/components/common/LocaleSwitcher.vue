<script setup lang="ts">
/**
 * 语言切换组件
 * 显示在页面右上角，用于切换界面语言
 */
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { IconLanguage } from '@arco-design/web-vue/es/icon'
import { setLocale, getLocale, supportedLocales, type SupportedLocale } from '@/i18n'

const { locale } = useI18n()

// 当前语言
const currentLocale = computed(() => getLocale())

// 当前语言显示名称
const currentLocaleName = computed(() => {
  const found = supportedLocales.find(l => l.value === currentLocale.value)
  return found?.label || currentLocale.value
})

// 切换语言
function handleLocaleChange(newLocale: SupportedLocale) {
  setLocale(newLocale)
  // 同步更新 vue-i18n 的 locale
  locale.value = newLocale
}
</script>

<template>
  <a-dropdown trigger="click" position="br">
    <a-button type="text" class="locale-btn">
      <template #icon>
        <IconLanguage />
      </template>
      <span class="locale-text">{{ currentLocaleName }}</span>
    </a-button>
    <template #content>
      <a-doption
        v-for="loc in supportedLocales"
        :key="loc.value"
        :class="{ 'locale-active': loc.value === currentLocale }"
        @click="handleLocaleChange(loc.value)"
      >
        {{ loc.label }}
      </a-doption>
    </template>
  </a-dropdown>
</template>

<style scoped>
.locale-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-2);
  padding: 0 8px;
}

.locale-btn:hover {
  color: var(--color-text-1);
}

.locale-text {
  font-size: 13px;
}

.locale-active {
  color: rgb(var(--primary-6));
  background-color: var(--color-fill-2);
}
</style>
