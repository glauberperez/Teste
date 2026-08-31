package com.quipux.cadastro.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que o valor e um CPF estruturalmente valido: 11 digitos e digitos
 * verificadores conferindo (modulo 11).
 */
@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cpf {

    String message() default "documento invalido: informe um CPF valido (11 digitos, sem pontuacao)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
