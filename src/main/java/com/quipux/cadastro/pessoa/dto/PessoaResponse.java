package com.quipux.cadastro.pessoa.dto;

import com.quipux.cadastro.pessoa.Pessoa;
import java.time.LocalDateTime;

public record PessoaResponse(
        Long id,
        String documento,
        String nome,
        String sobrenome,
        String email,
        LocalDateTime criadoEm) {

    public static PessoaResponse de(Pessoa pessoa) {
        return new PessoaResponse(
                pessoa.getId(),
                pessoa.getDocumento(),
                pessoa.getNome(),
                pessoa.getSobrenome(),
                pessoa.getEmail(),
                pessoa.getCriadoEm());
    }
}
