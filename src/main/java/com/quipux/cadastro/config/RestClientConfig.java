package com.quipux.cadastro.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NationalizeProperties.class)
public class RestClientConfig {
    @Bean
    public RestClient nationalizeRestClient(NationalizeProperties propriedades) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(propriedades.timeoutConexao());
        fabrica.setReadTimeout(propriedades.timeoutLeitura());

        return RestClient.builder()
                .baseUrl(propriedades.baseUrl())
                .requestFactory(fabrica)
                .build();
    }
}
