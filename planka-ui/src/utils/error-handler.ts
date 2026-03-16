import { Modal } from '@arco-design/web-vue'
import { h } from 'vue'
import i18n from '@/i18n'

const { t } = i18n.global

/**
 * 处理引用冲突错误
 * 当删除被引用的资源时，后端返回 409 和特定格式的错误消息
 * 此函数负责解析错误消息并显示友好的提示框
 * @param error 捕获的错误对象
 * @returns boolean 是否处理了引用冲突错误
 */
export function handleReferenceConflictError(error: any): boolean {
    const errorMessage = error.message || ''

    // 检查是否为引用冲突错误
    if (errorMessage.includes('Schema被其他Schema引用，无法删除') ||
        errorMessage.includes('无法删除') ||
        errorMessage.includes('cannot be deleted') ||
        errorMessage.includes('referenced')) {

        let references = t('common.error.unknownReference')

        // 尝试解析引用方信息
        // 格式: Schema被其他Schema引用，无法删除。引用方: TYPE[NAME], TYPE[NAME]
        if (errorMessage.includes('引用方:')) {
            const parts = errorMessage.split('引用方:')
            if (parts[1]) {
                references = parts[1].trim()
            }
        } else if (errorMessage.includes('Referenced by:')) {
            const parts = errorMessage.split('Referenced by:')
            if (parts[1]) {
                references = parts[1].trim()
            }
        } else {
            // 如果没有明确格式，显示整个消息或通用提示
            references = errorMessage
        }

        Modal.warning({
            title: t('common.error.cannotDelete'),
            content: () =>
                h('div', [
                    h('div', { style: 'margin-bottom: 12px; color: var(--color-text-2);' }, t('common.error.referencedAlert')),
                    h(
                        'div',
                        {
                            style:
                                'background: var(--color-fill-2); padding: 12px; border-radius: 4px; color: var(--color-text-1); font-size: 13px; line-height: 1.6; word-break: break-all;',
                        },
                        references,
                    ),
                ]),
            okText: t('common.error.iKnow'),
            hideCancel: true,
            modalClass: 'arco-modal-simple',
        })

        return true
    }

    return false
}
