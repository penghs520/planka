import { describe, it, expect } from 'vitest'
import {
  buildLinkFieldId,
  parseLinkFieldId,
  getLinkTypeId,
  getLinkPosition,
  isValidLinkFieldId
} from '../link-field-utils'

describe('link-field-utils', () => {
  const validLinkTypeId = '1234567890'
  const validSourceId = `${validLinkTypeId}:SOURCE`
  const validTargetId = `${validLinkTypeId}:TARGET`

  describe('buildLinkFieldId', () => {
    it('should build correct ID for SOURCE', () => {
      expect(buildLinkFieldId(validLinkTypeId, 'SOURCE')).toBe(validSourceId)
    })

    it('should build correct ID for TARGET', () => {
      expect(buildLinkFieldId(validLinkTypeId, 'TARGET')).toBe(validTargetId)
    })

    it('should throw error for empty linkTypeId', () => {
      expect(() => buildLinkFieldId('', 'SOURCE')).toThrow('linkTypeId不能为空')
    })
  })

  describe('parseLinkFieldId', () => {
    it('should parse valid SOURCE ID correctly', () => {
      const result = parseLinkFieldId(validSourceId)
      expect(result).toEqual({ linkTypeId: validLinkTypeId, position: 'SOURCE' })
    })

    it('should parse valid TARGET ID correctly', () => {
      const result = parseLinkFieldId(validTargetId)
      expect(result).toEqual({ linkTypeId: validLinkTypeId, position: 'TARGET' })
    })

    it('should throw error for empty input', () => {
      expect(() => parseLinkFieldId('')).toThrow('linkFieldId不能为空')
    })

    it('should throw error for invalid format', () => {
      expect(() => parseLinkFieldId('invalid-id')).toThrow('无效的linkFieldId格式')
      expect(() => parseLinkFieldId('123:')).toThrow('无效的linkFieldId格式')
      expect(() => parseLinkFieldId(':SOURCE')).toThrow('无效的linkFieldId格式') // Assuming this might be treated as empty linkTypeId which is caught by lastIndexOf check or split logic
    })
  })

  describe('getLinkTypeId', () => {
    it('should extract linkTypeId from valid ID', () => {
      expect(getLinkTypeId(validSourceId)).toBe(validLinkTypeId)
    })

    it('should return whole string if separator not found (fallback behavior)', () => {
       // Based on implementation: return idx > 0 ? ... : linkFieldId
      expect(getLinkTypeId('just-an-id')).toBe('just-an-id')
    })

    it('should throw error for empty input', () => {
      expect(() => getLinkTypeId('')).toThrow('linkFieldId不能为空')
    })
  })

  describe('getLinkPosition', () => {
    it('should extract SOURCE position', () => {
      expect(getLinkPosition(validSourceId)).toBe('SOURCE')
    })

    it('should extract TARGET position', () => {
      expect(getLinkPosition(validTargetId)).toBe('TARGET')
    })

    it('should throw error for invalid format', () => {
      expect(() => getLinkPosition('invalid')).toThrow('无效的linkFieldId格式')
    })
  })

  describe('isValidLinkFieldId', () => {
    it('should return true for valid SOURCE ID', () => {
      expect(isValidLinkFieldId(validSourceId)).toBe(true)
    })

    it('should return true for valid TARGET ID', () => {
      expect(isValidLinkFieldId(validTargetId)).toBe(true)
    })

    it('should return false for empty input', () => {
      expect(isValidLinkFieldId('')).toBe(false)
    })

    it('should return false for invalid format', () => {
      expect(isValidLinkFieldId('invalid')).toBe(false)
      expect(isValidLinkFieldId('123:UNKNOWN')).toBe(false)
      expect(isValidLinkFieldId('123:')).toBe(false)
      expect(isValidLinkFieldId(':SOURCE')).toBe(false)
    })
  })
})
