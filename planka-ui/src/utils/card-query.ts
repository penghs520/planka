import type { CardPageQueryRequest } from '@/types/card'
import type { Condition } from '@/types/condition'
import { NodeType } from '@/types/condition'

/**
 * 分页查询 Issue / Project / Team 列表的公共骨架
 */
export function createCardPageQuery(
  orgId: string,
  operatorId: string,
  cardTypeIds: string[],
  pageNum = 0,
  pageSize = 200,
): CardPageQueryRequest {
  return {
    queryContext: { orgId, operatorId },
    queryScope: { cardTypeIds, cardCycles: ['ACTIVE'] },
    sortAndPage: {
      page: { pageNum, pageSize },
    },
  }
}

/**
 * 关联条件：当前卡片上的 linkFieldId 指向的卡片 ID 在 cardIds 中（任一）
 */
export function conditionLinkIn(linkFieldId: string, cardIds: string[]): Condition {
  return {
    root: {
      nodeType: NodeType.LINK,
      subject: { linkFieldId },
      operator: {
        type: 'IN',
        value: { type: 'STATIC', cardIds },
      },
    },
  }
}
