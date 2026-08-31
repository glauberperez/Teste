package com.quipux.cadastro.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centraliza a traducao de excecoes em respostas HTTP, para que o cliente
 * sempre receba um corpo JSON com o mesmo formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Falhas de @Valid no corpo da requisicao (POST /registrarName). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        ErroResponse corpo = new ErroResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Requisicao invalida",
                "Um ou mais campos estao invalidos",
                request.getRequestURI(),
                campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    /** Falhas de validacao em parametros de rota/query (@Validated no controller). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> campos = new LinkedHashMap<>();
        for (ConstraintViolation<?> violacao : ex.getConstraintViolations()) {
            String caminho = violacao.getPropertyPath().toString();
            String campo = caminho.contains(".") ? caminho.substring(caminho.lastIndexOf('.') + 1) : caminho;
            campos.putIfAbsent(campo, violacao.getMessage());
        }
        ErroResponse corpo = new ErroResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Parametro invalido",
                "Um ou mais parametros estao invalidos",
                request.getRequestURI(),
                campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonIlegivel(HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ErroResponse.de(
                HttpStatus.BAD_REQUEST.value(),
                "Requisicao invalida",
                "Corpo da requisicao ausente ou em JSON malformado",
                request.getRequestURI()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErroResponse.de(
                HttpStatus.NOT_FOUND.value(), "Nao encontrado", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErroResponse.de(
                HttpStatus.CONFLICT.value(), "Conflito", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ServicoExternoException.class)
    public ResponseEntity<ErroResponse> tratarServicoExterno(
            ServicoExternoException ex, HttpServletRequest request) {

        log.warn("Falha ao consumir servico externo: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErroResponse.de(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Servico externo indisponivel",
                ex.getMessage(),
                request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado ao processar {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErroResponse.de(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno",
                "Ocorreu um erro inesperado ao processar a requisicao",
                request.getRequestURI()));
    }
}
