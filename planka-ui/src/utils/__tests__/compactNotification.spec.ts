import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

const successSpy = vi.fn()
const errorSpy = vi.fn()

vi.mock('@arco-design/web-vue', () => ({
  Notification: {
    success: successSpy,
    info: vi.fn(),
    warning: vi.fn(),
    error: errorSpy,
  },
}))

describe('compactNotification', () => {
  beforeEach(() => {
    vi.resetModules()
    successSpy.mockClear()
    errorSpy.mockClear()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('applies compact class, bottomRight, duration and merges style', async () => {
    const { compactNotification } = await import('../compactNotification')
    compactNotification.success({
      content: 'ok',
    })
    expect(successSpy).toHaveBeenCalledTimes(1)
    const arg = successSpy.mock.calls[0]![0]!
    expect(arg.class).toContain('planka-compact-notification')
    expect(arg.position).toBe('bottomRight')
    expect(arg.duration).toBe(2500)
    expect(arg.style).toMatchObject({
      padding: '8px 12px',
      minWidth: 'auto',
      width: 'fit-content',
    })
    expect(typeof arg.content).toBe('function')
  })

  it('allows overriding position and duration', async () => {
    const { compactNotification } = await import('../compactNotification')
    compactNotification.success({
      content: 'x',
      position: 'topRight',
      duration: 1000,
    })
    const arg = successSpy.mock.calls[0]![0]!
    expect(arg.position).toBe('topRight')
    expect(arg.duration).toBe(1000)
  })
})
