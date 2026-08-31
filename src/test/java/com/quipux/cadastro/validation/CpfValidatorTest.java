package com.quipux.cadastro.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CpfValidatorTest {
    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "11144477735", "39053344705"})
    @DisplayName("aceita CPFs com digitos verificadores corretos")
    void aceitaCpfValido(String cpf) {
        assertThat(CpfValidator.isCpfValido(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "52998224726",
            "1114447773",
            "111444777351",
            "529.982.247-25",
            "abcdefghijk",
            "11111111111",
            "00000000000"
    })
    @DisplayName("rejeita CPFs invalidos")
    void rejeitaCpfInvalido(String cpf) {
        assertThat(CpfValidator.isCpfValido(cpf)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("rejeita nulo e vazio na checagem direta")
    void rejeitaNuloEVazio(String cpf) {
        assertThat(CpfValidator.isCpfValido(cpf)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("delega nulo/vazio para @NotBlank, entao a constraint nao acusa erro")
    void constraintIgnoraNuloEVazio(String cpf) {
        assertThat(new CpfValidator().isValid(cpf, null)).isTrue();
    }
}
