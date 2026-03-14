<template>
  <div class="channel-config-page">
    <a-page-header :title="channelDef?.name" @back="handleBack">
      <template #subtitle>{{ channelDef?.description }}</template>
    </a-page-header>

    <a-card :loading="loading">
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <!-- 动态生成表单字段 -->
        <a-form-item
          v-for="field in channelDef?.configFields"
          :key="field.key"
          :label="field.label"
          :field="field.key"
          :required="field.required"
        >
          <!-- 文本输入 -->
          <a-input
            v-if="field.type === 'text'"
            v-model="formData[field.key]"
            :placeholder="field.placeholder"
          />

          <!-- 密码输入 -->
          <a-input-password
            v-else-if="field.type === 'password'"
            v-model="formData[field.key]"
            :placeholder="field.placeholder"
          />

          <!-- 数字输入 -->
          <a-input-number
            v-else-if="field.type === 'number'"
            v-model="formData[field.key]"
            :placeholder="field.placeholder"
          />

          <!-- 下拉选择 -->
          <a-select
            v-else-if="field.type === 'select'"
            v-model="formData[field.key]"
          >
            <a-option
              v-for="option in field.options"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-option>
          </a-select>

          <!-- 开关 -->
          <a-switch
            v-else-if="field.type === 'boolean'"
            v-model="formData[field.key]"
          />

          <template #extra>{{ field.description }}</template>
        </a-form-item>
      </a-form>

      <div class="form-actions">
        <a-space>
          <a-button :loading="testing" @click="handleTest">
            {{ t('admin.channel.test') }}
          </a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">
            {{ t('common.action.save') }}
          </a-button>
        </a-space>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Message } from '@arco-design/web-vue'
import { pluginApi, channelConfigApi } from '@/api/plugin'
import type { ChannelDef } from '@/api/plugin'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()

const channelId = route.params.channelId as string
const loading = ref(false)
const testing = ref(false)
const saving = ref(false)
const channelDef = ref<ChannelDef>()
const formData = ref<Record<string, any>>({})
const formRef = ref()

// 动态生成表单验证规则
const formRules = computed(() => {
  const rules: Record<string, any> = {}
  channelDef.value?.configFields.forEach(field => {
    if (field.required) {
      rules[field.key] = [
        { required: true, message: t('common.validation.required', { field: field.label }) }
      ]
    }
  })
  return rules
})

const loadChannelDef = async () => {
  loading.value = true
  try {
    const res = await pluginApi.getChannelDef(channelId)
    channelDef.value = res.data

    // 初始化表单默认值
    channelDef.value.configFields.forEach(field => {
      if (field.defaultValue) {
        formData.value[field.key] = field.defaultValue
      }
    })
  } catch {
    Message.error(t('admin.channel.loadError'))
  } finally {
    loading.value = false
  }
}

const handleTest = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  testing.value = true
  try {
    await channelConfigApi.test(channelId, formData.value)
    Message.success(t('admin.channel.testSuccess'))
  } catch {
    Message.error(t('admin.channel.testError'))
  } finally {
    testing.value = false
  }
}

const handleSave = async () => {
  const valid = await formRef.value?.validate()
  if (!valid) return

  saving.value = true
  try {
    await channelConfigApi.create({
      channelId,
      config: formData.value
    })
    Message.success(t('common.action.saveSuccess'))
    router.back()
  } catch {
    Message.error(t('common.action.saveError'))
  } finally {
    saving.value = false
  }
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  loadChannelDef()
})
</script>

<style scoped lang="scss">
.channel-config-page {
  padding: 20px;

  .form-actions {
    margin-top: 24px;
    text-align: right;
  }
}
</style>
