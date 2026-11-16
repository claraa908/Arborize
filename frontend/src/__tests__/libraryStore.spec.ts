// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { materialService } from '@/services/materialService'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { useLibraryStore } from '@/stores/libraryStore'
import type { MaterialBackendDTO } from '@/types/library'

const material: MaterialBackendDTO = {
  id: 12,
  title: 'Cartilha de teste',
  description: 'Conteúdo demonstrativo',
  materialType: 'CARTILHA',
  category: 'CAATINGA',
  status: 'PUBLICADO',
  uploadDate: '2026-03-14T12:00:00',
  documents: [{ id: 3, sizeBytes: 1_572_864 }],
}

beforeEach(() => {
  vi.restoreAllMocks()
  setActivePinia(createPinia())
})

describe('libraryStore', () => {
  it('converte os materiais públicos para o formato do catálogo e repassa os filtros', async () => {
    const store = useLibraryStore()
    const getPublic = vi.spyOn(materialService, 'getPublicMaterials').mockResolvedValue([material])
    vi.spyOn(materialService, 'getCoverUrl').mockReturnValue('/materials/12/cover')

    await store.fetchPublicCatalog('caatinga', 'CAATINGA')

    expect(getPublic).toHaveBeenCalledExactlyOnceWith('caatinga', 'CAATINGA')
    expect(store.publicMaterials).toEqual([
      {
        id: '12',
        titulo: material.title,
        descricao: material.description,
        categoria: 'CAATINGA',
        tipo: 'CARTILHA',
        data: new Date(material.uploadDate!).toLocaleDateString('pt-BR'),
        tamanho: '1.5 MB',
        capaUrl: '/materials/12/cover',
      },
    ])
    expect(store.isLoading).toBe(false)
  })

  it('usa valores padrão quando o material não tem arquivo ou data', async () => {
    const store = useLibraryStore()
    vi.spyOn(materialService, 'getPublicMaterials').mockResolvedValue([
      { ...material, uploadDate: undefined, documents: [] },
    ])
    vi.spyOn(materialService, 'getCoverUrl').mockReturnValue('/cover')

    await store.fetchPublicCatalog()

    expect(store.publicMaterials[0]).toMatchObject({ data: '-', tamanho: '2.0 MB', capaUrl: '/cover' })
  })

  it('apresenta erro e encerra o carregamento quando o catálogo falha', async () => {
    const store = useLibraryStore()
    const feedback = useFeedbackStore()
    const toast = vi.spyOn(feedback, 'showToast')
    vi.spyOn(materialService, 'getPublicMaterials').mockRejectedValue(new Error('offline'))

    await store.fetchPublicCatalog()

    expect(store.isLoading).toBe(false)
    expect(toast).toHaveBeenCalledWith(expect.stringContaining('Erro'), 'error')
  })

  it('mapeia a listagem administrativa e permite criar e atualizar documentos', async () => {
    const store = useLibraryStore()
    const feedback = useFeedbackStore()
    const toast = vi.spyOn(feedback, 'showToast')
    const adminMaterials = vi.spyOn(materialService, 'getAdminMaterials').mockResolvedValue([material])
    const create = vi.spyOn(materialService, 'createMaterial').mockResolvedValue(material)
    const update = vi.spyOn(materialService, 'updateMaterial').mockResolvedValue(material)

    await store.fetchAdminMaterials()
    expect(store.adminMaterials).toEqual([
      {
        id: '12',
        titulo: material.title,
        categoria: 'CAATINGA',
        status: 'PUBLICADO',
        data: new Date(material.uploadDate!).toLocaleDateString('pt-BR'),
      },
    ])

    const payload = {
      title: 'Material',
      description: 'Descrição',
      status: 'RASCUNHO' as const,
      type: 'Cartilha' as const,
      category: 'Caatinga' as const,
      file: new File(['conteúdo'], 'cartilha.pdf'),
    }
    expect(await store.createDocument(payload)).toBe(true)
    expect(create).toHaveBeenCalledExactlyOnceWith(payload)
    expect(await store.updateDocument('12', payload)).toBe(true)
    expect(update).toHaveBeenCalledExactlyOnceWith('12', payload)
    expect(adminMaterials).toHaveBeenCalledTimes(3)
    expect(toast).toHaveBeenCalledWith(expect.stringContaining('sucesso'), 'success', 4000, 'Sucesso')
    expect(store.isLoading).toBe(false)
  })

  it('retorna falha quando criação e atualização falham', async () => {
    const store = useLibraryStore()
    const feedback = useFeedbackStore()
    const toast = vi.spyOn(feedback, 'showToast')
    vi.spyOn(materialService, 'createMaterial').mockRejectedValue(new Error('create failed'))
    vi.spyOn(materialService, 'updateMaterial').mockRejectedValue(new Error('update failed'))
    const payload = {
      title: 'Material',
      description: 'Descrição',
      status: 'RASCUNHO' as const,
      type: 'Cartilha' as const,
      category: 'Caatinga' as const,
      file: new File(['conteúdo'], 'cartilha.pdf'),
    }

    expect(await store.createDocument(payload)).toBe(false)
    expect(await store.updateDocument(12, payload)).toBe(false)
    expect(toast).toHaveBeenCalledTimes(2)
    expect(store.isLoading).toBe(false)
  })

  it('só remove após confirmação e mantém o item se a exclusão falhar', async () => {
    const store = useLibraryStore()
    const feedback = useFeedbackStore()
    const confirm = vi.spyOn(feedback, 'showConfirm')
    const toast = vi.spyOn(feedback, 'showToast')
    const remove = vi.spyOn(materialService, 'deleteMaterial').mockResolvedValue()
    store.adminMaterials = [
      { id: '12', titulo: 'Cartilha', categoria: 'CAATINGA', status: 'PUBLICADO', data: '-' },
    ]

    confirm.mockResolvedValueOnce(false)
    expect(await store.removeDocument('12')).toBe(false)
    expect(remove).not.toHaveBeenCalled()
    expect(store.adminMaterials).toHaveLength(1)

    confirm.mockResolvedValueOnce(true)
    expect(await store.removeDocument('12')).toBe(true)
    expect(remove).toHaveBeenCalledExactlyOnceWith('12')
    expect(store.adminMaterials).toEqual([])
    expect(toast).toHaveBeenCalledWith(expect.stringContaining('removido'), 'success')
  })

  it('mantém o documento e informa erro quando a exclusão confirmada falha', async () => {
    const store = useLibraryStore()
    const feedback = useFeedbackStore()
    vi.spyOn(feedback, 'showConfirm').mockResolvedValue(true)
    const toast = vi.spyOn(feedback, 'showToast')
    vi.spyOn(materialService, 'deleteMaterial').mockRejectedValue(new Error('delete failed'))
    store.adminMaterials = [
      { id: '12', titulo: 'Cartilha', categoria: 'CAATINGA', status: 'PUBLICADO', data: '-' },
    ]

    expect(await store.removeDocument('12')).toBe(false)
    expect(store.adminMaterials).toHaveLength(1)
    expect(store.isLoading).toBe(false)
    expect(toast).toHaveBeenCalledWith(expect.stringContaining('excluir'), 'error')
  })
})
