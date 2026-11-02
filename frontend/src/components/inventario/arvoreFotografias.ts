export const LIMITE_FOTOGRAFIAS_ARVORE = 3
export const LIMITE_BYTES_FOTOGRAFIA = 2 * 1024 * 1024

const EXTENSOES_POR_TIPO: Record<string, string[]> = {
  'image/jpeg': ['jpg', 'jpeg'],
  'image/png': ['png'],
  'image/webp': ['webp'],
}

export function validarArquivoFotografia(
  arquivo: Pick<File, 'name' | 'type' | 'size'>,
): string | null {
  const extensao = arquivo.name.split('.').pop()?.toLowerCase() ?? ''
  if (!EXTENSOES_POR_TIPO[arquivo.type]?.includes(extensao)) {
    return `${arquivo.name}: use uma imagem JPG, PNG ou WebP válida.`
  }
  if (arquivo.size <= 0) return `${arquivo.name}: o arquivo está vazio.`
  if (arquivo.size > LIMITE_BYTES_FOTOGRAFIA) {
    return `${arquivo.name}: o limite é 2 MB por fotografia.`
  }
  return null
}

export function limiteFotografiasExcedido(quantidadeExistenteEPendente: number): boolean {
  return quantidadeExistenteEPendente >= LIMITE_FOTOGRAFIAS_ARVORE
}
