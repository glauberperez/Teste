package com.quipux.cadastro.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Propriedades de app.jwt.* no application.yml. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiracao) {
}
