import request from './request'
import { schemaApi } from './schema'
import type {
  CardActionConfigDefinition,
  ActionExecutionResult,
} from '@/types/card-action'

const CARD_ACTION_URL = '/api/v1/schemas/card-actions'
const CARD_EXECUTE_URL = '/api/v1/cards'

/**
 * 卡片动作配置 API
 */
export const cardActionApi = {
  /**
   * 获取单个动作配置
   */
  getById(actionId: string): Promise<CardActionConfigDefinition> {
    return schemaApi.getById<CardActionConfigDefinition>(actionId)
  },

  /**
   * 批量获取动作配置
   */
  getByIds(actionIds: string[]): Promise<CardActionConfigDefinition[]> {
    return schemaApi.getByIds<CardActionConfigDefinition>(actionIds)
  },

  /**
   * 根据卡片类型 ID 获取动作配置列表
   */
  getByCardTypeId(cardTypeId: string): Promise<CardActionConfigDefinition[]> {
    return request.get(`${CARD_ACTION_URL}/by-card-type/${cardTypeId}`)
  },

  /**
   * 获取组织下所有动作配置
   */
  list(): Promise<CardActionConfigDefinition[]> {
    return request.get(CARD_ACTION_URL)
  },

  /**
   * 创建动作配置
   */
  create(definition: CardActionConfigDefinition): Promise<CardActionConfigDefinition> {
    return schemaApi.create<CardActionConfigDefinition>({ definition })
  },

  /**
   * 更新动作配置
   * <p>
   * 对于内置动作（ID 以 builtin: 开头），使用专门的更新接口。
   * 对于已持久化的动作，使用通用 Schema 更新接口。
   */
  update(
    actionId: string,
    definition: CardActionConfigDefinition,
    expectedVersion?: number,
  ): Promise<CardActionConfigDefinition> {
    // 内置动作使用专门的更新接口
    if (actionId.startsWith('builtin:')) {
      return request.put(`${CARD_ACTION_URL}/${actionId}`, { definition, expectedVersion })
    }
    return schemaApi.update<CardActionConfigDefinition>(actionId, { definition, expectedVersion })
  },

  /**
   * 删除动作配置
   */
  delete(actionId: string): Promise<void> {
    return schemaApi.delete(actionId)
  },

  /**
   * 启用动作配置
   */
  activate(actionId: string): Promise<void> {
    return schemaApi.activate(actionId)
  },

  /**
   * 停用动作配置
   */
  disable(actionId: string): Promise<void> {
    return schemaApi.disable(actionId)
  },

  /**
   * 获取动作配置的引用摘要
   */
  getReferenceSummary(actionId: string) {
    return schemaApi.getReferenceSummary(actionId)
  },

  /**
   * 获取动作配置的变更历史
   */
  getChangelog(actionId: string, limit = 50) {
    return schemaApi.getChangelog(actionId, limit)
  },
}

/**
 * 卡片动作执行 API
 */
export const cardActionExecuteApi = {
  /**
   * 获取卡片可用的动作列表
   */
  getAvailableActions(cardId: string): Promise<CardActionConfigDefinition[]> {
    return request.get(`${CARD_EXECUTE_URL}/${cardId}/available-actions`)
  },

  /**
   * 执行卡片动作
   * @param actionId 动作配置ID
   * @param cardId 卡片ID
   * @param userInputs 用户输入的字段值（可选）
   */
  execute(actionId: string, cardId: string, userInputs?: Record<string, unknown>): Promise<ActionExecutionResult> {
    return request.post(`${CARD_EXECUTE_URL}/actions/${actionId}/execute`, userInputs ? { userInputs } : null, {
      params: { cardId },
    })
  },
}
