package br.com.arborize.validation;

import br.com.arborize.dtos.request.DadosArvoreRequest;
import br.com.arborize.enums.ClassificacaoTerritorial;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TerritorioConsistenteValidator implements ConstraintValidator<TerritorioConsistente, DadosArvoreRequest> {
    @Override
    public boolean isValid(DadosArvoreRequest dados, ConstraintValidatorContext context) {
        if (dados == null) {
            return true;
        }
        if (dados.zonaRural() != null) {
            return validarNovoContrato(dados, context);
        }
        if (dados.classificacaoTerritorial() == null) {
            return violar(context, "classificacaoTerritorial", "Informe a classificação territorial legada.");
        }
        if (dados.localidadeRural() != null) {
            return violar(context, "localidadeRural", "Use localidade rural somente no novo fluxo territorial.");
        }
        boolean identificado = dados.classificacaoTerritorial() == ClassificacaoTerritorial.BAIRRO_IDENTIFICADO;
        boolean nomeInformado = dados.bairroNome() != null && !dados.bairroNome().isBlank();
        boolean referenciaInformada = dados.bairroId() != null || nomeInformado;
        if (identificado == referenciaInformada) {
            return true;
        }
        String campo = identificado || nomeInformado ? "bairroNome" : "bairroId";
        return violar(context, campo, identificado
                ? "Bairro identificado exige um nome confirmado ou uma referência territorial legada."
                : "Informe bairro somente na situação de bairro identificado.");
    }

    private boolean validarNovoContrato(DadosArvoreRequest dados, ConstraintValidatorContext context) {
        if (dados.zonaRural()) {
            if (dados.localidadeRural() == null || dados.localidadeRural().isBlank()) {
                return violar(context, "localidadeRural", "Informe a localidade rural.");
            }
            if (dados.bairroId() != null || dados.bairroNome() != null) {
                return violar(context, "bairroId", "Uma árvore rural não pode ter bairro urbano associado.");
            }
            if (dados.classificacaoTerritorial() != null
                    && dados.classificacaoTerritorial() != ClassificacaoTerritorial.FORA_DA_AREA_URBANA) {
                return violar(context, "classificacaoTerritorial", "A classificação deve corresponder à zona rural.");
            }
            return true;
        }
        if (dados.bairroId() == null) {
            return violar(context, "bairroId", "Selecione um bairro ativo para a árvore urbana.");
        }
        if (dados.bairroNome() != null || dados.localidadeRural() != null) {
            return violar(context, "bairroNome", "O bairro urbano deve ser selecionado pelo catálogo.");
        }
        if (dados.classificacaoTerritorial() != null
                && dados.classificacaoTerritorial() != ClassificacaoTerritorial.BAIRRO_IDENTIFICADO) {
            return violar(context, "classificacaoTerritorial", "A classificação deve corresponder à área urbana.");
        }
        return true;
    }

    private boolean violar(ConstraintValidatorContext context, String campo, String mensagem) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(mensagem)
                .addPropertyNode(campo)
                .addConstraintViolation();
        return false;
    }
}
