import type { JSONContent } from '@tiptap/core'
import type { FieldOption } from '@/types/field-option'
import type { FieldProvider } from './types'
import { SYSTEM_VARIABLES, CARD_BUILTIN_PROPERTIES } from './types'

/** 模板变量匹配正则：匹配 ${...} 格式的表达式 */
const TEMPLATE_REGEX = /\$\{([^}]+)\}/g

/**
 * 将模板字符串解析为 Tiptap JSON 文档
 * 模板语法: ${source.path1.path2.fieldId}
 */
export function parseTemplate(
  template: string,
  nameResolver?: (expression: string) => string
): JSONContent {
  if (!template) {
    return { type: 'doc', content: [{ type: 'paragraph' }] }
  }

  const lines = template.split('\n')
  const paragraphs: JSONContent[] = lines.map((line) => {
    const content = parseLineContent(line, nameResolver)
    return { type: 'paragraph', content: content.length > 0 ? content : undefined }
  })

  return { type: 'doc', content: paragraphs }
}

function parseLineContent(
  line: string,
  nameResolver?: (expression: string) => string
): JSONContent[] {
  const nodes: JSONContent[] = []

  // 重置正则表达式状态（因为使用了 g 标志）
  TEMPLATE_REGEX.lastIndex = 0

  let lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = TEMPLATE_REGEX.exec(line)) !== null) {
    // 添加匹配前的纯文本
    if (match.index > lastIndex) {
      nodes.push({ type: 'text', text: line.slice(lastIndex, match.index) })
    }

    const expression = match[1]
    if (!expression) continue

    const label = nameResolver ? nameResolver(expression) : expression

    nodes.push({
      type: 'expressionVariable',
      attrs: { expression, label },
    })

    lastIndex = match.index + match[0].length
  }

  // 添加剩余的纯文本
  if (lastIndex < line.length) {
    nodes.push({ type: 'text', text: line.slice(lastIndex) })
  }

  return nodes
}

/**
 * 将 Tiptap JSON 文档序列化为模板字符串
 */
export function serializeTemplate(doc: JSONContent): string {
  if (!doc.content) return ''

  return doc.content
    .map((paragraph) => {
      if (!paragraph.content) return ''
      return paragraph.content
        .map((node) => {
          if (node.type === 'expressionVariable') {
            return `\${${node.attrs?.expression}}`
          }
          if (node.type === 'text') {
            return node.text || ''
          }
          if (node.type === 'hardBreak') {
            return '\n'
          }
          return ''
        })
        .join('')
    })
    .join('\n')
}

/**
 * 名称解析缓存 — 用于将 expression 路径转换为显示名称
 */
export interface NameResolverCache {
  resolve: (expression: string) => string
  preload: (fieldProvider: FieldProvider, t: (key: string) => string) => Promise<void>
  /** 解析模板中所有表达式（含多级路径），完成后回调 */
  resolveTemplate: (
    template: string,
    fieldProvider: FieldProvider,
    t: (key: string) => string
  ) => Promise<void>
}

export function createNameResolverCache(): NameResolverCache {
  const cache = new Map<string, string>()
  // 字段 ID → name 的映射（按 source 分组）
  const fieldNameMap = new Map<string, string>()
  // 已加载过的 linkFieldId 集合，避免重复请求
  const loadedLinkFields = new Set<string>()

  function resolve(expression: string): string {
    return cache.get(expression) || expression
  }

  async function preload(fieldProvider: FieldProvider, t: (key: string) => string): Promise<void> {
    // 预加载系统变量名称
    const systemSourceName = t('common.textExpressionTemplate.sources.system')
    for (const sv of SYSTEM_VARIABLES) {
      const name = t(sv.nameKey)
      cache.set(`system.${sv.id}`, `${systemSourceName}.${name}`)
      fieldNameMap.set(`system:${sv.id}`, name)
    }

    // 预加载卡片字段名称
    const cardSourceName = t('common.textExpressionTemplate.sources.card')
    // 先加载内置卡片属性
    for (const prop of CARD_BUILTIN_PROPERTIES) {
      const name = t(prop.nameKey)
      cache.set(`card.${prop.id}`, `${cardSourceName}.${name}`)
      fieldNameMap.set(`card:${prop.id}`, name)
    }
    try {
      const cardFields = await fieldProvider.getCardFields()
      for (const field of cardFields) {
        cache.set(`card.${field.id}`, `${cardSourceName}.${field.name}`)
        fieldNameMap.set(`card:${field.id}`, field.name)
      }
    } catch (error) {
      console.warn('[TextExpression] Failed to preload card fields:', error)
    }

    // 预加载成员字段名称
    const memberSourceName = t('common.textExpressionTemplate.sources.member')
    try {
      const memberFields = await fieldProvider.getMemberFields()
      for (const field of memberFields) {
        cache.set(`member.${field.id}`, `${memberSourceName}.${field.name}`)
        fieldNameMap.set(`member:${field.id}`, field.name)
      }
    } catch (error) {
      console.warn('[TextExpression] Failed to preload member fields:', error)
    }
  }

  /**
   * 扫描模板中的所有 ${...} 表达式，对多级路径异步加载中间 link 字段的目标字段名称
   */
  async function resolveTemplate(
    template: string,
    fieldProvider: FieldProvider,
    t: (key: string) => string
  ): Promise<void> {
    if (!template) return

    // 重置正则表达式状态
    TEMPLATE_REGEX.lastIndex = 0

    let match: RegExpExecArray | null
    const expressions: string[] = []

    while ((match = TEMPLATE_REGEX.exec(template)) !== null) {
      const expr = match[1]
      if (expr) expressions.push(expr)
    }

    for (const expr of expressions) {
      if (cache.has(expr)) continue

      const parts = expr.split('.')
      if (parts.length < 2) continue

      const source = parts[0]
      if (!source) continue

      const sourceName = t(`common.textExpressionTemplate.sources.${source}`)
      const nameParts: string[] = [sourceName]

      // 逐级解析路径
      for (let i = 1; i < parts.length; i++) {
        const fieldId = parts[i]
        if (!fieldId) continue

        const isLast = i === parts.length - 1

        // 先尝试从已有缓存中查找名称
        const nameKey = `${source}:${fieldId}`
        let fieldName = fieldNameMap.get(nameKey)

        // 如果是卡片源且找不到名称，检查是否是内置属性
        if (!fieldName && source === 'card' && i === 1) {
          const builtinProp = CARD_BUILTIN_PROPERTIES.find((p) => p.id === fieldId)
          if (builtinProp) {
            fieldName = t(builtinProp.nameKey)
            // 缓存结果供后续使用
            fieldNameMap.set(nameKey, fieldName)
            cache.set(expr, `${sourceName}.${fieldName}`)
          }
        }

        if (!fieldName) {
          // 对于中间路径的 link 字段，需要加载其目标字段
          // 前一个字段是 link 字段，当前字段是目标字段
          if (i >= 2) {
            const linkFieldId = parts[i - 1]
            if (linkFieldId && !loadedLinkFields.has(linkFieldId)) {
              try {
                const linkedFields = await fieldProvider.getFieldsByLinkFieldId(linkFieldId)
                for (const f of linkedFields) {
                  fieldNameMap.set(`${source}:${f.id}`, f.name)
                }
                loadedLinkFields.add(linkFieldId)
              } catch (error) {
                console.warn(`[TextExpression] Failed to load linked fields for ${linkFieldId}:`, error)
              }
            }
            fieldName = fieldNameMap.get(nameKey)
          }
        }

        nameParts.push(fieldName || fieldId)

        // 如果不是最后一个且是 link 字段，预加载其目标字段
        if (!isLast && !loadedLinkFields.has(fieldId)) {
          try {
            const linkedFields = await fieldProvider.getFieldsByLinkFieldId(fieldId)
            for (const f of linkedFields) {
              fieldNameMap.set(`${source}:${f.id}`, f.name)
            }
            loadedLinkFields.add(fieldId)
          } catch (error) {
            console.warn(`[TextExpression] Failed to preload linked fields for ${fieldId}:`, error)
          }
        }
      }

      cache.set(expr, nameParts.join('.'))
    }
  }

  return { resolve, preload, resolveTemplate }
}

/**
 * 同步格式化模板字符串用于预览显示
 * 将 ${source.fieldId} 替换为可读名称
 * @param template 模板字符串
 * @param cardFields 卡片字段列表
 * @param memberFields 成员字段列表
 * @param t i18n 翻译函数
 * @param linkTargetFieldsMap Link 字段目标字段映射（key: linkFieldId, value: 目标卡片类型字段列表）
 */
export function formatExpressionTemplate(
  template: string | undefined,
  cardFields: FieldOption[],
  memberFields: FieldOption[],
  t: (key: string) => string,
  linkTargetFieldsMap: Map<string, FieldOption[]> = new Map()
): string {
  if (!template) return ''

  const cardFieldMap = new Map<string, string>()
  // 添加内置卡片属性
  for (const prop of CARD_BUILTIN_PROPERTIES) {
    cardFieldMap.set(prop.id, t(prop.nameKey))
  }
  for (const f of cardFields) cardFieldMap.set(f.id, f.name)

  const memberFieldMap = new Map<string, string>()
  for (const f of memberFields) memberFieldMap.set(f.id, f.name)

  const sourceNames: Record<string, string> = {
    card: t('common.textExpressionTemplate.sources.card'),
    member: t('common.textExpressionTemplate.sources.member'),
    system: t('common.textExpressionTemplate.sources.system'),
  }

  return template.replace(TEMPLATE_REGEX, (_match, expr: string) => {
    const parts = expr.split('.')
    if (parts.length < 2) return `\${${expr}}`

    const source = parts[0]
    if (!source) return `\${${expr}}`

    const sourceName = sourceNames[source] || source

    if (source === 'system') {
      const systemFieldId = parts[1]
      const sv = systemFieldId ? SYSTEM_VARIABLES.find((v) => v.id === systemFieldId) : undefined
      return sv ? `${sourceName}.${t(sv.nameKey)}` : `${sourceName}.${systemFieldId ?? ''}`
    }

    const fieldMap = source === 'member' ? memberFieldMap : cardFieldMap
    const nameParts = [sourceName]

    for (let i = 1; i < parts.length; i++) {
      const part = parts[i]
      if (!part) continue

      // 尝试从主字段映射中查找
      let fieldName = fieldMap.get(part)

      // 如果找不到且是多级路径的中间字段，尝试从 linkTargetFieldsMap 中查找
      if (!fieldName && i >= 2) {
        // 前一个字段可能是 link 字段
        const prevFieldId = parts[i - 1]
        if (prevFieldId) {
          const targetFields = linkTargetFieldsMap.get(prevFieldId)
          if (targetFields) {
            const targetField = targetFields.find(f => f.id === part)
            if (targetField) {
              fieldName = targetField.name
            }
          }
        }
      }

      nameParts.push(fieldName || part)
    }
    return nameParts.join('.')
  })
}
