import { Node, mergeAttributes } from '@tiptap/core'

export interface ExpressionVariableOptions {
  HTMLAttributes: Record<string, unknown>
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    expressionVariable: {
      insertExpressionVariable: (attrs: { expression: string; label: string }) => ReturnType
    }
  }
}

export const ExpressionVariableNode = Node.create<ExpressionVariableOptions>({
  name: 'expressionVariable',
  group: 'inline',
  inline: true,
  atom: true,

  addOptions() {
    return { HTMLAttributes: {} }
  },

  addAttributes() {
    return {
      expression: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-expression'),
        renderHTML: (attributes) => ({ 'data-expression': attributes.expression }),
      },
      label: {
        default: '',
        parseHTML: (element) => element.getAttribute('data-label'),
        renderHTML: (attributes) => ({ 'data-label': attributes.label }),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'span[data-expression]' }]
  },

  renderHTML({ node, HTMLAttributes }): [string, Record<string, unknown>, string] {
    const label = String(node.attrs.label || node.attrs.expression || '')
    return [
      'span',
      mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, {
        class: 'text-expression-variable-chip',
        contenteditable: 'false',
      }),
      label,
    ]
  },

  addCommands() {
    return {
      insertExpressionVariable:
        (attrs) =>
        ({ chain }) => {
          return chain().insertContent({ type: this.name, attrs }).run()
        },
    }
  },
})
