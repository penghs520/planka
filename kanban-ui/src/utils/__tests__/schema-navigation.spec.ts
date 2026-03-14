import { describe, it, expect, vi } from 'vitest'
import {
  isNavigableSchemaType,
  getDefaultTab,
  getSchemaTypeDisplayName
} from '../schema-navigation'
import { SchemaType } from '@/types/schema'

// Mock i18n global
vi.mock('@/i18n', () => ({
  default: {
    global: {
      t: (key: string) => key
    }
  }
}))

describe('schema-navigation', () => {
  describe('isNavigableSchemaType', () => {
    it('should return true for navigable types', () => {
      expect(isNavigableSchemaType(SchemaType.CARD_TYPE)).toBe(true)
      expect(isNavigableSchemaType(SchemaType.VIEW)).toBe(true)
    })

    it('should return false for non-navigable types', () => {
      expect(isNavigableSchemaType(SchemaType.MENU)).toBe(false)
      expect(isNavigableSchemaType('UNKNOWN_TYPE')).toBe(false)
    })
  })

  describe('getDefaultTab', () => {
    it('should return undefined when no default tab is set', () => {
      expect(getDefaultTab(SchemaType.CARD_TYPE)).toBeUndefined()
    })

    // If there were types with default tabs, we would test them here
  })

  describe('getSchemaTypeDisplayName', () => {
    it('should return translation key for known types', () => {
      expect(getSchemaTypeDisplayName(SchemaType.CARD_TYPE)).toBe('common.schemaTypeName.CARD_TYPE')
    })

    it('should return type itself for unknown types', () => {
      expect(getSchemaTypeDisplayName('UNKNOWN')).toBe('UNKNOWN')
    })
  })
})
