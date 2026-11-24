package br.com.arborize.enums;

import java.util.Arrays;

public enum UsuarioRole {
    CIDADAO,
    ADMINISTRADOR_AMMA,
    FUNCIONARIO_AMMA,
    GESTOR_IFCE,
    BOLSISTA_IFCE;

    public String getAuthority() {
        return "ROLE_" + name();
    }

    public static UsuarioRole fromAuthority(String authority) {
        return Arrays.stream(values())
                .filter(role -> role.getAuthority().equals(authority))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Role inválida no token."));
    }
}
