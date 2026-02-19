import { describe, it, expect } from 'vitest'
import {
  DEFAULT_PAGE_SIZE,
  MAX_PAGE_SIZE,
  API_TIMEOUT,
  SIDEBAR_WIDTH,
  SIDEBAR_COLLAPSED_WIDTH,
  HEADER_HEIGHT
} from '../constants'

describe('constants', () => {
  it('should have correct values', () => {
    expect(DEFAULT_PAGE_SIZE).toBe(20)
    expect(MAX_PAGE_SIZE).toBe(100)
    expect(API_TIMEOUT).toBe(30000)
    expect(SIDEBAR_WIDTH).toBe(220)
    expect(SIDEBAR_COLLAPSED_WIDTH).toBe(64)
    expect(HEADER_HEIGHT).toBe(56)
  })
})
