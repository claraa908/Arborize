package br.com.arborize.dtos.response;

import java.time.LocalDateTime;

/** Não expõe chave MinIO, bucket ou URL pública. */
public record FotoArvoreResponseDTO(Long id, Long arvoreId, String contentType, long sizeBytes, LocalDateTime criadoEm) { }
