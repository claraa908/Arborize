// @vitest-environment node
import { describe, expect, it } from 'vitest'
import {
  criarControleEdicaoDuranteGeocodificacao,
  novoFormularioArvore,
} from '@/components/inventario/arvoreCadastroForm'

function adiada<T>() {
  let resolver!: (valor: T) => void
  const promise = new Promise<T>((resolve) => {
    resolver = resolve
  })
  return { promise, resolver }
}

describe('reverse geocoding e preservação de edição manual', () => {
  it('preserva a referência digitada enquanto a resposta do mapa está pendente', async () => {
    const formulario = novoFormularioArvore()
    const controle = criarControleEdicaoDuranteGeocodificacao()
    const respostaGeocoder = adiada<{
      logradouro: string | null
      bairro: string | null
      bairroNome: string | null
      cep: string | null
    }>()

    controle.iniciarSolicitacao()
    const aplicacao = respostaGeocoder.promise.then((localizacao) => {
      if (controle.podeAtualizar('referenciaLocalizacao')) {
        formulario.referenciaLocalizacao = localizacao.logradouro ?? ''
      }
    })

    formulario.referenciaLocalizacao = 'Entrada manual do técnico'
    controle.registrarEdicao('referenciaLocalizacao')
    respostaGeocoder.resolver({
      logradouro: 'Rua retornada pelo geocoder',
      bairro: 'Centro',
      bairroNome: 'Centro',
      cep: '63900-000',
    })
    await aplicacao

    expect(formulario.referenciaLocalizacao).toBe('Entrada manual do técnico')
  })

  it('aplica a referência do geocoder quando o campo não foi editado após a seleção', async () => {
    const formulario = novoFormularioArvore()
    const controle = criarControleEdicaoDuranteGeocodificacao()
    const respostaGeocoder = adiada<{
      logradouro: string | null
      bairro: string | null
      bairroNome: string | null
      cep: string | null
    }>()

    controle.iniciarSolicitacao()
    const aplicacao = respostaGeocoder.promise.then((localizacao) => {
      if (controle.podeAtualizar('referenciaLocalizacao')) {
        formulario.referenciaLocalizacao = localizacao.logradouro ?? ''
      }
    })
    respostaGeocoder.resolver({
      logradouro: 'Rua identificada pelo mapa',
      bairro: null,
      bairroNome: null,
      cep: null,
    })
    await aplicacao

    expect(formulario.referenciaLocalizacao).toBe('Rua identificada pelo mapa')
  })

  it('preserva situação territorial e bairro escolhidos manualmente durante a consulta', () => {
    const controle = criarControleEdicaoDuranteGeocodificacao()
    controle.iniciarSolicitacao()
    controle.registrarEdicao('classificacaoTerritorial')
    controle.registrarEdicao('bairroNome')
    controle.registrarEdicao('cep')

    expect(controle.podeAtualizar('classificacaoTerritorial')).toBe(false)
    expect(controle.podeAtualizar('bairroNome')).toBe(false)
    expect(controle.podeAtualizar('cep')).toBe(false)
  })

  it('não aplica bairro sugerido se o usuário escolheu outro estado territorial durante a espera', () => {
    const controle = criarControleEdicaoDuranteGeocodificacao()
    controle.iniciarSolicitacao()
    controle.registrarEdicao('classificacaoTerritorial')
    expect(controle.podeAtualizar('classificacaoTerritorial')).toBe(false)
    expect(controle.podeAtualizar('bairroNome')).toBe(true)
  })
})
