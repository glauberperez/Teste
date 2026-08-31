package com.quipux.cadastro.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.nationalize")
public record NationalizeProperties(
        String baseUrl,
        Duration timeoutConexao,
        Duration timeoutLeitura) {
}
