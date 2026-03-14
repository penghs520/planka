<script setup lang="ts">
import { computed } from 'vue'
import { OrganizationRole, OrganizationRoleConfig } from '@/types/member'

const props = defineProps<{
  role: OrganizationRole
}>()

const config = computed(() => {
  return OrganizationRoleConfig[props.role] || { label: props.role, color: 'gray' }
})
</script>

<template>
  <a-tag :color="config.color">{{ config.label }}</a-tag>
</template>
