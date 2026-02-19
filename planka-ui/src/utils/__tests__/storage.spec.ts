import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { setItem, getItem, removeItem, clear } from '../storage'

describe('storage utils', () => {
  const TEST_KEY = 'test_key'
  const TEST_VALUE = { foo: 'bar' }

  beforeEach(() => {
    localStorage.clear()
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('setItem', () => {
    it('should save data to localStorage with prefix', () => {
      setItem(TEST_KEY, TEST_VALUE)
      expect(localStorage.getItem(`kanban_${TEST_KEY}`)).toBe(JSON.stringify(TEST_VALUE))
    })

    it('should handle errors gracefully', () => {
      // Simulate quota exceeded or other error
      const setItemSpy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('QuotaExceededError')
      })

      setItem(TEST_KEY, TEST_VALUE)
      expect(console.error).toHaveBeenCalled()

      setItemSpy.mockRestore()
    })
  })

  describe('getItem', () => {
    it('should retrieve data from localStorage', () => {
      localStorage.setItem(`kanban_${TEST_KEY}`, JSON.stringify(TEST_VALUE))
      const result = getItem(TEST_KEY)
      expect(result).toEqual(TEST_VALUE)
    })

    it('should return defaultValue if item does not exist', () => {
      const defaultValue = 'default'
      const result = getItem('non_existent', defaultValue)
      expect(result).toBe(defaultValue)
    })

    it('should return undefined if item does not exist and no default value', () => {
      const result = getItem('non_existent')
      expect(result).toBeUndefined()
    })

    it('should handle JSON parse errors gracefully', () => {
      localStorage.setItem(`kanban_${TEST_KEY}`, 'invalid json')
      const result = getItem(TEST_KEY, 'default')
      expect(result).toBe('default')
      expect(console.error).toHaveBeenCalled()
    })
  })

  describe('removeItem', () => {
    it('should remove item from localStorage', () => {
      localStorage.setItem(`kanban_${TEST_KEY}`, JSON.stringify(TEST_VALUE))
      removeItem(TEST_KEY)
      expect(localStorage.getItem(`kanban_${TEST_KEY}`)).toBeNull()
    })
  })

  describe('clear', () => {
    it('should remove only kanban prefixed items', () => {
      localStorage.setItem('kanban_1', '1')
      localStorage.setItem('kanban_2', '2')
      localStorage.setItem('other_1', '1')

      clear()

      expect(localStorage.getItem('kanban_1')).toBeNull()
      expect(localStorage.getItem('kanban_2')).toBeNull()
      expect(localStorage.getItem('other_1')).toBe('1')
    })
  })
})
