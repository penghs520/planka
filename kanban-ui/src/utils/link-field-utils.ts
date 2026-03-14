/**
 * 关联属性ID工具函数
 *
 * 格式: "{linkTypeId}:{SOURCE|TARGET}"
 * 示例: "263671031548350464:SOURCE"
 */

export type LinkPosition = 'SOURCE' | 'TARGET'

const SEPARATOR = ':'

/**
 * 构建关联属性ID
 */
export function buildLinkFieldId(linkTypeId: string, position: LinkPosition): string {
  if (!linkTypeId) {
    throw new Error('linkTypeId不能为空')
  }
  return `${linkTypeId}${SEPARATOR}${position}`
}

/**
 * 解析关联属性ID
 */
export function parseLinkFieldId(linkFieldId: string): {
  linkTypeId: string
  position: LinkPosition
} {
  if (!linkFieldId) {
    throw new Error('linkFieldId不能为空')
  }
  const idx = linkFieldId.lastIndexOf(SEPARATOR)
  if (idx > 0) {
    const linkTypeId = linkFieldId.substring(0, idx)
    const positionStr = linkFieldId.substring(idx + 1)
    if (positionStr === 'SOURCE' || positionStr === 'TARGET') {
      return { linkTypeId, position: positionStr }
    }
  }
  throw new Error(`无效的linkFieldId格式: ${linkFieldId}`)
}

/**
 * 从关联属性ID中提取关联类型ID
 */
export function getLinkTypeId(linkFieldId: string): string {
  if (!linkFieldId) {
    throw new Error('linkFieldId不能为空')
  }
  const idx = linkFieldId.lastIndexOf(SEPARATOR)
  return idx > 0 ? linkFieldId.substring(0, idx) : linkFieldId
}

/**
 * 从关联属性ID中提取关联位置
 */
export function getLinkPosition(linkFieldId: string): LinkPosition {
  if (!linkFieldId) {
    throw new Error('linkFieldId不能为空')
  }
  const idx = linkFieldId.lastIndexOf(SEPARATOR)
  if (idx > 0 && idx < linkFieldId.length - 1) {
    const positionStr = linkFieldId.substring(idx + 1)
    if (positionStr === 'SOURCE' || positionStr === 'TARGET') {
      return positionStr
    }
  }
  throw new Error(`无效的linkFieldId格式: ${linkFieldId}`)
}

/**
 * 判断是否为有效的关联属性ID格式
 */
export function isValidLinkFieldId(linkFieldId: string): boolean {
  if (!linkFieldId) {
    return false
  }
  const idx = linkFieldId.lastIndexOf(SEPARATOR)
  if (idx <= 0 || idx >= linkFieldId.length - 1) {
    return false
  }
  const positionStr = linkFieldId.substring(idx + 1)
  return positionStr === 'SOURCE' || positionStr === 'TARGET'
}
