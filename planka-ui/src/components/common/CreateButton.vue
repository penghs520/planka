<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { IconPlusCircle } from '@arco-design/web-vue/es/icon'

const { t } = useI18n()

defineEmits(['click'])
</script>

<template>
  <a-button class="create-btn" size="small" @click="$emit('click')">
    <template #icon>
      <IconPlusCircle />
    </template>
    <slot>{{ t('common.action.create') }}</slot>
  </a-button>
</template>
