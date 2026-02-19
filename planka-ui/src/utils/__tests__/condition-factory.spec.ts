import { describe, it, expect } from 'vitest'
import {
  createEmptyCondition,
  createDefaultConditionItem,
  isConditionEmpty,
  isConditionItemComplete,
  operatorNeedsValue,
} from '../condition-factory'
import { NodeType } from '@/types/condition'

describe('condition-factory', () => {
  describe('createEmptyCondition', () => {
    it('should create a condition with an empty root group', () => {
      const condition = createEmptyCondition()
      expect(condition.root).toBeDefined()
      expect(condition.root?.nodeType).toBe('GROUP')
      if (condition.root?.nodeType === 'GROUP') {
        expect(condition.root.children).toEqual([])
      }
    })
  })

  describe('createDefaultConditionItem', () => {
    it('should create correct item for TEXT type', () => {
      const item = createDefaultConditionItem(NodeType.TEXT)
      expect(item.nodeType).toBe(NodeType.TEXT)
      expect(item.operator.type).toBe('CONTAINS')
    })

    it('should create correct item for NUMBER type', () => {
      const item = createDefaultConditionItem(NodeType.NUMBER)
      expect(item.nodeType).toBe(NodeType.NUMBER)
      expect(item.operator.type).toBe('EQ')
    })

    // Add more type checks as needed
  })

  describe('isConditionEmpty', () => {
    it('should return true for undefined condition', () => {
      expect(isConditionEmpty(undefined)).toBe(true)
    })

    it('should return true for empty group', () => {
      const condition = createEmptyCondition()
      expect(isConditionEmpty(condition)).toBe(true)
    })

    it('should return false for non-empty group', () => {
      const condition = createEmptyCondition()
      if (condition.root?.nodeType === 'GROUP') {
        condition.root.children.push(createDefaultConditionItem(NodeType.TEXT))
      }
      expect(isConditionEmpty(condition)).toBe(false)
    })
  })

  describe('operatorNeedsValue', () => {
    it('should return false for operators that do not need value', () => {
      expect(operatorNeedsValue('IS_EMPTY')).toBe(false)
      expect(operatorNeedsValue('IS_NOT_EMPTY')).toBe(false)
      expect(operatorNeedsValue('HAS_ANY')).toBe(false)
      expect(operatorNeedsValue('IS_CURRENT_USER')).toBe(false)
    })

    it('should return true for operators that need value', () => {
      expect(operatorNeedsValue('EQ')).toBe(true)
      expect(operatorNeedsValue('CONTAINS')).toBe(true)
      expect(operatorNeedsValue('GT')).toBe(true)
    })
  })

  describe('isConditionItemComplete', () => {
    it('should return false for incomplete TEXT item', () => {
      const item = createDefaultConditionItem(NodeType.TEXT)
      // Default text item has empty value
      expect(isConditionItemComplete(item)).toBe(false)
    })

    it('should return true for complete TEXT item', () => {
      const item = createDefaultConditionItem(NodeType.TEXT)
      if (item.nodeType === 'TEXT') {
        item.subject = { fieldId: 'field1' }
        item.operator = { type: 'CONTAINS', value: 'test' }
      }
      expect(isConditionItemComplete(item)).toBe(true)
    })

    it('should return true for operators that do not need value', () => {
      const item = createDefaultConditionItem(NodeType.TEXT)
      if (item.nodeType === 'TEXT') {
        item.subject = { fieldId: 'field1' }
        item.operator = { type: 'IS_EMPTY' } // No value needed
      }
      expect(isConditionItemComplete(item)).toBe(true)
    })
  })
})
