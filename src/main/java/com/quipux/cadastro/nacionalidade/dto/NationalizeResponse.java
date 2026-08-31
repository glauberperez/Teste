package com.quipux.cadastro.nacionalidade.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Resposta crua da API publica https://api.nationalize.io. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NationalizeResponse(
        String name,
        Integer count,
        List<Pais> country) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pais(
            @JsonProperty("country_id") String countryId,
            Double probability) {
    }

    public List<Pais> paisesOuVazio() {
        return country == null ? List.of() : country;
    }
}
