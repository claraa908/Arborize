package br.com.arborize.dtos.response;

import br.com.arborize.enums.UsuarioRole;

import java.time.Instant;

public record AdminUsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        UsuarioRole role,
        Instant dataCadastro,
        String status
) {
}
