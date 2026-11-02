// @vitest-environment node
import { afterEach, describe, expect, it, vi } from 'vitest'
import { AxiosError } from 'axios'
import { api } from '@/services/api'
import {
  inventarioService,
  InventarioApiError,
  interpretarErroInventario,
} from '@/services/inventarioService'
import {
  arvore,
  resumo,
  pagina,
  cadastro,
  atualizacao,
  resposta,
  falhaHttp,
} from './inventarioFixtures'

const feedbackLocalDeErro = { suppressGlobalErrorToast: true }

afterEach(() => vi.restoreAllMocks())

describe('inventarioService: contratos HTTP sem rede', () => {
  it('suprime toast global no catálogo que apresenta erro local no formulário', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(resposta([]))
    await inventarioService.getBairros()
    expect(get).toHaveBeenCalledExactlyOnceWith('/api/bairros', feedbackLocalDeErro)
  })

  it('lista com página zero e tamanho padrão do backend', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(resposta(pagina))
    expect(await inventarioService.getAll()).toEqual(pagina)
    expect(get).toHaveBeenCalledExactlyOnceWith('/api/arvores', {
      params: { page: 0, size: 20 },
      ...feedbackLocalDeErro,
    })
  })

  it('envia pesquisa, filtros e paginação pelos nomes reais, sem incrementar a página', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(resposta(pagina))
    await inventarioService.getAll({
      search: ' nome ',
      bairro: ' Centro ',
      especieId: 7,
      condicao: 'MORTA',
      page: 2,
      size: 10,
    })
    expect(get).toHaveBeenCalledWith('/api/arvores', {
      params: { search: 'nome', bairro: 'Centro', especieId: 7, condicao: 'MORTA', page: 2, size: 10 },
      ...feedbackLocalDeErro,
    })
  })

  it('omite filtros ausentes/nulos e pesquisa em branco sem eliminar valores zero', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(resposta(pagina))
    await inventarioService.getAll({
      search: '  ',
      bairro: '  ',
      especieId: undefined,
      condicao: null,
      page: 0,
    })
    expect(get).toHaveBeenLastCalledWith('/api/arvores', {
      params: { page: 0, size: 20 },
      ...feedbackLocalDeErro,
    })
    await inventarioService.getAll({ especieId: 0, size: 0 })
    expect(get).toHaveBeenLastCalledWith('/api/arvores', {
      params: { especieId: 0, page: 0, size: 0 },
      ...feedbackLocalDeErro,
    })
  })

  it('mapa envia somente filtros, mesmo recebendo objeto que também tenha paginação', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(resposta([resumo]))
    const params = {
      search: 'nome',
      especieId: 7,
      bairro: 'Centro',
      condicao: 'MORTA' as const,
      page: 9,
      size: 1,
    }
    expect(await inventarioService.getMapa(params)).toEqual([resumo])
    expect(get).toHaveBeenCalledExactlyOnceWith('/api/arvores/mapa', {
      params: { search: 'nome', bairro: 'Centro', especieId: 7, condicao: 'MORTA' },
      ...feedbackLocalDeErro,
    })
  })

  it('consulta a ficha pelo identificador', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue(resposta(arvore))
    expect(await inventarioService.getById(42)).toEqual(arvore)
    expect(get).toHaveBeenCalledExactlyOnceWith('/api/arvores/42', feedbackLocalDeErro)
  })

  it('cadastra JSON sem inventar valores para os campos omitidos', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue(resposta(arvore, 201))
    const original = structuredClone(cadastro)
    expect(await inventarioService.create(cadastro)).toEqual(arvore)
    expect(post).toHaveBeenCalledExactlyOnceWith('/api/arvores', cadastro, feedbackLocalDeErro)
    expect(cadastro).toEqual(original)
    expect(cadastro).not.toHaveProperty('dataPlantio')
  })

  it('atualiza via PUT completo preservando nulos explícitos e o payload original', async () => {
    const put = vi.spyOn(api, 'put').mockResolvedValue(resposta(arvore))
    const original = structuredClone(atualizacao)
    expect(await inventarioService.update(42, atualizacao)).toEqual(arvore)
    expect(put).toHaveBeenCalledExactlyOnceWith(
      '/api/arvores/42',
      atualizacao,
      feedbackLocalDeErro,
    )
    expect(atualizacao).toEqual(original)
    expect(atualizacao.dapCm).toBeNull()
  })

  it.each([
    ['GET lista', 'get', () => inventarioService.getAll()],
    ['GET mapa', 'get', () => inventarioService.getMapa()],
    ['GET ficha', 'get', () => inventarioService.getById(42)],
    ['POST', 'post', () => inventarioService.create(cadastro)],
    ['PUT', 'put', () => inventarioService.update(42, atualizacao)],
  ] as const)('%s propaga falha HTTP, sem fabricar resultado', async (_label, method, request) => {
    const original = falhaHttp(500)
    vi.spyOn(api, method).mockRejectedValue(original)
    await expect(request()).rejects.toMatchObject({
      name: 'InventarioApiError',
      status: 500,
      cause: original,
    })
  })

  it.each([401, 403])('trata %i sem ProblemDetail como acesso negado', async (status) => {
    vi.spyOn(api, 'get').mockRejectedValue(falhaHttp(status))
    await expect(inventarioService.getAll()).rejects.toMatchObject({
      status,
      acessoNegado: true,
      problem: null,
      message: 'Acesso negado ao inventário.',
    })
  })

  it('interpreta ProblemDetail e erros de campo, descartando propriedades desconhecidas', async () => {
    vi.spyOn(api, 'put').mockRejectedValue(
      falhaHttp(400, {
        type: 'urn:arborize:inventario:entrada-invalida',
        title: 'Requisição não concluída',
        status: 400,
        detail: 'Confira os campos informados.',
        code: 'entrada-invalida',
        instance: '/api/arvores/42',
        errors: [
          { field: 'dapCm', message: 'O DAP deve ser positivo.', segredo: 'não copiar' },
          { field: 3 },
        ],
        segredo: 'não copiar',
      }),
    )
    await expect(inventarioService.update(42, atualizacao)).rejects.toMatchObject({
      status: 400,
      acessoNegado: false,
      problem: {
        code: 'entrada-invalida',
        errors: [{ field: 'dapCm', message: 'O DAP deve ser positivo.' }],
      },
    })
    const error = interpretarErroInventario(
      falhaHttp(400, { type: 't', title: 't', status: 400, detail: 'd', segredo: 'x' }),
    )
    expect(error.problem).not.toHaveProperty('segredo')
  })

  it('mantém o status HTTP como autoridade mesmo se o corpo tiver outro status', () => {
    const error = interpretarErroInventario(
      falhaHttp(403, { type: 't', title: 't', status: 404, detail: 'Não encontrado' }),
    )
    expect(error.status).toBe(403)
    expect(error.acessoNegado).toBe(true)
    expect(error.message).toBe('Acesso negado ao inventário.')
  })

  it('distingue 404 de acesso negado', () => {
    expect(interpretarErroInventario(falhaHttp(404))).toMatchObject({
      status: 404,
      acessoNegado: false,
      message: 'O recurso solicitado não foi encontrado.',
    })
  })

  it('não apresenta HTML ou corpo arbitrário como mensagem de falha', () => {
    const error = interpretarErroInventario(falhaHttp(500, '<html>detalhe interno</html>'))
    expect(error).toBeInstanceOf(InventarioApiError)
    expect(error.problem).toBeNull()
    expect(error.message).not.toContain('detalhe interno')
    expect(error.status).toBe(500)
  })

  it('distingue falha de rede sem inventar status HTTP', () => {
    const cause = new AxiosError('Network Error', 'ERR_NETWORK')
    expect(interpretarErroInventario(cause)).toMatchObject({
      status: null,
      problem: null,
      acessoNegado: false,
      cause,
    })
  })

  it('suporta erros inesperados e normalização repetida', () => {
    const normalized = interpretarErroInventario(new Error('interno'))
    expect(normalized.status).toBeNull()
    expect(normalized.message).not.toContain('interno')
    expect(interpretarErroInventario(normalized)).toBe(normalized)
  })

  it('lista metadados das fotografias por árvore', async () => {
    const metadados = [
      {
        id: 5,
        arvoreId: 42,
        contentType: 'image/jpeg' as const,
        sizeBytes: 1024,
        criadoEm: '2026-06-01T10:30:00',
      },
    ]
    const get = vi.spyOn(api, 'get').mockResolvedValue(resposta(metadados))
    expect(await inventarioService.listarFotos(42)).toEqual(metadados)
    expect(get).toHaveBeenCalledExactlyOnceWith('/api/arvores/42/fotos')
  })

  it('recupera bytes pelo backend como Blob, sem expor URL de armazenamento', async () => {
    const blob = new Blob(['conteudo'], { type: 'image/png' })
    const get = vi.spyOn(api, 'get').mockResolvedValue(resposta(blob))
    expect(await inventarioService.recuperarFoto(42, 5)).toBe(blob)
    expect(get).toHaveBeenCalledExactlyOnceWith('/api/arvores/42/fotos/5', {
      responseType: 'blob',
    })
  })

  it('permite que miniaturas tratem localmente erros do conteúdo protegido', async () => {
    const blob = new Blob(['conteudo'], { type: 'image/png' })
    const get = vi.spyOn(api, 'get').mockResolvedValue(resposta(blob))
    await inventarioService.recuperarFoto(42, 5, { suppressGlobalErrorToast: true })
    expect(get).toHaveBeenCalledExactlyOnceWith('/api/arvores/42/fotos/5', {
      responseType: 'blob',
      suppressGlobalErrorToast: true,
    })
  })

  it('envia uma fotografia multipart no campo file', async () => {
    const arquivo = new File(['imagem'], 'arvore.webp', { type: 'image/webp' })
    const metadata = {
      id: 5,
      arvoreId: 42,
      contentType: 'image/webp' as const,
      sizeBytes: arquivo.size,
      criadoEm: '2026-06-01T10:30:00',
    }
    const post = vi.spyOn(api, 'post').mockResolvedValue(resposta(metadata, 201))
    expect(await inventarioService.adicionarFoto(42, arquivo)).toEqual(metadata)
    const [url, body] = post.mock.calls[0]
    expect(url).toBe('/api/arvores/42/fotos')
    expect(body).toBeInstanceOf(FormData)
    expect((body as FormData).get('file')).toMatchObject({
      name: 'arvore.webp',
      type: 'image/webp',
    })
  })

  it('remove somente a associação solicitada após DELETE confirmado', async () => {
    const remove = vi.spyOn(api, 'delete').mockResolvedValue(resposta(undefined, 204))
    await expect(inventarioService.removerFoto(42, 5)).resolves.toBeUndefined()
    expect(remove).toHaveBeenCalledExactlyOnceWith('/api/arvores/42/fotos/5')
  })

  it('propaga falha no DELETE para que a interface preserve a fotografia', async () => {
    const erro = falhaHttp(500)
    vi.spyOn(api, 'delete').mockRejectedValue(erro)
    await expect(inventarioService.removerFoto(42, 5)).rejects.toMatchObject({
      status: 500,
      acessoNegado: false,
      cause: erro,
    })
  })

  it('propaga 413, árvore inexistente e 401/403 nas operações de fotografia', async () => {
    vi.spyOn(api, 'post').mockRejectedValue(falhaHttp(413))
    await expect(
      inventarioService.adicionarFoto(
        42,
        new File(['imagem'], 'arvore.png', { type: 'image/png' }),
      ),
    ).rejects.toMatchObject({ status: 413, acessoNegado: false })

    vi.spyOn(api, 'get').mockRejectedValueOnce(falhaHttp(404))
    await expect(inventarioService.listarFotos(999)).rejects.toMatchObject({
      status: 404,
      acessoNegado: false,
    })

    for (const status of [401, 403]) {
      vi.spyOn(api, 'get').mockRejectedValueOnce(falhaHttp(status))
      await expect(inventarioService.listarFotos(42)).rejects.toMatchObject({
        status,
        acessoNegado: true,
      })
    }
  })
})
