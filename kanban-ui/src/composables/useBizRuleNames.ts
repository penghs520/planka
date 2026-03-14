/**
 * 业务规则名称管理 Composable
 *
 * 用于批量获取业务规则名称，支持缓存和错误处理
 * 如果规则被删除，返回原名称并标记为已删除
 */
import { ref, computed } from 'vue'
import { bizRuleApi } from '@/api/biz-rule'

interface RuleNameInfo {
  name: string
  isDeleted: boolean
}

const ruleCache = new Map<string, RuleNameInfo>()
const pendingRequests = new Set<string>()

export function useBizRuleNames() {
  const loading = ref(false)
  const error = ref<string | null>(null)

  /**
   * 批量获取业务规则名称
   */
  async function fetchRuleNames(ruleIds: string[]): Promise<Map<string, RuleNameInfo>> {
    if (ruleIds.length === 0) return new Map()

    // 过滤出需要请求的规则ID
    const needFetch = ruleIds.filter(id => !ruleCache.has(id) && !pendingRequests.has(id))
    const result = new Map<string, RuleNameInfo>()

    // 从缓存获取已存在的规则
    ruleIds.forEach(id => {
      if (ruleCache.has(id)) {
        result.set(id, ruleCache.get(id)!)
      }
    })

    if (needFetch.length === 0) return result

    loading.value = true
    error.value = null

    try {
      const rules = await bizRuleApi.getByIds(needFetch)
      const foundIds = new Set(rules.map(r => r.id))

      // 处理返回的规则
      rules.forEach(rule => {
        if (rule.id) {
          const info = { name: rule.name, isDeleted: false }
          ruleCache.set(rule.id, info)
          result.set(rule.id, info)
        }
      })

      // 处理未找到的规则（已被删除）
      needFetch.forEach(id => {
        if (!foundIds.has(id)) {
          const info = { name: '', isDeleted: true }
          ruleCache.set(id, info)
          result.set(id, info)
        }
      })
    } catch {
      error.value = '获取业务规则失败'
      // 请求失败时，将所有待请求的规则标记为未知
      needFetch.forEach(id => {
        if (!result.has(id)) {
          result.set(id, { name: '', isDeleted: true })
        }
      })
    } finally {
      loading.value = false
    }

    return result
  }

  /**
   * 获取单个规则名称信息
   */
  function getRuleNameInfo(ruleId: string | undefined): RuleNameInfo | null {
    if (!ruleId) return null
    return ruleCache.get(ruleId) || null
  }

  /**
   * 清空缓存
   */
  function clearCache() {
    ruleCache.clear()
  }

  return {
    loading: computed(() => loading.value),
    error: computed(() => error.value),
    fetchRuleNames,
    getRuleNameInfo,
    clearCache,
  }
}
