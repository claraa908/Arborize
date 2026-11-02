// @vitest-environment node
import { describe, expect, it } from 'vitest'
import {
  LIMITE_BYTES_FOTOGRAFIA,
  LIMITE_FOTOGRAFIAS_ARVORE,
  limiteFotografiasExcedido,
  validarArquivoFotografia,
} from '@/components/inventario/arvoreFotografias'

describe('validação de fotografias do inventário', () => {
  it.each([
    ['arvore.jpg', 'image/jpeg'],
    ['arvore.jpeg', 'image/jpeg'],
    ['arvore.png', 'image/png'],
    ['arvore.webp', 'image/webp'],
  ])('aceita o formato declarado de %s', (name, type) => {
    expect(validarArquivoFotografia({ name, type, size: 1024 })).toBeNull()
  })

  it.each([
    ['arvore.gif', 'image/gif'],
    ['arvore.png', 'image/jpeg'],
    ['arvore.jpg', 'application/octet-stream'],
  ])('rejeita extensão/tipo incompatível (%s, %s)', (name, type) => {
    expect(validarArquivoFotografia({ name, type, size: 1024 })).toContain(
      'use uma imagem JPG, PNG ou WebP válida',
    )
  })

  it('aceita até 2 MB e rejeita arquivo maior ou vazio', () => {
    expect(
      validarArquivoFotografia({
        name: 'arvore.jpg',
        type: 'image/jpeg',
        size: LIMITE_BYTES_FOTOGRAFIA,
      }),
    ).toBeNull()
    expect(
      validarArquivoFotografia({
        name: 'arvore.jpg',
        type: 'image/jpeg',
        size: LIMITE_BYTES_FOTOGRAFIA + 1,
      }),
    ).toContain('limite é 2 MB')
    expect(validarArquivoFotografia({ name: 'arvore.jpg', type: 'image/jpeg', size: 0 })).toContain(
      'arquivo está vazio',
    )
  })

  it('permite até três itens no total entre existentes e pendentes', () => {
    expect(LIMITE_FOTOGRAFIAS_ARVORE).toBe(3)
    expect(limiteFotografiasExcedido(2)).toBe(false)
    expect(limiteFotografiasExcedido(3)).toBe(true)
  })
})
