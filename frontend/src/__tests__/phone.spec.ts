// @vitest-environment node
import { describe, expect, it } from 'vitest'
import { formatBrazilianPhone } from '@/utils/phone'

describe('formatBrazilianPhone', () => {
  it('formats mobile and landline numbers', () => {
    expect(formatBrazilianPhone('88999991234')).toBe('(88) 99999-1234')
    expect(formatBrazilianPhone('8833331234')).toBe('(88) 3333-1234')
  })

  it('formats partial input without adding a dangling separator', () => {
    expect(formatBrazilianPhone('88')).toBe('(88')
    expect(formatBrazilianPhone('889')).toBe('(88) 9')
    expect(formatBrazilianPhone('889999')).toBe('(88) 9999')
    expect(formatBrazilianPhone('8899999')).toBe('(88) 9999-9')
  })

  it('limits the value to eleven digits', () => {
    expect(formatBrazilianPhone('8899999123456')).toBe('(88) 99999-1234')
  })
})
