export type CompensacaoStatus = 'EM_ABERTO' | 'PARCIALMENTE_DOADO' | 'TOTALMENTE_DOADO'

export interface CompensacaoEntrega {
  id: number
  quantidade: number
  arquivoNome: string
  registradaEm: string
}

export interface TermoCompensacao {
  id: number
  razaoSocial: string
  cnpj: string
  numeroTermo: string
  dataAssinatura: string
  prazoEntrega: string
  quantidadeAcordada: number
  quantidadeEntregue: number
  status: CompensacaoStatus
  arquivoTermo: string
  entregas: CompensacaoEntrega[]
}

export const statusLabel: Record<CompensacaoStatus, string> = {
  EM_ABERTO: 'Em aberto',
  PARCIALMENTE_DOADO: 'Parcialmente doado',
  TOTALMENTE_DOADO: 'Totalmente doado/Fechado',
}

export function calcularStatusCompensacao(entregue: number, acordado: number): CompensacaoStatus {
  if (entregue <= 0) return 'EM_ABERTO'
  if (entregue < acordado) return 'PARCIALMENTE_DOADO'
  return 'TOTALMENTE_DOADO'
}

export const termosDemonstracao: TermoCompensacao[] = [
  {
    id: 1,
    razaoSocial: 'Construtora Vale Verde Ltda. — Demonstração',
    cnpj: '12.345.678/0001-90',
    numeroTermo: 'TC-DEMO-004',
    dataAssinatura: '2026-04-12',
    prazoEntrega: '2026-12-20',
    quantidadeAcordada: 500,
    quantidadeEntregue: 120,
    status: 'PARCIALMENTE_DOADO',
    arquivoTermo: 'termo-demo-004.pdf',
    entregas: [
      { id: 1, quantidade: 80, arquivoNome: 'recibo-demo-001.pdf', registradaEm: '2026-05-20' },
      { id: 2, quantidade: 40, arquivoNome: 'recibo-demo-002.jpg', registradaEm: '2026-07-18' },
    ],
  },
  {
    id: 2,
    razaoSocial: 'Mercado Central Ltda. — Demonstração',
    cnpj: '98.765.432/0001-10',
    numeroTermo: 'TC-DEMO-007',
    dataAssinatura: '2026-02-03',
    prazoEntrega: '2026-10-30',
    quantidadeAcordada: 300,
    quantidadeEntregue: 300,
    status: 'TOTALMENTE_DOADO',
    arquivoTermo: 'termo-demo-007.pdf',
    entregas: [
      { id: 3, quantidade: 300, arquivoNome: 'recibo-demo-003.pdf', registradaEm: '2026-09-01' },
    ],
  },
  {
    id: 3,
    razaoSocial: 'Residencial Monólitos SPE — Demonstração',
    cnpj: '45.678.901/0001-23',
    numeroTermo: 'TC-DEMO-002',
    dataAssinatura: '2026-08-07',
    prazoEntrega: '2027-02-28',
    quantidadeAcordada: 400,
    quantidadeEntregue: 0,
    status: 'EM_ABERTO',
    arquivoTermo: 'termo-demo-002.pdf',
    entregas: [],
  },
]
