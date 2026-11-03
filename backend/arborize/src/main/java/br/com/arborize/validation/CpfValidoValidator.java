package br.com.arborize.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidoValidator implements ConstraintValidator<CpfValido, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || !value.matches("\\d{11}") || value.matches("(\\d)\\1{10}")) return false;
        return calculateDigit(value, 9) == Character.digit(value.charAt(9), 10)
                && calculateDigit(value, 10) == Character.digit(value.charAt(10), 10);
    }

    private int calculateDigit(String cpf, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.digit(cpf.charAt(index), 10) * (length + 1 - index);
        }
        int remainder = (sum * 10) % 11;
        return remainder == 10 ? 0 : remainder;
    }
}
