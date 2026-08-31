package com.quipux.cadastro.security.dto;

public record LoginResponse(
        String token,
        String tipo,
        long expiraEmSegundos) {
    public static LoginResponse de(String token, long expiraEmSegundos) {
        return new LoginResponse(token, "Bearer", expiraEmSegundos);
    }
}
