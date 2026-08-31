package com.quipux.cadastro.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Geracao e validacao dos tokens JWT (assinatura HMAC-SHA256). */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey chave;
    private final Duration expiracao;

    public JwtService(JwtProperties propriedades) {
        this.chave = Keys.hmacShaKeyFor(propriedades.secret().getBytes(StandardCharsets.UTF_8));
        this.expiracao = propriedades.expiracao();
    }

    public String gerarToken(String usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario)
                .issuer("cadastro-pessoas")
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracao)))
                .signWith(chave)
                .compact();
    }

    /** Devolve o usuario do token, ou {@code null} se o token for invalido/expirado. */
    public String extrairUsuario(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (ExpiredJwtException ex) {
            log.debug("Token expirado: {}", ex.getMessage());
            return null;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token invalido: {}", ex.getMessage());
            return null;
        }
    }

    public long segundosDeValidade() {
        return expiracao.toSeconds();
    }
}
