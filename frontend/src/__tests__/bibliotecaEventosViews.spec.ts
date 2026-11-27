import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useRouter } from 'vue-router'
import DocumentoCard from '@/components/biblioteca/DocumentoCard.vue'
import { eventoService } from '@/services/eventoService'
import { materialService } from '@/services/materialService'
import EventosView from '@/views/auth/eventos/EventosView.vue'
import CatalogoView from '@/views/public/biblioteca/CatalogoView.vue'
import type { EventoDTO } from '@/types/eventos'
import type { MaterialBackendDTO } from '@/types/library'

vi.mock('vue-router', () => ({ useRouter: vi.fn() }))

const routerPush = vi.fn()
const eventos: EventoDTO[] = [
  {
    id: 1,
    nome: 'Plantio na Praça',
    descricao: 'Evento de teste',
    dataInicio: '2026-05-01',
    dataTermino: '2026-05-01',
    localConcentracao: 'Praça Central',
    horarioInicio: '08:00',
    horarioTermino: '11:00',
    status: 'PLANEJADO',
  },
  {
    id: 2,
    nome: 'Mutirão na Escola',
    descricao: 'Outro evento de teste',
    dataInicio: '2026-05-02',
    dataTermino: '2026-05-02',
    localConcentracao: 'Escola Municipal',
    horarioInicio: '09:00',
    horarioTermino: '12:00',
    status: 'CONFIRMADO',
  },
]

const material: MaterialBackendDTO = {
  id: 23,
  title: 'Guia de teste',
  description: 'Material público demonstrativo',
  materialType: 'GUIA',
  category: 'CAATINGA',
  status: 'PUBLICADO',
  uploadDate: '2026-06-15T12:00:00',
  documents: [{ id: 1, sizeBytes: 512_000 }],
}

beforeEach(() => {
  vi.restoreAllMocks()
  setActivePinia(createPinia())
  routerPush.mockReset()
  vi.mocked(useRouter).mockReturnValue({ push: routerPush } as never)
})

describe('catálogo público da biblioteca', () => {
  it('carrega os materiais, aplica busca e categoria e abre o documento escolhido', async () => {
    const getMaterials = vi.spyOn(materialService, 'getPublicMaterials').mockResolvedValue([material])
    const getCover = vi.spyOn(materialService, 'getCoverUrl').mockReturnValue('/cover/23')
    const wrapper = mount(CatalogoView, { global: { plugins: [createPinia()] } })

    await flushPromises()
    expect(getMaterials).toHaveBeenLastCalledWith(undefined, undefined)
    expect(wrapper.text()).toContain('Guia de teste')
    expect(getCover).toHaveBeenCalledWith(23)

    await wrapper.get('input[type="text"]').setValue('caatinga')
    await flushPromises()
    expect(getMaterials).toHaveBeenLastCalledWith('caatinga', undefined)

    const caatingaButton = wrapper.findAll('button').find((button) => button.text().includes('Caatinga'))
    expect(caatingaButton).toBeDefined()
    await caatingaButton!.trigger('click')
    await flushPromises()
    expect(getMaterials).toHaveBeenLastCalledWith('caatinga', 'CAATINGA')

    await wrapper.findComponent(DocumentoCard).trigger('click')
    expect(routerPush).toHaveBeenCalledWith({
      name: 'biblioteca-documento',
      params: { id: '23' },
    })
    wrapper.unmount()
  })

  it('permite limpar os filtros quando a busca não encontra materiais', async () => {
    const getMaterials = vi.spyOn(materialService, 'getPublicMaterials').mockResolvedValue([])
    const wrapper = mount(CatalogoView, { global: { plugins: [createPinia()] } })

    await flushPromises()
    await wrapper.get('input[type="text"]').setValue('sem resultado')
    await flushPromises()
    expect(wrapper.text()).toContain('Nenhum documento encontrado')

    await wrapper.findAll('button').find((button) => button.text().includes('Limpar Filtros'))!.trigger('click')
    await flushPromises()
    expect((wrapper.get('input[type="text"]').element as HTMLInputElement).value).toBe('')
    expect(getMaterials).toHaveBeenLastCalledWith(undefined, undefined)
    wrapper.unmount()
  })
})

describe('gestão de eventos', () => {
  it('carrega os eventos, filtra por status e pesquisa por local', async () => {
    const getAll = vi.spyOn(eventoService, 'getAll').mockResolvedValue(eventos)
    const wrapper = mount(EventosView, { global: { plugins: [createPinia()] } })

    await flushPromises()
    expect(getAll).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Plantio na Praça')
    expect(wrapper.text()).toContain('Mutirão na Escola')

    const confirmedButton = wrapper.findAll('button').find((button) => button.text().includes('Confirmados'))
    expect(confirmedButton).toBeDefined()
    await confirmedButton!.trigger('click')
    expect(wrapper.text()).toContain('Mutirão na Escola')
    expect(wrapper.text()).not.toContain('Plantio na Praça')

    await wrapper.get('input[type="text"]').setValue('Escola Municipal')
    expect(wrapper.text()).toContain('Mutirão na Escola')
    await wrapper.get('input[type="text"]').setValue('local inexistente')
    expect(wrapper.text()).toContain('Nenhum evento socioambiental encontrado.')
    wrapper.unmount()
  })

  it('abre os detalhes e encaminha para o cadastro de um novo evento', async () => {
    vi.spyOn(eventoService, 'getAll').mockResolvedValue(eventos)
    const wrapper = mount(EventosView, { global: { plugins: [createPinia()] } })
    await flushPromises()

    await wrapper.get('[title="Visualizar Detalhes"]').trigger('click')
    expect(routerPush).toHaveBeenCalledWith({ name: 'eventos-detalhes', params: { id: 1 } })

    const table = wrapper.get('table')
    expect(table.classes()).toContain('lg:table-fixed')
    const headers = wrapper.findAll('thead th').map((th) => th.text().trim())
    expect(headers).toContain('Ações')
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(2)
    expect(rows[0]!.find('[title="Visualizar Detalhes"]').exists()).toBe(true)
    expect(rows[0]!.find('[title="Editar Evento"]').exists()).toBe(true)
    expect(rows[0]!.find('[title="Excluir Evento"]').exists()).toBe(true)
    expect(rows[0]!.get('[title="Plantio na Praça"]').classes()).toContain('truncate')
    expect(rows[0]!.get('[title="Praça Central"]').classes()).toContain('truncate')

    await wrapper.findAll('button').find((button) => button.text().includes('Cadastrar Evento'))!.trigger('click')
    expect(routerPush).toHaveBeenCalledWith({ name: 'eventos-cadastrar' })
    wrapper.unmount()
  })
})
