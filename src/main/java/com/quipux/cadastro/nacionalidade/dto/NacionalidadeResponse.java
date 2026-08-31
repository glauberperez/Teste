package com.quipux.cadastro.nacionalidade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NacionalidadeResponse(
        String documento,
        String nome,
        Nacionalidade nacionalidadeProvavel,
        List<Nacionalidade> outrasPossibilidades,
        String mensagem) {
    public record Nacionalidade(
            String codigoIso,
            String pais,
            Double probabilidade) {
    }

    public static NacionalidadeResponse semPrevisao(String documento, String nome) {
        return new NacionalidadeResponse(documento, nome, null, List.of(),
                "A API publica nao retornou previsao de nacionalidade para o nome " + nome);
    }
}
