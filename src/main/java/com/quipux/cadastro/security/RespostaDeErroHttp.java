package com.quipux.cadastro.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quipux.cadastro.exception.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

/** Escreve as respostas 401/403 no mesmo formato JSON dos demais erros da API. */
final class RespostaDeErroHttp {

    private RespostaDeErroHttp() {
    }

    static void escrever(ObjectMapper objectMapper, HttpServletRequest request,
            HttpServletResponse response, int status, String erro, String mensagem) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
                response.getWriter(),
                ErroResponse.de(status, erro, mensagem, request.getRequestURI()));
    }
}
