/** Arco Notification 紧凑封装（右下、内边距与图标间距收紧）。样式见 `styles/compact-notification.css`。 */
import { Notification } from '@arco-design/web-vue'
import { h } from 'vue'
import type { NotificationConfig } from '@arco-design/web-vue/es/notification/interface'

export const PLANKA_COMPACT_NOTIFICATION_CLASS = 'planka-compact-notification'

const defaultBoxStyle: NonNullable<NotificationConfig['style']> = {
  padding: '8px 12px',
  minWidth: 'auto',
  width: 'fit-content',
}

const defaultTextStyle: Record<string, string | number> = {
  fontSize: '13px',
  fontWeight: 600,
}

const defaultDuration = 2500

function mergeClass(userClass?: NotificationConfig['class']): NotificationConfig['class'] {
  if (userClass === undefined || userClass === '') {
    return PLANKA_COMPACT_NOTIFICATION_CLASS
  }
  if (typeof userClass === 'string') {
    return `${PLANKA_COMPACT_NOTIFICATION_CLASS} ${userClass}`.trim()
  }
  if (Array.isArray(userClass)) {
    return [PLANKA_COMPACT_NOTIFICATION_CLASS, ...userClass]
  }
  return [PLANKA_COMPACT_NOTIFICATION_CLASS, userClass]
}

function normalizeContent(content: NotificationConfig['content']): NotificationConfig['content'] {
  if (typeof content === 'string') {
    return () => h('span', { style: defaultTextStyle }, content)
  }
  return content
}

export type CompactNotificationConfig = Omit<NotificationConfig, 'style' | 'class'> & {
  style?: NotificationConfig['style']
  class?: NotificationConfig['class']
}

function show(
  type: 'success' | 'info' | 'warning' | 'error',
  config: CompactNotificationConfig,
): ReturnType<typeof Notification.success> {
  const { style: userStyle, class: userClass, content, position, duration, ...rest } = config
  const api = Notification[type]
  return api({
    ...rest,
    content: normalizeContent(content),
    class: mergeClass(userClass),
    style: { ...defaultBoxStyle, ...userStyle },
    position: position ?? 'bottomRight',
    duration: duration ?? defaultDuration,
  })
}

export const compactNotification = {
  success: (config: CompactNotificationConfig) => show('success', config),
  info: (config: CompactNotificationConfig) => show('info', config),
  warning: (config: CompactNotificationConfig) => show('warning', config),
  error: (config: CompactNotificationConfig) => show('error', config),
}
