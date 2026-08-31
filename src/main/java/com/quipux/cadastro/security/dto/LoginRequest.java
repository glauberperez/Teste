package com.quipux.cadastro.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "usuario e obrigatorio")
        @Size(max = 60, message = "usuario deve ter no maximo 60 caracteres")
        String usuario,

        @NotBlank(message = "senha e obrigatoria")
        @Size(min = 4, max = 100, message = "senha deve ter entre 4 e 100 caracteres")
        String senha) {
}
