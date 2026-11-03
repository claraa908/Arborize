package br.com.arborize.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TerritorioConsistenteValidator.class)
public @interface TerritorioConsistente {
    String message() default "A referência de bairro deve ser coerente com a classificação territorial.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
