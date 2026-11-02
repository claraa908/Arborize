package br.com.arborize.dtos.response;

/** Conteúdo para a futura resposta administrativa autenticada. */
public record FotoArvoreConteudoDTO(FotoArvoreResponseDTO metadados, byte[] conteudo) {
    public FotoArvoreConteudoDTO {
        conteudo = conteudo.clone();
    }
    @Override
    public byte[] conteudo() {
        return conteudo.clone();
    }
}
