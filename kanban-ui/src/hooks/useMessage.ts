import { Message, Modal } from '@arco-design/web-vue'

/**
 * 消息提示 Hook
 */
export function useMessage() {
  function success(content: string) {
    Message.success(content)
  }

  function error(content: string) {
    Message.error(content)
  }

  function warning(content: string) {
    Message.warning(content)
  }

  function info(content: string) {
    Message.info(content)
  }

  function confirm(options: {
    title: string
    content: string
    okText?: string
    cancelText?: string
  }): Promise<boolean> {
    return new Promise((resolve) => {
      Modal.confirm({
        title: options.title,
        content: options.content,
        okText: options.okText || '确定',
        cancelText: options.cancelText || '取消',
        modalClass: 'arco-modal-simple', // 使用全局优化样式
        onOk: () => resolve(true),
        onCancel: () => resolve(false),
      })
    })
  }

  return {
    success,
    error,
    warning,
    info,
    confirm,
  }
}
