package com.quipux.cadastro.nacionalidade;

import com.quipux.cadastro.exception.ServicoExternoException;
import com.quipux.cadastro.nacionalidade.dto.NationalizeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente HTTP da API publica de previsao de nacionalidade. */
@Component
public class NationalizeClient {

    private static final Logger log = LoggerFactory.getLogger(NationalizeClient.class);

    private final RestClient restClient;

    public NationalizeClient(RestClient nationalizeRestClient) {
        this.restClient = nationalizeRestClient;
    }

    public NationalizeResponse preverPorNome(String nome) {
        try {
            NationalizeResponse resposta = restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("name", nome).build())
                    .retrieve()
                    .body(NationalizeResponse.class);

            if (resposta == null) {
                throw new ServicoExternoException(
                        "A API de nacionalidade retornou uma resposta vazia", null);
            }
            return resposta;
        } catch (RestClientException ex) {
            log.warn("Erro ao consultar a API de nacionalidade para o nome '{}'", nome, ex);
            throw new ServicoExternoException(
                    "Nao foi possivel consultar a API de previsao de nacionalidade", ex);
        }
    }
}
