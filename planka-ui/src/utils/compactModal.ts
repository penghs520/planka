import { Modal } from '@arco-design/web-vue'

/**
 * 紧凑型 Modal 配置接口
 */
export interface CompactModalConfig {
  /** 标题 */
  title: string
  /** 内容 */
  content: string
  /** 确认按钮文本 */
  okText: string
  /** 点击确认按钮的回调 */
  onOk?: () => void
  /** 是否隐藏取消按钮，默认 true */
  hideCancel?: boolean
  /** 是否显示右上角关闭按钮，默认 false */
  closable?: boolean
  /** 点击遮罩是否关闭，默认 false */
  maskClosable?: boolean
  /** 按 ESC 是否关闭，默认 false */
  escToClose?: boolean
  /** 取消按钮文本 */
  cancelText?: string
  /** 点击取消按钮的回调 */
  onCancel?: () => void
}

/**
 * 显示紧凑型 Modal
 * 用于组织切换、强制确认等需要用户操作的场景
 *
 * @example
 * ```typescript
 * showCompactModal({
 *   title: t('common.hint'),
 *   content: t('common.message'),
 *   okText: t('common.action.ok'),
 *   onOk: () => {
 *     // 处理确认操作
 *   },
 * })
 * ```
 */
export function showCompactModal(config: CompactModalConfig) {
  Modal.info({
    title: config.title,
    content: config.content,
    okText: config.okText,
    hideCancel: config.hideCancel ?? true,
    closable: config.closable ?? false,
    maskClosable: config.maskClosable ?? false,
    escToClose: config.escToClose ?? false,
    cancelText: config.cancelText,
    titleAlign: 'start',
    modalClass: 'compact-modal',
    onOk: config.onOk,
    onCancel: config.onCancel,
  })
}
