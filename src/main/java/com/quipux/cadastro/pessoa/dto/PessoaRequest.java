package com.quipux.cadastro.pessoa.dto;

import com.quipux.cadastro.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PessoaRequest(

        @NotBlank(message = "documento e obrigatorio")
        @Cpf
        String documento,

        @NotBlank(message = "nome e obrigatorio")
        @Size(min = 2, max = 60, message = "nome deve ter entre 2 e 60 caracteres")
        @Pattern(regexp = "^[\\p{L} '-]+$", message = "nome deve conter apenas letras")
        String nome,

        @NotBlank(message = "sobrenome e obrigatorio")
        @Size(min = 2, max = 60, message = "sobrenome deve ter entre 2 e 60 caracteres")
        @Pattern(regexp = "^[\\p{L} '-]+$", message = "sobrenome deve conter apenas letras")
        String sobrenome,

        @NotBlank(message = "email e obrigatorio")
        @Email(message = "email deve ser um endereco valido")
        @Size(max = 120, message = "email deve ter no maximo 120 caracteres")
        String email) {
}
