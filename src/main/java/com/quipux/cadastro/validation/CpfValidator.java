package com.quipux.cadastro.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        // @NotBlank e responsavel por rejeitar nulo/vazio; aqui so validamos o formato.
        if (valor == null || valor.isBlank()) {
            return true;
        }
        return isCpfValido(valor);
    }

    public static boolean isCpfValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) {
            return false;
        }
        // Sequencias repetidas (00000000000, 11111111111, ...) passam no modulo 11,
        // mas nao sao CPFs validos.
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        int primeiroDigito = calcularDigito(cpf, 9);
        int segundoDigito = calcularDigito(cpf, 10);
        return primeiroDigito == Character.getNumericValue(cpf.charAt(9))
                && segundoDigito == Character.getNumericValue(cpf.charAt(10));
    }

    private static int calcularDigito(String cpf, int tamanho) {
        int soma = 0;
        int peso = tamanho + 1;
        for (int i = 0; i < tamanho; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * peso--;
        }
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }
}
