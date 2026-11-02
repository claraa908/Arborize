// @vitest-environment node
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AxiosError, type AxiosAdapter } from 'axios'
import { createPinia, setActivePinia } from 'pinia'
import { api } from '@/services/api'
import { useFeedbackStore } from '@/stores/feedbackStore'

const problem = {
  type: 'about:blank',
  title: 'Acesso não permitido',
  status: 403,
  detail: 'Esta consulta não está disponível.',
}

const adapterComFalha: AxiosAdapter = async (config) => {
  throw new AxiosError('Request failed', 'ERR_BAD_REQUEST', config, undefined, {
    data: problem,
    status: 403,
    statusText: 'Forbidden',
    headers: {},
    config,
  })
}

beforeEach(() => setActivePinia(createPinia()))
afterEach(() => vi.restoreAllMocks())

describe('feedback global de erros HTTP', () => {
  it('não cria toast redundante quando a tela do inventário trata o erro localmente', async () => {
    const feedback = useFeedbackStore()
    await expect(
      api.get('/api/arvores', {
        adapter: adapterComFalha,
        suppressGlobalErrorToast: true,
      }),
    ).rejects.toMatchObject({ response: { status: 403 } })

    expect(feedback.toasts).toHaveLength(0)
  })

  it('mantém o toast global para requisições sem tratamento local', async () => {
    const feedback = useFeedbackStore()
    await expect(api.get('/api/outro-modulo', { adapter: adapterComFalha })).rejects.toMatchObject(
      { response: { status: 403 } },
    )

    expect(feedback.toasts).toHaveLength(1)
    expect(feedback.toasts[0]).toMatchObject({
      type: 'error',
      title: 'Acesso negado',
      message: 'Esta consulta não está disponível.',
    })
  })
})
