package br.com.arborize.dtos.request;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class TermoCompensacaoRequestValidationTest {
    @Test void exigeRazaoSocialCnpjEQuantidadePositiva() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var invalid = new TermoCompensacaoRequestDTO(" ", "123", "", LocalDate.now(), null, 0);
            var fields = validator.validate(invalid).stream().map(v -> v.getPropertyPath().toString()).toList();
            assertTrue(fields.contains("razaoSocial"));
            assertTrue(fields.contains("cnpj"));
            assertTrue(fields.contains("quantidadeAcordada"));
            var valid = new TermoCompensacaoRequestDTO("Empresa Exemplo", "12345678000190", null, null, null, 1);
            assertTrue(validator.validate(valid).isEmpty());
        }
    }
}
