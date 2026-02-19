import { describe, it, expect } from 'vitest'
import {
  getFieldName,
  isBuiltinEnumField,
  getEnumSelectedOptions,
  renderFieldValue,
} from '../field-render'
import type { EnumRenderConfig } from '@/types/view-data'
import type { CardDTO } from '@/types/card'

describe('field-render', () => {
  describe('getFieldName', () => {
    it('should return title for ColumnMeta', () => {
      const source = [{ fieldId: 'f1', title: 'Field 1' }] as any
      expect(getFieldName('f1', source)).toBe('Field 1')
    })

    it('should return name for FieldRenderMeta', () => {
      const source = [{ fieldId: 'f1', name: 'Field 1' }] as any
      expect(getFieldName('f1', source)).toBe('Field 1')
    })

    it('should return fieldId if not found', () => {
      expect(getFieldName('f1', [])).toBe('f1')
    })
  })

  describe('isBuiltinEnumField', () => {
    it('should return true for $cardStyle', () => {
      expect(isBuiltinEnumField('$cardStyle')).toBe(true)
    })

    it('should return true for $statusId', () => {
      expect(isBuiltinEnumField('$statusId')).toBe(true)
    })

    it('should return false for other fields', () => {
      expect(isBuiltinEnumField('title')).toBe(false)
      expect(isBuiltinEnumField('$createdAt')).toBe(false)
    })
  })

  describe('getEnumSelectedOptions', () => {
    it('should return selected options in order', () => {
      const fieldValue = { type: 'ENUM' as const, fieldId: 'testField', readable: true, value: ['opt2', 'opt1'] }
      const renderConfig: EnumRenderConfig = {
        type: 'ENUM',
        multiSelect: false,
        options: [
          { id: 'opt1', label: 'Option 1', enabled: true },
          { id: 'opt2', label: 'Option 2', enabled: true },
          { id: 'opt3', label: 'Option 3', enabled: true }
        ]
      }

      const result = getEnumSelectedOptions(fieldValue, renderConfig)
      expect(result).toHaveLength(2)
      // Should match the order in renderConfig options
      expect(result[0]?.id).toBe('opt1')
      expect(result[1]?.id).toBe('opt2')
    })

    it('should return empty array if no value', () => {
      expect(getEnumSelectedOptions(null, null)).toEqual([])
    })
  })

  describe('renderFieldValue', () => {
    // Basic test to ensure it runs without error for simple cases
    // Comprehensive testing would require more elaborate mock data
    it('should render dash for null/undefined values', () => {
      const card: CardDTO = { id: '1', fieldValues: {} } as any
      expect(renderFieldValue(card, 'someField')).toBe('-')
    })

    it('should render built-in created at', () => {
      const card: CardDTO = {
        id: '1',
        createdAt: new Date('2023-01-01T12:00:00').getTime()
      } as any
      const result = renderFieldValue(card, '$createdAt')
      expect(result).toContain('2023/01/01')
    })
  })
})
