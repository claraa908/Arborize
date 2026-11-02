// @vitest-environment node
import { describe, expect, it } from 'vitest'
import {
  criarPayloadAtualizacaoArvore,
  criarPayloadCadastroArvore,
  localizarBairroUnico,
  novoFormularioArvore,
  normalizarNomeTerritorial,
  preencherFormularioArvore,
  validarCadastroArvore,
} from '@/components/inventario/arvoreCadastroForm'
import { arvore } from './inventarioFixtures'

const idsEspecies = new Set([7])
const bairros = [
  { id: 1, nome: 'Bôto', ativo: true },
  { id: 2, nome: 'Centro', ativo: true },
  { id: 3, nome: 'Centro', ativo: true },
  { id: 4, nome: 'Bairro inativo', ativo: false },
]
const idsBairros = new Set([1, 2, 3])

function formularioUrbanoValido() {
  return {
    ...novoFormularioArvore(),
    especieId: '7',
    latitude: '-4.97',
    longitude: '-39.015',
    bairroId: '2',
  }
}

describe('formulário territorial urbano/rural', () => {
  it('inicia no modo urbano e mantém campos técnicos opcionais vazios', () => {
    const formulario = novoFormularioArvore()
    expect(formulario.zonaRural).toBe(false)
    expect(formulario.bairroId).toBe('')
    expect(formulario.localidadeRural).toBe('')
    expect(formulario.condicaoFitossanitaria).toBe('NAO_AVALIADA')
    expect(formulario.dataPlantio).toBe('')
    expect(formulario.dapCm).toBe('')
    expect(formulario.alturaEstimadaM).toBe('')
  })

  it('exige uma opção de bairro ativa no modo urbano', () => {
    const formulario = formularioUrbanoValido()
    expect(validarCadastroArvore(formulario, idsEspecies, idsBairros)).toEqual({})
    formulario.bairroId = ''
    expect(validarCadastroArvore(formulario, idsEspecies, idsBairros).bairroId).toContain(
      'bairro ativo',
    )
    formulario.bairroId = '4'
    expect(validarCadastroArvore(formulario, idsEspecies, idsBairros).bairroId).toContain(
      'bairro ativo',
    )
  })

  it('exige localidade no modo rural e dispensa associação urbana', () => {
    const formulario = {
      ...formularioUrbanoValido(),
      zonaRural: true,
      bairroId: '',
      localidadeRural: 'Sítio Boa Vista',
    }
    expect(validarCadastroArvore(formulario, idsEspecies, idsBairros)).toEqual({})
    formulario.localidadeRural = '  '
    expect(validarCadastroArvore(formulario, idsEspecies, idsBairros).localidadeRural).toContain(
      'localidade rural',
    )
  })

  it('só pré-seleciona uma correspondência ativa única normalizada', () => {
    expect(normalizarNomeTerritorial('  BÔTO   ')).toBe('boto')
    expect(localizarBairroUnico('bôto', bairros)?.id).toBe(1)
    expect(localizarBairroUnico('Centro', bairros)).toBeNull()
    expect(localizarBairroUnico('Bairro inativo', bairros)).toBeNull()
    expect(localizarBairroUnico('Sem correspondência', bairros)).toBeNull()
  })

  it('envia cadastro urbano por ID e nula os campos rurais/incompatíveis', () => {
    const formulario = formularioUrbanoValido()
    formulario.cep = '06390-000'
    formulario.referenciaLocalizacao = 'Praça central'
    formulario.dataPlantio = '2020-06-15'
    formulario.dapCm = '12.5'
    formulario.observacoes = 'Registro de campo'

    expect(criarPayloadCadastroArvore(formulario)).toEqual({
      especieId: 7,
      latitude: -4.97,
      longitude: -39.015,
      zonaRural: false,
      bairroId: 2,
      bairroNome: null,
      localidadeRural: null,
      cep: '06390-000',
      referenciaLocalizacao: 'Praça central',
      dataPlantio: '2020-06-15',
      dapCm: 12.5,
      alturaEstimadaM: null,
      condicaoFitossanitaria: 'NAO_AVALIADA',
      observacoes: 'Registro de campo',
    })
  })

  it('envia cadastro rural por localidade sem atribuir bairro urbano', () => {
    const formulario = {
      ...formularioUrbanoValido(),
      zonaRural: true,
      bairroId: '2',
      localidadeRural: ' Comunidade São João ',
    }

    expect(criarPayloadCadastroArvore(formulario)).toMatchObject({
      zonaRural: true,
      bairroId: null,
      bairroNome: null,
      localidadeRural: 'Comunidade São João',
    })
  })

  it('carrega árvores existentes e não converte bairro legado textual em seleção', () => {
    const preenchido = preencherFormularioArvore({
      ...arvore,
      classificacaoTerritorial: 'BAIRRO_NAO_IDENTIFICADO',
      bairroNome: 'Centro',
      cep: '01234-000',
    })
    expect(preenchido).toMatchObject({
      zonaRural: false,
      bairroId: '',
      localidadeRural: '',
      cep: '01234-000',
    })
    expect(preenchido).not.toHaveProperty('id')
    expect(preenchido).not.toHaveProperty('criadoEm')
  })

  it('carrega contexto rural legado sem inferir localidade ausente', () => {
    const preenchido = preencherFormularioArvore({
      ...arvore,
      classificacaoTerritorial: 'FORA_DA_AREA_URBANA',
      zonaRural: true,
      localidadeRural: null,
    })
    expect(preenchido.zonaRural).toBe(true)
    expect(preenchido.localidadeRural).toBe('')
  })

  it('gera PUT urbano completo e limpa valores opcionais sem perder o bairro', () => {
    const formulario = formularioUrbanoValido()
    formulario.dapCm = ''
    formulario.observacoes = '  '
    expect(criarPayloadAtualizacaoArvore(formulario)).toMatchObject({
      zonaRural: false,
      bairroId: 2,
      localidadeRural: null,
      dapCm: null,
      observacoes: null,
      condicaoFitossanitaria: 'NAO_AVALIADA',
    })
  })

  it('monta payload numérico mesmo quando o v-model fornece number', () => {
    const formulario = formularioUrbanoValido()
    formulario.dapCm = 12.5 as unknown as string
    formulario.alturaEstimadaM = 4 as unknown as string

    expect(criarPayloadCadastroArvore(formulario)).toMatchObject({
      dapCm: 12.5,
      alturaEstimadaM: 4,
    })
  })
})
