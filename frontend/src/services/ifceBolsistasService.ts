import { api } from '@/services/api'

export interface IfceBolsista {
  id: number
  nome: string
  email: string
  whatsapp: string
}

export interface CreateIfceBolsistaRequest {
  nome: string
  cpf: string
  email: string
  whatsapp: string
  senha: string
}

export async function listarBolsistasIfce() {
  const response = await api.get<IfceBolsista[]>('/ifce/bolsistas')
  return response.data
}

export async function cadastrarBolsistaIfce(payload: CreateIfceBolsistaRequest) {
  const response = await api.post<IfceBolsista>('/ifce/bolsistas', payload)
  return response.data
}
