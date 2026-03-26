import { inject, type InjectionKey } from 'vue'

/**
 * 工作流全屏编辑器内，Arco Select/Trigger/Dropdown 等应挂载的容器（selector）。
 * 浮层挂在 wf-editor-overlay 内的 #workflow-popup-root，与全屏层同属一棵子树，不依赖全局 CSS 抢 z-index。
 */
export const WORKFLOW_POPUP_CONTAINER_KEY: InjectionKey<string> = Symbol('workflowPopupContainer')

export const WORKFLOW_POPUP_CONTAINER_SELECTOR = '#workflow-popup-root'

export function useWorkflowPopupContainer(): string | undefined {
  return inject(WORKFLOW_POPUP_CONTAINER_KEY, undefined)
}
