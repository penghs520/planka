import type { SortField } from '@/types/view'
import type { Condition, ConditionNode } from '@/types/condition'
import { NodeType } from '@/types/condition'

/**
 * 与视图数据接口 {@link cn.planka.api.view.request.ViewDataRequest} 对齐的请求体
 */
export function andMergeConditions(
  a: Condition | undefined,
  b: Condition | undefined,
): Condition | undefined {
  const ra = a?.root
  const rb = b?.root
  if (!ra && !rb) {
    return undefined
  }
  if (!ra) {
    return b
  }
  if (!rb) {
    return a
  }
  return {
    root: {
      nodeType: NodeType.GROUP,
      operator: 'AND',
      children: [ra, rb] as ConditionNode[],
    },
  }
}

export function toViewDataQueryBody(input: {
  page?: number
  size?: number
  groupValue?: string
  userSorts?: SortField[]
  additionalCondition?: Condition
}): Record<string, unknown> {
  const body: Record<string, unknown> = {}
  if (input.page !== undefined) {
    body.page = input.page
  }
  if (input.size !== undefined) {
    body.size = input.size
  }
  if (input.groupValue !== undefined) {
    body.groupValue = input.groupValue
  }
  if (input.additionalCondition?.root) {
    body.additionalCondition = input.additionalCondition
  }
  if (input.userSorts?.length) {
    body.sorts = input.userSorts.map((s) => ({
      fieldId: s.field,
      direction: s.direction,
    }))
  }
  return body
}
