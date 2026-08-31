package com.quipux.cadastro.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credenciais do usuario da aplicacao (app.auth.* no application.yml). */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(String usuario, String senha) {
}
