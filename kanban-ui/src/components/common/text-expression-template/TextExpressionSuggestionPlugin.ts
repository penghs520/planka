import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Extension } from '@tiptap/core'
import type { EditorView } from '@tiptap/pm/view'

export interface SuggestionState {
  active: boolean
  triggerPos: number
  query: string
}

const suggestionPluginKey = new PluginKey<SuggestionState>('textExpressionSuggestion')

export { suggestionPluginKey }

export interface SuggestionPluginOptions {
  onActivate: (state: SuggestionState, view: EditorView) => void
  onDeactivate: () => void
  onUpdate: (state: SuggestionState, view: EditorView) => void
}

export function createSuggestionExtension(options: SuggestionPluginOptions) {
  return Extension.create({
    name: 'textExpressionSuggestion',

    addProseMirrorPlugins() {
      return [
        new Plugin<SuggestionState>({
          key: suggestionPluginKey,

          state: {
            init: () => ({ active: false, triggerPos: -1, query: '' }),
            apply(tr, prev) {
              const meta = tr.getMeta(suggestionPluginKey)
              if (meta !== undefined) return meta
              if (!prev.active) return prev

              // 如果文档发生变化，重新计算 query
              if (tr.docChanged) {
                const { $head } = tr.selection
                const cursorPos = $head.pos
                // 如果光标在触发位置之前，关闭建议
                if (cursorPos <= prev.triggerPos) {
                  return { active: false, triggerPos: -1, query: '' }
                }
                const textBetween = tr.doc.textBetween(prev.triggerPos, cursorPos, '')
                return { active: true, triggerPos: prev.triggerPos, query: textBetween }
              }

              // 选区变化但文档未变化
              if (tr.selectionSet) {
                const { $head } = tr.selection
                const cursorPos = $head.pos
                if (cursorPos <= prev.triggerPos) {
                  return { active: false, triggerPos: -1, query: '' }
                }
              }

              return prev
            },
          },

          props: {
            handleTextInput(view, from, _to, text) {
              if (text === '$') {
                const state: SuggestionState = { active: true, triggerPos: from + 1, query: '' }
                view.dispatch(view.state.tr.setMeta(suggestionPluginKey, state))
                // 延迟通知，等 DOM 更新后获取坐标
                setTimeout(() => options.onActivate(state, view), 0)
                return false
              }

              const pluginState = suggestionPluginKey.getState(view.state)
              if (pluginState?.active) {
                // 输入时更新 query — 在 apply 中自动处理
                setTimeout(() => {
                  const newState = suggestionPluginKey.getState(view.state)
                  if (newState?.active) {
                    options.onUpdate(newState, view)
                  }
                }, 0)
              }

              return false
            },

            handleKeyDown(view, event) {
              const pluginState = suggestionPluginKey.getState(view.state)
              if (!pluginState?.active) return false

              if (event.key === 'Escape') {
                const deactivated: SuggestionState = { active: false, triggerPos: -1, query: '' }
                view.dispatch(view.state.tr.setMeta(suggestionPluginKey, deactivated))
                options.onDeactivate()
                return true
              }

              // 让面板处理上下键和 Enter
              if (event.key === 'ArrowUp' || event.key === 'ArrowDown' || event.key === 'Enter') {
                return true
              }

              // Backspace 可能删除 $ 触发字符
              if (event.key === 'Backspace') {
                const { $head } = view.state.selection
                if ($head.pos <= pluginState.triggerPos) {
                  const deactivated: SuggestionState = { active: false, triggerPos: -1, query: '' }
                  view.dispatch(view.state.tr.setMeta(suggestionPluginKey, deactivated))
                  options.onDeactivate()
                }
              }

              return false
            },
          },
        }),
      ]
    },
  })
}

/**
 * 关闭建议面板（从外部调用）
 */
export function deactivateSuggestion(view: EditorView) {
  if (!view?.state) return
  try {
    const deactivated: SuggestionState = { active: false, triggerPos: -1, query: '' }
    view.dispatch(view.state.tr.setMeta(suggestionPluginKey, deactivated))
  } catch {
    // 忽略组件卸载时的状态不一致错误
  }
}
